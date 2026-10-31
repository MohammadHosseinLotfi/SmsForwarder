package com.example.smsforwarder.rules

enum class DestinationType {
    SMS,
    URL
}

data class Rule(
    val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val sender: String,
    val containsText: String? = null,
    val destinationType: DestinationType,
    val destinationAddress: String,
    val outgoingSlotIndex: Int = 0, // 0 برای سیم 1، 1 برای سیم 2
    val smsTemplate: String = "{{content}}",
    val jsonFields: Map<String, String> = emptyMap()
)