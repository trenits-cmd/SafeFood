package com.example.safefood.ai

import com.example.safefood.data.model.FoodItem
import com.example.safefood.data.model.FoodRatings
import com.example.safefood.data.model.MealPlan
import com.example.safefood.data.model.MealPlanDay
import com.example.safefood.data.model.NutrientType
import java.util.Calendar
import java.util.Locale
import java.util.UUID

object AiMenuPlanner {

    enum class PlanningGoal(val label: String, val description: String) {
        SAFE_ONLY("Strict Safe Foods (4-5 Stars)", "Use only foods rated Good or Yummy"),
        BALANCED("Balanced Meals", "Combine protein, produce, and carbs from rated-okay-or-better foods"),
        LOW_PREPARATION("Low Energy / Fast Prep", "Prefer simple, low-effort safe items"),
        ROTATION_HELPER("Safe Food Rotation", "Avoid repeating the same item across days")
    }

    fun generateMealPlan(
        foods: List<FoodItem>,
        goal: PlanningGoal = PlanningGoal.SAFE_ONLY,
        customInstructions: String = "",
        daysCount: Int = 3
    ): MealPlan {
        val minRating = when (goal) {
            PlanningGoal.SAFE_ONLY, PlanningGoal.ROTATION_HELPER, PlanningGoal.LOW_PREPARATION -> FoodRatings.SAFE_MIN
            PlanningGoal.BALANCED -> FoodRatings.OKAY
        }
        var pool = foods.filter { it.rating >= minRating }
        pool = applyCustomInstructions(pool, customInstructions)

        val emptyMessage = if (foods.none { it.rating >= minRating }) {
            "No matching foods yet. Rate more items first."
        } else {
            "No foods match these instructions. Try a shorter prompt."
        }

        val dayNames = rotatingDayNames(daysCount.coerceIn(1, 14))
        val usedIds = mutableSetOf<String>()
        val daysList = dayNames.map { dayName ->
            if (pool.isEmpty()) {
                MealPlanDay(
                    dayName = dayName,
                    breakfast = emptyMessage,
                    lunch = "",
                    dinner = "",
                    snacks = ""
                )
            } else {
                buildDay(dayName, pool, goal, usedIds)
            }
        }

        return MealPlan(
            id = UUID.randomUUID().toString(),
            title = "${goal.label} (${daysCount} Days)",
            days = daysList
        )
    }

    fun extractIngredientsFromDay(day: MealPlanDay): List<String> {
        val result = linkedSetOf<String>()
        val raw = listOf(day.breakfast, day.lunch, day.dinner, day.snacks)
        for (item in raw) {
            if (item.isBlank()) continue
            if (item.startsWith("No matching") || item.startsWith("No foods match")) continue
            val splitItems = item.split(",", "&", " and ", "+")
            for (s in splitItems) {
                val cleaned = s.trim()
                if (cleaned.isNotEmpty()) result.add(cleaned)
            }
        }
        return result.toList()
    }

    internal fun applyCustomInstructions(pool: List<FoodItem>, instructions: String): List<FoodItem> {
        val text = instructions.lowercase(Locale.US).trim()
        if (text.isBlank()) return pool

        val excluded = Regex("""(?:no|without|exclude|avoid)\s+([a-z0-9][a-z0-9\s-]{1,40})""")
            .findAll(text)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotEmpty() }
            .toList()

        var result = pool
        if (excluded.isNotEmpty()) {
            result = result.filter { food ->
                excluded.none { token -> food.name.contains(token, ignoreCase = true) }
            }
        }

        val preferred = result.filter { food ->
            text.contains(food.name.lowercase(Locale.US))
        }
        if (preferred.isNotEmpty()) return preferred
        return result
    }

    private fun buildDay(
        dayName: String,
        pool: List<FoodItem>,
        goal: PlanningGoal,
        usedIds: MutableSet<String>
    ): MealPlanDay {
        val breakfast = pickFoods(
            pool = breakfastCandidates(pool).ifEmpty { pool },
            goal = goal,
            usedIds = usedIds,
            count = 2
        )
        val lunch = pickFoods(
            pool = if (goal == PlanningGoal.BALANCED) balancedPlate(pool) else lunchDinnerCandidates(pool).ifEmpty { pool },
            goal = goal,
            usedIds = usedIds,
            count = if (goal == PlanningGoal.BALANCED) 3 else 2
        )
        val dinner = pickFoods(
            pool = if (goal == PlanningGoal.BALANCED) balancedPlate(pool) else lunchDinnerCandidates(pool).ifEmpty { pool },
            goal = goal,
            usedIds = usedIds,
            count = if (goal == PlanningGoal.BALANCED) 3 else 2
        )
        val snacks = pickFoods(
            pool = snackCandidates(pool).ifEmpty { pool },
            goal = goal,
            usedIds = usedIds,
            count = 2
        )
        return MealPlanDay(
            dayName = dayName,
            breakfast = breakfast.joinToString(" & ") { it.name },
            lunch = lunch.joinToString(" & ") { it.name },
            dinner = dinner.joinToString(" & ") { it.name },
            snacks = snacks.joinToString(", ") { it.name }
        )
    }

    private fun pickFoods(
        pool: List<FoodItem>,
        goal: PlanningGoal,
        usedIds: MutableSet<String>,
        count: Int
    ): List<FoodItem> {
        val preferFresh = when (goal) {
            PlanningGoal.ROTATION_HELPER -> pool.filter { it.id !in usedIds }.ifEmpty { pool }
            else -> pool
        }
        val ranked = when (goal) {
            PlanningGoal.LOW_PREPARATION -> preferFresh.sortedBy { lowPrepScore(it) }
            else -> preferFresh.shuffled()
        }
        val picked = mutableListOf<FoodItem>()
        for (food in ranked) {
            if (picked.any { it.id == food.id }) continue
            picked.add(food)
            usedIds.add(food.id)
            if (picked.size >= count) break
        }
        if (picked.isEmpty()) ranked.firstOrNull()?.let { picked.add(it) }
        return picked
    }

    private fun breakfastCandidates(pool: List<FoodItem>): List<FoodItem> {
        val keys = listOf("milk", "yogurt", "oat", "egg", "apple", "banana", "berry", "cheese", "skyr")
        return pool.filter { food -> keys.any { food.name.contains(it, true) } }
    }

    private fun lunchDinnerCandidates(pool: List<FoodItem>): List<FoodItem> {
        val keys = listOf(
            "rice", "chicken", "beef", "turkey", "pork", "lamb", "pasta", "lentil",
            "potato", "tomato", "broccoli", "salmon", "bean", "tofu", "quinoa"
        )
        return pool.filter { food -> keys.any { food.name.contains(it, true) } }
    }

    private fun snackCandidates(pool: List<FoodItem>): List<FoodItem> {
        val keys = listOf("cheese", "yogurt", "apple", "banana", "carrot", "cucumber", "berry", "grape")
        return pool.filter { food -> keys.any { food.name.contains(it, true) } }
    }

    private fun balancedPlate(pool: List<FoodItem>): List<FoodItem> {
        val protein = pool.filter { NutrientType.PROTEIN in it.nutrientTags }
        val produce = pool.filter {
            NutrientType.VITAMINS_MINERALS in it.nutrientTags || NutrientType.FIBER in it.nutrientTags
        }
        val carb = pool.filter { NutrientType.CARBOHYDRATE in it.nutrientTags }
        return (protein.shuffled().take(2) + produce.shuffled().take(2) + carb.shuffled().take(2))
            .ifEmpty { pool }
    }

    private fun lowPrepScore(food: FoodItem): Int {
        val name = food.name.lowercase(Locale.US)
        val easy = listOf("yogurt", "cheese", "milk", "banana", "apple", "carrot", "rice", "oat", "ham", "bread")
        return if (easy.any { name.contains(it) }) 0 else 1
    }

    private fun rotatingDayNames(daysCount: Int): List<String> {
        val names = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val calendar = Calendar.getInstance()
        val mondayBased = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
        return (0 until daysCount).map { offset ->
            names[(mondayBased + offset) % 7]
        }
    }
}
