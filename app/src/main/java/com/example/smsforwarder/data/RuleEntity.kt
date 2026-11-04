package com.example.smsforwarder.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.smsforwarder.rules.DestinationType
import com.example.smsforwarder.rules.RuleDestination
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Immutable
@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val sender: String,
    val containsText: String? = null,
    val destinationType: DestinationType = DestinationType.URL,
    val destinationAddress: String = "",
    val outgoingSlotIndex: Int = 0,
    val smsTemplate: String = "{{content}}",
    val jsonFieldsJson: String = "{}",
    val destinationsJson: String = "[]"
)

fun RuleEntity.getDestinations(gson: Gson): List<RuleDestination> {
    if (destinationsJson.isNotBlank() && destinationsJson != "[]") {
        try {
            val type = object : TypeToken<List<RuleDestination>>() {}.type
            val list = gson.fromJson<List<RuleDestination>>(destinationsJson, type)
            if (!list.isNullOrEmpty()) {
                return list
            }
        } catch (_: Exception) {
        }
    }

    if (destinationAddress.isNotBlank()) {
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        val jsonMap: Map<String, String> = try {
            gson.fromJson(jsonFieldsJson, mapType) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }

        return listOf(
            RuleDestination(
                id = "legacy_$id",
                destinationType = destinationType,
                destinationAddress = destinationAddress,
                outgoingSlotIndex = outgoingSlotIndex,
                smsTemplate = smsTemplate,
                jsonFields = if (jsonMap.isNotEmpty()) jsonMap else mapOf("sender" to "{{sender}}", "content" to "{{content}}")
            )
        )
    }

    return emptyList()
}
