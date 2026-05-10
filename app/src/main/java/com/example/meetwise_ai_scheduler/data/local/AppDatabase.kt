package com.example.meetwise_ai_scheduler.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.meetwise_ai_scheduler.data.local.dao.*
import com.example.meetwise_ai_scheduler.data.local.entities.*

@Database(
    entities = [
        UserEntity::class, 
        MeetingEntity::class, 
        AvailabilityEntity::class, 
        MinutesEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun meetingDao(): MeetingDao
    abstract fun availabilityDao(): AvailabilityDao
    abstract fun minutesDao(): MinutesDao
}
