package com.example.smsforwarder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.smsforwarder.data.AppDatabase
import com.example.smsforwarder.data.DeliveryEntity
import com.example.smsforwarder.data.RuleEntity
import com.example.smsforwarder.data.ServicePreferences
import com.example.smsforwarder.delivery.DeliveryWorker
import com.example.smsforwarder.rules.DestinationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val ruleDao = db.ruleDao()
    private val deliveryDao = db.deliveryDao()

    private val _isServiceEnabled = MutableStateFlow(ServicePreferences.isServiceEnabled(application))
    val isServiceEnabled: StateFlow<Boolean> = _isServiceEnabled.asStateFlow()

    init {
        ServicePreferences.syncReceiverComponentState(application, _isServiceEnabled.value)
    }

    val rules: StateFlow<List<RuleEntity>> = ruleDao.getAllRulesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deliveries: StateFlow<List<DeliveryEntity>> = deliveryDao.getAllDeliveriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setServiceEnabled(enabled: Boolean) {
        if (_isServiceEnabled.value == enabled) return
        _isServiceEnabled.value = enabled
        val appContext = getApplication<Application>()
        ServicePreferences.setServiceEnabled(appContext, enabled)

        if (!enabled) {
            viewModelScope.launch {
                WorkManager.getInstance(appContext).cancelAllWork()
                deliveryDao.cancelAllPendingDeliveries()
            }
        }
    }

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
            if (!_isServiceEnabled.value) return@launch

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
}
