package com.example.smsforwarder.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.smsforwarder.rules.DestinationType

@Immutable
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
    val contentRaw: String = "",
    val receivedAt: Long = System.currentTimeMillis(),
    val destinationType: DestinationType,
    val destinationAddress: String,
    val renderedPayload: String, // بدنه نهایی آماده برای ارسال
    val outgoingSlotIndex: Int,
    val state: DeliveryState = DeliveryState.QUEUED,
    val attemptCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val httpStatusCode: Int? = null,
    val errorMessage: String? = null
)
