package com.example.smsforwarder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getRuleById(id: Long): RuleEntity?

    @Query("SELECT * FROM rules WHERE enabled = 1")
    suspend fun getActiveRules(): List<RuleEntity>

    @Query("SELECT * FROM rules ORDER BY id DESC")
    fun getAllRulesFlow(): Flow<List<RuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RuleEntity): Long

    @Update
    suspend fun updateRule(rule: RuleEntity)

    @Transaction
    suspend fun updateRuleAndSnapshots(ruleEntity: RuleEntity, oldName: String?, deliveryDao: DeliveryDao) {
        if (oldName != null && oldName != ruleEntity.name) {
            deliveryDao.updateRuleNameSnapshotByRuleId(ruleEntity.id, ruleEntity.name)
            deliveryDao.updateRuleNameSnapshotByOldName(oldName, ruleEntity.name)
        }
        updateRule(ruleEntity)
    }

    @Delete
    suspend fun deleteRule(rule: RuleEntity)

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)
}
