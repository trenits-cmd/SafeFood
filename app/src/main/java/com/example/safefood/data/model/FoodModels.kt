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

/**
 * Single 5-star scale used everywhere (UI, persistence, planner, export).
 * 0 = unrated, 1 = Yucky … 5 = Yummy. Safe foods are 4 and 5.
 */
object FoodRatings {
    const val UNRATED = 0
    const val YUCKY = 1
    const val UNSURE = 2
    const val OKAY = 3
    const val GOOD = 4
    const val YUMMY = 5
    const val SAFE_MIN = GOOD

    fun coerce(rating: Int): Int = if (rating <= 0) UNRATED else rating.coerceIn(YUCKY, YUMMY)

    fun isSafe(rating: Int): Boolean = coerce(rating) >= SAFE_MIN

    fun isUsableInBalancedPlan(rating: Int): Boolean = coerce(rating) >= OKAY

    fun label(rating: Int): String = when (coerce(rating)) {
        YUMMY -> "Yummy (safe favorite)"
        GOOD -> "Good (safe option)"
        OKAY -> "Okay (neutral)"
        UNSURE -> "Unsure (borderline)"
        YUCKY -> "Yucky (not safe)"
        else -> "Unrated"
    }
}

@Serializable
data class FoodItem(
    val id: String = UUID.randomUUID().toString(),
    val categoryId: String,
    val name: String,
    val rating: Int = FoodRatings.UNRATED,
    val lastRatedTimestamp: Long? = null,
    val notes: String = "",
    val nutrientTags: List<NutrientType> = emptyList(),
    val season: String = "All Year",
    val imageUrl: String = "",
    /** User marked this food as something they would like to turn into a meal. */
    val isPotentialMeal: Boolean = false,
    /** Category to restore when a blacklisted food is rated 2-5 stars again. */
    val previousCategoryId: String? = null
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
