package com.example.meetwise_ai_scheduler.data.repository

import com.example.meetwise_ai_scheduler.data.network.MeetingApiService
import com.example.meetwise_ai_scheduler.data.local.Converters
import com.example.meetwise_ai_scheduler.data.local.dao.MinutesDao
import com.example.meetwise_ai_scheduler.data.local.entities.MinutesEntity
import com.example.meetwise_ai_scheduler.data.network.model.CreateMeetingRequest
import com.example.meetwise_ai_scheduler.data.network.model.toDomain
import com.example.meetwise_ai_scheduler.domain.engine.SlotSuggestionEngine
import com.example.meetwise_ai_scheduler.domain.model.Minutes
import com.example.meetwise_ai_scheduler.domain.model.Meeting
import com.example.meetwise_ai_scheduler.domain.model.MeetingIntent
import com.example.meetwise_ai_scheduler.domain.model.ScoredSlot
import com.example.meetwise_ai_scheduler.domain.model.Slot
import com.example.meetwise_ai_scheduler.domain.repository.MeetingRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeetingRepositoryImpl @Inject constructor(
    private val apiService: MeetingApiService,
    private val engine: SlotSuggestionEngine,
    private val minutesDao: MinutesDao
) : MeetingRepository {

    private val meetWiseZone = ZoneOffset.ofHours(1)
    private val converters = Converters()

    override suspend fun getMeetings(): Result<List<Meeting>> {
        return try {
            val response = apiService.getMeetings()
            Result.success(response.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createMeeting(meeting: Meeting): Result<Unit> {
        return try {
            apiService.createMeeting(
                CreateMeetingRequest(
                    title = meeting.title,
                    dateTime = meeting.dateTime.toString(),
                    duration = meeting.durationMinutes,
                    location = meeting.location,
                    status = meeting.status,
                    participants = emptyList(),
                    participantEmails = meeting.participantEmails
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSuggestedSlots(intent: MeetingIntent): Result<List<ScoredSlot>> {
        return try {
            // 1. Fetch current meetings to calculate buffer and history factors
            val existingMeetings = apiService.getMeetings().map { it.toDomain() }

            // 2. Build candidate windows from the parsed natural-language intent.
            // The backend slot endpoint is still a hardcoded shell, so local candidates
            // keep suggestions tied to what the user typed.
            val candidateWindows = buildCandidateWindows(
                intent = intent,
                durationMinutes = intent.durationMinutes ?: 60
            )

            // 3. Use our client-side engine to apply the full scoring formula
            val scoredSlots = engine.suggestSlots(
                availableWindows = candidateWindows,
                conflicts = existingMeetings.map { meeting ->
                    Slot(
                        startDateTime = meeting.dateTime,
                        endDateTime = meeting.dateTime.plusMinutes(meeting.durationMinutes.toLong())
                    )
                },
                existingMeetings = existingMeetings,
                durationMinutes = intent.durationMinutes ?: 60
            )
            
            Result.success(scoredSlots)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildCandidateWindows(intent: MeetingIntent, durationMinutes: Int): List<Slot> {
        val now = LocalDateTime.now(meetWiseZone)
        val startDate = intent.date ?: now.toLocalDate()
        val endDate = intent.endDate ?: startDate
        val dates = generateSequence(startDate) { date ->
            date.plusDays(1).takeIf { it <= endDate }
        }.toList()

        val candidateTimes = when {
            intent.timeWindow != null -> buildTimesFromWindow(intent.timeWindow)
            intent.time != null -> buildTimesAround(intent.time)
            startDate == now.toLocalDate() && endDate == startDate -> buildRemainingTimesToday(now)
            else -> defaultDayTimes()
        }

        return dates.flatMap { date ->
            candidateTimes.map { time -> LocalDateTime.of(date, time) }
        }
            .filter { start -> start.toLocalDate() in startDate..endDate }
            .filter { start -> start.isAfter(now) }
            .distinct()
            .sorted()
            .map { start ->
                Slot(
                    startDateTime = start,
                    endDateTime = start.plusMinutes(durationMinutes.toLong())
                )
            }
    }

    private fun buildTimesAround(requestedTime: LocalTime): List<LocalTime> {
        val baseMinutes = requestedTime.hour * 60 + requestedTime.minute
        return listOf(-30, 0, 30, 60, 120, 180)
            .map { offset -> baseMinutes + offset }
            .filter { minutes -> minutes in 0 until (24 * 60) }
            .map { minutes -> LocalTime.of(minutes / 60, minutes % 60) }
            .distinct()
    }

    private fun buildTimesFromWindow(window: ClosedRange<LocalTime>): List<LocalTime> {
        val startMinutes = window.start.hour * 60 + window.start.minute
        val endMinutes = window.endInclusive.hour * 60 + window.endInclusive.minute
        if (endMinutes < startMinutes) {
            return listOf(window.start)
        }

        val times = mutableListOf<LocalTime>()
        var currentMinutes = startMinutes
        while (currentMinutes <= endMinutes) {
            times.add(LocalTime.of(currentMinutes / 60, currentMinutes % 60))
            currentMinutes += 60
        }
        return times.distinct()
    }

    private fun buildRemainingTimesToday(now: LocalDateTime): List<LocalTime> {
        val nextHour = now.plusHours(1).withMinute(0).withSecond(0).withNano(0)
        if (nextHour.toLocalDate() != now.toLocalDate()) {
            return emptyList()
        }
        val times = mutableListOf<LocalTime>()
        var currentHour = nextHour.hour
        while (currentHour <= 23) {
            times.add(LocalTime.of(currentHour, 0))
            currentHour += 1
        }
        return times
    }

    private fun defaultDayTimes(): List<LocalTime> {
        return listOf(
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            LocalTime.of(14, 0),
            LocalTime.of(15, 0),
            LocalTime.of(16, 0),
            LocalTime.of(18, 0),
            LocalTime.of(20, 0)
        )
    }

    override suspend fun confirmMeeting(meeting: Meeting, selectedSlot: ScoredSlot): Result<Unit> {
        return try {
            // Confirm the slot with the backend
            apiService.confirmMeeting(
                meetingId = meeting.meetingId,
                slotId = selectedSlot.slot.startDateTime.toString() // Or use a real ID from SlotDto
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMeeting(meetingId: String): Result<Unit> {
        return try {
            apiService.deleteMeeting(meetingId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadRecording(meetingId: String, audioFile: File): Result<String> {
        return try {
            val requestBody = audioFile.asRequestBody("audio/mp4".toMediaType())
            val part = MultipartBody.Part.createFormData(
                name = "audio_file",
                filename = audioFile.name,
                body = requestBody
            )
            val response = apiService.transcribeAudio(meetingId, part)
            Result.success(response.jobId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTranscriptionStatus(jobId: String): Result<com.example.meetwise_ai_scheduler.domain.model.TranscriptionJobStatus> {
        return try {
            Result.success(apiService.getJobStatus(jobId).toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMinutes(meetingId: String): Result<Minutes> {
        return try {
            val minutes = apiService.getMinutes(meetingId).toDomain()
            cacheMinutes(minutes)
            Result.success(minutes)
        } catch (e: Exception) {
            val cachedMinutes = meetingId.toIntOrNull()?.let { minutesDao.getMinutesForMeeting(it) }
            if (cachedMinutes != null) {
                Result.success(cachedMinutes.toDomain())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getRecordedMinutes(): Result<List<Minutes>> {
        return try {
            val minutes = apiService.getRecordedMinutes().map { it.toDomain() }
            minutes.forEach { cacheMinutes(it) }
            Result.success(minutes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun cacheMinutes(minutes: Minutes) {
        minutesDao.insertMinutes(
            MinutesEntity(
                minutesId = minutes.minutesId,
                meetingId = minutes.meetingId,
                summary = minutes.summary,
                actionItemsJson = converters.fromActionItemList(minutes.actionItems),
                generatedAt = minutes.generatedAt,
                rawNotes = minutes.rawNotes
            )
        )
    }

    private fun MinutesEntity.toDomain(): Minutes {
        return Minutes(
            minutesId = minutesId,
            meetingId = meetingId,
            summary = summary,
            actionItems = converters.toActionItemList(actionItemsJson),
            generatedAt = generatedAt,
            rawNotes = rawNotes
        )
    }
}
