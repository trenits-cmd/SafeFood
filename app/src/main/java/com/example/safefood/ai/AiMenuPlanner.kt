package com.example.safefood.ai

import com.example.safefood.data.model.FoodItem
import com.example.safefood.data.model.MealPlan
import com.example.safefood.data.model.MealPlanDay
import java.util.UUID

object AiMenuPlanner {

    enum class PlanningGoal(val label: String, val description: String) {
        SAFE_ONLY("Strict Safe Foods (4-5 Stars)", "Focus exclusively on highest rated comfortable safe foods"),
        TEXTURE_BALANCED("Texture Variety", "Balance crunchy and soft safe foods throughout the day"),
        LOW_PREPARATION("Low Energy / Fast Prep", "Minimal cooking effort with predictable safe items"),
        ROTATION_HELPER("Safe Food Rotation", "Prevent safe food burnout by rotating options")
    }

    fun generateMealPlan(
        foods: List<FoodItem>,
        goal: PlanningGoal = PlanningGoal.SAFE_ONLY,
        customInstructions: String = "",
        daysCount: Int = 3
    ): MealPlan {
        // Filter safe foods (rating >= 3 if available, prioritize 4 & 5)
        val highlySafe = foods.filter { it.rating >= 4 }
        val moderatelySafe = foods.filter { it.rating == 3 }
        val safePool = if (highlySafe.isNotEmpty()) highlySafe else if (moderatelySafe.isNotEmpty()) moderatelySafe else foods

        val breakfastPool = safePool.filter {
            it.name.contains("Toast", ignoreCase = true) ||
            it.name.contains("Oatmeal", ignoreCase = true) ||
            it.name.contains("Applesauce", ignoreCase = true) ||
            it.name.contains("Yogurt", ignoreCase = true) ||
            it.name.contains("Pancakes", ignoreCase = true) ||
            it.name.contains("Cereal", ignoreCase = true) ||
            it.name.contains("Apple", ignoreCase = true) ||
            it.name.contains("Banana", ignoreCase = true) ||
            it.textureTags.contains("Sweet") ||
            it.textureTags.contains("Soft")
        }.ifEmpty { safePool }

        val lunchDinnerPool = safePool.filter {
            it.name.contains("Rice", ignoreCase = true) ||
            it.name.contains("Nuggets", ignoreCase = true) ||
            it.name.contains("Noodles", ignoreCase = true) ||
            it.name.contains("Fries", ignoreCase = true) ||
            it.name.contains("Macaroni", ignoreCase = true) ||
            it.name.contains("Cheese", ignoreCase = true) ||
            it.name.contains("Chicken", ignoreCase = true) ||
            it.name.contains("Potato", ignoreCase = true) ||
            it.textureTags.contains("Warm") ||
            it.textureTags.contains("Savory")
        }.ifEmpty { safePool }

        val snackPool = safePool.filter {
            it.name.contains("Crackers", ignoreCase = true) ||
            it.name.contains("Pretzels", ignoreCase = true) ||
            it.name.contains("Chips", ignoreCase = true) ||
            it.name.contains("Pudding", ignoreCase = true) ||
            it.name.contains("Juice", ignoreCase = true) ||
            it.name.contains("Water", ignoreCase = true) ||
            it.name.contains("Slices", ignoreCase = true) ||
            it.textureTags.contains("Crunchy") ||
            it.textureTags.contains("Dry")
        }.ifEmpty { safePool }

        val daysList = mutableListOf<MealPlanDay>()

        val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        for (i in 0 until daysCount) {
            val dayName = dayNames.getOrElse(i) { "Day ${i + 1}" }

            val breakfast = breakfastPool.shuffled().firstOrNull()?.name ?: "Plain Toast & Water"
            val lunch = lunchDinnerPool.shuffled().firstOrNull()?.name ?: "Chicken Nuggets & Rice"
            val dinner = lunchDinnerPool.shuffled().firstOrNull()?.name ?: "Butter Noodles"
            val snack1 = snackPool.shuffled().firstOrNull()?.name ?: "Saltine Crackers"
            val snack2 = snackPool.shuffled().firstOrNull()?.name ?: "Applesauce"

            daysList.add(
                MealPlanDay(
                    dayName = dayName,
                    breakfast = breakfast,
                    lunch = lunch,
                    dinner = dinner,
                    snacks = "$snack1, $snack2"
                )
            )
        }

        val title = "${goal.label} (${daysCount} Days)"

        return MealPlan(
            id = UUID.randomUUID().toString(),
            title = title,
            days = daysList
        )
    }

    fun extractIngredientsFromDay(day: MealPlanDay): List<String> {
        val result = mutableSetOf<String>()
        val raw = listOf(day.breakfast, day.lunch, day.dinner, day.snacks)

        for (item in raw) {
            if (item.isBlank()) continue
            // Split by comma or '&' or 'and'
            val splitItems = item.split(",", "&", " and ")
            for (s in splitItems) {
                val cleaned = s.trim()
                if (cleaned.isNotEmpty()) {
                    result.add(cleaned)
                }
            }
        }
        return result.toList()
    }
}
