package com.example.eventmate

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event)

    @Update
    suspend fun updateEvent(event: Event)

    @Delete
    suspend fun deleteEvent(event: Event)

    @Transaction
    @Query("SELECT * FROM events")
    suspend fun getEventsWithParticipants(): List<EventWithParticipants>

    @Query("SELECT * FROM events WHERE date >= :from ORDER BY date")
    suspend fun getUpcomingEvents(from: Long): List<Event>

    @Query("""
        SELECT events.* FROM events
        INNER JOIN event_participants
        ON events.id = event_participants.eventId
        WHERE event_participants.participantId = :participantId
    """)
    suspend fun getEventsForParticipant(participantId: Int): List<Event>

    @Query("SELECT * FROM events ORDER BY date ASC")
    fun getAllEvents(): Flow<List<Event>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventParticipant(crossRef: EventParticipant)
}
