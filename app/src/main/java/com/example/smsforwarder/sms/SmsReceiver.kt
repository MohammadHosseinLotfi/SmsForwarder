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
import com.example.smsforwarder.data.ServicePreferences
import com.example.smsforwarder.data.getDestinations
import com.example.smsforwarder.delivery.DeliveryWorker
import com.example.smsforwarder.delivery.HttpSender
import com.example.smsforwarder.rules.DestinationType
import com.example.smsforwarder.rules.Rule
import com.example.smsforwarder.rules.RuleMatcher
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class SmsReceiver : BroadcastReceiver() {

    private val gson = Gson()

    override fun onReceive(context: Context, intent: Intent) {
        if (!ServicePreferences.isServiceEnabled(context)) {
            Log.d("SmsReceiver", "سرویس انتقال پیامک غیرفعال است؛ پیامک نادیده گرفته شد.")
            return
        }

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
                        val destinations = ruleEntity.getDestinations(gson)
                        val rule = Rule(
                            id = ruleEntity.id,
                            name = ruleEntity.name,
                            enabled = ruleEntity.enabled,
                            sender = ruleEntity.sender,
                            containsText = ruleEntity.containsText,
                            destinations = destinations
                        )

                        if (RuleMatcher.matches(rule, sender, fullBody)) {
                            for ((destIndex, destination) in rule.destinations.withIndex()) {
                                if (!destination.enabled || destination.destinationAddress.isBlank()) continue

                                val renderedBody = RuleMatcher.renderOutput(destination, sender, fullBody)
                                val fingerprintKey = "${destination.destinationType}_${destination.destinationAddress}_$destIndex"
                                val fingerprint = generateFingerprint(sender, fullBody, messages[0].timestampMillis, fingerprintKey)

                                val delivery = DeliveryEntity(
                                    ruleId = rule.id,
                                    ruleNameSnapshot = rule.name,
                                    eventFingerprint = fingerprint,
                                    senderRaw = sender,
                                    contentRaw = fullBody,
                                    destinationType = destination.destinationType,
                                    destinationAddress = destination.destinationAddress,
                                    renderedPayload = renderedBody,
                                    outgoingSlotIndex = destination.outgoingSlotIndex,
                                    state = DeliveryState.IN_FLIGHT
                                )

                                val insertedId = deliveryDao.insertDelivery(delivery)
                                if (insertedId == -1L) {
                                    Log.w("SmsReceiver", "رویداد تکراری (fingerprint + ruleId) نادیده گرفته شد")
                                    continue
                                }

                                when (destination.destinationType) {
                                    DestinationType.URL -> {
                                        val quickResult = HttpSender.sendPost(destination.destinationAddress, renderedBody, isQuickAttempt = true)
                                        if (quickResult is HttpSender.Result.Success) {
                                            Log.d("SmsReceiver", "ارسال سریع HTTP با موفقیت انجام شد (کد ${quickResult.code})")
                                            deliveryDao.updateDeliveryStatus(
                                                id = insertedId,
                                                state = DeliveryState.SUCCEEDED,
                                                attemptTime = System.currentTimeMillis(),
                                                httpStatusCode = quickResult.code
                                            )
                                        } else {
                                            Log.w("SmsReceiver", "ارسال سریع HTTP ناموفق بود؛ ثبت در صف WorkManager")
                                            val failureMsg = (quickResult as? HttpSender.Result.Failure)?.message
                                            deliveryDao.updateDeliveryStatus(
                                                id = insertedId,
                                                state = DeliveryState.WAITING_NETWORK,
                                                attemptTime = System.currentTimeMillis(),
                                                httpStatusCode = quickResult.code,
                                                error = failureMsg
                                            )
                                            schedulePersistentWork(context, insertedId, rule.id, requiresNetwork = true)
                                        }
                                    }
                                    DestinationType.SMS -> {
                                        val smsResult = SmsSender.sendSms(
                                            context,
                                            destination.destinationAddress,
                                            renderedBody,
                                            destination.outgoingSlotIndex
                                        )
                                        when (smsResult) {
                                            is SmsSender.SmsResult.Success -> {
                                                Log.d("SmsReceiver", "ارسال سریع پیامک با موفقیت انجام شد")
                                                deliveryDao.updateDeliveryStatus(
                                                    id = insertedId,
                                                    state = DeliveryState.SUCCEEDED,
                                                    attemptTime = System.currentTimeMillis()
                                                )
                                            }
                                            is SmsSender.SmsResult.Failure -> {
                                                Log.w("SmsReceiver", "ارسال پیامک ناموفق بود: ${smsResult.message}")
                                                val targetState = if (smsResult.isBlocked) DeliveryState.BLOCKED else DeliveryState.RETRY_WAIT
                                                deliveryDao.updateDeliveryStatus(
                                                    id = insertedId,
                                                    state = targetState,
                                                    attemptTime = System.currentTimeMillis(),
                                                    error = smsResult.message
                                                )
                                                if (!smsResult.isBlocked) {
                                                    schedulePersistentWork(context, insertedId, rule.id, requiresNetwork = false)
                                                }
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

    private fun schedulePersistentWork(context: Context, deliveryId: Long, ruleId: Long, requiresNetwork: Boolean) {
        val constraintsBuilder = Constraints.Builder()
        if (requiresNetwork) {
            constraintsBuilder.setRequiredNetworkType(NetworkType.CONNECTED)
        }

        val workRequest = OneTimeWorkRequestBuilder<DeliveryWorker>()
            .setConstraints(constraintsBuilder.build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .addTag("delivery_$deliveryId")
            .addTag("rule_$ruleId")
            .setInputData(workDataOf("deliveryId" to deliveryId))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "delivery_$deliveryId",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun generateFingerprint(sender: String, body: String, timestamp: Long, extraKey: String = ""): String {
        val raw = "$sender-$body-$timestamp-$extraKey"
        val bytes = MessageDigest.getInstance("MD5").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
