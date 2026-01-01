package com.example.eventmate

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class EventWithParticipants(
    @Embedded val event: Event,
    @Relation(
        parentColumn = "id",
        entityColumn = "participantId",
        associateBy = Junction(
            value = EventParticipant::class,
            parentColumn = "eventId",
            entityColumn = "participantId"
        )
    )
    val participants: List<Participant>
)
