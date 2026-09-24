package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryItem(
    @PrimaryKey val slug: String,
    val name: String,
    val emoji: String,
    val description: String = "",
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

@Entity(tableName = "questions")
data class QuestionItem(
    @PrimaryKey val id: String,
    val categorySlug: String,
    val questionText: String,
    val orderIndex: Int
)
