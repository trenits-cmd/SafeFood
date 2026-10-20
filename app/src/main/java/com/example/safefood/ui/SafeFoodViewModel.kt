package com.example.safefood.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.safefood.ai.AiMenuPlanner
import com.example.safefood.data.export.CsvExporter
import com.example.safefood.data.model.FoodCategory
import com.example.safefood.data.model.FoodItem
import com.example.safefood.data.model.MealPlan
import com.example.safefood.data.model.MealPlanDay
import com.example.safefood.data.model.NutrientType
import com.example.safefood.data.model.ShoppingItem
import com.example.safefood.data.model.ShoppingItemStatus
import com.example.safefood.data.repository.SafeFoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SafeFoodViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SafeFoodRepository(application)

    val categories: StateFlow<List<FoodCategory>> = repository.categories
    val foods: StateFlow<List<FoodItem>> = repository.foods
    val shoppingList: StateFlow<List<ShoppingItem>> = repository.shoppingList
    val mealPlans: StateFlow<List<MealPlan>> = repository.mealPlans

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Safe Food Tab filtered state: Best rated (4-5 stars) and recently rated
    val bestRatedFoods: StateFlow<List<FoodItem>> = combine(foods, _searchQuery) { foodList, query ->
        foodList
            .filter { it.rating >= 4 }
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) || it.notes.contains(query, ignoreCase = true) }
            .sortedByDescending { it.rating }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyRatedFoods: StateFlow<List<FoodItem>> = combine(foods, _searchQuery) { foodList, query ->
        foodList
            .filter { it.lastRatedTimestamp != null && it.lastRatedTimestamp > 0 }
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .sortedByDescending { it.lastRatedTimestamp ?: 0L }
            .take(10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.loadData()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    // --- Fold/Unfold & Category ---
    fun toggleCategoryExpanded(categoryId: String) {
        viewModelScope.launch {
            repository.toggleCategoryExpanded(categoryId)
        }
    }

    fun addCategory(name: String, description: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addCategory(name.trim(), description.trim())
            showMessage("Category '$name' created")
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
            showMessage("Category removed")
        }
    }

    // --- Ratings & Food Actions ---
    fun rateFood(foodId: String, newRating: Int) {
        viewModelScope.launch {
            repository.rateFood(foodId, newRating)
            val starsText = if (newRating == 1) "1 Star" else "$newRating Stars"
            showMessage("Rating updated to $starsText")
        }
    }

    fun addFoodItem(
        categoryId: String,
        name: String,
        notes: String = "",
        nutrientTags: List<NutrientType> = emptyList(),
        initialRating: Int = 0
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addFoodItem(categoryId, name.trim(), notes.trim(), nutrientTags, initialRating)
            showMessage("Food item '$name' added")
        }
    }

    fun updateFoodItem(item: FoodItem) {
        viewModelScope.launch {
            repository.updateFoodItem(item)
            showMessage("Food item updated")
        }
    }

    fun deleteFoodItem(foodId: String) {
        viewModelScope.launch {
            repository.deleteFoodItem(foodId)
            showMessage("Food item deleted")
        }
    }

    // --- Shopping List Actions ---
    fun updateShoppingStatus(itemId: String, newStatus: ShoppingItemStatus) {
        viewModelScope.launch {
            repository.updateShoppingStatus(itemId, newStatus)
        }
    }

    fun addShoppingItem(name: String, category: String = "General") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addShoppingItem(name.trim(), category)
            showMessage("Added '$name' to shopping list")
        }
    }

    fun deleteShoppingItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteShoppingItem(itemId)
        }
    }

    fun clearCompletedShoppingItems() {
        viewModelScope.launch {
            repository.clearCompletedShoppingItems()
            showMessage("Cleared checked items")
        }
    }

    // --- AI Menu Planner ---
    fun generateAiMealPlan(
        goal: AiMenuPlanner.PlanningGoal = AiMenuPlanner.PlanningGoal.SAFE_ONLY,
        customInstructions: String = "",
        daysCount: Int = 3
    ) {
        viewModelScope.launch {
            val allFoods = foods.value
            val generated = AiMenuPlanner.generateMealPlan(allFoods, goal, customInstructions, daysCount)
            repository.saveMealPlan(generated)
            showMessage("New AI Menu Plan created (${generated.days.size} days)")
        }
    }

    fun addDayIngredientsToShoppingList(day: MealPlanDay) {
        viewModelScope.launch {
            val ingredients = AiMenuPlanner.extractIngredientsFromDay(day)
            repository.addIngredientsToShoppingList(ingredients, sourceMeal = day.dayName)
            showMessage("Added ${ingredients.size} ingredients to Shopping List")
        }
    }

    // --- CSV Export ---
    fun getFoodsCsvText(): String {
        return CsvExporter.exportFoodsToCsv(foods.value, categories.value)
    }

    fun getShoppingCsvText(): String {
        return CsvExporter.exportShoppingListToCsv(shoppingList.value)
    }

    fun shareCsvData(context: Context, csvText: String, title: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, csvText)
            type = "text/csv"
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        context.startActivity(shareIntent)
    }
}
