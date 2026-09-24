package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: String = "usr_default_01",
    val fullName: String = "Alex Rivera",
    val email: String = "operator@nexusdata.io",
    val avatarUrl: String = "",
    val city: String = "Casablanca",
    val country: String = "Morocco",
    val trustScore: Int = 85,
    val balanceUsd: Double = 14.85,
    val paypalEmail: String = "alex.rivera@example.com",
    val binanceId: String = "89201481"
)
