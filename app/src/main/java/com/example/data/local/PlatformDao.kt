package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CategoryItem
import com.example.data.model.ChatMessage
import com.example.data.model.QuestionItem
import com.example.data.model.StructuredDataPoint
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface PlatformDao {

    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUserProfileOnce(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)

    @Query("UPDATE user_profile SET paypalEmail = :paypal, binanceId = :binance")
    suspend fun updatePayoutSettings(paypal: String, binance: String)

    @Query("UPDATE user_profile SET balanceUsd = balanceUsd + :reward, trustScore = CASE WHEN (trustScore + :trustDelta) > 100 THEN 100 WHEN (trustScore + :trustDelta) < 0 THEN 0 ELSE (trustScore + :trustDelta) END")
    suspend fun rewardContribution(reward: Double, trustDelta: Int)

    // Categories
    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun getActiveCategories(): Flow<List<CategoryItem>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryItem)

    @Query("DELETE FROM categories WHERE slug = :slug")
    suspend fun deleteCategory(slug: String)

    @Query("DELETE FROM questions WHERE categorySlug = :slug")
    suspend fun deleteQuestionsForCategory(slug: String)

    // Questions
    @Query("SELECT * FROM questions WHERE categorySlug = :slug ORDER BY orderIndex ASC")
    fun getQuestionsForCategory(slug: String): Flow<List<QuestionItem>>

    @Query("SELECT * FROM questions WHERE categorySlug = :slug ORDER BY orderIndex ASC")
    suspend fun getQuestionsForCategorySync(slug: String): List<QuestionItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionItem)

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun deleteQuestion(id: String)

    // Chat messages
    @Query("SELECT * FROM chat_messages WHERE categorySlug = :categorySlug ORDER BY timestamp ASC")
    fun getMessagesForCategory(categorySlug: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE categorySlug = :categorySlug")
    suspend fun clearMessagesForCategory(categorySlug: String)

    // Structured data
    @Query("SELECT * FROM structured_data ORDER BY timestamp DESC")
    fun getAllStructuredData(): Flow<List<StructuredDataPoint>>

    @Query("SELECT * FROM structured_data WHERE category = :category ORDER BY timestamp DESC")
    fun getStructuredDataByCategory(category: String): Flow<List<StructuredDataPoint>>

    @Query("SELECT * FROM structured_data WHERE category = :category AND isValid = 1")
    suspend fun getValidDataForCategorySync(category: String): List<StructuredDataPoint>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStructuredData(data: StructuredDataPoint)

    @Query("SELECT COUNT(*) FROM structured_data WHERE isValid = 1")
    fun getValidDataCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM structured_data")
    fun getTotalDataCount(): Flow<Int>
}
