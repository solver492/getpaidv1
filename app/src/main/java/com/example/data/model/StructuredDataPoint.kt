package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "structured_data")
data class StructuredDataPoint(
    @PrimaryKey val id: String,
    val sessionId: String,
    val category: String,
    val product: String,
    val brand: String,
    val price: Double,
    val currency: String = "USD",
    val location: String = "Casablanca",
    val frequency: String = "Hebdomadaire",
    val isValid: Boolean = true,
    val flagReason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class LeaderboardUser(
    val id: String,
    val fullName: String,
    val avatarInitials: String,
    val trustScore: Int,
    val verifiedPoints: Int,
    val totalEarnedUsd: Double,
    val rank: Int
)
