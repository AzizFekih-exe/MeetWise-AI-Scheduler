package com.example.meetwise_ai_scheduler

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.meetwise_ai_scheduler.data.local.AppDatabase
import com.example.meetwise_ai_scheduler.data.local.dao.*
import com.example.meetwise_ai_scheduler.data.local.entities.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class RoomDatabaseTest {
    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var meetingDao: MeetingDao
    private lateinit var availabilityDao: AvailabilityDao
    private lateinit var minutesDao: MinutesDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        userDao = db.userDao()
        meetingDao = db.meetingDao()
        availabilityDao = db.availabilityDao()
        minutesDao = db.minutesDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun testAvailabilityPersistence() = runBlocking {
        val user = UserEntity(1, "Test User", "test@example.com")
        userDao.insertUser(user)

        val window = AvailabilityEntity(
            availId = 1,
            userId = 1,
            dayOfWeek = 1,
            startHour = 9,
            endHour = 17
        )
        availabilityDao.insertAvailability(window)

        val retrieved = availabilityDao.getAvailabilityForUser(1).first()
        assertEquals(1, retrieved.size)
        assertEquals(9, retrieved[0].startHour)
    }

    @Test
    @Throws(Exception::class)
    fun testCascadingDeletes() = runBlocking {
        // Setup User and Meeting
        val user = UserEntity(1, "Organizer", "org@example.com")
        userDao.insertUser(user)
        
        val meeting = MeetingEntity(101, "Strategy Meeting", "2026-05-10", 1)
        meetingDao.insertMeeting(meeting)

        // Setup Minutes linked to Meeting
        val minutes = MinutesEntity(
            minutesId = 501,
            meetingId = 101,
            summary = "Great plan.",
            actionItemsJson = "[]",
            generatedAt = "2026-05-06"
        )
        minutesDao.insertMinutes(minutes)

        // Verify insertion
        var retrievedMinutes = minutesDao.getMinutesForMeeting(101).first()
        assertEquals("Great plan.", retrievedMinutes?.summary)

        // Delete Meeting and check Cascade
        meetingDao.deleteMeeting(meeting)
        
        retrievedMinutes = minutesDao.getMinutesForMeeting(101).first()
        assertNull("Minutes should have been deleted by cascade", retrievedMinutes)
    }
}
