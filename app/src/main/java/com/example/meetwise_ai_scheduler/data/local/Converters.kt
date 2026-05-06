package com.example.meetwise_ai_scheduler.data.local

import androidx.room.TypeConverter
import com.example.meetwise_ai_scheduler.domain.model.ActionItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromActionItemList(value: List<ActionItem>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toActionItemList(value: String): List<ActionItem> {
        val listType = object : TypeToken<List<ActionItem>>() {}.type
        return gson.fromJson(value, listType)
    }
}
