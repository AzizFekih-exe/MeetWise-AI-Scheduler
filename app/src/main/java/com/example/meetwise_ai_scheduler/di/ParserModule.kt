package com.example.meetwise_ai_scheduler.di

import com.example.meetwise_ai_scheduler.domain.parser.DateTimeParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ParserModule {

    @Provides
    @Singleton
    fun provideDateTimeParser(): DateTimeParser {
        return DateTimeParser()
    }
}
