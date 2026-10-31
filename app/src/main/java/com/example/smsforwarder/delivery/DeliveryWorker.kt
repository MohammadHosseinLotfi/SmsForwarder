package com.example.smsforwarder.delivery

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smsforwarder.data.AppDatabase
import com.example.smsforwarder.data.DeliveryState
import com.example.smsforwarder.rules.DestinationType
import com.example.smsforwarder.sms.SmsSender

class DeliveryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val deliveryId = inputData.getLong("deliveryId", -1L)
        if (deliveryId == -1L) return Result.failure()

        val db = AppDatabase.getDatabase(applicationContext)
        val deliveryDao = db.deliveryDao()
        val delivery = deliveryDao.getDeliveryById(deliveryId) ?: return Result.failure()

        if (delivery.state == DeliveryState.SUCCEEDED || delivery.state == DeliveryState.CANCELLED) {
            return Result.success()
        }

        return when (delivery.destinationType) {
            DestinationType.URL -> {
                val sendResult = HttpSender.sendPost(delivery.destinationAddress, delivery.renderedPayload)

                when (sendResult) {
                    is HttpSender.Result.Success -> {
                        deliveryDao.updateDeliveryStatus(
                            deliveryId,
                            DeliveryState.SUCCEEDED,
                            System.currentTimeMillis()
                        )
                        Result.success()
                    }
                    is HttpSender.Result.Failure -> {
                        val nextState = if (sendResult.isRetryable && runAttemptCount < 8) {
                            DeliveryState.RETRY_WAIT
                        } else {
                            DeliveryState.NEEDS_REVIEW
                        }

                        deliveryDao.updateDeliveryStatus(
                            deliveryId,
                            nextState,
                            System.currentTimeMillis(),
                            sendResult.message
                        )

                        if (sendResult.isRetryable && runAttemptCount < 8) {
                            Result.retry()
                        } else {
                            Result.failure()
                        }
                    }
                }
            }
            DestinationType.SMS -> {
                val smsResult = SmsSender.sendSms(
                    applicationContext,
                    delivery.destinationAddress,
                    delivery.renderedPayload,
                    delivery.outgoingSlotIndex
                )

                when (smsResult) {
                    is SmsSender.SmsResult.Success -> {
                        deliveryDao.updateDeliveryStatus(
                            deliveryId,
                            DeliveryState.SUCCEEDED,
                            System.currentTimeMillis()
                        )
                        Result.success()
                    }
                    is SmsSender.SmsResult.Failure -> {
                        val nextState = if (smsResult.isBlocked) {
                            DeliveryState.BLOCKED
                        } else if (runAttemptCount < 8) {
                            DeliveryState.RETRY_WAIT
                        } else {
                            DeliveryState.NEEDS_REVIEW
                        }

                        deliveryDao.updateDeliveryStatus(
                            deliveryId,
                            nextState,
                            System.currentTimeMillis(),
                            smsResult.message
                        )

                        if (!smsResult.isBlocked && runAttemptCount < 8) {
                            Result.retry()
                        } else {
                            Result.failure()
                        }
                    }
                }
            }
        }
    }
}