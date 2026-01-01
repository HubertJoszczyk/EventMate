package com.example.eventmate

import androidx.room.Embedded
import androidx.room.Relation

data class CategoryWithEvents(
    @Embedded val category: Category,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "categoryId"
    )
    val events: List<Event>
)
