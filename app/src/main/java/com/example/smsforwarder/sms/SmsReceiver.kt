package com.example.smsforwarder.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.*
import com.example.smsforwarder.data.AppDatabase
import com.example.smsforwarder.data.DeliveryEntity
import com.example.smsforwarder.data.DeliveryState
import com.example.smsforwarder.delivery.DeliveryWorker
import com.example.smsforwarder.delivery.HttpSender
import com.example.smsforwarder.rules.DestinationType
import com.example.smsforwarder.rules.Rule
import com.example.smsforwarder.rules.RuleMatcher
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest

class SmsReceiver : BroadcastReceiver() {

    private val gson = Gson()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val sender = messages[0].displayOriginatingAddress ?: "Unknown"
            val fullBody = buildString {
                for (sms in messages) {
                    sms.displayMessageBody?.let { append(it) }
                }
            }

            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val ruleDao = db.ruleDao()
                    val deliveryDao = db.deliveryDao()

                    val activeRules = ruleDao.getActiveRules()
                    if (activeRules.isEmpty()) {
                        Log.d("SmsReceiver", "هیچ قانون فعالی برای بررسی یافت نشد")
                        return@launch
                    }

                    for (ruleEntity in activeRules) {
                        val mapType = TypeToken.getParameterized(Map::class.java, String::class.java, String::class.java).type
                        val jsonFields: Map<String, String> = try {
                            gson.fromJson(ruleEntity.jsonFieldsJson, mapType) ?: emptyMap()
                        } catch (_: Exception) {
                            emptyMap()
                        }

                        val rule = Rule(
                            id = ruleEntity.id,
                            name = ruleEntity.name,
                            enabled = ruleEntity.enabled,
                            sender = ruleEntity.sender,
                            containsText = ruleEntity.containsText,
                            destinationType = ruleEntity.destinationType,
                            destinationAddress = ruleEntity.destinationAddress,
                            outgoingSlotIndex = ruleEntity.outgoingSlotIndex,
                            smsTemplate = ruleEntity.smsTemplate,
                            jsonFields = jsonFields
                        )

                        if (RuleMatcher.matches(rule, sender, fullBody)) {
                            val renderedBody = RuleMatcher.renderOutput(rule, sender, fullBody)
                            val fingerprint = generateFingerprint(sender, fullBody, messages[0].timestampMillis)

                            val delivery = DeliveryEntity(
                                ruleId = rule.id,
                                ruleNameSnapshot = rule.name,
                                eventFingerprint = fingerprint,
                                senderRaw = sender,
                                destinationType = rule.destinationType,
                                destinationAddress = rule.destinationAddress,
                                renderedPayload = renderedBody,
                                outgoingSlotIndex = rule.outgoingSlotIndex,
                                state = DeliveryState.IN_FLIGHT
                            )

                            val insertedId = deliveryDao.insertDelivery(delivery)
                            if (insertedId == -1L) {
                                Log.w("SmsReceiver", "رویداد تکراری (fingerprint + ruleId) نادیده گرفته شد")
                                continue
                            }

                            when (rule.destinationType) {
                                DestinationType.URL -> {
                                    val quickResult = HttpSender.sendPost(rule.destinationAddress, renderedBody)
                                    if (quickResult is HttpSender.Result.Success) {
                                        Log.d("SmsReceiver", "ارسال سریع HTTP با موفقیت انجام شد (کد ${quickResult.code})")
                                        deliveryDao.updateDeliveryStatus(insertedId, DeliveryState.SUCCEEDED, System.currentTimeMillis())
                                    } else {
                                        Log.w("SmsReceiver", "ارسال سریع HTTP ناموفق بود؛ ثبت در صف WorkManager")
                                        val failureMsg = (quickResult as? HttpSender.Result.Failure)?.message
                                        deliveryDao.updateDeliveryStatus(
                                            insertedId,
                                            DeliveryState.WAITING_NETWORK,
                                            System.currentTimeMillis(),
                                            failureMsg
                                        )
                                        schedulePersistentWork(context, insertedId, requiresNetwork = true)
                                    }
                                }
                                DestinationType.SMS -> {
                                    val smsResult = SmsSender.sendSms(
                                        context,
                                        rule.destinationAddress,
                                        renderedBody,
                                        rule.outgoingSlotIndex
                                    )
                                    when (smsResult) {
                                        is SmsSender.SmsResult.Success -> {
                                            Log.d("SmsReceiver", "ارسال سریع پیامک با موفقیت انجام شد")
                                            deliveryDao.updateDeliveryStatus(insertedId, DeliveryState.SUCCEEDED, System.currentTimeMillis())
                                        }
                                        is SmsSender.SmsResult.Failure -> {
                                            Log.w("SmsReceiver", "ارسال پیامک ناموفق بود: ${smsResult.message}")
                                            val targetState = if (smsResult.isBlocked) DeliveryState.BLOCKED else DeliveryState.RETRY_WAIT
                                            deliveryDao.updateDeliveryStatus(
                                                insertedId,
                                                targetState,
                                                System.currentTimeMillis(),
                                                smsResult.message
                                            )
                                            if (!smsResult.isBlocked) {
                                                schedulePersistentWork(context, insertedId, requiresNetwork = false)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "خطا در پردازش پیامک‌های دریافتی", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun schedulePersistentWork(context: Context, deliveryId: Long, requiresNetwork: Boolean) {
        val constraintsBuilder = Constraints.Builder()
        if (requiresNetwork) {
            constraintsBuilder.setRequiredNetworkType(NetworkType.CONNECTED)
        }

        val workRequest = OneTimeWorkRequestBuilder<DeliveryWorker>()
            .setConstraints(constraintsBuilder.build())
            .setInputData(workDataOf("deliveryId" to deliveryId))
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    private fun generateFingerprint(sender: String, body: String, timestamp: Long): String {
        val raw = "$sender-$body-$timestamp"
        val bytes = MessageDigest.getInstance("MD5").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}