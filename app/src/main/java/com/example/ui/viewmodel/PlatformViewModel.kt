package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CategoryItem
import com.example.data.model.ChatMessage
import com.example.data.model.LeaderboardUser
import com.example.data.model.QuestionItem
import com.example.data.model.StructuredDataPoint
import com.example.data.model.UserProfile
import com.example.data.repository.DataPlatformRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlatformViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DataPlatformRepository

    val userProfile: StateFlow<UserProfile?>
    val categories: StateFlow<List<CategoryItem>>
    val allCategories: StateFlow<List<CategoryItem>>
    val allStructuredData: StateFlow<List<StructuredDataPoint>>
    val validCount: StateFlow<Int>
    val totalCount: StateFlow<Int>

    private val _selectedCategory = MutableStateFlow<CategoryItem?>(null)
    val selectedCategory: StateFlow<CategoryItem?> = _selectedCategory.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessage>>

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminCategoryQuestions: StateFlow<List<QuestionItem>>

    private val _adminSelectedCategorySlug = MutableStateFlow<String?>(null)
    val adminSelectedCategorySlug: StateFlow<String?> = _adminSelectedCategorySlug.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<LeaderboardUser>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardUser>> = _leaderboard.asStateFlow()

    private val _userFeedback = MutableStateFlow<String?>(null)
    val userFeedback: StateFlow<String?> = _userFeedback.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = DataPlatformRepository(db.platformDao())

        userProfile = repository.userProfile.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), null
        )

        categories = repository.activeCategories.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        allCategories = repository.allCategories.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        allStructuredData = repository.allStructuredData.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        validCount = repository.validDataCount.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), 0
        )

        totalCount = repository.totalDataCount.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), 0
        )

        @OptIn(ExperimentalCoroutinesApi::class)
        messages = _selectedCategory.flatMapLatest { cat ->
            if (cat != null) repository.getMessagesForCategory(cat.slug) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        @OptIn(ExperimentalCoroutinesApi::class)
        adminCategoryQuestions = _adminSelectedCategorySlug.flatMapLatest { slug ->
            if (slug != null) repository.getQuestionsForCategory(slug) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        _leaderboard.value = repository.getLeaderboard()

        viewModelScope.launch {
            categories.collect { list ->
                if (_selectedCategory.value == null && list.isNotEmpty()) {
                    _selectedCategory.value = list.first()
                }
                if (_adminSelectedCategorySlug.value == null && list.isNotEmpty()) {
                    _adminSelectedCategorySlug.value = list.first().slug
                }
            }
        }
    }

    fun selectCategory(category: CategoryItem) {
        _selectedCategory.value = category
    }

    fun selectAdminCategory(slug: String) {
        _adminSelectedCategorySlug.value = slug
    }

    fun clearFeedback() {
        _userFeedback.value = null
    }

    fun sendMessage(text: String, imageUri: String? = null) {
        val currentCategory = _selectedCategory.value ?: return
        if (text.isBlank() && imageUri == null) return

        viewModelScope.launch {
            _isTyping.value = true
            try {
                delay(400)
                val (_, extracted) = repository.sendMessage(
                    categorySlug = currentCategory.slug,
                    categoryName = currentCategory.name,
                    text = text,
                    imageUri = imageUri
                )
                if (extracted != null) {
                    if (extracted.isValid) {
                        _userFeedback.value = "+0.05$ crédités ! Donnée validée : ${extracted.product}"
                    } else {
                        _userFeedback.value = "Donnée signalée : ${extracted.flagReason ?: "Incohérence détectée"}"
                    }
                }
            } catch (e: Exception) {
                _userFeedback.value = "Erreur de transmission: ${e.localizedMessage}"
            } finally {
                _isTyping.value = false
            }
        }
    }

    fun resetCurrentChat() {
        val currentCategory = _selectedCategory.value ?: return
        viewModelScope.launch {
            repository.resetChat(
                currentCategory.slug,
                currentCategory.name,
                currentCategory.emoji
            )
            _userFeedback.value = "Conversation réinitialisée pour ${currentCategory.name}"
        }
    }

    fun savePayout(paypalEmail: String, binanceId: String) {
        viewModelScope.launch {
            repository.updatePayout(paypalEmail, binanceId)
            _userFeedback.value = "Coordonnées de paiement enregistrées avec succès !"
        }
    }

    fun addCategory(slug: String, name: String, emoji: String, description: String) {
        if (slug.isBlank() || name.isBlank()) return
        viewModelScope.launch {
            repository.addCategory(slug, name, emoji, description)
            _userFeedback.value = "Nouvelle rubrique \"$name\" créée !"
        }
    }

    fun deleteCategory(slug: String) {
        viewModelScope.launch {
            repository.deleteCategory(slug)
            _userFeedback.value = "Rubrique supprimée."
        }
    }

    fun addQuestion(categorySlug: String, questionText: String, orderIndex: Int) {
        if (questionText.isBlank()) return
        viewModelScope.launch {
            repository.addQuestion(categorySlug, questionText, orderIndex)
            _userFeedback.value = "Nouvelle question ajoutée à l'IA."
        }
    }

    fun deleteQuestion(id: String) {
        viewModelScope.launch {
            repository.deleteQuestion(id)
            _userFeedback.value = "Question retirée."
        }
    }
}
