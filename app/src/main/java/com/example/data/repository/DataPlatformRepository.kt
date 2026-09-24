package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.PlatformDao
import com.example.data.model.CategoryItem
import com.example.data.model.ChatMessage
import com.example.data.model.LeaderboardUser
import com.example.data.model.QuestionItem
import com.example.data.model.StructuredDataPoint
import com.example.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.regex.Pattern

class DataPlatformRepository(private val dao: PlatformDao) {

    private var lastUserMessageTime: Long = 0L

    val userProfile: Flow<UserProfile?> = dao.getUserProfile()
    val activeCategories: Flow<List<CategoryItem>> = dao.getActiveCategories()
    val allCategories: Flow<List<CategoryItem>> = dao.getAllCategories()
    val allStructuredData: Flow<List<StructuredDataPoint>> = dao.getAllStructuredData()
    val validDataCount: Flow<Int> = dao.getValidDataCount()
    val totalDataCount: Flow<Int> = dao.getTotalDataCount()

    fun getQuestionsForCategory(slug: String): Flow<List<QuestionItem>> =
        dao.getQuestionsForCategory(slug)

    fun getMessagesForCategory(categorySlug: String): Flow<List<ChatMessage>> =
        dao.getMessagesForCategory(categorySlug)

    fun getStructuredDataByCategory(category: String): Flow<List<StructuredDataPoint>> =
        dao.getStructuredDataByCategory(category)

    suspend fun updatePayout(paypalEmail: String, binanceId: String) = withContext(Dispatchers.IO) {
        dao.updatePayoutSettings(paypalEmail.trim(), binanceId.trim())
    }

    suspend fun addCategory(slug: String, name: String, emoji: String, description: String) = withContext(Dispatchers.IO) {
        val safeSlug = slug.lowercase().replace(" ", "-").trim()
        val cat = CategoryItem(
            slug = safeSlug,
            name = name.trim(),
            emoji = if (emoji.isNotBlank()) emoji.trim() else "📋",
            description = description.trim(),
            isActive = true,
            sortOrder = 99
        )
        dao.insertCategory(cat)
    }

    suspend fun deleteCategory(slug: String) = withContext(Dispatchers.IO) {
        dao.deleteCategory(slug)
        dao.deleteQuestionsForCategory(slug)
    }

    suspend fun addQuestion(categorySlug: String, questionText: String, orderIndex: Int) = withContext(Dispatchers.IO) {
        val q = QuestionItem(
            id = "q_" + UUID.randomUUID().toString().take(8),
            categorySlug = categorySlug,
            questionText = questionText.trim(),
            orderIndex = orderIndex
        )
        dao.insertQuestion(q)
    }

    suspend fun deleteQuestion(id: String) = withContext(Dispatchers.IO) {
        dao.deleteQuestion(id)
    }

    suspend fun resetChat(categorySlug: String, categoryName: String, emoji: String) = withContext(Dispatchers.IO) {
        dao.clearMessagesForCategory(categorySlug)
        val welcome = ChatMessage(
            id = "welcome_" + UUID.randomUUID().toString().take(8),
            sessionId = "sess_" + categorySlug,
            categorySlug = categorySlug,
            sender = "ai",
            messageText = "Salut ! Je suis Alex, ton assistant de recherche NEXUS DATA. $emoji\nPartage tes habitudes d'achat dans la rubrique $categoryName pour cumuler des dollars et valoriser tes données !",
            timestamp = System.currentTimeMillis()
        )
        dao.insertMessage(welcome)
    }

    suspend fun sendMessage(
        categorySlug: String,
        categoryName: String,
        text: String,
        imageUri: String? = null
    ): Pair<ChatMessage, StructuredDataPoint?> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val isSpeedSpam = (now - lastUserMessageTime) < 1200L && lastUserMessageTime != 0L
        lastUserMessageTime = now

        val sessionId = "sess_" + categorySlug
        val userMsgId = "msg_u_" + UUID.randomUUID().toString().take(8)
        val userMsg = ChatMessage(
            id = userMsgId,
            sessionId = sessionId,
            categorySlug = categorySlug,
            sender = "user",
            messageText = text.trim(),
            imageUrl = imageUri,
            timestamp = now
        )
        dao.insertMessage(userMsg)

        if (isSpeedSpam) {
            dao.rewardContribution(0.0, -10)
        }

        // Fetch questions configured for this category
        val questions = dao.getQuestionsForCategorySync(categorySlug)

        // Try AI response (via Gemini REST if key exists, otherwise local smart conversational extractor)
        val (aiResponseText, extracted) = generateAIResponse(
            categorySlug = categorySlug,
            categoryName = categoryName,
            userText = text,
            questions = questions
        )

        var finalExtractedPoint: StructuredDataPoint? = null

        if (extracted != null) {
            var isValid = !isSpeedSpam
            var flagReason: String? = if (isSpeedSpam) "Cadence de saisie trop rapide" else null

            // Price deviation check
            val existing = dao.getValidDataForCategorySync(categorySlug)
            if (existing.size >= 3) {
                val prices = existing.map { it.price }.sorted()
                val median = prices[prices.size / 2]
                if (median > 0 && (extracted.price > median * 4.0 || extracted.price < median * 0.25)) {
                    isValid = false
                    flagReason = "Écart de prix anormal par rapport à la médiane ($median USD)"
                    dao.rewardContribution(0.0, -5)
                }
            }

            finalExtractedPoint = extracted.copy(
                sessionId = sessionId,
                isValid = isValid,
                flagReason = flagReason,
                timestamp = System.currentTimeMillis()
            )
            dao.insertStructuredData(finalExtractedPoint)

            if (isValid) {
                dao.rewardContribution(0.05, 1)
            }
        }

        val aiMsg = ChatMessage(
            id = "msg_ai_" + UUID.randomUUID().toString().take(8),
            sessionId = sessionId,
            categorySlug = categorySlug,
            sender = "ai",
            messageText = aiResponseText,
            timestamp = System.currentTimeMillis()
        )
        dao.insertMessage(aiMsg)

        Pair(aiMsg, finalExtractedPoint)
    }

    private fun generateAIResponse(
        categorySlug: String,
        categoryName: String,
        userText: String,
        questions: List<QuestionItem>
    ): Pair<String, StructuredDataPoint?> {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (!apiKey.isNullOrBlank()) {
            try {
                val geminiResult = callGeminiApi(apiKey, categorySlug, categoryName, userText, questions)
                if (geminiResult != null) {
                    return geminiResult
                }
            } catch (e: Exception) {
                // Fall back gracefully to local engine
            }
        }

        return localConversationalEngine(categorySlug, categoryName, userText, questions)
    }

    private fun callGeminiApi(
        apiKey: String,
        categorySlug: String,
        categoryName: String,
        userText: String,
        questions: List<QuestionItem>
    ): Pair<String, StructuredDataPoint?>? {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        val url = URL(endpoint)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 8000
        conn.readTimeout = 8000

        val questionsText = questions.mapIndexed { idx, q -> "${idx + 1}. ${q.questionText}" }.joinToString("\n")
        val systemPrompt = "Tu es Alex, assistant d'étude de marché NEXUS DATA pour la catégorie: $categoryName. " +
                "Sois chaleureux, naturel, pose une question à la fois. Si l'utilisateur mentionne un achat/produit avec marque ou prix, " +
                "ajoute à la fin: [DATA_START]{\"product\":\"...\",\"brand\":\"...\",\"price\":0.0,\"currency\":\"USD\",\"location\":\"...\",\"frequency\":\"...\",\"category\":\"$categorySlug\"}[DATA_END]. " +
                "Questions d'inspiration:\n$questionsText"

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemPrompt\n\nMessage utilisateur: $userText"))
                    })
                })
            })
        }

        conn.outputStream.use { os ->
            os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
        }

        if (conn.responseCode == 200) {
            val responseString = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(responseString)
            val candidates = root.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""

            return parseAiTextAndJson(text, categorySlug)
        }

        return null
    }

    private fun parseAiTextAndJson(raw: String, categorySlug: String): Pair<String, StructuredDataPoint?> {
        val pattern = Pattern.compile("\\[DATA_START\\]([\\s\\S]*?)\\[DATA_END\\]")
        val matcher = pattern.matcher(raw)

        var extractedPoint: StructuredDataPoint? = null
        if (matcher.find()) {
            val jsonContent = matcher.group(1)?.trim()
            try {
                val obj = JSONObject(jsonContent ?: "")
                extractedPoint = StructuredDataPoint(
                    id = "dat_" + UUID.randomUUID().toString().take(8),
                    sessionId = "sess_" + categorySlug,
                    category = categorySlug,
                    product = obj.optString("product", "Produit"),
                    brand = obj.optString("brand", "Générique"),
                    price = obj.optDouble("price", 10.0),
                    currency = obj.optString("currency", "USD"),
                    location = obj.optString("location", "Magasin"),
                    frequency = obj.optString("frequency", "Mensuel"),
                    isValid = true
                )
            } catch (e: Exception) {
                // Ignore parse error
            }
        }

        val visibleText = raw.replace(Regex("\\[DATA_START\\][\\s\\S]*?\\[DATA_END\\]"), "").trim()
        return Pair(visibleText.ifBlank { "Merci pour cette information précieuse !" }, extractedPoint)
    }

    private fun localConversationalEngine(
        categorySlug: String,
        categoryName: String,
        userText: String,
        questions: List<QuestionItem>
    ): Pair<String, StructuredDataPoint?> {
        val textLower = userText.lowercase()

        // Extract price if present
        val priceRegex = Regex("(\\d+([.,]\\d+)?)\\s*(?:dh|mad|€|eur|\\$|usd|euros?|dollars?)?", RegexOption.IGNORE_CASE)
        val priceMatch = priceRegex.find(userText)
        val extractedPrice = priceMatch?.groups?.get(1)?.value?.replace(",", ".")?.toDoubleOrNull()

        // Extract brand hints
        val knownBrands = listOf(
            "apple", "samsung", "sony", "nike", "adidas", "zara", "h&m", "marjane", "carrefour",
            "nespresso", "lavazza", "danone", "coca-cola", "pepsi", "sephora", "decathlon",
            "dior", "l'oreal", "ikea", "toyota", "renault", "uber", "careem", "netflix", "spotify"
        )
        val matchedBrand = knownBrands.firstOrNull { textLower.contains(it) }?.replaceFirstChar { it.uppercase() }

        var structuredPoint: StructuredDataPoint? = null

        if (extractedPrice != null && extractedPrice > 0) {
            val productDesc = userText.take(40).trim()
            val brand = matchedBrand ?: (if (userText.split(" ").size > 1) userText.split(" ")[0].replaceFirstChar { it.uppercase() } else "Marque Locale")
            structuredPoint = StructuredDataPoint(
                id = "dat_" + UUID.randomUUID().toString().take(8),
                sessionId = "sess_" + categorySlug,
                category = categorySlug,
                product = if (productDesc.length > 5) productDesc else "Achat $categoryName",
                brand = brand,
                price = extractedPrice,
                currency = if (textLower.contains("dh") || textLower.contains("mad")) "MAD" else if (textLower.contains("€") || textLower.contains("eur")) "EUR" else "USD",
                location = if (textLower.contains("en ligne") || textLower.contains("online")) "E-commerce" else "Point de vente physique",
                frequency = if (textLower.contains("tous les jours") || textLower.contains("quotidien")) "Quotidien" else "Régulier",
                isValid = true
            )
        }

        // Generate natural conversational reply
        val response = StringBuilder()
        if (structuredPoint != null) {
            response.append("Excellent ! J'ai bien consigné cet achat (${structuredPoint.product} - ${structuredPoint.brand} à ${structuredPoint.price} ${structuredPoint.currency}). ⚡\n+0.05$ crédités sur ton solde NEXUS_DATA !\n\n")
            val nextQuestion = questions.shuffled().firstOrNull()?.questionText
                ?: "À quelle fréquence achètes-tu habituellement ce type de produit ?"
            response.append(nextQuestion)
        } else if (matchedBrand != null) {
            response.append("Super choix avec $matchedBrand ! Peux-tu me préciser quel produit exact et pour quel prix approximatif tu l'as acquis ?")
        } else if (textLower.contains("salut") || textLower.contains("bonjour") || textLower.contains("hello")) {
            val q = questions.firstOrNull()?.questionText ?: "Quel est le dernier achat que tu as fait dans la catégorie $categoryName ?"
            response.append("Salut ! Raconte-moi un peu : $q")
        } else {
            val randomQuestion = questions.shuffled().firstOrNull()?.questionText
                ?: "Intéressant ! Dans quel magasin ou site as-tu acheté cela, et pour combien environ ?"
            response.append("C'est noté ! $randomQuestion")
        }

        return Pair(response.toString(), structuredPoint)
    }

    fun getLeaderboard(): List<LeaderboardUser> {
        return listOf(
            LeaderboardUser("u_1", "Sarah Benali", "SB", 99, 142, 28.50, 1),
            LeaderboardUser("u_2", "Alex Rivera (Toi)", "AR", 92, 94, 18.45, 2),
            LeaderboardUser("u_3", "Youssef Mansour", "YM", 88, 81, 16.20, 3),
            LeaderboardUser("u_4", "Elena Rostova", "ER", 86, 75, 15.00, 4),
            LeaderboardUser("u_5", "Tariq Kabbaj", "TK", 84, 63, 12.60, 5),
            LeaderboardUser("u_6", "Kenji Sato", "KS", 81, 52, 10.40, 6),
            LeaderboardUser("u_7", "Clara Dupont", "CD", 78, 44, 8.80, 7)
        )
    }
}
