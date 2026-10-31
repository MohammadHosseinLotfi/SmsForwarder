package com.example.smsforwarder.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.smsforwarder.rules.DestinationType

@Entity(
    tableName = "deliveries",
    indices = [
        // قید یکتایی طبق سند برای جلوگیری از تکرار ارسال همان رویداد و قانون
        Index(value = ["eventFingerprint", "ruleId"], unique = true)
    ]
)
data class DeliveryEntity(
    @PrimaryKey(autoGenerate = true)
    val deliveryId: Long = 0,
    val ruleId: Long,
    val ruleNameSnapshot: String,
    val eventFingerprint: String,
    val senderRaw: String,
    val receivedAt: Long = System.currentTimeMillis(),
    val destinationType: DestinationType,
    val destinationAddress: String,
    val renderedPayload: String, // بدنه نهایی آماده برای ارسال
    val outgoingSlotIndex: Int,
    val state: DeliveryState = DeliveryState.QUEUED,
    val attemptCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val errorMessage: String? = null
)

private val DeliveryEntity.httpCode: Int? get() = httpStatus
private val DeliveryEntity.sentAtMillis: Long get() = lastAttemptAt
private val DeliveryEntity.smsText: String get() = contentRaw