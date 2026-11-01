package com.example.smsforwarder.ui

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.smsforwarder.data.AppDatabase
import com.example.smsforwarder.data.DeliveryEntity
import com.example.smsforwarder.data.RuleEntity
import com.example.smsforwarder.delivery.DeliveryWorker
import com.example.smsforwarder.rules.DestinationType
import com.example.smsforwarder.rules.Rule
import com.example.smsforwarder.rules.RuleMatcher
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Immutable
data class DryRunResult(
    val isMatched: Boolean,
    val matchReason: String,
    val renderedOutput: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val ruleDao = db.ruleDao()
    private val deliveryDao = db.deliveryDao()

    private val gson = Gson()

    val rules: StateFlow<List<RuleEntity>> = ruleDao.getAllRulesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deliveries: StateFlow<List<DeliveryEntity>> = deliveryDao.getAllDeliveriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveRule(ruleEntity: RuleEntity) {
        viewModelScope.launch {
            if (ruleEntity.id == 0L) {
                ruleDao.insertRule(ruleEntity)
            } else {
                val oldRule = rules.value.find { it.id == ruleEntity.id }
                ruleDao.updateRuleAndSnapshots(ruleEntity, oldRule?.name, deliveryDao)
            }
        }
    }

    fun toggleRuleEnabled(ruleEntity: RuleEntity) {
        viewModelScope.launch {
            val newEnabled = !ruleEntity.enabled
            if (!newEnabled) {
                deliveryDao.cancelPendingDeliveriesForRule(ruleEntity.id)
                WorkManager.getInstance(getApplication()).cancelAllWorkByTag("rule_${ruleEntity.id}")
            }
            ruleDao.updateRule(ruleEntity.copy(enabled = newEnabled))
        }
    }

    fun deleteRule(ruleEntity: RuleEntity) {
        viewModelScope.launch {
            deliveryDao.cancelPendingDeliveriesForRule(ruleEntity.id)
            WorkManager.getInstance(getApplication()).cancelAllWorkByTag("rule_${ruleEntity.id}")
            ruleDao.deleteRule(ruleEntity)
        }
    }

    fun duplicateRule(ruleEntity: RuleEntity) {
        viewModelScope.launch {
            val copyEntity = ruleEntity.copy(
                id = 0,
                name = "${ruleEntity.name} (کپی)",
                enabled = false // غیرفعال برای جلوگیری از ارسال تکراری ناخواسته طبق سند
            )
            ruleDao.insertRule(copyEntity)
        }
    }

    fun resendDelivery(deliveryId: Long) {
        viewModelScope.launch {
            val delivery = deliveryDao.getDeliveryById(deliveryId) ?: return@launch
            deliveryDao.resetDeliveryForResend(deliveryId)

            val requiresNetwork = delivery.destinationType == DestinationType.URL
            val constraintsBuilder = Constraints.Builder()
            if (requiresNetwork) {
                constraintsBuilder.setRequiredNetworkType(NetworkType.CONNECTED)
            }

            val workRequest = OneTimeWorkRequestBuilder<DeliveryWorker>()
                .setConstraints(constraintsBuilder.build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .addTag("delivery_$deliveryId")
                .addTag("rule_${delivery.ruleId}")
                .setInputData(workDataOf("deliveryId" to deliveryId))
                .build()

            WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                "delivery_$deliveryId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    fun cancelDelivery(deliveryId: Long) {
        viewModelScope.launch {
            deliveryDao.cancelDelivery(deliveryId)
            WorkManager.getInstance(getApplication()).cancelUniqueWork("delivery_$deliveryId")
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            deliveryDao.clearAllDeliveries()
            WorkManager.getInstance(getApplication()).cancelAllWork()
            WorkManager.getInstance(getApplication()).pruneWork()
        }
    }

    /**
     * اجرای تست خشک (Dry Run) بدون انجام هیچ‌گونه درخواست واقعی شبکه یا SMS
     */
    fun runDryRunTest(
        ruleEntity: RuleEntity,
        testSender: String,
        testContent: String
    ): DryRunResult {
        val mapType = TypeToken.getParameterized(Map::class.java, String::class.java, String::class.java).type
        val jsonFields: Map<String, String> = try {
            gson.fromJson(ruleEntity.jsonFieldsJson, mapType) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }

        val rule = Rule(
            id = ruleEntity.id,
            name = ruleEntity.name,
            enabled = true, // برای تست خشک، شرط فعال بودن را true در نظر می‌گیریم
            sender = ruleEntity.sender,
            containsText = ruleEntity.containsText,
            destinationType = ruleEntity.destinationType,
            destinationAddress = ruleEntity.destinationAddress,
            outgoingSlotIndex = ruleEntity.outgoingSlotIndex,
            smsTemplate = ruleEntity.smsTemplate,
            jsonFields = jsonFields
        )

        val isMatched = RuleMatcher.matches(rule, testSender, testContent)
        val reason = if (isMatched) {
            if (ruleEntity.sender.isBlank() || ruleEntity.sender.trim() == "*") {
                "فرستنده: همه فرستنده‌ها (بدون محدودیت)" + if (!ruleEntity.containsText.isNullOrBlank()) " و عبارت متنی تطبیق دارد." else "."
            } else {
                "شرط فرستنده و عبارت داخل متن تطبیق دارد."
            }
        } else {
            "فرستنده یا عبارت داخل متن با شرط قانون تطبیق ندارد."
        }

        val output = if (isMatched) {
            RuleMatcher.renderOutput(rule, testSender, testContent)
        } else {
            "عدم تطبیق - خروجی تولید نشد."
        }

        return DryRunResult(isMatched, reason, output)
    }
}
