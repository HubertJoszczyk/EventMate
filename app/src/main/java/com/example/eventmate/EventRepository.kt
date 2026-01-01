package com.example.eventmate

import kotlinx.coroutines.flow.Flow

class EventRepository(
    private val eventDao: EventDao,
    private val categoryDao: CategoryDao,
    private val participantDao: ParticipantDao
) {
    // Events
    val allEvents: Flow<List<Event>> = eventDao.getAllEvents()

    suspend fun insertEvent(event: Event) = eventDao.insertEvent(event)
    suspend fun updateEvent(event: Event) = eventDao.updateEvent(event)
    suspend fun deleteEvent(event: Event) = eventDao.deleteEvent(event)
    suspend fun getEventsWithParticipants(): List<EventWithParticipants> = eventDao.getEventsWithParticipants()
    suspend fun getUpcomingEvents(from: Long): List<Event> = eventDao.getUpcomingEvents(from)
    suspend fun getEventsForParticipant(participantId: Int): List<Event> = eventDao.getEventsForParticipant(participantId)
    suspend fun assignParticipantToEvent(eventId: Int, participantId: Int) {
        eventDao.insertEventParticipant(EventParticipant(eventId, participantId))
    }

    // Categories
    suspend fun insertCategory(category: Category) = categoryDao.insert(category)
    suspend fun getAllCategories(): List<Category> = categoryDao.getAll()
    suspend fun getCategoriesWithEvents(): List<CategoryWithEvents> = categoryDao.getCategoriesWithEvents()
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)
    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)

    // Participants
    suspend fun insertParticipant(participant: Participant) = participantDao.insert(participant)
    suspend fun getAllParticipants(): List<Participant> = participantDao.getAll()
    suspend fun deleteParticipant(participant: Participant) = participantDao.deleteParticipant(participant)
    suspend fun updateParticipant(participant: Participant) = participantDao.updateParticipant(participant)
}
