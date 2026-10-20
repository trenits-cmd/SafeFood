package com.example.safefood.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class NutrientType(val displayName: String, val description: String) {
    PROTEIN("Proteins", "Essential for growth, muscle repair & satiety"),
    FIBER("Fibers", "Digestive health, gut comfort & steady energy release"),
    CARBOHYDRATE("Carbohydrates", "Primary energy fuel for brain & daily activity"),
    HEALTHY_FAT("Healthy Fats", "Brain function, cell health & nutrient absorption"),
    VITAMINS_MINERALS("Vitamins & Minerals", "Immunity, vitality & overall wellness")
}

@Serializable
data class FoodItem(
    val id: String = UUID.randomUUID().toString(),
    val categoryId: String,
    val name: String,
    val rating: Int = 0, // 0 = unrated, 1..5 stars
    val lastRatedTimestamp: Long? = null,
    val notes: String = "",
    val textureTags: List<String> = emptyList(),
    val nutrientTags: List<NutrientType> = emptyList()
)

@Serializable
data class FoodCategory(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val isExpanded: Boolean = true
)

@Serializable
enum class ShoppingItemStatus {
    NEEDED,      // Needs to be purchased
    IN_CART,     // Put in cart at store
    ALREADY_HAVE // User already has this ingredient at home
}

@Serializable
data class ShoppingItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "General",
    val status: ShoppingItemStatus = ShoppingItemStatus.NEEDED,
    val sourceMeal: String? = null
)

@Serializable
data class MealPlanDay(
    val dayName: String,
    val breakfast: String = "",
    val lunch: String = "",
    val dinner: String = "",
    val snacks: String = ""
)

@Serializable
data class MealPlan(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val days: List<MealPlanDay>
)
