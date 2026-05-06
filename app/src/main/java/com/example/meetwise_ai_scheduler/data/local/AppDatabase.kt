package com.example.meetwise_ai_scheduler.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.meetwise_ai_scheduler.data.local.dao.AvailabilityDao
import com.example.meetwise_ai_scheduler.data.local.dao.MinutesDao
import com.example.meetwise_ai_scheduler.data.local.entities.AvailabilityEntity
import com.example.meetwise_ai_scheduler.data.local.entities.MinutesEntity

@Database(
    entities = [AvailabilityEntity::class, MinutesEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun availabilityDao(): AvailabilityDao
    abstract fun minutesDao(): MinutesDao
}
