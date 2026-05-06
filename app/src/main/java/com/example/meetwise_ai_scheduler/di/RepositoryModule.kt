package com.example.meetwise_ai_scheduler.di

import com.example.meetwise_ai_scheduler.data.repository.MeetingRepositoryImpl
import com.example.meetwise_ai_scheduler.domain.repository.MeetingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMeetingRepository(
        meetingRepositoryImpl: MeetingRepositoryImpl
    ): MeetingRepository
}
