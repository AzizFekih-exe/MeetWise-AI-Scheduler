package com.example.meetwise_ai_scheduler.domain.parser

import com.example.meetwise_ai_scheduler.domain.model.MeetingIntent
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/**
 * Concept: NLP Parser (Task 7)
 * This class uses Regex to extract structured meeting information from natural language.
 * 
 * Rules:
 * - "tomorrow", "today", "next [day]" -> extracted as LocalDate
 * - "at 2pm", "at 14:00" -> extracted as LocalTime
 * - "for 1 hour", "for 30 mins" -> extracted as duration in minutes
 */
class DateTimeParser {

    fun parse(input: String): MeetingIntent? {
        val cleanInput = input.lowercase(Locale.ROOT).trim()
        
        val date = parseDate(cleanInput)
        val time = parseTime(cleanInput)
        val duration = parseDuration(cleanInput)
        
        // If we couldn't find a date AND a time, we consider it a parse failure
        // so the UI can fall back to the manual structured form.
        if (date == null && time == null) return null

        return MeetingIntent(
            title = extractTitle(cleanInput),
            date = date,
            time = time,
            durationMinutes = duration ?: 60, // Default to 60 minutes if not specified
            rawInput = input
        )
    }

    private fun parseDate(input: String): LocalDate? {
        val today = LocalDate.now()
        
        return when {
            input.contains("today") -> today
            input.contains("tomorrow") -> today.plusDays(1)
            else -> {
                // Simplified "next [day]" detection (e.g., "next monday")
                // In a real app, you'd use a DayOfWeek enum search here.
                null
            }
        }
    }

    private fun parseTime(input: String): LocalTime? {
        // Regex for: "at 2pm", "at 2:30pm", "at 14:00"
        val timeRegex = Regex("""(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""")
        val match = timeRegex.find(input) ?: return null
        
        var hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].takeIf { it.isNotEmpty() }?.toInt() ?: 0
        val amPm = match.groupValues[3].lowercase(Locale.ROOT)

        if (amPm.isNotEmpty()) {
            when (amPm) {
                "pm" -> if (hour < 12) hour += 12 // 2pm -> 14, 12pm stays 12
                "am" -> if (hour == 12) hour = 0  // 12am -> 0
            }
        }
        
        return try {
            LocalTime.of(hour, minute)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDuration(input: String): Int? {
        // Regex for: "for 1 hour", "for 30 mins"
        val durationRegex = Regex("""for\s+(\d+)\s*(hour|hr|min|minute)s?""")
        val match = durationRegex.find(input) ?: return null
        
        val value = match.groupValues[1].toInt()
        val unit = match.groupValues[2]
        
        return if (unit.startsWith("hour") || unit == "hr") {
            value * 60
        } else {
            value
        }
    }

    private fun extractTitle(input: String): String? {
        // Very basic title extraction: everything before "today", "tomorrow", or "at"
        val delimiters = listOf("today", "tomorrow", "at", "for")
        var title = input
        for (delim in delimiters) {
            val index = title.indexOf(delim)
            if (index != -1) {
                title = title.substring(0, index)
            }
        }
        return title.trim().capitalize(Locale.ROOT).takeIf { it.isNotEmpty() }
    }
}
