# 🥑 SafeFood — Food & Safe Ingredient Tracker for Sensory-Sensitive Eaters

**SafeFood** is a native Android app for autistic individuals, sensory-sensitive eaters, and their caregivers or dietitians. It offers a calm, low-cognitive-load way to categorize, rate, and track safe foods, filter by nutrients, generate meal plans, cook from portion-adjusted recipes, and manage groceries.

All data stays on the device. There is no account, no cloud sync, and no network connection required for the food catalog or photos.

---

## 📥 Download & Install

Download the latest debug APK directly:

**[SafeFood-debug.apk](https://github.com/trenits-cmd/SafeFood/releases/latest/download/SafeFood-debug.apk)**

Every push to `main` is built by GitHub Actions. The workflow also uploads the APK as a `SafeFood-debug` artifact on the [Actions](https://github.com/trenits-cmd/SafeFood/actions) tab.

> The APK is a debug build. Android may ask you to allow installation from your browser or file manager. Enable that permission for the installing app only.

---

## ✨ Key Features

### 📂 Foldable Categories & Food Group Tabs

* 542 built-in foods grouped into **Dairy**, **Fruits**, **Legumes**, **Meat**, **Vegetables**, **Carbohydrates**, and **Pantry & Spices**.
* A red **Blacklist** tab sits directly after **All Groups**, so it is always visible without horizontal scrolling.
* Food-group sub-tabs (*All Groups, Blacklist, Dairy, Fruits, Legumes, Meat, Vegetables, Carbohydrates, Pantry & Spices*) jump straight to a group.
* Categories can be collapsed or expanded to reduce visual clutter.
* Season tabs (**All Seasons, Spring, Summer, Autumn**) filter foods by availability and are shared with the SafeFood tab.
* Foods can be added, edited, deleted, searched, and filtered. Default groups and the Blacklist are protected from accidental deletion.

### 🚫 Blacklist

* Rating a food **1 star** moves it automatically to the **Blacklist**.
* Blacklisted foods are excluded from meal planning and recommendations and do not appear inside the other category groups.
* Rating a blacklisted food **2–5 stars** restores it to the group it came from.
* The Blacklist tab shows the full count, even while nutrient or season filters are active.

### ⭐ 5-Emoji & 5-Star Rating System

* Expressive 5-emoji scale alongside the 5-star rating:
  * 😋 **5 Stars** — Yummy! (Safe favorite)
  * 🙂 **4 Stars** — Good (Safe option)
  * 😐 **3 Stars** — Okay (Neutral)
  * 🙁 **2 Stars** — Unsure (Borderline)
  * 🤢 **1 Star** — Yucky (Not safe / disliked)
* Records the exact date and timestamp of the last rating.
* Custom notes per food (*e.g. "no skin", "cinnamon-free", "extra crisp"*).
* Large food photo and rating controls are available when tapping a food card.

### 🛡️ SafeFood Highlights Tab

* Shows your **best-rated (4–5★)** safe foods and your **latest rated** history.
* Quick stats summarize how many foods are verified safe.
* Token-based search ignores word order and plurals: `"red onion"` finds **Onion (Red)**.
* Shares the nutrient and season filters with the Categories tab.

### 🥗 Nutrient Filter (Categories & SafeFood)

Filter both tabs by nutritional building blocks:

* 🥩 **Proteins** — growth, muscle repair, satiety
* 🌾 **Fibers** — digestive health and steady energy
* 🥔 **Carbohydrates** — primary energy fuel
* 🥑 **Healthy Fats** — brain, cell, and hormone health
* 🍊 **Vitamins & Minerals** — micronutrients and immunity

Multi-select chips use OR logic (any selected nutrient matches), and **All nutrients** resets the filter. The filter selection is shared between both tabs.

### 📦 Food Catalog & Offline Images

* The catalog lives in `app/src/main/assets/food_catalog.csv` (542 rows) instead of being hard-coded.
* Stable IDs (`food_<slug>`) keep ratings and notes attached to the same food after a catalog update.
* **669 bundled images** cover 641 foods, 21 cooked dishes, and 7 category fallbacks. Everything ships in `app/src/main/assets/food_images/` and loads offline through Coil — no hotlinks, no empty tiles, no network dependency.
* Neutral, calm imagery is preferred, especially for meat and sensitive ingredients: cooked dishes instead of raw organs, packaging, or botanical field photos.
* New custom foods are matched by name or given a neutral category image.
* Image licenses and sources are documented in [`CREDITS.md`](CREDITS.md) and `app/src/main/assets/food_image_credits.json`.

### 🤖 Smart Meal Planner (Offline)

* Generates 1-day, 3-day, or 7-day plans from your rated foods. The planner is rule-based and runs offline; no prompt or food list leaves the device.
* Every food is classified into a meal role: grain, protein, dairy, fruit, vegetable, or fat.
* Breakfast uses a breakfast grain or a fruit + dairy plate. Lunch and dinner combine protein + side + vegetable.
* Plain vegetables, rice, or potatoes cannot become breakfast.
* Fruits are not mistaken for vegetables (pear/peach), and main meals prefer a real vegetable over an aromatic base such as onion.
* Planning strategies:
  * *Strict Safe Foods (4–5 Stars)*
  * *Balanced Meals*
  * *Low Energy / Fast Prep*
  * *Safe Food Rotation*
* Optional custom instructions (*e.g. "No dairy, focus on chicken breast and rice"*).
* **Generate Fresh Menu** clears old plans and saves the new plan only after it was created successfully. **Clear All** removes every saved plan.
* Each meal slot gets a dish name and a large photo of the finished dish (e.g. **Turkey Rice Bowl**).
* **Add Ingredients** transfers a day’s ingredients to the shopping list.

### 📖 Recipes, Portions & Cooking Instructions

* Tap a meal slot or its dish photo to open a **full-screen recipe** with a large dish photo, prep/cook times, ingredients, and numbered steps.
* Set portions from **1 to 12**; ingredient amounts scale automatically.
* Instructions are written per dish family — grain bowl, pasta, potato plate, egg toast, oatmeal, pancakes, yogurt bowl, cheese/bean snack, veggie sticks, and more.
* Steps mention the actual foods in the meal instead of repeating one generic text for every dish.
* Recipe data is derived from the meal plan, so older saved plans also get the improved recipe view.

### 🍽️ “Potential Meal” Selection

* Every food card has a **Potential meal** toggle.
* Marked foods are collected at the top of the Menu tab.
* Generated plans prefer these foods. An optional switch restricts planning to them only.
* The selection is stored with the food and survives restarts.

### 🛒 Shopping List with Pantry Tracking

Three ingredient states keep grocery shopping predictable:

* 🛒 **Needed** — buy at the store
* ✅ **In Cart** — already picked up
* 🏠 **Already Have** — in your pantry, so it is not bought twice

Single-tap status buttons and filter pills (*All, Needed, In Cart, Already Have*) keep the list short. Completed items can be cleared in one action.

### 📄 Full CSV Export & Import

* Export foods, ratings, timestamps, notes, nutrients, seasons, image URLs, potential-meal flags, blacklist state, and previous category IDs.
* Export the shopping list separately.
* Live CSV preview, **Share CSV** through the Android share sheet, and **Copy Text** to the clipboard.
* **Import CSV** from the same screen:
  * Foods are matched by **ID first, then name**.
  * Importing the same file twice updates ratings and notes instead of creating duplicates.
  * Missing groups are created automatically.
  * Foods marked as blacklisted in the file are placed in the Blacklist group.
* Malformed rows are skipped, so one bad line does not stop the rest of the import.

---

## 🛠️ Architecture & Tech Stack

* **Language:** Kotlin 2.2.10
* **UI:** Jetpack Compose with Material Design 3 (BOM 2026.02.01)
* **Architecture:** MVVM with unidirectional data flow
* **State:** Kotlin Coroutines and `StateFlow`
* **Navigation:** Jetpack Navigation Compose 2.8.8
* **Storage:** `kotlinx.serialization` JSON files in the app’s internal storage
* **Images:** Coil 2.7.0 with bundled offline JPEG assets
* **Build:** Gradle Kotlin DSL, version catalogs, AGP 9.4.1
* **Tests:** JUnit 4 — 43 passing unit tests

### Local data files

| File | Contents |
|---|---|
| `categories.json` | Food groups and UI expansion state |
| `foods.json` | Foods, ratings, notes, nutrients, images, blacklist metadata |
| `shopping.json` | Shopping list and item status |
| `meal_plans.json` | Saved meal plans |
| `meta.json` | Catalog version used for migrations |

Each file is written atomically with a `.bak` copy.

---

## 📁 Project Structure

```text
com.example.safefood/
├── data/
│   ├── model/
│   │   └── FoodModels.kt              # FoodItem, FoodCategory, ratings, nutrients, meal plans
│   ├── catalog/
│   │   ├── FoodCatalogParser.kt       # CSV catalog parser, category/nutrient/season mapping
│   │   ├── FoodSearch.kt              # Token search with plural handling
│   │   ├── FoodImageCatalog.kt        # Food -> bundled image mapping
│   │   ├── FoodImageResolver.kt       # Image lookup and fallbacks
│   │   └── MealDishResolver.kt        # Meal text -> dish title and cooked-dish photo
│   ├── repository/
│   │   └── SafeFoodRepository.kt      # Atomic JSON storage, catalog migrations, import merge
│   └── export/
│       ├── CsvExporter.kt             # Full metadata CSV export
│       └── CsvImporter.kt             # Quoted-field CSV parser and import result
├── ai/
│   ├── AiMenuPlanner.kt               # Role-based meal composition
│   ├── MealRecipe.kt                  # Recipe and ingredient data classes
│   └── RecipeGenerator.kt             # Dish-specific instructions and portion scaling
├── ui/
│   ├── components/
│   │   ├── StarRatingBar.kt           # Interactive 5-star rating with emojis
│   │   ├── EmojiPreferenceBar.kt      # Emoji-based rating row
│   │   ├── SensoryTagChip.kt          # Sensory / preference tags
│   │   ├── NutrientUi.kt              # Nutrient badges and shared filter chips
│   │   ├── PotentialMealButton.kt     # "Potential meal" toggle
│   │   ├── FoodDetailDialog.kt        # Large photo + rating dialog
│   │   └── FoodThumbnail.kt           # Offline thumbnail with placeholder
│   ├── screens/
│   │   ├── categories/CategoriesScreen.kt
│   │   ├── safefood/SafeFoodScreen.kt
│   │   ├── menuplanner/
│   │   │   ├── MenuPlannerScreen.kt
│   │   │   └── RecipeDialog.kt        # Full-screen recipe view
│   │   ├── shopping/ShoppingListScreen.kt
│   │   └── export/ExportScreen.kt     # CSV export and import UI
│   ├── navigation/NavGraph.kt         # Bottom navigation and routes
│   └── SafeFoodViewModel.kt           # UI state and business logic
└── MainActivity.kt
```

---

## 🚀 Getting Started

### Prerequisites

* **Android Studio** with support for **AGP 9.4.1**
* **JDK 17** (the project targets Java 11 bytecode)
* **Android SDK:** compile SDK 37, target SDK 35, min SDK 24 (Android 7.0)
* An Android device or emulator running Android 7.0 or newer

### Build & test

```bash
git clone https://github.com/trenits-cmd/SafeFood.git
cd SafeFood
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### GitHub Actions

`.github/workflows/build-apk.yml` runs on every push to `main` and on manual dispatch. It:

1. Sets up JDK 17.
2. Builds `assembleDebug`.
3. Uploads the `SafeFood-debug` artifact.
4. Publishes/updates the **latest** GitHub Release with `SafeFood-debug.apk`.

---

## 🔒 Privacy

* No account, analytics, ads, or tracking SDKs.
* Food data, ratings, notes, plans, and shopping lists are stored only in the app’s internal storage.
* The catalog and all 669 images are bundled with the APK.
* CSV export/import is manual; nothing is uploaded automatically.

---

## 📜 License

Distributed under the MIT License. See [`LICENSE`](LICENSE) for details.

## 🖼️ Image Credits

Food and dish photos come from Wikimedia Commons/Wikipedia and are used under free licenses (CC BY, CC BY-SA, CC0, or public domain). The full source, author, and license list is in [`CREDITS.md`](CREDITS.md).
