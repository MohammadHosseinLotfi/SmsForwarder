package com.example.smsforwarder.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import android.util.Log

object SmsSender {

    sealed class SmsResult {
        data object Success : SmsResult()
        data class Failure(val message: String, val isBlocked: Boolean = false) : SmsResult()
    }

    /**
     * ارسال پیامک خروجی با سیم‌کارت انتخاب‌شده (slotIndex: 0 برای سیم ۱، 1 برای سیم ۲)
     */
    fun sendSms(
        context: Context,
        destinationAddress: String,
        messageText: String,
        outgoingSlotIndex: Int
    ): SmsResult {
        // ۱. بررسی مجوزهای لازم
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return SmsResult.Failure("مجوز ارسال پیامک (SEND_SMS) اعطا نشده است", isBlocked = true)
        }

        if (destinationAddress.isBlank()) {
            return SmsResult.Failure("شماره مقصد پیامک خالی است", isBlocked = true)
        }

        return try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                ?: return SmsResult.Failure("امکان دسترسی به مدیریت سیم‌کارت وجود ندارد")

            // بررسی مجوز خواندن وضعیت سیم‌کارت
            val hasPhoneStatePermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPhoneStatePermission) {
                return SmsResult.Failure("مجوز خواندن وضعیت سیم‌کارت (READ_PHONE_STATE) اعطا نشده است", isBlocked = true)
            }

            val activeSubscriptions = subscriptionManager.activeSubscriptionInfoList
            if (activeSubscriptions.isNullOrEmpty()) {
                return SmsResult.Failure("هیچ سیم‌کارت فعالی روی دستگاه یافت نشد", isBlocked = true)
            }

            // پیدا کردن subscriptionId مربوط به سیم انتخاب‌شده (سیم ۱ = slot 0، سیم ۲ = slot 1)
            val matchingSubInfo = activeSubscriptions.find { it.simSlotIndex == outgoingSlotIndex }
                ?: return SmsResult.Failure(
                    "سیم‌کارت انتخاب‌شده (سیم ${outgoingSlotIndex + 1}) غیرفعال یا از دستگاه خارج شده است",
                    isBlocked = true
                )

            val subscriptionId = matchingSubInfo.subscriptionId

            // دریافت SmsManager مربوط به subscriptionId انتخاب‌شده (minSdk 31+)
            val systemSmsManager = context.getSystemService(SmsManager::class.java)
            val smsManager = systemSmsManager.createForSubscriptionId(subscriptionId)

            // تقسیم متن‌های بلند به پیامک‌های چندبخشی
            val parts = smsManager.divideMessage(messageText)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(
                    destinationAddress,
                    null,
                    ArrayList(parts),
                    null,
                    null
                )
            } else {
                smsManager.sendTextMessage(
                    destinationAddress,
                    null,
                    messageText,
                    null,
                    null
                )
            }

            Log.d("SmsSender", "پیامک با موفقیت تحویل رادیو شد (سیم ${outgoingSlotIndex + 1})")
            SmsResult.Success
        } catch (e: SecurityException) {
            Log.e("SmsSender", "خطای دسترسی امنیتی در ارسال پیامک", e)
            SmsResult.Failure("خطای دسترسی امنیتی: ${e.message}", isBlocked = true)
        } catch (e: Exception) {
            Log.e("SmsSender", "خطا در ارسال پیامک", e)
            SmsResult.Failure("خطا در ارسال پیامک: ${e.message}")
        }
    }
}