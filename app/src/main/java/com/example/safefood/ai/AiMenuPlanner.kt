package com.example.safefood.ai

import com.example.safefood.data.model.FoodItem
import com.example.safefood.data.model.MealPlan
import com.example.safefood.data.model.MealPlanDay
import java.util.UUID

object AiMenuPlanner {

    enum class PlanningGoal(val label: String, val description: String) {
        SAFE_ONLY("Strict Safe Foods (4-5 Stars)", "Focus exclusively on highest rated comfortable safe foods"),
        BALANCED("Balanced Meals", "Combines proteins, vegetables and wholesome carbs"),
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
            it.name.contains("Milk", ignoreCase = true) ||
            it.name.contains("Yogurt", ignoreCase = true) ||
            it.name.contains("Oats", ignoreCase = true) ||
            it.name.contains("Oatmeal", ignoreCase = true) ||
            it.name.contains("Egg", ignoreCase = true) ||
            it.name.contains("Apple", ignoreCase = true) ||
            it.name.contains("Banana", ignoreCase = true) ||
            it.name.contains("Berry", ignoreCase = true)
        }.ifEmpty { safePool }

        val lunchDinnerPool = safePool.filter {
            it.name.contains("Rice", ignoreCase = true) ||
            it.name.contains("Chicken", ignoreCase = true) ||
            it.name.contains("Salmon", ignoreCase = true) ||
            it.name.contains("Beef", ignoreCase = true) ||
            it.name.contains("Turkey", ignoreCase = true) ||
            it.name.contains("Pork", ignoreCase = true) ||
            it.name.contains("Pasta", ignoreCase = true) ||
            it.name.contains("Lentil", ignoreCase = true) ||
            it.name.contains("Potato", ignoreCase = true) ||
            it.name.contains("Tomato", ignoreCase = true) ||
            it.name.contains("Broccoli", ignoreCase = true)
        }.ifEmpty { safePool }

        val snackPool = safePool.filter {
            it.name.contains("Cheese", ignoreCase = true) ||
            it.name.contains("Fruit", ignoreCase = true) ||
            it.name.contains("Juice", ignoreCase = true) ||
            it.name.contains("Water", ignoreCase = true) ||
            it.name.contains("Carrot", ignoreCase = true) ||
            it.name.contains("Cucumber", ignoreCase = true) ||
            it.name.contains("Crackers", ignoreCase = true)
        }.ifEmpty { safePool }

        val daysList = mutableListOf<MealPlanDay>()

        val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        for (i in 0 until daysCount) {
            val dayName = dayNames.getOrElse(i) { "Day ${i + 1}" }

            val breakfast = breakfastPool.shuffled().firstOrNull()?.name ?: "Whole Milk & Apple"
            val lunch = lunchDinnerPool.shuffled().firstOrNull()?.name ?: "Chicken Breast & Brown Rice"
            val dinner = lunchDinnerPool.shuffled().firstOrNull()?.name ?: "Salmon & Broccoli"
            val snack1 = snackPool.shuffled().firstOrNull()?.name ?: "Greek Yogurt"
            val snack2 = snackPool.shuffled().firstOrNull()?.name ?: "Banana"

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
