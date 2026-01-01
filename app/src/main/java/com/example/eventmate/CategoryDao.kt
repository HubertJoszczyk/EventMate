package com.example.eventmate

import androidx.room.*

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category)

    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<Category>

    @Transaction
    @Query("SELECT * FROM categories")
    suspend fun getCategoriesWithEvents(): List<CategoryWithEvents>

    @Delete
    suspend fun deleteCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)
}
