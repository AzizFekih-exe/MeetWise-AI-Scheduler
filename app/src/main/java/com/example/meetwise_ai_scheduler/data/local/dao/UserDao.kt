package com.example.meetwise_ai_scheduler.data.local.dao

import androidx.room.*
import com.example.meetwise_ai_scheduler.data.local.entities.UserEntity

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}
