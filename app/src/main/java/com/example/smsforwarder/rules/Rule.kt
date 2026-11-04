package com.example.smsforwarder.rules

import androidx.compose.runtime.Immutable
import java.util.UUID

enum class DestinationType {
    SMS,
    URL
}

@Immutable
data class RuleDestination(
    val id: String = UUID.randomUUID().toString(),
    val enabled: Boolean = true,
    val destinationType: DestinationType = DestinationType.URL,
    val destinationAddress: String = "",
    val outgoingSlotIndex: Int = 0, // 0 برای سیم 1، 1 برای سیم 2
    val smsTemplate: String = "{{content}}",
    val jsonFields: Map<String, String> = mapOf("sender" to "{{sender}}", "content" to "{{content}}")
)

@Immutable
data class Rule(
    val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val sender: String,
    val containsText: String? = null,
    val destinations: List<RuleDestination> = emptyList()
) {
    val primaryDestination: RuleDestination?
        get() = destinations.firstOrNull()

    val destinationType: DestinationType
        get() = primaryDestination?.destinationType ?: DestinationType.URL

    val destinationAddress: String
        get() = primaryDestination?.destinationAddress ?: ""

    val outgoingSlotIndex: Int
        get() = primaryDestination?.outgoingSlotIndex ?: 0

    val smsTemplate: String
        get() = primaryDestination?.smsTemplate ?: "{{content}}"

    val jsonFields: Map<String, String>
        get() = primaryDestination?.jsonFields ?: emptyMap()
}
