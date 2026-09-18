package com.medicationreminder.mobile.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun observeAll(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications ORDER BY name ASC")
    suspend fun getAll(): List<MedicationEntity>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: String): MedicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MedicationEntity)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules")
    fun observeAll(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules")
    suspend fun getAll(): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE medicationId = :medicationId")
    suspend fun getForMedication(medicationId: String): List<ScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM schedules WHERE medicationId = :medicationId")
    suspend fun deleteForMedication(medicationId: String)
}

@Dao
interface DoseEventDao {
    @Query("SELECT * FROM dose_events ORDER BY scheduledAt DESC")
    fun observeAll(): Flow<List<DoseEventEntity>>

    @Query("SELECT * FROM dose_events ORDER BY scheduledAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 200): List<DoseEventEntity>

    @Query("SELECT * FROM dose_events WHERE id = :id")
    suspend fun getById(id: String): DoseEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DoseEventEntity)

    @Update
    suspend fun update(entity: DoseEventEntity)

    @Query(
        """
        SELECT * FROM dose_events
        WHERE scheduledAt >= :from AND scheduledAt < :to
        ORDER BY scheduledAt ASC
        """
    )
    suspend fun getBetween(from: Long, to: Long): List<DoseEventEntity>
}
