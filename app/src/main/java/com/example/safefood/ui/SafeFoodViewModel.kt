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

    private val _selectedSeasonFilter = MutableStateFlow("All Seasons")
    val selectedSeasonFilter: StateFlow<String> = _selectedSeasonFilter.asStateFlow()

    fun setSeasonFilter(season: String) {
        _selectedSeasonFilter.value = season
    }

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Safe Food Tab filtered state: Like (3) or Love (4) and recently rated
    val bestRatedFoods: StateFlow<List<FoodItem>> = combine(foods, _searchQuery) { foodList, query ->
        foodList
            .filter { it.rating >= 3 }
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

    private val _showStartupRatePrompt = MutableStateFlow(false)
    val showStartupRatePrompt: StateFlow<Boolean> = _showStartupRatePrompt.asStateFlow()

    private val _activeRandomFood = MutableStateFlow<FoodItem?>(null)
    val activeRandomFood: StateFlow<FoodItem?> = _activeRandomFood.asStateFlow()

    init {
        viewModelScope.launch {
            repository.loadData()
            if (repository.hasFoodsNeedingRating()) {
                _showStartupRatePrompt.value = true
            }
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
            val prefText = when (newRating) {
                4 -> "Love ❤️"
                3 -> "Like 👍"
                2 -> "Neutral 😐"
                1 -> "Dislike 👎"
                else -> "Unrated"
            }
            showMessage("Preference updated to $prefText")
        }
    }

    fun dismissStartupRatePrompt() {
        _showStartupRatePrompt.value = false
    }

    fun startRandomRating() {
        _showStartupRatePrompt.value = false
        _activeRandomFood.value = repository.getEligibleRandomFood()
        if (_activeRandomFood.value == null) {
            showMessage("All foods have been rated recently!")
        }
    }

    fun stopRandomRating() {
        _activeRandomFood.value = null
    }

    fun rateRandomFoodAndNext(foodId: String, newRating: Int) {
        viewModelScope.launch {
            repository.rateFood(foodId, newRating)
            val prefText = when (newRating) {
                4 -> "Love ❤️"
                3 -> "Like 👍"
                2 -> "Neutral 😐"
                1 -> "Dislike 👎"
                else -> "Unrated"
            }
            showMessage("Rated food as $prefText")
            val next = repository.getEligibleRandomFood()
            _activeRandomFood.value = next
            if (next == null) {
                showMessage("Great job! All foods have been reviewed.")
            }
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
