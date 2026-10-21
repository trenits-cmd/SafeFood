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

    fun hasFoodsNeedingRating(): Boolean {
        val fiveDaysAgo = System.currentTimeMillis() - (5L * 24 * 60 * 60 * 1000L)
        return _foods.value.any { it.lastRatedTimestamp == null || it.lastRatedTimestamp < fiveDaysAgo }
    }

    fun getEligibleRandomFood(): FoodItem? {
        val fiveDaysAgo = System.currentTimeMillis() - (5L * 24 * 60 * 60 * 1000L)
        val candidateFoods = _foods.value.filter {
            it.lastRatedTimestamp == null || it.lastRatedTimestamp < fiveDaysAgo
        }
        if (candidateFoods.isEmpty()) return null

        // Older ratings should be privileged (sort by timestamp ascending, null/0 first)
        val sortedCandidates = candidateFoods.sortedBy { it.lastRatedTimestamp ?: 0L }
        val topPool = sortedCandidates.take(10)
        return topPool.randomOrNull()
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

    // --- Initial Default Data Seed from 500 Food Items CSV ---
    private fun getInitialCategories(): List<FoodCategory> {
        return listOf(
            FoodCategory(id = "cat_dairy", name = "Dairy", description = "Milk, Cheeses, Yogurt & Butter"),
            FoodCategory(id = "cat_fruits", name = "Fruits", description = "Fresh, Dried & Citrus Fruits"),
            FoodCategory(id = "cat_legumes", name = "Legumes", description = "Beans, Lentils, Peas & Soy Products"),
            FoodCategory(id = "cat_meat", name = "Meat", description = "Poultry, Beef, Pork, Lamb & Cuts"),
            FoodCategory(id = "cat_vegetables", name = "Vegetables", description = "Leafy Greens, Root Veggies, Tomatoes & Squash"),
            FoodCategory(id = "cat_carbs", name = "Carbohydrates", description = "Grains, Rices, Oats & Pastas")
        )
    }

    private fun getInitialFoods(categories: List<FoodCategory>): List<FoodItem> {
        val categoryMap = categories.associate { it.name.lowercase() to it.id }
        val now = System.currentTimeMillis()
        val dayMillis = 86400000L

        val rawItems = getCsvFoodRawData()

        return rawItems.mapIndexed { index, (name, categoryName, season) ->
            val catId = categoryMap[categoryName.lowercase()] ?: categories.first().id
            val defaultNutrients = when (categoryName.lowercase()) {
                "dairy" -> listOf(NutrientType.PROTEIN, NutrientType.HEALTHY_FAT)
                "fruits" -> listOf(NutrientType.VITAMINS_MINERALS, NutrientType.FIBER)
                "legumes" -> listOf(NutrientType.PROTEIN, NutrientType.FIBER)
                "meat" -> listOf(NutrientType.PROTEIN, NutrientType.HEALTHY_FAT)
                "vegetables" -> listOf(NutrientType.VITAMINS_MINERALS, NutrientType.FIBER)
                "carbohydrates" -> listOf(NutrientType.CARBOHYDRATE, NutrientType.FIBER)
                else -> listOf(NutrientType.VITAMINS_MINERALS)
            }

            val rating = when (name.lowercase()) {
                "milk", "butter", "yogurt", "cheddar", "apple", "pear", "beef", "pork", "chicken", "carrot", "potato", "onion", "wheat", "rice" -> 4
                "brie", "camembert", "gouda", "strawberry", "raspberry", "salmon", "broccoli", "spinach", "oats", "quinoa" -> 3
                else -> 0
            }

            val ratedTime = if (rating > 0) now - (dayMillis * (index % 10 + 1)) else null
            val imageUrl = getFoodImageUrl(categoryName, name)

            FoodItem(
                categoryId = catId,
                name = name,
                rating = rating,
                lastRatedTimestamp = ratedTime,
                notes = if (rating >= 3) "Western European dataset item" else "",
                nutrientTags = defaultNutrients,
                season = season,
                imageUrl = imageUrl
            )
        }
    }

    private fun getFoodImageUrl(category: String, name: String): String {
        val key = name.lowercase().trim()
        val explicitMap = mapOf(
            // Dairy
            "milk" to "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=500&auto=format&fit=crop&q=60",
            "skim milk" to "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=500&auto=format&fit=crop&q=60",
            "whole milk" to "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=500&auto=format&fit=crop&q=60",
            "butter" to "https://images.unsplash.com/photo-1589985270826-4b7bb135bc9d?w=500&auto=format&fit=crop&q=60",
            "yogurt" to "https://images.unsplash.com/photo-1488477181946-6428a0291777?w=500&auto=format&fit=crop&q=60",
            "greek yogurt" to "https://images.unsplash.com/photo-1488477181946-6428a0291777?w=500&auto=format&fit=crop&q=60",
            "cheddar" to "https://images.unsplash.com/photo-1618160702438-9b02ab6515c9?w=500&auto=format&fit=crop&q=60",
            "cheddar cheese" to "https://images.unsplash.com/photo-1618160702438-9b02ab6515c9?w=500&auto=format&fit=crop&q=60",
            "brie" to "https://images.unsplash.com/photo-1534422298391-e4f8c172dddb?w=500&auto=format&fit=crop&q=60",
            "brie cheese" to "https://images.unsplash.com/photo-1534422298391-e4f8c172dddb?w=500&auto=format&fit=crop&q=60",
            "camembert" to "https://images.unsplash.com/photo-1534422298391-e4f8c172dddb?w=500&auto=format&fit=crop&q=60",
            "gouda" to "https://images.unsplash.com/photo-1452195100486-9cc805987862?w=500&auto=format&fit=crop&q=60",
            "gouda cheese" to "https://images.unsplash.com/photo-1452195100486-9cc805987862?w=500&auto=format&fit=crop&q=60",
            "roquefort" to "https://images.unsplash.com/photo-1486297678162-eb2a19b0a32d?w=500&auto=format&fit=crop&q=60",
            "parmesan" to "https://images.unsplash.com/photo-1554998171-894172f32997?w=500&auto=format&fit=crop&q=60",
            "mozzarella" to "https://images.unsplash.com/photo-1584288019318-7b98f2604d70?w=500&auto=format&fit=crop&q=60",
            "mozzarella cheese" to "https://images.unsplash.com/photo-1584288019318-7b98f2604d70?w=500&auto=format&fit=crop&q=60",
            "feta" to "https://images.unsplash.com/photo-1559561853-08451507cbe7?w=500&auto=format&fit=crop&q=60",
            "feta cheese" to "https://images.unsplash.com/photo-1559561853-08451507cbe7?w=500&auto=format&fit=crop&q=60",
            "gruyere" to "https://images.unsplash.com/photo-1452195100486-9cc805987862?w=500&auto=format&fit=crop&q=60",
            "emmental" to "https://images.unsplash.com/photo-1452195100486-9cc805987862?w=500&auto=format&fit=crop&q=60",
            "ricotta" to "https://images.unsplash.com/photo-1588166524941-3bf61a9c41db?w=500&auto=format&fit=crop&q=60",
            "mascarpone" to "https://images.unsplash.com/photo-1588166524941-3bf61a9c41db?w=500&auto=format&fit=crop&q=60",
            "gorgonzola" to "https://images.unsplash.com/photo-1486297678162-eb2a19b0a32d?w=500&auto=format&fit=crop&q=60",
            "manchego" to "https://images.unsplash.com/photo-1452195100486-9cc805987862?w=500&auto=format&fit=crop&q=60",
            "pecorino" to "https://images.unsplash.com/photo-1554998171-894172f32997?w=500&auto=format&fit=crop&q=60",
            "cream" to "https://images.unsplash.com/photo-1537139179934-202d08383e40?w=500&auto=format&fit=crop&q=60",
            "sour cream" to "https://images.unsplash.com/photo-1537139179934-202d08383e40?w=500&auto=format&fit=crop&q=60",
            "ice cream" to "https://images.unsplash.com/photo-1501443762994-82bd5dace89a?w=500&auto=format&fit=crop&q=60",
            "gelato" to "https://images.unsplash.com/photo-1557142046-c704a3adf364?w=500&auto=format&fit=crop&q=60",

            // Fruits
            "apple" to "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=500&auto=format&fit=crop&q=60",
            "pear" to "https://images.unsplash.com/photo-1514733670139-4d87a1941d55?w=500&auto=format&fit=crop&q=60",
            "plum" to "https://images.unsplash.com/photo-1595855759920-86582396756a?w=500&auto=format&fit=crop&q=60",
            "peach" to "https://images.unsplash.com/photo-1595855759920-86582396756a?w=500&auto=format&fit=crop&q=60",
            "cherry" to "https://images.unsplash.com/photo-1528821154947-1d85d9472f3d?w=500&auto=format&fit=crop&q=60",
            "strawberry" to "https://images.unsplash.com/photo-1464965911861-746a04b4bca6?w=500&auto=format&fit=crop&q=60",
            "raspberry" to "https://images.unsplash.com/photo-1498557850523-fd3d118b962e?w=500&auto=format&fit=crop&q=60",
            "blackberry" to "https://images.unsplash.com/photo-1498557850523-fd3d118b962e?w=500&auto=format&fit=crop&q=60",
            "blueberry" to "https://images.unsplash.com/photo-1498557850523-fd3d118b962e?w=500&auto=format&fit=crop&q=60",
            "cranberry" to "https://images.unsplash.com/photo-1498557850523-fd3d118b962e?w=500&auto=format&fit=crop&q=60",
            "grape" to "https://images.unsplash.com/photo-1537640538966-79f369143f8f?w=500&auto=format&fit=crop&q=60",
            "melon" to "https://images.unsplash.com/photo-1587049352847-4a222e784d38?w=500&auto=format&fit=crop&q=60",
            "watermelon" to "https://images.unsplash.com/photo-1587049352847-4a222e784d38?w=500&auto=format&fit=crop&q=60",
            "lemon" to "https://images.unsplash.com/photo-1538883652877-628f8f2b7473?w=500&auto=format&fit=crop&q=60",
            "orange" to "https://images.unsplash.com/photo-1611080626919-7cf5a9dbab5b?w=500&auto=format&fit=crop&q=60",
            "banana" to "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=500&auto=format&fit=crop&q=60",
            "pineapple" to "https://images.unsplash.com/photo-1550258987-190a2d41a8ba?w=500&auto=format&fit=crop&q=60",
            "mango" to "https://images.unsplash.com/photo-1553279768-865429fa0078?w=500&auto=format&fit=crop&q=60",
            "kiwi" to "https://images.unsplash.com/photo-1585059895050-66c5b2ce0b74?w=500&auto=format&fit=crop&q=60",
            "avocado" to "https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?w=500&auto=format&fit=crop&q=60",
            "tomato" to "https://images.unsplash.com/photo-1592924357228-91a4daadcfea?w=500&auto=format&fit=crop&q=60",
            "cucumber" to "https://images.unsplash.com/photo-1563585324-4849a623f954?w=500&auto=format&fit=crop&q=60",
            "zucchini" to "https://images.unsplash.com/photo-1563585324-4849a623f954?w=500&auto=format&fit=crop&q=60",
            "pumpkin" to "https://images.unsplash.com/photo-1506843107748-2b10f545163f?w=500&auto=format&fit=crop&q=60",

            // Legumes
            "chickpea" to "https://images.unsplash.com/photo-1585998016460-2d2cdbf3e85a?w=500&auto=format&fit=crop&q=60",
            "lentil" to "https://images.unsplash.com/photo-1585998016460-2d2cdbf3e85a?w=500&auto=format&fit=crop&q=60",
            "kidney bean" to "https://images.unsplash.com/photo-1585998016460-2d2cdbf3e85a?w=500&auto=format&fit=crop&q=60",
            "black bean" to "https://images.unsplash.com/photo-1585998016460-2d2cdbf3e85a?w=500&auto=format&fit=crop&q=60",
            "peanut" to "https://images.unsplash.com/photo-1563227812-0ea4c22e6cc8?w=500&auto=format&fit=crop&q=60",
            "soybean" to "https://images.unsplash.com/photo-1585998016460-2d2cdbf3e85a?w=500&auto=format&fit=crop&q=60",

            // Meat
            "beef" to "https://images.unsplash.com/photo-1603048588665-791ca8aea617?w=500&auto=format&fit=crop&q=60",
            "steak" to "https://images.unsplash.com/photo-1603048588665-791ca8aea617?w=500&auto=format&fit=crop&q=60",
            "pork" to "https://images.unsplash.com/photo-1607623814075-e51df1bdc82f?w=500&auto=format&fit=crop&q=60",
            "lamb" to "https://images.unsplash.com/photo-1544025162-d76694265947?w=500&auto=format&fit=crop&q=60",
            "chicken" to "https://images.unsplash.com/photo-1604503468506-a8da13d82791?w=500&auto=format&fit=crop&q=60",
            "turkey" to "https://images.unsplash.com/photo-1604503468506-a8da13d82791?w=500&auto=format&fit=crop&q=60",
            "duck" to "https://images.unsplash.com/photo-1604503468506-a8da13d82791?w=500&auto=format&fit=crop&q=60",
            "bacon" to "https://images.unsplash.com/photo-1526367790999-0150786686a2?w=500&auto=format&fit=crop&q=60",
            "ham" to "https://images.unsplash.com/photo-1607623814075-e51df1bdc82f?w=500&auto=format&fit=crop&q=60",
            "sausage" to "https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=500&auto=format&fit=crop&q=60",
            "salami" to "https://images.unsplash.com/photo-1541529086526-db283c563270?w=500&auto=format&fit=crop&q=60",

            // Vegetables
            "carrot" to "https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?w=500&auto=format&fit=crop&q=60",
            "potato" to "https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=500&auto=format&fit=crop&q=60",
            "onion" to "https://images.unsplash.com/photo-1508747703725-719777637510?w=500&auto=format&fit=crop&q=60",
            "garlic" to "https://images.unsplash.com/photo-1540148426995-1770efd4fb54?w=500&auto=format&fit=crop&q=60",
            "broccoli" to "https://images.unsplash.com/photo-1459411621453-7b0397aeed4f?w=500&auto=format&fit=crop&q=60",
            "cauliflower" to "https://images.unsplash.com/photo-1568584711205-3277977a7f45?w=500&auto=format&fit=crop&q=60",
            "cabbage" to "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=500&auto=format&fit=crop&q=60",
            "spinach" to "https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=500&auto=format&fit=crop&q=60",
            "lettuce" to "https://images.unsplash.com/photo-1622206151226-18ca2c9ab4a1?w=500&auto=format&fit=crop&q=60",
            "arugula" to "https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=500&auto=format&fit=crop&q=60",
            "radish" to "https://images.unsplash.com/photo-1592394533824-f5bc14a35017?w=500&auto=format&fit=crop&q=60",
            "beetroot" to "https://images.unsplash.com/photo-1593105544546-120d5652f62e?w=500&auto=format&fit=crop&q=60",
            "sweet potato" to "https://images.unsplash.com/photo-1596040033229-a9821ebd058d?w=500&auto=format&fit=crop&q=60",
            "asparagus" to "https://images.unsplash.com/photo-1519996529931-28324d5a630e?w=500&auto=format&fit=crop&q=60",
            "eggplant" to "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?w=500&auto=format&fit=crop&q=60",

            // Carbohydrates
            "wheat" to "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?w=500&auto=format&fit=crop&q=60",
            "rice" to "https://images.unsplash.com/photo-1516684732162-798406f4be23?w=500&auto=format&fit=crop&q=60",
            "barley" to "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?w=500&auto=format&fit=crop&q=60",
            "oats" to "https://images.unsplash.com/photo-1517673132405-a56a62b18caf?w=500&auto=format&fit=crop&q=60",
            "rye" to "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?w=500&auto=format&fit=crop&q=60",
            "quinoa" to "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=500&auto=format&fit=crop&q=60",
            "buckwheat" to "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?w=500&auto=format&fit=crop&q=60",
            "bread" to "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=500&auto=format&fit=crop&q=60",
            "baguette" to "https://images.unsplash.com/photo-1549931319-a545dcf3bc73?w=500&auto=format&fit=crop&q=60",
            "croissant" to "https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=500&auto=format&fit=crop&q=60",
            "pasta" to "https://images.unsplash.com/photo-1621996346565-e3d5d6281298?w=500&auto=format&fit=crop&q=60",
            "spaghetti" to "https://images.unsplash.com/photo-1551183053-bf91a1d81141?w=500&auto=format&fit=crop&q=60",
            "macaroni" to "https://images.unsplash.com/photo-1621996346565-e3d5d6281298?w=500&auto=format&fit=crop&q=60",
            "penne" to "https://images.unsplash.com/photo-1621996346565-e3d5d6281298?w=500&auto=format&fit=crop&q=60",
            "noodle" to "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=500&auto=format&fit=crop&q=60",
            "couscous" to "https://images.unsplash.com/photo-1541529086526-db283c563270?w=500&auto=format&fit=crop&q=60",
            "cake" to "https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=500&auto=format&fit=crop&q=60",
            "cookie" to "https://images.unsplash.com/photo-1499636136210-6f49bc155fe2?w=500&auto=format&fit=crop&q=60",
            "granola" to "https://images.unsplash.com/photo-1517673132405-a56a62b18caf?w=500&auto=format&fit=crop&q=60"
        }

        if (explicitMap.containsKey(key)) {
            return explicitMap[key]!!
        }

        for ((k, url) in explicitMap) {
            if (key.contains(k)) {
                return url
            }
        }

        return "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&auto=format&fit=crop&q=60"
    }

    private fun getInitialShopping(): List<ShoppingItem> {
        return listOf(
            ShoppingItem(name = "Whole Milk", category = "Dairy", status = ShoppingItemStatus.NEEDED),
            ShoppingItem(name = "Brown Rice", category = "Carbohydrates", status = ShoppingItemStatus.ALREADY_HAVE),
            ShoppingItem(name = "Tomato", category = "Vegetables", status = ShoppingItemStatus.NEEDED),
            ShoppingItem(name = "Chicken Breast", category = "Meat", status = ShoppingItemStatus.IN_CART)
        )
    }

    private fun getCsvFoodRawData(): List<Triple<String, String, String>> {
        return listOf(
            // Dairy
            Triple("Milk", "Dairy", "All Year"), Triple("Butter", "Dairy", "All Year"), Triple("Yogurt", "Dairy", "All Year"),
            Triple("Cheddar", "Dairy", "All Year"), Triple("Brie", "Dairy", "All Year"), Triple("Camembert", "Dairy", "All Year"),
            Triple("Gouda", "Dairy", "All Year"), Triple("Roquefort", "Dairy", "All Year"), Triple("Parmesan", "Dairy", "All Year"),
            Triple("Mozzarella", "Dairy", "All Year"), Triple("Feta", "Dairy", "All Year"), Triple("Gruyere", "Dairy", "All Year"),
            Triple("Emmental", "Dairy", "All Year"), Triple("Ricotta", "Dairy", "All Year"), Triple("Mascarpone", "Dairy", "All Year"),
            Triple("Gorgonzola", "Dairy", "All Year"), Triple("Manchego", "Dairy", "All Year"), Triple("Pecorino", "Dairy", "All Year"),
            Triple("Cream", "Dairy", "All Year"), Triple("Sour Cream", "Dairy", "All Year"), Triple("Creme Fraiche", "Dairy", "All Year"),
            Triple("Quark", "Dairy", "All Year"), Triple("Cottage Cheese", "Dairy", "All Year"), Triple("Provolone", "Dairy", "All Year"),
            Triple("Edam", "Dairy", "All Year"), Triple("Havarti", "Dairy", "All Year"), Triple("Jarlsberg", "Dairy", "All Year"),
            Triple("Fontina", "Dairy", "All Year"), Triple("Taleggio", "Dairy", "All Year"), Triple("Halloumi", "Dairy", "All Year"),
            Triple("Paneer", "Dairy", "All Year"), Triple("Burrata", "Dairy", "All Year"), Triple("Stracciatella", "Dairy", "All Year"),
            Triple("Clotted Cream", "Dairy", "All Year"), Triple("Buttermilk", "Dairy", "All Year"), Triple("Kefir", "Dairy", "All Year"),
            Triple("Whey", "Dairy", "All Year"), Triple("Ghee", "Dairy", "All Year"), Triple("Condensed Milk", "Dairy", "All Year"),
            Triple("Evaporated Milk", "Dairy", "All Year"), Triple("Ice Cream", "Dairy", "All Year"), Triple("Gelato", "Dairy", "All Year"),
            Triple("Skim Milk", "Dairy", "All Year"), Triple("Whole Milk", "Dairy", "All Year"), Triple("Goat Cheese", "Dairy", "All Year"),
            Triple("Sheep Cheese", "Dairy", "All Year"), Triple("Blue Cheese", "Dairy", "All Year"), Triple("Swiss Cheese", "Dairy", "All Year"),
            Triple("Munster", "Dairy", "All Year"), Triple("Neufchatel", "Dairy", "All Year"),

            // Fruits
            Triple("Apple", "Fruits", "Autumn"), Triple("Pear", "Fruits", "Autumn"), Triple("Plum", "Fruits", "Summer"),
            Triple("Peach", "Fruits", "Summer"), Triple("Cherry", "Fruits", "Spring"), Triple("Strawberry", "Fruits", "Spring"),
            Triple("Raspberry", "Fruits", "Summer"), Triple("Blackberry", "Fruits", "Summer"), Triple("Blueberry", "Fruits", "Summer"),
            Triple("Cranberry", "Fruits", "Autumn"), Triple("Grape", "Fruits", "Autumn"), Triple("Melon", "Fruits", "Summer"),
            Triple("Watermelon", "Fruits", "Summer"), Triple("Lemon", "Fruits", "Winter"), Triple("Orange", "Fruits", "Winter"),
            Triple("Mandarin", "Fruits", "Winter"), Triple("Clementine", "Fruits", "Winter"), Triple("Grapefruit", "Fruits", "Winter"),
            Triple("Lime", "Fruits", "Winter"), Triple("Apricot", "Fruits", "Spring"), Triple("Nectarine", "Fruits", "Summer"),
            Triple("Fig", "Fruits", "Summer"), Triple("Pomegranate", "Fruits", "Autumn"), Triple("Kiwi", "Fruits", "Spring"),
            Triple("Banana", "Fruits", "All Year"), Triple("Pineapple", "Fruits", "All Year"), Triple("Mango", "Fruits", "All Year"),
            Triple("Papaya", "Fruits", "All Year"), Triple("Coconut", "Fruits", "All Year"), Triple("Guava", "Fruits", "All Year"),
            Triple("Passion Fruit", "Fruits", "All Year"), Triple("Lychee", "Fruits", "All Year"), Triple("Dragon Fruit", "Fruits", "All Year"),
            Triple("Starfruit", "Fruits", "All Year"), Triple("Persimmon", "Fruits", "Autumn"), Triple("Quince", "Fruits", "Autumn"),
            Triple("Medlar", "Fruits", "Autumn"), Triple("Mulberry", "Fruits", "Summer"), Triple("Gooseberry", "Fruits", "Summer"),
            Triple("Redcurrant", "Fruits", "Summer"), Triple("Blackcurrant", "Fruits", "Summer"), Triple("Whitecurrant", "Fruits", "Summer"),
            Triple("Elderberry", "Fruits", "Autumn"), Triple("Rowanberry", "Fruits", "Autumn"), Triple("Sloe", "Fruits", "Autumn"),
            Triple("Rosehip", "Fruits", "Autumn"), Triple("Sea Buckthorn", "Fruits", "Autumn"), Triple("Cloudberry", "Fruits", "Summer"),
            Triple("Lingonberry", "Fruits", "Autumn"), Triple("Boysenberry", "Fruits", "Summer"), Triple("Loganberry", "Fruits", "Summer"),
            Triple("Tayberry", "Fruits", "Summer"), Triple("Marionberry", "Fruits", "Summer"), Triple("Salmonberry", "Fruits", "Summer"),
            Triple("Thimbleberry", "Fruits", "Summer"), Triple("Bearberry", "Fruits", "Summer"), Triple("Crowberry", "Fruits", "Summer"),
            Triple("Hackberry", "Fruits", "Summer"), Triple("Huckleberry", "Fruits", "Summer"), Triple("Serviceberry", "Fruits", "Summer"),
            Triple("Chokeberry", "Fruits", "Autumn"), Triple("Buffaloberry", "Fruits", "Autumn"), Triple("Silverberry", "Fruits", "Autumn"),
            Triple("Autumn Olive", "Fruits", "Autumn"), Triple("Goumi", "Fruits", "Autumn"), Triple("Gumi", "Fruits", "Autumn"),
            Triple("Jujube", "Fruits", "Autumn"), Triple("Date", "Fruits", "All Year"), Triple("Olive", "Fruits", "All Year"),
            Triple("Avocado", "Fruits", "All Year"), Triple("Tomato", "Fruits", "Summer"), Triple("Bell Pepper", "Fruits", "Summer"),
            Triple("Eggplant", "Fruits", "Summer"), Triple("Cucumber", "Fruits", "Summer"), Triple("Zucchini", "Fruits", "Summer"),
            Triple("Pumpkin", "Fruits", "Autumn"), Triple("Squash", "Fruits", "Autumn"),

            // Legumes
            Triple("Chickpea", "Legumes", "All Year"), Triple("Lentil", "Legumes", "All Year"), Triple("Green Pea", "Legumes", "All Year"),
            Triple("Black-Eyed Pea", "Legumes", "All Year"), Triple("Kidney Bean", "Legumes", "All Year"), Triple("Black Bean", "Legumes", "All Year"),
            Triple("Pinto Bean", "Legumes", "All Year"), Triple("Navy Bean", "Legumes", "All Year"), Triple("Cannellini Bean", "Legumes", "All Year"),
            Triple("Lima Bean", "Legumes", "All Year"), Triple("Fava Bean", "Legumes", "All Year"), Triple("Mung Bean", "Legumes", "All Year"),
            Triple("Adzuki Bean", "Legumes", "All Year"), Triple("Soybean", "Legumes", "All Year"), Triple("Peanut", "Legumes", "All Year"),
            Triple("Macadamia Nut", "Legumes", "All Year"), Triple("Cashew", "Legumes", "All Year"),

            // Meat
            Triple("Beef", "Meat", "All Year"), Triple("Pork", "Meat", "All Year"), Triple("Lamb", "Meat", "All Year"),
            Triple("Chicken", "Meat", "All Year"), Triple("Turkey", "Meat", "All Year"), Triple("Duck", "Meat", "All Year"),
            Triple("Bacon", "Meat", "All Year"), Triple("Ham", "Meat", "All Year"), Triple("Sausage", "Meat", "All Year"),
            Triple("Steak", "Meat", "All Year"), Triple("Burger", "Meat", "All Year"), Triple("Salami", "Meat", "All Year"),

            // Vegetables
            Triple("Carrot", "Vegetables", "All Year"), Triple("Potato", "Vegetables", "All Year"), Triple("Onion", "Vegetables", "All Year"),
            Triple("Garlic", "Vegetables", "All Year"), Triple("Leek", "Vegetables", "Winter"), Triple("Celery", "Vegetables", "Winter"),
            Triple("Broccoli", "Vegetables", "Winter"), Triple("Cauliflower", "Vegetables", "Winter"), Triple("Cabbage", "Vegetables", "Winter"),
            Triple("Brussels Sprout", "Vegetables", "Autumn"), Triple("Kale", "Vegetables", "Winter"), Triple("Spinach", "Vegetables", "Spring"),
            Triple("Lettuce", "Vegetables", "All Year"), Triple("Arugula", "Vegetables", "Spring"), Triple("Radish", "Vegetables", "Spring"),
            Triple("Turnip", "Vegetables", "Autumn"), Triple("Parsnip", "Vegetables", "Autumn"), Triple("Beetroot", "Vegetables", "Autumn"),
            Triple("Sweet Potato", "Vegetables", "Autumn"), Triple("Artichoke", "Vegetables", "Spring"), Triple("Asparagus", "Vegetables", "Spring"),
            Triple("Corn", "Vegetables", "Summer"), Triple("Cucumber Green", "Vegetables", "Summer"), Triple("Eggplant Purple", "Vegetables", "Summer"),
            Triple("Tomato Red", "Vegetables", "Summer"), Triple("Zucchini Green", "Vegetables", "Summer"), Triple("Butternut Squash", "Vegetables", "Autumn"),

            // Carbohydrates
            Triple("Wheat", "Carbohydrates", "All Year"), Triple("Rice", "Carbohydrates", "All Year"), Triple("Barley", "Carbohydrates", "All Year"),
            Triple("Oats", "Carbohydrates", "All Year"), Triple("Rye", "Carbohydrates", "All Year"), Triple("Quinoa", "Carbohydrates", "All Year"),
            Triple("Buckwheat", "Carbohydrates", "All Year"), Triple("Bread", "Carbohydrates", "All Year"), Triple("Baguette", "Carbohydrates", "All Year"),
            Triple("Croissant", "Carbohydrates", "All Year"), Triple("Pasta", "Carbohydrates", "All Year"), Triple("Spaghetti", "Carbohydrates", "All Year"),
            Triple("Macaroni", "Carbohydrates", "All Year"), Triple("Penne", "Carbohydrates", "All Year"), Triple("Noodle", "Carbohydrates", "All Year"),
            Triple("Couscous", "Carbohydrates", "All Year"), Triple("Polenta", "Carbohydrates", "All Year"), Triple("Cake", "Carbohydrates", "All Year"),
            Triple("Cookie", "Carbohydrates", "All Year"), Triple("Granola", "Carbohydrates", "All Year")
        )
    }
}
