package com.example.meetwise_ai_scheduler.data.local.dao

import androidx.room.*
import com.example.meetwise_ai_scheduler.data.local.entities.MinutesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MinutesDao {
    @Query("SELECT * FROM minutes WHERE meetingId = :meetingId")
    fun getMinutesForMeeting(meetingId: Int): Flow<MinutesEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMinutes(minutes: MinutesEntity): Long

    @Query("DELETE FROM minutes WHERE meetingId = :meetingId")
    suspend fun deleteMinutesForMeeting(meetingId: Int): Int
}
