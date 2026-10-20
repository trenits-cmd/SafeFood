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

        return rawItems.mapIndexed { index, (name, categoryName) ->
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

            // Give a few initial ratings for demo / immediate tab population
            val rating = when (name.lowercase()) {
                "salmon", "whole milk", "cheddar cheese", "apple", "chicken breast", "tomato", "white rice", "brown rice", "butter" -> 5
                "greek yogurt", "banana", "black beans", "beef steak", "broccoli", "rolled oats" -> 4
                "skim milk", "lemon", "lentils", "pork chop", "spinach", "spaghetti" -> 3
                else -> 0
            }

            val ratedTime = if (rating > 0) now - (dayMillis * (index % 10 + 1)) else null

            FoodItem(
                categoryId = catId,
                name = name,
                rating = rating,
                lastRatedTimestamp = ratedTime,
                notes = if (rating >= 4) "Favorite comfort ingredient" else "",
                nutrientTags = defaultNutrients
            )
        }
    }

    private fun getInitialShopping(): List<ShoppingItem> {
        return listOf(
            ShoppingItem(name = "Whole Milk", category = "Dairy", status = ShoppingItemStatus.NEEDED),
            ShoppingItem(name = "Brown Rice", category = "Carbohydrates", status = ShoppingItemStatus.ALREADY_HAVE),
            ShoppingItem(name = "Tomato", category = "Vegetables", status = ShoppingItemStatus.NEEDED),
            ShoppingItem(name = "Chicken Breast", category = "Meat", status = ShoppingItemStatus.IN_CART)
        )
    }

    private fun getCsvFoodRawData(): List<Pair<String, String>> {
        return listOf(
            // Dairy
            "Whole Milk" to "Dairy", "Skim Milk" to "Dairy", "Cheddar Cheese" to "Dairy", "Mozzarella Cheese" to "Dairy",
            "Greek Yogurt" to "Dairy", "Butter" to "Dairy", "Heavy Cream" to "Dairy", "Sour Cream" to "Dairy",
            "Cottage Cheese" to "Dairy", "Parmesan Cheese" to "Dairy", "Swiss Cheese" to "Dairy", "Gouda Cheese" to "Dairy",
            "Brie Cheese" to "Dairy", "Camembert" to "Dairy", "Provolone Cheese" to "Dairy", "Feta Cheese" to "Dairy",
            "Ricotta Cheese" to "Dairy", "Paneer" to "Dairy", "Ghee" to "Dairy", "Cream Cheese" to "Dairy",
            "Kefir" to "Dairy", "Buttermilk" to "Dairy", "Condensed Milk" to "Dairy", "Evaporated Milk" to "Dairy",
            "Mascarpone" to "Dairy", "Quark" to "Dairy", "Frozen Yogurt" to "Dairy", "Ice Cream" to "Dairy",
            "Whipped Cream" to "Dairy", "String Cheese" to "Dairy", "Skyr" to "Dairy", "Labneh" to "Dairy",

            // Fruits
            "Apple" to "Fruits", "Banana" to "Fruits", "Orange" to "Fruits", "Strawberry" to "Fruits",
            "Blueberry" to "Fruits", "Raspberry" to "Fruits", "Blackberry" to "Fruits", "Pineapple" to "Fruits",
            "Mango" to "Fruits", "Papaya" to "Fruits", "Peach" to "Fruits", "Nectarine" to "Fruits",
            "Plum" to "Fruits", "Apricot" to "Fruits", "Cherry" to "Fruits", "Grape" to "Fruits",
            "Watermelon" to "Fruits", "Cantaloupe" to "Fruits", "Kiwi" to "Fruits", "Lemon" to "Fruits",
            "Lime" to "Fruits", "Grapefruit" to "Fruits", "Pomegranate" to "Fruits", "Fig" to "Fruits",
            "Date" to "Fruits", "Cranberry" to "Fruits", "Raisin" to "Fruits", "Dried Apricot" to "Fruits",

            // Legumes
            "Lentils" to "Legumes", "Red Lentils" to "Legumes", "Green Lentils" to "Legumes", "Chickpeas (Garbanzo Beans)" to "Legumes",
            "Black Beans" to "Legumes", "Kidney Beans" to "Legumes", "Pinto Beans" to "Legumes", "Navy Beans" to "Legumes",
            "Lima Beans" to "Legumes", "Soybeans" to "Legumes", "Edamame" to "Legumes", "Peanuts" to "Legumes",
            "Green Peas" to "Legumes", "Split Peas" to "Legumes", "Black-eyed Peas" to "Legumes", "Fava Beans" to "Legumes",
            "Tofu" to "Legumes", "Tempeh" to "Legumes", "Peanut Butter" to "Legumes", "Hummus" to "Legumes",
            "Falafel" to "Legumes", "Lentil Pasta" to "Legumes", "Chickpea Pasta" to "Legumes",

            // Meat
            "Chicken Breast" to "Meat", "Chicken Thigh" to "Meat", "Chicken Wing" to "Meat", "Chicken Drumstick" to "Meat",
            "Whole Chicken" to "Meat", "Turkey Breast" to "Meat", "Ground Turkey" to "Meat", "Duck Breast" to "Meat",
            "Beef Steak" to "Meat", "Ribeye Steak" to "Meat", "Sirloin Steak" to "Meat", "Filet Mignon" to "Meat",
            "Ground Beef" to "Meat", "Beef Brisket" to "Meat", "Pork Chop" to "Meat", "Pork Loin" to "Meat",
            "Pork Tenderloin" to "Meat", "Pork Belly" to "Meat", "Bacon" to "Meat", "Ham" to "Meat",
            "Salami" to "Meat", "Pepperoni" to "Meat", "Pork Sausage" to "Meat", "Lamb Chop" to "Meat",
            "Ground Lamb" to "Meat", "Beef Jerky" to "Meat", "Turkey Bacon" to "Meat",

            // Vegetables
            "Broccoli" to "Vegetables", "Cauliflower" to "Vegetables", "Spinach" to "Vegetables", "Kale" to "Vegetables",
            "Swiss Chard" to "Vegetables", "Arugula" to "Vegetables", "Romaine Lettuce" to "Vegetables", "Iceberg Lettuce" to "Vegetables",
            "Lettuce" to "Vegetables", "Cabbage" to "Vegetables", "Brussels Sprouts" to "Vegetables", "Asparagus" to "Vegetables",
            "Celery" to "Vegetables", "Cucumber" to "Vegetables", "Zucchini" to "Vegetables", "Eggplant" to "Vegetables",
            "Bell Pepper (Red)" to "Vegetables", "Bell Pepper (Green)" to "Vegetables", "Jalapeño Pepper" to "Vegetables",
            "Tomato" to "Vegetables", "Cherry Tomato" to "Vegetables", "Roma Tomato" to "Vegetables", "Carrot" to "Vegetables",
            "Radish" to "Vegetables", "Garlic" to "Vegetables", "Onion (Yellow)" to "Vegetables", "Onion (Red)" to "Vegetables",
            "Sweet Potato" to "Vegetables", "Russet Potato" to "Vegetables", "Red Potato" to "Vegetables", "Yukon Gold Potato" to "Vegetables",
            "Pumpkin" to "Vegetables", "Butternut Squash" to "Vegetables", "Zucchini" to "Vegetables",

            // Carbohydrates
            "White Rice" to "Carbohydrates", "Brown Rice" to "Carbohydrates", "Jasmine Rice" to "Carbohydrates", "Basmati Rice" to "Carbohydrates",
            "Wild Rice" to "Carbohydrates", "Arborio Rice" to "Carbohydrates", "Rolled Oats" to "Carbohydrates", "Steel-Cut Oats" to "Carbohydrates",
            "Quinoa" to "Carbohydrates", "Couscous" to "Carbohydrates", "Bulgur" to "Carbohydrates", "Farro" to "Carbohydrates",
            "Barley" to "Carbohydrates", "Buckwheat" to "Carbohydrates", "Gnocchi" to "Carbohydrates", "Spaghetti" to "Carbohydrates",
            "Penne" to "Carbohydrates", "Fettuccine" to "Carbohydrates", "Rigatoni" to "Carbohydrates", "Macaroni" to "Carbohydrates",
            "Ramen Noodles" to "Carbohydrates", "Udon Noodles" to "Carbohydrates", "Soba Noodles" to "Carbohydrates"
        )
    }
}
