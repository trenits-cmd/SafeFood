# 🥑 SafeFood - Sensory & Safe Food Tracker for Autistic Individuals

**SafeFood** is a native Android application designed specifically for autistic individuals, sensory-sensitive eaters, and their caregivers or dietitians. It provides an intuitive, low-cognitive-load environment to categorize, rate, track, and plan safe foods, organize ingredients by macronutrients, generate AI meal plans, and streamline grocery shopping.

---

## ✨ Key Features

### 📂 Foldable & Expandable Categories
* Organize basic ingredients (*Salmon, Tomatoes, Brown Rice, Lettuce, etc.*) and prepared meals into sensory categories.
* Fold (collapse) or unfold (expand) categories with smooth animations to reduce screen clutter and visual overwhelm.
* Custom categories and ingredients can be added, edited, or removed at any time.

### ⭐ 5-Star Rating System & Date Tracking
* Interactive 5-star rating system: tap stars to immediately rate or update a food item's comfort level.
* Automatically records the exact date and timestamp of the last rating (e.g., *"May 20, 2026 at 14:30"*).
* Attach custom sensory notes (*e.g., "no skin", "cinnamon-free", "extra crisp"*) and texture tags (*Crunchy, Smooth, Soft, Bland, Warm, Cold, Salty, Sweet, Fresh*).

### 🛡️ "SafeFood" Highlights Tab
* Dedicated tab featuring your **Best Rated (4-5★)** safe comfort foods and **Latest Rated** history.
* Quick stats summary showing count of 100% safe verified foods.
* Real-time search bar for quick lookup during anxious or overwhelming meal choices.

### 🥗 Nutrients Tab (Macronutrient Classification)
* Sort and filter foods by nutritional building blocks:
  * 🥩 **Proteins**: Growth, muscle repair & satiety (*Salmon, Chicken Breast, Eggs, Greek Yogurt*)
  * 🌾 **Fibers**: Digestive health & gut comfort (*Brown Rice, Lettuce, Tomatoes, Oatmeal*)
  * 🥔 **Carbohydrates**: Primary energy fuel (*Brown Rice, Sweet Potato, Saltine Crackers*)
  * 🥑 **Healthy Fats**: Brain health & nutrient absorption (*Salmon, Eggs*)
  * 🍊 **Vitamins & Minerals**: Micronutrients & immunity (*Tomatoes, Lettuce, Fresh Fruit*)
* Visual macronutrient badges on food cards and educational banners.

### 🤖 AI Menu Planner
* Generates personalized 1-day, 3-day, or 7-day safe meal plans using your highest rated comfort foods.
* Customizable AI planning strategies:
  * *Strict Safe Foods (4-5 Stars)*
  * *Texture Variety*
  * *Low Energy / Fast Prep*
  * *Safe Food Rotation*
* Optional custom prompt instructions (*e.g., "Only dry crisp snacks, no dairy"*).
* **1-Click Shopping List Integration**: Tap *"Add Ingredients"* on any planned day to instantly transfer needed items to your grocery list.

### 🛒 Shopping List with Pantry Tracking ("Already Have")
* Grocery list designed to handle 3 distinct ingredient statuses:
  * 🛒 **Needed**: Items you need to buy at the store.
  * ✅ **In Cart**: Items picked up while shopping.
  * 🏠 **Already Have**: Ingredients you already have in your home pantry, preventing accidental duplicate purchases!
* Single-tap status switcher buttons and quick filter pills (*All, Needed, In Cart, Already Have*).

### 📄 CSV Data Export
* Export food lists, ratings, timestamps, texture tags, and shopping lists into standard CSV format.
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
│   ├── repository/
│   │   └── SafeFoodRepository.kt  # JSON persistent storage & StateFlow streams
│   └── export/
│       └── CsvExporter.kt         # CSV formatting & escape logic
├── ai/
│   └── AiMenuPlanner.kt           # AI meal plan generator & ingredient extraction engine
├── ui/
│   ├── components/
│   │   ├── StarRatingBar.kt       # Interactive 5-star rating composable
│   │   └── SensoryTagChip.kt      # Color-coded sensory texture chips
│   ├── screens/
│   │   ├── categories/
│   │   │   └── CategoriesScreen.kt  # Foldable/expandable categories UI
│   │   ├── safefood/
│   │   │   └── SafeFoodScreen.kt    # Best & latest rated safe foods UI
│   │   ├── nutrients/
│   │   │   └── NutrientsScreen.kt   # Macronutrient sorting UI
│   │   ├── menuplanner/
│   │   │   └── MenuPlannerScreen.kt # AI Menu Planning UI
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
Distributed under the MIT License. See `LICENSE` for more details.
