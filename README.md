# GetPai (NEXUS_DATA) — Android Consumer Research & Data Intelligence Platform

GetPai is a native Android application built with Kotlin and Jetpack Compose that empowers users to monetize their everyday consumer purchase insights through interactive conversational AI research.

## Features

- **Consumer Research AI Chat (Alex)**:
  - Multi-category research topics (Alimentation, Mode, Sport, Style de vie, Tech, Beauté, Maison, Transport, Loisirs, Voyages).
  - Conversational natural interviewer guiding users one question at a time.
  - Image and receipt attachment with photo picker.
  - Automatic extraction of structured consumer data points (product, brand, price, currency, location, frequency).
  - Gemini API integration with fallback to local intelligent heuristic engine.

- **Anti-Cheat & Data Integrity**:
  - Speed-based anti-bot detection.
  - Category price deviation anomaly detection against statistical median.
  - Trust score system dynamically adjusting user credibility (0 - 100).

- **Wallet & Payouts**:
  - Live balance in USD ($0.05 per verified data point).
  - PayPal email and Binance Pay ID wallet configuration.
  - Radial animated Trust Score gauge.
  - Monthly milestone target progress bar.

- **Data Vault**:
  - Real-time ledger of recorded consumer data points.
  - Validation status badges (`VALIDE` / `SIGNALÉ`).
  - Expandable raw JSON payload inspector.

- **Leaderboard**:
  - Global consumer researcher standings with podium rankings, trust scores, and lifetime earnings.

- **Admin Back-Office**:
  - Rubriques & questions management (add/delete categories, emoji, slug, description, and custom AI questions).
  - Platform-wide telemetry (total submissions, validation rate %, community payouts).

## Technology Stack

- **UI**: Jetpack Compose, Material 3, Adaptive Navigation (Bottom Navigation for mobile, Navigation Rail for tablets/foldables).
- **Architecture**: MVVM with Kotlin Coroutines and StateFlow.
- **Persistence**: Room Database with pre-seeded survey categories, questions, and baseline transactions.
- **Image Handling**: Modern Android Photo Picker and Coil Compose.
- **Security**: AI Studio Secrets Gradle Plugin for API keys.
