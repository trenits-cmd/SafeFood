package com.example.safefood.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.safefood.ai.AiMenuPlanner
import com.example.safefood.data.export.CsvExporter
import com.example.safefood.data.export.CsvImporter
import com.example.safefood.data.model.FoodCategory
import com.example.safefood.data.model.FoodItem
import com.example.safefood.data.model.FoodRatings
import com.example.safefood.data.model.MealPlan
import com.example.safefood.data.model.MealPlanDay
import com.example.safefood.data.model.NutrientType
import com.example.safefood.data.model.ShoppingItem
import com.example.safefood.data.model.ShoppingItemStatus
import com.example.safefood.data.repository.SafeFoodRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SafeFoodViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SafeFoodRepository(application)

    val categories: StateFlow<List<FoodCategory>> = repository.categories
    val foods: StateFlow<List<FoodItem>> = repository.foods
    val shoppingList: StateFlow<List<ShoppingItem>> = repository.shoppingList
    val mealPlans: StateFlow<List<MealPlan>> = repository.mealPlans

    private val _selectedSeasonFilter = MutableStateFlow("All Seasons")
    val selectedSeasonFilter: StateFlow<String> = _selectedSeasonFilter.asStateFlow()

    fun setSeasonFilter(season: String) {
        _selectedSeasonFilter.value = season
    }

    private val _selectedNutrientFilters = MutableStateFlow<Set<NutrientType>>(emptySet())
    val selectedNutrientFilters: StateFlow<Set<NutrientType>> = _selectedNutrientFilters.asStateFlow()

    fun toggleNutrientFilter(nutrient: NutrientType) {
        _selectedNutrientFilters.value = _selectedNutrientFilters.value.let { current ->
            if (nutrient in current) current - nutrient else current + nutrient
        }
    }

    fun clearNutrientFilters() {
        _selectedNutrientFilters.value = emptySet()
    }

    /** Foods the user marked as "potential meal" – collected in the AI Menu tab. */
    val potentialMealFoods: StateFlow<List<FoodItem>> = foods
        .map { foodList -> foodList.filter { it.isPotentialMeal }.sortedBy { it.name.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun togglePotentialMeal(foodId: String) {
        val food = foods.value.find { it.id == foodId } ?: return
        runRepo {
            repository.setPotentialMeal(foodId, !food.isPotentialMeal)
        }
    }

    fun clearPotentialMeals() {
        runRepo("Potential meal selection cleared") { repository.clearPotentialMeals() }
    }

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val bestRatedFoods: StateFlow<List<FoodItem>> = foods
        .map { foodList ->
            foodList.filter { FoodRatings.isSafe(it.rating) }.sortedByDescending { it.rating }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyRatedFoods: StateFlow<List<FoodItem>> = foods
        .map { foodList ->
            foodList
                .filter { it.lastRatedTimestamp != null && it.lastRatedTimestamp > 0 }
                .sortedByDescending { it.lastRatedTimestamp ?: 0L }
                .take(10)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showStartupRatePrompt = MutableStateFlow(false)
    val showStartupRatePrompt: StateFlow<Boolean> = _showStartupRatePrompt.asStateFlow()

    private val _activeRandomFood = MutableStateFlow<FoodItem?>(null)
    val activeRandomFood: StateFlow<FoodItem?> = _activeRandomFood.asStateFlow()

    private val _isGeneratingPlan = MutableStateFlow(false)
    val isGeneratingPlan: StateFlow<Boolean> = _isGeneratingPlan.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.loadData()
                if (repository.hasFoodsDueForReRate()) {
                    _showStartupRatePrompt.value = true
                }
            } catch (e: Exception) {
                showMessage(e.message ?: "Could not load saved foods")
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    private fun runRepo(successMessage: String? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                if (successMessage != null) showMessage(successMessage)
            } catch (e: Exception) {
                showMessage(e.message ?: "Save failed")
            }
        }
    }

    fun toggleCategoryExpanded(categoryId: String) {
        runRepo { repository.toggleCategoryExpanded(categoryId) }
    }

    fun addCategory(name: String, description: String = "") {
        if (name.isBlank()) return
        runRepo("Category '${name.trim()}' created") {
            repository.addCategory(name.trim(), description.trim())
        }
    }

    fun deleteCategory(categoryId: String) {
        runRepo("Category removed") { repository.deleteCategory(categoryId) }
    }

    fun rateFood(foodId: String, newRating: Int) {
        runRepo("Preference updated to ${FoodRatings.label(newRating)}") {
            repository.rateFood(foodId, newRating)
        }
    }

    fun dismissStartupRatePrompt() {
        _showStartupRatePrompt.value = false
    }

    fun startRandomRating() {
        _showStartupRatePrompt.value = false
        val currentId = _activeRandomFood.value?.id
        _activeRandomFood.value = repository.getEligibleRandomFood(excludeId = currentId)
        if (_activeRandomFood.value == null) {
            showMessage("All foods have been rated recently")
        }
    }

    fun stopRandomRating() {
        _activeRandomFood.value = null
    }

    fun rateRandomFoodAndNext(foodId: String, newRating: Int) {
        viewModelScope.launch {
            try {
                repository.rateFood(foodId, newRating)
                showMessage("Rated as ${FoodRatings.label(newRating)}")
                val next = repository.getEligibleRandomFood(excludeId = foodId)
                _activeRandomFood.value = next
                if (next == null) {
                    showMessage("Great job. All due foods have been reviewed.")
                }
            } catch (e: Exception) {
                showMessage(e.message ?: "Save failed")
            }
        }
    }

    fun addFoodItem(
        categoryId: String,
        name: String,
        notes: String = "",
        nutrientTags: List<NutrientType> = emptyList(),
        initialRating: Int = FoodRatings.UNRATED
    ) {
        if (name.isBlank()) return
        runRepo("Food item '${name.trim()}' added") {
            repository.addFoodItem(categoryId, name.trim(), notes.trim(), nutrientTags, initialRating)
        }
    }

    fun updateFoodItem(item: FoodItem) {
        runRepo("Food item updated") { repository.updateFoodItem(item) }
    }

    fun deleteFoodItem(foodId: String) {
        runRepo("Food item deleted") { repository.deleteFoodItem(foodId) }
    }

    fun updateShoppingStatus(itemId: String, newStatus: ShoppingItemStatus) {
        runRepo { repository.updateShoppingStatus(itemId, newStatus) }
    }

    fun addShoppingItem(name: String, category: String = "General") {
        if (name.isBlank()) return
        runRepo("Added '${name.trim()}' to shopping list") {
            repository.addShoppingItem(name.trim(), category)
        }
    }

    fun deleteShoppingItem(itemId: String) {
        runRepo { repository.deleteShoppingItem(itemId) }
    }

    fun clearCompletedShoppingItems() {
        runRepo("Cleared checked items") { repository.clearCompletedShoppingItems() }
    }

    fun generateAiMealPlan(
        goal: AiMenuPlanner.PlanningGoal = AiMenuPlanner.PlanningGoal.SAFE_ONLY,
        customInstructions: String = "",
        daysCount: Int = 3,
        usePotentialMealsOnly: Boolean = false,
        clearExisting: Boolean = false
    ) {
        viewModelScope.launch {
            _isGeneratingPlan.value = true
            try {
                val preferredIds = foods.value.filter { it.isPotentialMeal }.map { it.id }.toSet()
                val generated = withContext(Dispatchers.Default) {
                    AiMenuPlanner.generateMealPlan(
                        foods = foods.value,
                        categories = categories.value,
                        goal = goal,
                        customInstructions = customInstructions,
                        daysCount = daysCount,
                        preferredIds = preferredIds,
                        onlyPreferred = usePotentialMealsOnly
                    )
                }
                if (clearExisting) repository.clearMealPlans()
                repository.saveMealPlan(generated)
                showMessage("Menu plan created (${generated.days.size} days)")
            } catch (e: Exception) {
                showMessage(e.message ?: "Could not create menu plan")
            } finally {
                _isGeneratingPlan.value = false
            }
        }
    }

    fun deleteMealPlan(planId: String) {
        runRepo("Meal plan deleted") { repository.deleteMealPlan(planId) }
    }
    
    fun clearMealPlans() {
        runRepo("All meal plans cleared") { repository.clearMealPlans() }
    }

    fun addDayIngredientsToShoppingList(day: MealPlanDay) {
        runRepo {
            val ingredients = AiMenuPlanner.extractIngredientsFromDay(day)
            repository.addIngredientsToShoppingList(ingredients, sourceMeal = day.dayName)
            showMessage("Added ${ingredients.size} ingredients to shopping list")
        }
    }

    fun getFoodsCsvText(): String {
        return CsvExporter.exportFoodsToCsv(foods.value, categories.value)
    }

    fun importFoodsCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                val csvContent = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: throw IllegalStateException("Could not read the selected file")
                }
                val result = CsvImporter.parseFoodsCsv(
                    csvContent = csvContent,
                    existingCategories = categories.value,
                    existingFoods = foods.value
                )
                if (result.foodsToImport.isEmpty()) {
                    showMessage(result.errors.firstOrNull() ?: "No food rows found in this CSV")
                    return@launch
                }

                val importedCount = repository.importFoods(
                    importedFoods = result.foodsToImport,
                    newCategories = result.newCategories
                )
                val newCategoryCount = result.newCategories.size
                val categoryNote = if (newCategoryCount > 0) " and $newCategoryCount new group${if (newCategoryCount == 1) "" else "s"}" else ""
                showMessage("Imported $importedCount food${if (importedCount == 1) "" else "s"}$categoryNote")
            } catch (e: Exception) {
                showMessage(e.message ?: "Could not import CSV")
            }
        }
    }

    fun importShoppingListCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                val csvContent = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: throw IllegalStateException("Could not read the selected file")
                }
                val shoppingList = CsvImporter.parseShoppingListCsv(
                    csvContent = csvContent,
                    existingShoppingList = shoppingList.value
                )
                val importedCount = shoppingList.size
                if (importedCount > 0) {
                    showMessage("Imported $importedCount item${if (importedCount == 1) "" else "s"} from CSV")
                } else {
                    showMessage("No valid rows found in this CSV")
                }
            } catch (e: Exception) {
                showMessage(e.message ?: "Could not import CSV")
            }
        }
    }

    fun getShoppingCsvText(): String {
        return CsvExporter.exportShoppingListToCsv(shoppingList.value)
    }

    fun shareCsvData(context: Context, csvText: String, title: String, fileName: String) {
        viewModelScope.launch {
            try {
                val uri = withContext(Dispatchers.IO) {
                    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                    val file = File(exportDir, fileName)
                    file.writeText(csvText)
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                }
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(sendIntent, title))
            } catch (e: Exception) {
                showMessage(e.message ?: "Could not share CSV")
            }
        }
    }

    /**
     * Downloads a CSV template file to the app's cache directory and opens the
     * system file picker so the user can save it or share it immediately.
     * @param csvText the template CSV text to write
     * @param fileName suggested file name
     * @param context Android context, obtained from the composable
     */
    fun downloadCsvFile(csvText: String, fileName: String, context: Context) {
        viewModelScope.launch {
            try {
                val uri = withContext(Dispatchers.IO) {
                    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                    val file = File(exportDir, fileName)
                    file.writeText(csvText)
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                }
                val openIntent = Intent(Intent.ACTION_VIEW).apply {
                    type = "text/csv"
                    data = uri
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(openIntent)
            } catch (e: Exception) {
                showMessage(e.message ?: "Could not download template")
            }
        }
    }
}
