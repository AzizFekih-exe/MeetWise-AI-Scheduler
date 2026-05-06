package com.example.meetwise_ai_scheduler.data.local.dao

import androidx.room.*
import com.example.meetwise_ai_scheduler.data.local.entities.AvailabilityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AvailabilityDao {
    @Query("SELECT * FROM availability WHERE userId = :userId")
    fun getAvailabilityForUser(userId: Int): Flow<List<AvailabilityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAvailability(availability: AvailabilityEntity)

    @Delete
    suspend fun deleteAvailability(availability: AvailabilityEntity)

    @Query("DELETE FROM availability WHERE userId = :userId")
    suspend fun clearAvailabilityForUser(userId: Int)
}
