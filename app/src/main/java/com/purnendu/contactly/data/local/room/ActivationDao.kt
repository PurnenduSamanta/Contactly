package com.purnendu.contactly.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao

interface ActivationDao {
    @Query("SELECT * FROM activation ORDER BY startAtMillis DESC")
    fun getAll(): Flow<List<ActivationEntity>>

    @Query("SELECT * FROM activation WHERE activationId = :id")
    suspend fun getById(id: Long): ActivationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ActivationEntity): Long

    @Update
    suspend fun update(entity: ActivationEntity): Int

    @Query("DELETE FROM activation WHERE activationId = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM activation WHERE contactId = :contactId")
    suspend fun deleteByContactId(contactId: Long): Int
}
