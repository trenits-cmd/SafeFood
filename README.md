# 🥑 SafeFood - Food & Safe Ingredient Tracker for Autistic Individuals

**SafeFood** is a native Android application designed specifically for autistic individuals, sensory-sensitive eaters, and their caregivers or dietitians. It provides an intuitive, low-cognitive-load environment to categorize, rate, track, and plan safe foods, organize ingredients by macronutrients, generate AI meal plans, and streamline grocery shopping.

---

## ✨ Key Features

### 📂 Foldable Categories with Food Group Sub-Tabs
* Comprehensive library of 500+ ingredients grouped into major food groups: **Dairy**, **Fruits**, **Legumes**, **Meat**, **Vegetables**, and **Carbohydrates**.
* Interactive **Food Group Sub-Tabs** at the top of the Categories tab (*All, Dairy, Fruits, Legumes, Meat, Vegetables, Carbohydrates*) for instant navigation.
* Fold (collapse) or unfold (expand) categories with smooth animations to reduce screen clutter and visual overwhelm.
* Custom categories and ingredients can be added, edited, or removed at any time.

### ⭐ 5-Emoji & 5-Star Rating System (Yummy to Yucky)
* Expressive 5-emoji scale alongside 5-star rating:
  * 😋 **5 Stars**: Yummy! (Safe Favorite)
  * 🙂 **4 Stars**: Good (Safe Option)
  * 😐 **3 Stars**: Okay (Neutral)
  * 🙁 **2 Stars**: Unsure (Borderline)
  * 🤢 **1 Star**: Yucky (Not Safe / Disliked)
* Automatically records the exact date and timestamp of the last rating (e.g., *"May 20, 2026 at 14:30"*).
* Attach custom notes (*e.g., "no skin", "cinnamon-free", "extra crisp"*).

### 🛡️ "SafeFood" Highlights Tab
* Dedicated tab featuring your **Best Rated (4-5★)** safe comfort foods and **Latest Rated** history.
* Quick stats summary showing count of 100% safe verified foods.
* Real-time search bar for quick lookup during anxious or overwhelming meal choices.

### 🥗 Nutrient Filter (in Categories & SafeFood)
* Filter both the Categories and SafeFood tabs by nutritional building blocks:
  * 🥩 **Proteins**: Growth, muscle repair & satiety (*Salmon, Chicken Breast, Eggs, Greek Yogurt, Cheddar Cheese*)
  * 🌾 **Fibers**: Digestive health & gut comfort (*Brown Rice, Lettuce, Tomatoes, Oatmeal, Lentils*)
  * 🥔 **Carbohydrates**: Primary energy fuel (*Brown Rice, Sweet Potato, White Rice, Spaghetti*)
  * 🥑 **Healthy Fats**: Brain health & nutrient absorption (*Salmon, Eggs, Whole Milk*)
  * 🍊 **Vitamins & Minerals**: Micronutrients & immunity (*Tomatoes, Lettuce, Fresh Fruits, Vegetables*)
* Multi-select chips (any selected nutrient matches) and a single-tap "All nutrients" reset. The selection is shared between both tabs.
* Visual macronutrient badges remain on every food card.

### 📦 Food Catalog (CSV) & Bild-Resolver
* **500+ Lebensmittel** werden aus `app/src/main/assets/food_catalog.csv` eingelesen (`Food Catalog,Category` – 506 Einträge) statt fest im Code zu stehen.
* Beim ersten Start wird der Katalog automatisch in **6 Kategorien** einsortiert und mit Makronutrient-Tags sowie **Saison-Hinweisen** (All Year / Spring / Summer / Autumn / Winter) angereichert.
* **Stabile IDs** (`food_<slug>`) sorgen dafür, dass Ratings und Notizen auch nach einem Katalog-Update am selben Lebensmittel hängen bleiben.
* **Eigenes Foto für jedes Lebensmittel**: Alle 506 Katalogeinträge (502 Namen) **plus 102 selbst angelegte Lebensmittel** haben ein konkretes Foto (`FoodImageCatalog`). Die Bilder liegen als komprimierte JPEGs in `app/src/main/assets/food_images/` und werden **offline** ausgeliefert – kein Nachladen, keine fehlenden Kacheln, keine Hotlink-Probleme. Geladen wird per Coil mit Crossfade und ruhigem Platzhalter (`FoodThumbnail`).
* **Ruhige, neutrale Motive**: Für Fleisch und heikle Zutaten werden zubereitete Gerichte statt roher Ware, Organe oder Verpackungen gezeigt; botanische Feld-/Blattfotos und Produktpackungen wurden ersetzt. Das reduziert sensorische Reize für empfindliche Nutzer:innen.
* **Neue eigene Lebensmittel** werden automatisch per Namensabgleich (z. B. „Cheddar“ → „Cheddar Cheese“) oder mit einem neutralen Kategorie-Foto versorgt – nie mit einem kaputten oder unpassenden Netz-Bild.
* **Bildnachweise** liegen vollständig in [`CREDITS.md`](CREDITS.md) und maschinenlesbar in `app/src/main/assets/food_image_credits.json`.
* Eigene Kategorien und Lebensmittel lassen sich weiterhin jederzeit ergänzen, bearbeiten oder löschen.

### 🤖 AI Menu Planner
* Generates personalized 1-day, 3-day, or 7-day safe meal plans using your highest rated comfort foods.
* **Real meal composition**: every food is classified into a meal role (grain, protein, dairy, fruit, vegetable, fat) and each slot is built from roles – breakfast gets a breakfast grain or a fruit + dairy plate, lunch and dinner get protein + side + vegetable. Plain vegetables, rice or potatoes can no longer end up as breakfast, and a food never appears twice in the same meal.
* Fruits are never mistaken for vegetables (e.g. pear/peach), and main meals prefer a real vegetable over an aromatic base like onion when one is available.
* Customizable AI planning strategies:
  * *Strict Safe Foods (4-5 Stars)*
  * *Balanced Meals*
  * *Low Energy / Fast Prep*
  * *Safe Food Rotation*
* Optional custom prompt instructions (*e.g., "No dairy, focus on chicken breast and rice"*).
* **1-Click Shopping List Integration**: Tap *"Add Ingredients"* on any planned day to instantly transfer needed items to your grocery list.

### 🍽️ "Potential Meal" Selection
* Every food card in Categories and SafeFood has a **Potential meal** button.
* Marked foods are collected at the top of the AI Menu tab.
* Generated plans prefer these foods; an optional switch restricts planning to them only. The selection is saved with the food data and survives app restarts.

### 🛒 Shopping List with Pantry Tracking ("Already Have")
* Grocery list designed to handle 3 distinct ingredient statuses:
  * 🛒 **Needed**: Items you need to buy at the store.
  * ✅ **In Cart**: Items picked up while shopping.
  * 🏠 **Already Have**: Ingredients you already have in your home pantry, preventing accidental duplicate purchases!
* Single-tap status switcher buttons and quick filter pills (*All, Needed, In Cart, Already Have*).

### 📄 CSV Data Export
* Export food lists, ratings, timestamps, macronutrient tags, and shopping lists into standard CSV format.
* Includes a live CSV code preview, 1-click **Share CSV** (Android Share Sheet to save as file or send via email/messaging), and **Copy Text** to clipboard.

---

## 🛠️ Architecture & Tech Stack

* **Language**: [Kotlin](https://kotlinlang.org/)
* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3
* **Architecture Pattern**: MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF)
* **Asynchronous Flow**: Kotlin Coroutines & `StateFlow`
* **Navigation**: Jetpack Navigation Compose
* **Data Serialization & Storage**: `kotlinx.serialization` JSON persistent disk repository
* **Build System**: Gradle Kotlin DSL with Version Catalogs (`libs.versions.toml`)
* **Testing**: JUnit 4 unit test suite

---

## 📁 Project Structure

```
com.example.safefood/
├── data/
│   ├── model/
│   │   └── FoodModels.kt          # FoodItem, Category, ShoppingItem, NutrientType, MealPlan
│   ├── catalog/
│   │   ├── FoodCatalogParser.kt   # CSV-Parser, Kategorie-/Nährwert-/Saison-Zuordnung
│   │   ├── FoodImageCatalog.kt    # Generierte Zuordnung Lebensmittel -> eingebettetes Foto
│   │   └── FoodImageResolver.kt   # Bild-Auflösung pro Lebensmittel (Coil)
│   ├── repository/
│   │   └── SafeFoodRepository.kt  # JSON persistent storage & StateFlow streams
│   └── export/
│       └── CsvExporter.kt         # CSV formatting & escape logic
│   └── assets/
│       ├── food_catalog.csv       # Auslieferbarer Lebensmittelkatalog (500+ Einträge)
│       └── food_images/           # 604 eingebettete Lebensmittelfotos + 6 Kategorie-Fallbacks (offline)
├── ai/
│   └── AiMenuPlanner.kt           # Rollenbasierter Meal-Composer & Zutaten-Extraktion
├── ui/
│   ├── components/
│   │   ├── StarRatingBar.kt       # Interactive 5-star rating composable with emojis
│   │   ├── NutrientUi.kt          # Nutrient badges + shared filter chips
│   │   ├── PotentialMealButton.kt # "Potential meal" toggle on food cards
│   │   └── FoodThumbnail.kt       # Offline-Bild mit Crossfade & Platzhalter
│   ├── screens/
│   │   ├── categories/
│   │   │   └── CategoriesScreen.kt  # Food Group sub-tabs & foldable categories UI
│   │   ├── safefood/
│   │   │   └── SafeFoodScreen.kt    # Best & latest rated safe foods UI
│   │   ├── menuplanner/
│   │   │   └── MenuPlannerScreen.kt # AI Menu Planning UI + potential meal selection
│   │   ├── shopping/
│   │   │   └── ShoppingListScreen.kt# Shopping list & pantry tracking UI
│   │   └── export/
│   │       └── ExportScreen.kt      # CSV export & sharing UI
│   ├── navigation/
│   │   └── NavGraph.kt            # Bottom navigation bar & routing
│   └── SafeFoodViewModel.kt       # Core ViewModel handling business logic & UI state
└── MainActivity.kt                # Main activity entry point
```

---

## 🚀 Getting Started

### Prerequisites
* **Android Studio**: Ladybug (2024.2.1+) or newer
* **JDK**: Java 11 or higher
* **Android SDK**: Compile SDK 37, Min SDK 24 (Android 7.0 Nougat)

### Building & Running
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/SafeFood.git
   cd SafeFood
   ```
2. Open the project in Android Studio.
3. Build the project using Gradle:
   ```bash
   ./gradlew assembleDebug
   ```
4. Run unit tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```
5. Deploy to an Android emulator or physical device.

---

## 📜 License
Distributed under the MIT License. See [`LICENSE`](LICENSE) for more details.

## 🖼️ Bildnachweise
Die Lebensmittel-Fotos stammen aus Wikimedia Commons bzw. den Wikipedia-Artikeln und stehen
unter freien Lizenzen (CC BY, CC BY-SA, CC0 oder Public Domain). Die vollständige Liste mit
Quelle, Autor und Lizenz je Lebensmittel steht in [`CREDITS.md`](CREDITS.md).
