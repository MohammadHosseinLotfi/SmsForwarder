package com.example.smsforwarder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsforwarder.data.AppDatabase
import com.example.smsforwarder.data.DeliveryEntity
import com.example.smsforwarder.data.RuleEntity
import com.example.smsforwarder.rules.Rule
import com.example.smsforwarder.rules.RuleMatcher
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
                ruleDao.updateRule(ruleEntity)
            }
        }
    }

    fun toggleRuleEnabled(ruleEntity: RuleEntity) {
        viewModelScope.launch {
            ruleDao.updateRule(ruleEntity.copy(enabled = !ruleEntity.enabled))
        }
    }

    fun deleteRule(ruleEntity: RuleEntity) {
        viewModelScope.launch {
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

    fun cancelDelivery(deliveryId: Long) {
        viewModelScope.launch {
            deliveryDao.cancelDelivery(deliveryId)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            deliveryDao.clearAllDeliveries()
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
            "شرط فرستنده و عبارت داخل متن تطبیق دارد."
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