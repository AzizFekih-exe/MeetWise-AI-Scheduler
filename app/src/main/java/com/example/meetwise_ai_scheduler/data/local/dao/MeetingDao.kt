package com.example.meetwise_ai_scheduler.data.local.dao

import androidx.room.*
import com.example.meetwise_ai_scheduler.data.local.entities.MeetingEntity

@Dao
interface MeetingDao {
    @Insert
    suspend fun insertMeeting(meeting: MeetingEntity): Long

    @Delete
    suspend fun deleteMeeting(meeting: MeetingEntity): Int
}
