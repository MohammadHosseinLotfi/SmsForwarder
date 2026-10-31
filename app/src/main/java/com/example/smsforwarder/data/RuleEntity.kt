package com.example.smsforwarder.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.smsforwarder.rules.DestinationType

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val sender: String,
    val containsText: String? = null,
    val destinationType: DestinationType,
    val destinationAddress: String,
    val outgoingSlotIndex: Int = 0,
    val smsTemplate: String = "{{content}}",
    val jsonFieldsJson: String = "{}" // ذخیره فیلدهای JSON به صورت رشته سریال‌شده
)