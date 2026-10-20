package com.example.safefood.data.repository

import android.content.Context
import com.example.safefood.data.model.FoodCategory
import com.example.safefood.data.model.FoodItem
import com.example.safefood.data.model.MealPlan
import com.example.safefood.data.model.NutrientType
import com.example.safefood.data.model.ShoppingItem
import com.example.safefood.data.model.ShoppingItemStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class SafeFoodRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val categoriesFile = File(context.filesDir, "categories.json")
    private val foodsFile = File(context.filesDir, "foods.json")
    private val shoppingFile = File(context.filesDir, "shopping.json")
    private val mealPlansFile = File(context.filesDir, "meal_plans.json")

    private val _categories = MutableStateFlow<List<FoodCategory>>(emptyList())
    val categories: StateFlow<List<FoodCategory>> = _categories.asStateFlow()

    private val _foods = MutableStateFlow<List<FoodItem>>(emptyList())
    val foods: StateFlow<List<FoodItem>> = _foods.asStateFlow()

    private val _shoppingList = MutableStateFlow<List<ShoppingItem>>(emptyList())
    val shoppingList: StateFlow<List<ShoppingItem>> = _shoppingList.asStateFlow()

    private val _mealPlans = MutableStateFlow<List<MealPlan>>(emptyList())
    val mealPlans: StateFlow<List<MealPlan>> = _mealPlans.asStateFlow()

    suspend fun loadData() = withContext(Dispatchers.IO) {
        val loadedCategories = readListFromFile<FoodCategory>(categoriesFile)
        val loadedFoods = readListFromFile<FoodItem>(foodsFile)
        val loadedShopping = readListFromFile<ShoppingItem>(shoppingFile)
        val loadedMealPlans = readListFromFile<MealPlan>(mealPlansFile)

        if (loadedCategories.isEmpty()) {
            val initialCategories = getInitialCategories()
            val initialFoods = getInitialFoods(initialCategories)
            _categories.value = initialCategories
            _foods.value = initialFoods
            saveCategoriesToDisk(initialCategories)
            saveFoodsToDisk(initialFoods)
        } else {
            _categories.value = loadedCategories
            _foods.value = loadedFoods
        }

        if (loadedShopping.isEmpty()) {
            val initialShopping = getInitialShopping()
            _shoppingList.value = initialShopping
            saveShoppingToDisk(initialShopping)
        } else {
            _shoppingList.value = loadedShopping
        }

        _mealPlans.value = loadedMealPlans
    }

    private inline fun <reified T> readListFromFile(file: File): List<T> {
        return try {
            if (file.exists()) {
                val content = file.readText()
                json.decodeFromString<List<T>>(content)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun saveCategoriesToDisk(list: List<FoodCategory>) = withContext(Dispatchers.IO) {
        try {
            categoriesFile.writeText(json.encodeToString(list))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun saveFoodsToDisk(list: List<FoodItem>) = withContext(Dispatchers.IO) {
        try {
            foodsFile.writeText(json.encodeToString(list))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun saveShoppingToDisk(list: List<ShoppingItem>) = withContext(Dispatchers.IO) {
        try {
            shoppingFile.writeText(json.encodeToString(list))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun saveMealPlansToDisk(list: List<MealPlan>) = withContext(Dispatchers.IO) {
        try {
            mealPlansFile.writeText(json.encodeToString(list))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Category Actions ---
    suspend fun toggleCategoryExpanded(categoryId: String) {
        val updated = _categories.value.map {
            if (it.id == categoryId) it.copy(isExpanded = !it.isExpanded) else it
        }
        _categories.value = updated
        saveCategoriesToDisk(updated)
    }

    suspend fun addCategory(name: String, description: String = "") {
        val newCategory = FoodCategory(name = name, description = description)
        val updated = _categories.value + newCategory
        _categories.value = updated
        saveCategoriesToDisk(updated)
    }

    suspend fun deleteCategory(categoryId: String) {
        val updatedCategories = _categories.value.filter { it.id != categoryId }
        val updatedFoods = _foods.value.filter { it.categoryId != categoryId }
        _categories.value = updatedCategories
        _foods.value = updatedFoods
        saveCategoriesToDisk(updatedCategories)
        saveFoodsToDisk(updatedFoods)
    }

    // --- Food Actions ---
    suspend fun rateFood(foodId: String, newRating: Int) {
        val now = System.currentTimeMillis()
        val updated = _foods.value.map { food ->
            if (food.id == foodId) {
                food.copy(rating = newRating, lastRatedTimestamp = now)
            } else food
        }
        _foods.value = updated
        saveFoodsToDisk(updated)
    }

    suspend fun addFoodItem(
        categoryId: String,
        name: String,
        notes: String = "",
        textureTags: List<String> = emptyList(),
        nutrientTags: List<NutrientType> = emptyList(),
        initialRating: Int = 0
    ) {
        val now = if (initialRating > 0) System.currentTimeMillis() else null
        val newItem = FoodItem(
            categoryId = categoryId,
            name = name,
            rating = initialRating,
            lastRatedTimestamp = now,
            notes = notes,
            textureTags = textureTags,
            nutrientTags = nutrientTags
        )
        val updated = _foods.value + newItem
        _foods.value = updated
        saveFoodsToDisk(updated)
    }

    suspend fun updateFoodItem(updatedItem: FoodItem) {
        val updated = _foods.value.map { if (it.id == updatedItem.id) updatedItem else it }
        _foods.value = updated
        saveFoodsToDisk(updated)
    }

    suspend fun deleteFoodItem(foodId: String) {
        val updated = _foods.value.filter { it.id != foodId }
        _foods.value = updated
        saveFoodsToDisk(updated)
    }

    // --- Shopping List Actions ---
    suspend fun updateShoppingStatus(itemId: String, newStatus: ShoppingItemStatus) {
        val updated = _shoppingList.value.map {
            if (it.id == itemId) it.copy(status = newStatus) else it
        }
        _shoppingList.value = updated
        saveShoppingToDisk(updated)
    }

    suspend fun addShoppingItem(name: String, category: String = "General", sourceMeal: String? = null) {
        if (name.isBlank()) return
        val newItem = ShoppingItem(name = name.trim(), category = category, sourceMeal = sourceMeal)
        val updated = _shoppingList.value + newItem
        _shoppingList.value = updated
        saveShoppingToDisk(updated)
    }

    suspend fun deleteShoppingItem(itemId: String) {
        val updated = _shoppingList.value.filter { it.id != itemId }
        _shoppingList.value = updated
        saveShoppingToDisk(updated)
    }

    suspend fun clearCompletedShoppingItems() {
        val updated = _shoppingList.value.filter { it.status == ShoppingItemStatus.NEEDED }
        _shoppingList.value = updated
        saveShoppingToDisk(updated)
    }

    // --- Meal Plan Actions ---
    suspend fun saveMealPlan(mealPlan: MealPlan) {
        val updated = listOf(mealPlan) + _mealPlans.value
        _mealPlans.value = updated
        saveMealPlansToDisk(updated)
    }

    suspend fun addIngredientsToShoppingList(ingredients: List<String>, sourceMeal: String) {
        val currentNames = _shoppingList.value.map { it.name.lowercase() }.toSet()
        val newItems = ingredients
            .map { it.trim() }
            .filter { it.isNotEmpty() && !currentNames.contains(it.lowercase()) }
            .map { ShoppingItem(name = it, sourceMeal = sourceMeal, status = ShoppingItemStatus.NEEDED) }

        if (newItems.isNotEmpty()) {
            val updated = _shoppingList.value + newItems
            _shoppingList.value = updated
            saveShoppingToDisk(updated)
        }
    }

    // --- Initial Default Data Seed ---
    private fun getInitialCategories(): List<FoodCategory> {
        return listOf(
            FoodCategory(id = "cat_1", name = "Fresh Ingredients", description = "Simple single-ingredient whole foods (Salmon, Tomatoes, Lettuce, etc.)"),
            FoodCategory(id = "cat_2", name = "Dry & Crunchy", description = "Low predictability risk, crispy, consistent texture"),
            FoodCategory(id = "cat_3", name = "Warm & Plain", description = "Simple warm comfort staples (Brown Rice, Butter Noodles, etc.)"),
            FoodCategory(id = "cat_4", name = "Smooth & Soft", description = "No lumps, uniform smooth consistency"),
            FoodCategory(id = "cat_5", name = "Drinks & Hydration", description = "Safe liquid sensory hydration options")
        )
    }

    private fun getInitialFoods(categories: List<FoodCategory>): List<FoodItem> {
        val c1 = categories.getOrNull(0)?.id ?: "cat_1"
        val c2 = categories.getOrNull(1)?.id ?: "cat_2"
        val c3 = categories.getOrNull(2)?.id ?: "cat_3"
        val c4 = categories.getOrNull(3)?.id ?: "cat_4"
        val c5 = categories.getOrNull(4)?.id ?: "cat_5"

        val now = System.currentTimeMillis()
        val dayMillis = 86400000L

        return listOf(
            // Basic Single Ingredients
            FoodItem(
                categoryId = c1,
                name = "Salmon",
                rating = 5,
                lastRatedTimestamp = now - (dayMillis * 1),
                notes = "Pan-seared or baked, rich in protein & omega-3",
                textureTags = listOf("Soft", "Warm", "Savory"),
                nutrientTags = listOf(NutrientType.PROTEIN, NutrientType.HEALTHY_FAT)
            ),
            FoodItem(
                categoryId = c1,
                name = "Tomatoes",
                rating = 4,
                lastRatedTimestamp = now - (dayMillis * 2),
                notes = "Sliced fresh or cherry tomatoes, rich in vitamin C",
                textureTags = listOf("Fresh", "Juicy"),
                nutrientTags = listOf(NutrientType.VITAMINS_MINERALS, NutrientType.FIBER)
            ),
            FoodItem(
                categoryId = c1,
                name = "Lettuce",
                rating = 4,
                lastRatedTimestamp = now - (dayMillis * 3),
                notes = "Crisp Romaine or iceberg leaves",
                textureTags = listOf("Crunchy", "Fresh"),
                nutrientTags = listOf(NutrientType.FIBER, NutrientType.VITAMINS_MINERALS)
            ),
            FoodItem(
                categoryId = c1,
                name = "Chicken Breast",
                rating = 5,
                lastRatedTimestamp = now - (dayMillis * 1),
                notes = "Plain grilled or boiled, high clean protein",
                textureTags = listOf("Warm", "Savory"),
                nutrientTags = listOf(NutrientType.PROTEIN)
            ),
            FoodItem(
                categoryId = c1,
                name = "Eggs",
                rating = 5,
                lastRatedTimestamp = now - (dayMillis * 2),
                notes = "Scrambled or hard-boiled",
                textureTags = listOf("Soft", "Warm"),
                nutrientTags = listOf(NutrientType.PROTEIN, NutrientType.HEALTHY_FAT)
            ),

            // Category 2: Dry & Crunchy
            FoodItem(
                categoryId = c2,
                name = "Saltine Crackers",
                rating = 5,
                lastRatedTimestamp = now - (dayMillis * 1),
                notes = "Always crisp and consistent",
                textureTags = listOf("Crunchy", "Dry", "Bland"),
                nutrientTags = listOf(NutrientType.CARBOHYDRATE)
            ),
            FoodItem(
                categoryId = c2,
                name = "Pretzels",
                rating = 5,
                lastRatedTimestamp = now - (dayMillis * 2),
                notes = "Salty and crunch-safe",
                textureTags = listOf("Crunchy", "Salty"),
                nutrientTags = listOf(NutrientType.CARBOHYDRATE)
            ),

            // Category 3: Warm & Plain
            FoodItem(
                categoryId = c3,
                name = "Brown Rice",
                rating = 5,
                lastRatedTimestamp = now - (dayMillis * 1),
                notes = "Steamed brown rice, wholesome fiber & complex carbs",
                textureTags = listOf("Warm", "Soft", "Bland"),
                nutrientTags = listOf(NutrientType.CARBOHYDRATE, NutrientType.FIBER)
            ),
            FoodItem(
                categoryId = c3,
                name = "Chicken Nuggets",
                rating = 5,
                lastRatedTimestamp = now - (dayMillis * 2),
                notes = "Crispy coating, familiar safe brand",
                textureTags = listOf("Warm", "Crispy", "Savory"),
                nutrientTags = listOf(NutrientType.PROTEIN, NutrientType.CARBOHYDRATE)
            ),
            FoodItem(
                categoryId = c3,
                name = "Sweet Potato",
                rating = 4,
                lastRatedTimestamp = now - (dayMillis * 3),
                notes = "Baked sweet potato, high fiber and beta-carotene",
                textureTags = listOf("Warm", "Soft", "Sweet"),
                nutrientTags = listOf(NutrientType.CARBOHYDRATE, NutrientType.FIBER, NutrientType.VITAMINS_MINERALS)
            ),

            // Category 4: Smooth & Soft
            FoodItem(
                categoryId = c4,
                name = "Smooth Applesauce",
                rating = 5,
                lastRatedTimestamp = now - (dayMillis * 1),
                notes = "Cinnamon-free, completely smooth",
                textureTags = listOf("Smooth", "Cold", "Sweet"),
                nutrientTags = listOf(NutrientType.CARBOHYDRATE, NutrientType.FIBER)
            ),
            FoodItem(
                categoryId = c4,
                name = "Plain Greek Yogurt",
                rating = 4,
                lastRatedTimestamp = now - (dayMillis * 2),
                notes = "High protein smooth yogurt",
                textureTags = listOf("Smooth", "Cold"),
                nutrientTags = listOf(NutrientType.PROTEIN)
            ),
            FoodItem(
                categoryId = c4,
                name = "Oatmeal",
                rating = 4,
                lastRatedTimestamp = now - (dayMillis * 3),
                notes = "Warm rolled oats with light honey",
                textureTags = listOf("Warm", "Soft"),
                nutrientTags = listOf(NutrientType.FIBER, NutrientType.CARBOHYDRATE)
            ),

            // Category 5: Drinks & Hydration
            FoodItem(
                categoryId = c5,
                name = "Ice Cold Water",
                rating = 5,
                lastRatedTimestamp = now - (dayMillis * 1),
                notes = "Filtered water with ice",
                textureTags = listOf("Liquid", "Cold", "Refreshing"),
                nutrientTags = listOf(NutrientType.VITAMINS_MINERALS)
            )
        )
    }

    private fun getInitialShopping(): List<ShoppingItem> {
        return listOf(
            ShoppingItem(name = "Salmon", category = "Fresh", status = ShoppingItemStatus.NEEDED),
            ShoppingItem(name = "Brown Rice", category = "Pantry", status = ShoppingItemStatus.ALREADY_HAVE),
            ShoppingItem(name = "Tomatoes", category = "Produce", status = ShoppingItemStatus.NEEDED),
            ShoppingItem(name = "Lettuce", category = "Produce", status = ShoppingItemStatus.IN_CART),
            ShoppingItem(name = "Chicken Breast", category = "Meat", status = ShoppingItemStatus.ALREADY_HAVE)
        )
    }
}
