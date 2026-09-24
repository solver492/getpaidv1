package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CategoryItem
import com.example.data.model.ChatMessage
import com.example.data.model.QuestionItem
import com.example.data.model.StructuredDataPoint
import com.example.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfile::class,
        CategoryItem::class,
        QuestionItem::class,
        ChatMessage::class,
        StructuredDataPoint::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun platformDao(): PlatformDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "getpai_nexus_db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    seedInitialData(database.platformDao())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedInitialData(dao: PlatformDao) {
            // Seed profile
            dao.insertOrUpdateProfile(
                UserProfile(
                    id = "usr_nexus_01",
                    fullName = "Alex Rivera",
                    email = "alex.rivera@operator.io",
                    avatarUrl = "",
                    city = "Casablanca",
                    country = "Morocco",
                    trustScore = 92,
                    balanceUsd = 18.45,
                    paypalEmail = "alex.rivera@example.com",
                    binanceId = "89201481"
                )
            )

            // Seed categories
            val categories = listOf(
                CategoryItem("alimentation", "Alimentation & Snack", "🍔", "Habitudes d'achat épicerie, snacks et boissons", true, 1),
                CategoryItem("mode", "Mode & Vêtements", "👗", "Prêt-à-porter, chaussures et accessoires", true, 2),
                CategoryItem("sport", "Sport & Bien-être", "🏋️", "Équipements fitness, compléments et abonnements", true, 3),
                CategoryItem("style", "Style de vie & Mobilier", "🛋️", "Aménagement intérieur, déco et électroménager", true, 4),
                CategoryItem("tech", "Technologie & Gadgets", "📱", "Smartphones, audio, informatique et gaming", true, 5),
                CategoryItem("beaute", "Beauté & Cosmétiques", "💄", "Soins de la peau, maquillage et parfumerie", true, 6),
                CategoryItem("maison", "Maison & Quotidien", "🏠", "Produits d'entretien et articles pour l'habitat", true, 7),
                CategoryItem("transport", "Transport & Mobilité", "🚗", "Carburant, VTC, transports et entretien auto", true, 8),
                CategoryItem("loisirs", "Loisirs & Divertissement", "🎬", "Cinéma, streaming, jeux vidéo et sorties", true, 9),
                CategoryItem("voyage", "Voyages & Tourisme", "✈️", "Billets, hôtels, bagages et séjours", true, 10)
            )
            dao.insertCategories(categories)

            // Seed sample questions
            val questions = listOf(
                QuestionItem("q_alim_1", "alimentation", "Quelle est la dernière marque de snack ou boisson que tu as achetée récemment ?", 1),
                QuestionItem("q_alim_2", "alimentation", "Dans quelle enseigne ou magasin as-tu fait cet achat et pour quel montant approximatif ?", 2),
                QuestionItem("q_alim_3", "alimentation", "À quelle fréquence achètes-tu habituellement ce type de produit alimentaire ?", 3),

                QuestionItem("q_mode_1", "mode", "Quel est le dernier vêtement ou paire de chaussures que tu as acheté(e) ?", 1),
                QuestionItem("q_mode_2", "mode", "De quelle marque s'agissait-il et combien cela t'a coûté environ ?", 2),
                QuestionItem("q_mode_3", "mode", "As-tu commandé en ligne ou acheté en boutique physique ?", 3),

                QuestionItem("q_tech_1", "tech", "Quel accessoire ou gadget tech as-tu acheté ou renouvelé récemment ?", 1),
                QuestionItem("q_tech_2", "tech", "Peux-tu me citer la marque exacte et le tarif d'achat ?", 2),

                QuestionItem("q_sport_1", "sport", "Pratiques-tu une activité sportive nécessitant du matériel ou un abonnement en salle ?", 1),
                QuestionItem("q_sport_2", "sport", "Quel budget mensuel ou annuel consacres-tu à cette pratique et avec quelles marques ?", 2)
            )
            dao.insertQuestions(questions)

            // Seed initial structured data points
            val sampleData = listOf(
                StructuredDataPoint(
                    id = "dat_001",
                    sessionId = "sess_seed_1",
                    category = "alimentation",
                    product = "Café Espresso Grains",
                    brand = "Lavazza",
                    price = 8.50,
                    currency = "EUR",
                    location = "Carrefour Casablanca",
                    frequency = "Bimensuel",
                    isValid = true,
                    timestamp = System.currentTimeMillis() - 7200000
                ),
                StructuredDataPoint(
                    id = "dat_002",
                    sessionId = "sess_seed_1",
                    category = "tech",
                    product = "Écouteurs Bluetooth Pro",
                    brand = "Sony",
                    price = 79.99,
                    currency = "USD",
                    location = "Fnac Online",
                    frequency = "Annuel",
                    isValid = true,
                    timestamp = System.currentTimeMillis() - 14400000
                ),
                StructuredDataPoint(
                    id = "dat_003",
                    sessionId = "sess_seed_2",
                    category = "sport",
                    product = "Abonnement Fitness Salle",
                    brand = "CityClub",
                    price = 35.00,
                    currency = "EUR",
                    location = "Casablanca Centre",
                    frequency = "Mensuel",
                    isValid = true,
                    timestamp = System.currentTimeMillis() - 86400000
                ),
                StructuredDataPoint(
                    id = "dat_004",
                    sessionId = "sess_seed_3",
                    category = "mode",
                    product = "Baskets Running Boost",
                    brand = "Adidas",
                    price = 110.00,
                    currency = "USD",
                    location = "Adidas Store Morocco Mall",
                    frequency = "Occasionnel",
                    isValid = true,
                    timestamp = System.currentTimeMillis() - 172800000
                )
            )
            sampleData.forEach { dao.insertStructuredData(it) }

            // Seed initial welcome chat message for alimentation
            dao.insertMessage(
                ChatMessage(
                    id = "msg_welcome_alim",
                    sessionId = "sess_alim",
                    categorySlug = "alimentation",
                    sender = "ai",
                    messageText = "Salut ! Je suis Alex, ton assistant de recherche NEXUS DATA. 🍔\nPartage avec moi tes habitudes d'achat et gagne des récompenses en direct ! Quel est le dernier produit ou snack que tu as acheté récemment ?",
                    timestamp = System.currentTimeMillis() - 3600000
                )
            )
        }
    }
}
