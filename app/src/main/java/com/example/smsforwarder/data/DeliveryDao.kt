package com.example.smsforwarder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDelivery(delivery: DeliveryEntity): Long

    @Query("SELECT * FROM deliveries WHERE deliveryId = :id")
    suspend fun getDeliveryById(id: Long): DeliveryEntity?

    @Query("SELECT * FROM deliveries ORDER BY receivedAt DESC")
    fun getAllDeliveriesFlow(): Flow<List<DeliveryEntity>>

    @Query("UPDATE deliveries SET state = :state, attemptCount = attemptCount + 1, lastAttemptAt = :attemptTime, httpStatusCode = COALESCE(:httpStatusCode, httpStatusCode), errorMessage = :error WHERE deliveryId = :id")
    suspend fun updateDeliveryStatus(
        id: Long,
        state: DeliveryState,
        attemptTime: Long,
        httpStatusCode: Int? = null,
        error: String? = null
    )

    @Query("UPDATE deliveries SET state = 'QUEUED', errorMessage = null WHERE deliveryId = :id")
    suspend fun resetDeliveryForResend(id: Long)

    @Query("UPDATE deliveries SET state = 'CANCELLED' WHERE deliveryId = :id")
    suspend fun cancelDelivery(id: Long)

    @Query("SELECT COUNT(*) FROM deliveries WHERE ruleId = :ruleId AND state IN ('QUEUED', 'WAITING_NETWORK', 'IN_FLIGHT', 'RETRY_WAIT')")
    suspend fun countPendingDeliveriesForRule(ruleId: Long): Int

    @Query("UPDATE deliveries SET state = 'CANCELLED' WHERE ruleId = :ruleId AND state IN ('QUEUED', 'WAITING_NETWORK', 'IN_FLIGHT', 'RETRY_WAIT')")
    suspend fun cancelPendingDeliveriesForRule(ruleId: Long)

    @Query("UPDATE deliveries SET ruleNameSnapshot = :newName WHERE ruleNameSnapshot = :oldName")
    suspend fun updateRuleNameSnapshotByOldName(oldName: String, newName: String)

    @Query("UPDATE deliveries SET ruleNameSnapshot = :newName WHERE ruleId = :ruleId")
    suspend fun updateRuleNameSnapshotByRuleId(ruleId: Long, newName: String)

    @Query("DELETE FROM deliveries")
    suspend fun clearAllDeliveries()
}
