package com.example.meetwise_ai_scheduler.di

import android.content.Context
// import androidx.room.Room
// import com.example.meetwise_ai_scheduler.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * DRAFT FOR NOUTAYLA'S REVIEW
     * 
     * This module tells Hilt how to create and provide the Room Database instance.
     * We make it a Singleton because we only want ONE database connection active across the whole app.
     * 
     * Uncomment this once AppDatabase is created.
     */

    /*
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "meetwise_database"
        ).build()
    }
    */

    /**
     * DRAFT FOR DAOs
     * 
     * Once the DAOs (like MeetingDao, UserDao) are defined, we provide them here.
     * This way, our Repositories can just request a `MeetingDao` in their constructor, 
     * and Hilt will know how to get it from the database.
     */

    /*
    @Provides
    fun provideMeetingDao(database: AppDatabase): MeetingDao {
        return database.meetingDao()
    }
    */
}
