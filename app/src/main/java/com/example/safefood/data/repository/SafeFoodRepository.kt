package com.example.safefood.data.repository

import android.content.Context
import com.example.safefood.data.catalog.FoodCatalogParser
import com.example.safefood.data.catalog.FoodImageResolver
import com.example.safefood.data.model.FoodCategory
import com.example.safefood.data.model.FoodItem
import com.example.safefood.data.model.FoodRatings
import com.example.safefood.data.model.MealPlan
import com.example.safefood.data.model.NutrientType
import com.example.safefood.data.model.ShoppingItem
import com.example.safefood.data.model.ShoppingItemStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class SafeFoodRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val mutex = Mutex()

    private val categoriesFile = File(context.filesDir, "categories.json")
    private val foodsFile = File(context.filesDir, "foods.json")
    private val shoppingFile = File(context.filesDir, "shopping.json")
    private val mealPlansFile = File(context.filesDir, "meal_plans.json")
    private val metaFile = File(context.filesDir, "meta.json")

    private val _categories = MutableStateFlow<List<FoodCategory>>(emptyList())
    val categories: StateFlow<List<FoodCategory>> = _categories.asStateFlow()

    private val _foods = MutableStateFlow<List<FoodItem>>(emptyList())
    val foods: StateFlow<List<FoodItem>> = _foods.asStateFlow()

    private val _shoppingList = MutableStateFlow<List<ShoppingItem>>(emptyList())
    val shoppingList: StateFlow<List<ShoppingItem>> = _shoppingList.asStateFlow()

    private val _mealPlans = MutableStateFlow<List<MealPlan>>(emptyList())
    val mealPlans: StateFlow<List<MealPlan>> = _mealPlans.asStateFlow()

    suspend fun loadData() = mutex.withLock {
        withContext(Dispatchers.IO) {
            val meta = readMeta()
            val loadedCategories = readListFromFile<FoodCategory>(categoriesFile)
            val loadedFoods = readListFromFile<FoodItem>(foodsFile)
            val loadedShopping = readListFromFile<ShoppingItem>(shoppingFile)
            val loadedMealPlans = readListFromFile<MealPlan>(mealPlansFile)

            val categories = loadedCategories.ifEmpty { FoodCatalogParser.defaultCategories }
            val catalogFoods = loadCatalogFoods(categories)
            val foods = mergeCatalog(
                existing = loadedFoods,
                catalog = catalogFoods,
                categories = categories,
                refreshImages = loadedFoods.isEmpty() || meta.catalogVersion < CATALOG_VERSION
            )

            _categories.value = categories
            _foods.value = foods
            _shoppingList.value = loadedShopping
            _mealPlans.value = loadedMealPlans

            if (loadedCategories.isEmpty()) saveCategoriesToDisk(categories)
            if (loadedFoods.isEmpty() || meta.catalogVersion < CATALOG_VERSION) {
                saveFoodsToDisk(foods)
                saveMeta(AppMeta(catalogVersion = CATALOG_VERSION))
            }
        }
    }

    private fun loadCatalogFoods(categories: List<FoodCategory>): List<FoodItem> {
        val csv = context.assets.open(CATALOG_ASSET).bufferedReader().use { it.readText() }
        val rows = FoodCatalogParser.parseCsv(csv)
        return FoodCatalogParser.toFoodItems(rows, categories)
    }

    private fun mergeCatalog(
        existing: List<FoodItem>,
        catalog: List<FoodItem>,
        categories: List<FoodCategory>,
        refreshImages: Boolean
    ): List<FoodItem> {
        if (existing.isEmpty()) return catalog

        val byName = existing.associateBy { it.name.lowercase() }
        val categoryNames = categories.associate { it.id to it.name }
        val mergedExisting = existing.map { food ->
            if (!refreshImages) food
            else {
                val categoryName = categoryNames[food.categoryId] ?: "General"
                food.copy(imageUrl = FoodImageResolver.urlFor(food.name, categoryName))
            }
        }

        val extras = catalog.filter { candidate ->
            byName[candidate.name.lowercase()] == null
        }
        return mergedExisting + extras
    }

    private inline fun <reified T> readListFromFile(file: File): List<T> {
        if (!file.exists()) return emptyList()
        val content = file.readText()
        if (content.isBlank()) return emptyList()
        return try {
            json.decodeFromString<List<T>>(content)
        } catch (e: Exception) {
            val backup = File(file.parent, "${file.name}.corrupt.bak")
            file.copyTo(backup, overwrite = true)
            throw IllegalStateException("Could not read ${file.name}. A backup was saved as ${backup.name}.", e)
        }
    }

    private fun readMeta(): AppMeta {
        if (!metaFile.exists()) return AppMeta()
        return try {
            json.decodeFromString<AppMeta>(metaFile.readText())
        } catch (_: Exception) {
            AppMeta()
        }
    }

    private fun saveMeta(meta: AppMeta) {
        writeAtomic(metaFile, json.encodeToString(meta))
    }

    private fun writeAtomic(file: File, text: String) {
        val tmp = File(file.parent, "${file.name}.tmp")
        tmp.writeText(text)
        if (file.exists()) {
            val bak = File(file.parent, "${file.name}.bak")
            file.copyTo(bak, overwrite = true)
        }
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }

    private fun saveCategoriesToDisk(list: List<FoodCategory>) {
        writeAtomic(categoriesFile, json.encodeToString(list))
    }

    private fun saveFoodsToDisk(list: List<FoodItem>) {
        writeAtomic(foodsFile, json.encodeToString(list))
    }

    private fun saveShoppingToDisk(list: List<ShoppingItem>) {
        writeAtomic(shoppingFile, json.encodeToString(list))
    }

    private fun saveMealPlansToDisk(list: List<MealPlan>) {
        writeAtomic(mealPlansFile, json.encodeToString(list))
    }

    suspend fun toggleCategoryExpanded(categoryId: String) = mutex.withLock {
        val updated = _categories.value.map {
            if (it.id == categoryId) it.copy(isExpanded = !it.isExpanded) else it
        }
        _categories.value = updated
        withContext(Dispatchers.IO) { saveCategoriesToDisk(updated) }
    }

    suspend fun addCategory(name: String, description: String = "") = mutex.withLock {
        val newCategory = FoodCategory(name = name, description = description)
        val updated = _categories.value + newCategory
        _categories.value = updated
        withContext(Dispatchers.IO) { saveCategoriesToDisk(updated) }
    }

    suspend fun deleteCategory(categoryId: String) = mutex.withLock {
        val updatedCategories = _categories.value.filter { it.id != categoryId }
        val updatedFoods = _foods.value.filter { it.categoryId != categoryId }
        _categories.value = updatedCategories
        _foods.value = updatedFoods
        withContext(Dispatchers.IO) {
            saveCategoriesToDisk(updatedCategories)
            saveFoodsToDisk(updatedFoods)
        }
    }

    suspend fun rateFood(foodId: String, newRating: Int) = mutex.withLock {
        val rating = FoodRatings.coerce(newRating)
        val now = System.currentTimeMillis()
        val updated = _foods.value.map { food ->
            if (food.id == foodId) {
                food.copy(
                    rating = rating,
                    lastRatedTimestamp = if (rating > 0) now else food.lastRatedTimestamp
                )
            } else food
        }
        _foods.value = updated
        withContext(Dispatchers.IO) { saveFoodsToDisk(updated) }
    }

    suspend fun addFoodItem(
        categoryId: String,
        name: String,
        notes: String = "",
        nutrientTags: List<NutrientType> = emptyList(),
        initialRating: Int = FoodRatings.UNRATED
    ) = mutex.withLock {
        val rating = FoodRatings.coerce(initialRating)
        val categoryName = _categories.value.find { it.id == categoryId }?.name ?: "General"
        val newItem = FoodItem(
            categoryId = categoryId,
            name = name,
            rating = rating,
            lastRatedTimestamp = if (rating > 0) System.currentTimeMillis() else null,
            notes = notes,
            nutrientTags = nutrientTags.ifEmpty { FoodCatalogParser.nutrientsFor(categoryName) },
            season = FoodCatalogParser.seasonFor(name, categoryName),
            imageUrl = FoodImageResolver.urlFor(name, categoryName)
        )
        val updated = _foods.value + newItem
        _foods.value = updated
        withContext(Dispatchers.IO) { saveFoodsToDisk(updated) }
    }

    suspend fun updateFoodItem(updatedItem: FoodItem) = mutex.withLock {
        val coerced = updatedItem.copy(rating = FoodRatings.coerce(updatedItem.rating))
        val updated = _foods.value.map { if (it.id == coerced.id) coerced else it }
        _foods.value = updated
        withContext(Dispatchers.IO) { saveFoodsToDisk(updated) }
    }

    suspend fun setPotentialMeal(foodId: String, value: Boolean) = mutex.withLock {
        if (_foods.value.none { it.id == foodId && it.isPotentialMeal != value }) return@withLock
        val updated = _foods.value.map { food ->
            if (food.id == foodId) food.copy(isPotentialMeal = value) else food
        }
        _foods.value = updated
        withContext(Dispatchers.IO) { saveFoodsToDisk(updated) }
    }

    suspend fun clearPotentialMeals() = mutex.withLock {
        if (_foods.value.none { it.isPotentialMeal }) return@withLock
        val updated = _foods.value.map { if (it.isPotentialMeal) it.copy(isPotentialMeal = false) else it }
        _foods.value = updated
        withContext(Dispatchers.IO) { saveFoodsToDisk(updated) }
    }

    suspend fun deleteFoodItem(foodId: String) = mutex.withLock {
        val updated = _foods.value.filter { it.id != foodId }
        _foods.value = updated
        withContext(Dispatchers.IO) { saveFoodsToDisk(updated) }
    }

    fun hasFoodsDueForReRate(): Boolean {
        val fiveDaysAgo = System.currentTimeMillis() - FIVE_DAYS_MS
        return _foods.value.any { food ->
            food.lastRatedTimestamp != null && food.lastRatedTimestamp < fiveDaysAgo
        }
    }

    fun getEligibleRandomFood(excludeId: String? = null): FoodItem? {
        val fiveDaysAgo = System.currentTimeMillis() - FIVE_DAYS_MS
        val candidateFoods = _foods.value.filter {
            it.id != excludeId && (it.lastRatedTimestamp == null || it.lastRatedTimestamp < fiveDaysAgo)
        }
        if (candidateFoods.isEmpty()) return null
        val sortedCandidates = candidateFoods.sortedBy { it.lastRatedTimestamp ?: 0L }
        return sortedCandidates.take(10).randomOrNull()
    }

    suspend fun updateShoppingStatus(itemId: String, newStatus: ShoppingItemStatus) = mutex.withLock {
        val updated = _shoppingList.value.map {
            if (it.id == itemId) it.copy(status = newStatus) else it
        }
        _shoppingList.value = updated
        withContext(Dispatchers.IO) { saveShoppingToDisk(updated) }
    }

    suspend fun addShoppingItem(name: String, category: String = "General", sourceMeal: String? = null) = mutex.withLock {
        if (name.isBlank()) return@withLock
        val trimmed = name.trim()
        val exists = _shoppingList.value.any { it.name.equals(trimmed, ignoreCase = true) }
        if (exists) return@withLock
        val newItem = ShoppingItem(name = trimmed, category = category, sourceMeal = sourceMeal)
        val updated = _shoppingList.value + newItem
        _shoppingList.value = updated
        withContext(Dispatchers.IO) { saveShoppingToDisk(updated) }
    }

    suspend fun deleteShoppingItem(itemId: String) = mutex.withLock {
        val updated = _shoppingList.value.filter { it.id != itemId }
        _shoppingList.value = updated
        withContext(Dispatchers.IO) { saveShoppingToDisk(updated) }
    }

    suspend fun clearCompletedShoppingItems() = mutex.withLock {
        val updated = _shoppingList.value.filter { it.status == ShoppingItemStatus.NEEDED }
        _shoppingList.value = updated
        withContext(Dispatchers.IO) { saveShoppingToDisk(updated) }
    }

    suspend fun saveMealPlan(mealPlan: MealPlan) = mutex.withLock {
        val updated = listOf(mealPlan) + _mealPlans.value
        _mealPlans.value = updated
        withContext(Dispatchers.IO) { saveMealPlansToDisk(updated) }
    }

    suspend fun deleteMealPlan(planId: String) = mutex.withLock {
        val updated = _mealPlans.value.filter { it.id != planId }
        _mealPlans.value = updated
        withContext(Dispatchers.IO) { saveMealPlansToDisk(updated) }
    }

    suspend fun addIngredientsToShoppingList(ingredients: List<String>, sourceMeal: String) = mutex.withLock {
        val currentNames = _shoppingList.value.map { it.name.lowercase() }.toSet()
        val newItems = ingredients
            .map { it.trim() }
            .filter { it.isNotEmpty() && !currentNames.contains(it.lowercase()) }
            .map { ShoppingItem(name = it, sourceMeal = sourceMeal, status = ShoppingItemStatus.NEEDED) }

        if (newItems.isNotEmpty()) {
            val updated = _shoppingList.value + newItems
            _shoppingList.value = updated
            withContext(Dispatchers.IO) { saveShoppingToDisk(updated) }
        }
    }

    companion object {
        const val CATALOG_VERSION = 5
        private const val CATALOG_ASSET = "food_catalog.csv"
        private const val FIVE_DAYS_MS = 5L * 24 * 60 * 60 * 1000L
    }
}

@Serializable
private data class AppMeta(
    val catalogVersion: Int = 0
)
