package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey val id: String,
    val sessionId: String,
    val categorySlug: String,
    val sender: String, // "user" or "ai"
    val messageText: String,
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
