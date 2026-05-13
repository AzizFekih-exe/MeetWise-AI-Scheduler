package com.example.meetwise_ai_scheduler.di

import android.content.Context
import androidx.room.Room
import com.example.meetwise_ai_scheduler.data.local.AppDatabase
import com.example.meetwise_ai_scheduler.data.local.dao.MinutesDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "meetwise_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideMinutesDao(database: AppDatabase): MinutesDao {
        return database.minutesDao()
    }
}
