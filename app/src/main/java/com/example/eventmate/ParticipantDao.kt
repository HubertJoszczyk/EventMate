package com.example.eventmate

import androidx.room.*

@Dao
interface ParticipantDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(participant: Participant)

    @Query("SELECT * FROM participants")
    suspend fun getAll(): List<Participant>

    @Delete
    suspend fun deleteParticipant(participant: Participant)

    @Update
    suspend fun updateParticipant(participant: Participant)
}
