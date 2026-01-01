package com.example.eventmate

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "participants")
data class Participant(
    @PrimaryKey(autoGenerate = true)
    val participantId: Int = 0,
    val name: String,
    val email: String
)
