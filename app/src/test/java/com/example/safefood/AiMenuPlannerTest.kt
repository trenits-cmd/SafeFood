package com.example.safefood

import com.example.safefood.ai.AiMenuPlanner
import com.example.safefood.data.model.FoodCategory
import com.example.safefood.data.model.FoodItem
import com.example.safefood.data.model.FoodRatings
import com.example.safefood.data.model.MealPlanDay
import com.example.safefood.data.model.NutrientType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiMenuPlannerTest {

    private val categories = listOf(
        FoodCategory(id = "fruits", name = "Fruits"),
        FoodCategory(id = "vegetables", name = "Vegetables"),
        FoodCategory(id = "dairy", name = "Dairy"),
        FoodCategory(id = "meat", name = "Meat"),
        FoodCategory(id = "carbs", name = "Carbohydrates"),
        FoodCategory(id = "legumes", name = "Legumes")
    )

    private fun food(
        name: String,
        categoryId: String,
        rating: Int = FoodRatings.YUMMY,
        tags: List<NutrientType> = emptyList(),
        potential: Boolean = false
    ) = FoodItem(
        categoryId = categoryId,
        name = name,
        rating = rating,
        nutrientTags = tags,
        isPotentialMeal = potential
    )

    private val pool = listOf(
        food("Rolled Oats", "carbs", tags = listOf(NutrientType.CARBOHYDRATE, NutrientType.FIBER)),
        food("Banana", "fruits", tags = listOf(NutrientType.VITAMINS_MINERALS, NutrientType.FIBER)),
        food("Greek Yogurt", "dairy", tags = listOf(NutrientType.PROTEIN)),
        food("Chicken Breast", "meat", tags = listOf(NutrientType.PROTEIN)),
        food("Lentils", "legumes", tags = listOf(NutrientType.PROTEIN, NutrientType.FIBER)),
        food("Rice", "carbs", tags = listOf(NutrientType.CARBOHYDRATE)),
        food("Broccoli", "vegetables", tags = listOf(NutrientType.VITAMINS_MINERALS, NutrientType.FIBER)),
        food("Carrots", "vegetables", tags = listOf(NutrientType.VITAMINS_MINERALS, NutrientType.FIBER)),
        food("Apple", "fruits", tags = listOf(NutrientType.FIBER))
    )

    private val breakfastItems = listOf("Rolled Oats", "Banana", "Greek Yogurt")
    private val mainProteins = listOf("Chicken Breast", "Lentils")
    private val vegetables = listOf("Broccoli", "Carrots")

    @Test
    fun `breakfast never consists of plain vegetables`() {
        val plan = AiMenuPlanner.generateMealPlan(
            foods = pool,
            categories = categories,
            goal = AiMenuPlanner.PlanningGoal.SAFE_ONLY,
            daysCount = 7
        )

        assertEquals(7, plan.days.size)
        for (day in plan.days) {
            assertTrue("Breakfast should contain a breakfast food, was '${day.breakfast}'",
                breakfastItems.any { day.breakfast.contains(it) })
            assertFalse("Breakfast must not be a plain vegetable, was '${day.breakfast}'",
                day.breakfast.contains("Carrots") || day.breakfast.contains("Broccoli"))
            assertFalse("Breakfast must not be a meat dish, was '${day.breakfast}'",
                day.breakfast.contains("Chicken"))
        }
    }

    @Test
    fun `main meals combine protein, side and vegetable`() {
        val plan = AiMenuPlanner.generateMealPlan(
            foods = pool,
            categories = categories,
            goal = AiMenuPlanner.PlanningGoal.SAFE_ONLY,
            daysCount = 5
        )

        for (day in plan.days) {
            assertTrue("Lunch should contain a protein: '${day.lunch}'", mainProteins.any { day.lunch.contains(it) })
            assertTrue("Lunch should contain a vegetable: '${day.lunch}'", vegetables.any { day.lunch.contains(it) })
            assertTrue("Dinner should contain a protein: '${day.dinner}'", mainProteins.any { day.dinner.contains(it) })
            assertTrue("Dinner should contain a vegetable: '${day.dinner}'", vegetables.any { day.dinner.contains(it) })
        }
    }

    @Test
    fun `potential meal foods are used when only preferred is set`() {
        val preferredNames = setOf(
            "Rolled Oats", "Banana", "Greek Yogurt", "Chicken Breast", "Rice", "Broccoli"
        )
        val preferred = pool.filter { it.name in preferredNames }.map { it.copy(isPotentialMeal = true) }
        val foods = pool.filterNot { f -> preferred.any { it.id == f.id } } + preferred

        val plan = AiMenuPlanner.generateMealPlan(
            foods = foods,
            categories = categories,
            goal = AiMenuPlanner.PlanningGoal.SAFE_ONLY,
            daysCount = 3,
            preferredIds = preferred.map { it.id }.toSet(),
            onlyPreferred = true
        )

        assertTrue(plan.title.contains("Potential meal"))
        for (day in plan.days) {
            val text = "${day.breakfast} | ${day.lunch} | ${day.dinner} | ${day.snacks}"
            assertFalse("Excluded food leaked into the plan: $text", text.contains("Carrots"))
            assertFalse("Excluded food leaked into the plan: $text", text.contains("Apple"))
            assertFalse("Excluded food leaked into the plan: $text", text.contains("Lentils"))
        }
    }

    @Test
    fun `unrated potential meal foods produce a helpful message`() {
        val plan = AiMenuPlanner.generateMealPlan(
            foods = listOf(
                food("Banana", "fruits", rating = FoodRatings.UNRATED, potential = true)
            ),
            categories = categories,
            daysCount = 2,
            preferredIds = setOf("missing"),
            onlyPreferred = true
        )

        assertTrue(plan.days.all { it.breakfast.startsWith("No 'potential meal'") })
    }

    @Test
    fun `ingredients are extracted from composed meals`() {
        val day = MealPlanDay(
            dayName = "Monday",
            breakfast = "Rolled Oats with Banana and Greek Yogurt",
            lunch = "Chicken Breast with Rice and Broccoli"
        )

        val ingredients = AiMenuPlanner.extractIngredientsFromDay(day)

        assertEquals(
            listOf("Rolled Oats", "Banana", "Greek Yogurt", "Chicken Breast", "Rice", "Broccoli"),
            ingredients
        )
    }

    @Test
    fun `breakfast without bread or oats is a light fruit and dairy plate`() {
        val foods = listOf(
            food("Milk", "dairy"),
            food("Greek Yogurt", "dairy"),
            food("Apple", "fruits"),
            food("Pear", "fruits"),
            food("Chicken Breast", "meat"),
            food("Rice", "carbs"),
            food("Potato", "vegetables"),
            food("Carrots", "vegetables")
        )

        val plan = AiMenuPlanner.generateMealPlan(
            foods = foods,
            categories = categories,
            goal = AiMenuPlanner.PlanningGoal.SAFE_ONLY,
            daysCount = 3
        )

        for (day in plan.days) {
            assertTrue(
                "Breakfast should be fruit and/or dairy, was '${day.breakfast}'",
                day.breakfast.contains("Apple") || day.breakfast.contains("Pear") ||
                    day.breakfast.contains("Yogurt") || day.breakfast.contains("Milk")
            )
            assertFalse(
                "Breakfast must not be a plain side dish, was '${day.breakfast}'",
                day.breakfast.contains("Rice") || day.breakfast.contains("Potato") ||
                    day.breakfast.contains("Chicken")
            )
        }
    }

    @Test
    fun `pear is used as fruit and never as the vegetable of a main meal`() {
        val foods = listOf(
            food("Chicken Breast", "meat"),
            food("Rice", "carbs"),
            food("Pear", "fruits"),
            food("Apple", "fruits"),
            food("Greek Yogurt", "dairy")
        )

        val plan = AiMenuPlanner.generateMealPlan(
            foods = foods,
            categories = categories,
            goal = AiMenuPlanner.PlanningGoal.SAFE_ONLY,
            daysCount = 4
        )

        for (day in plan.days) {
            assertFalse("Pear must not stand in as a vegetable: '${day.lunch}'", day.lunch.contains("Pear"))
            assertFalse("Pear must not stand in as a vegetable: '${day.dinner}'", day.dinner.contains("Pear"))
            assertFalse("A meal must not repeat the same food", Regex("Potato with Potato").containsMatchIn(day.dinner))
        }
    }

    @Test
    fun `meals never repeat the same food within one meal`() {
        val foods = listOf(
            food("Pork", "meat"),
            food("Rice", "carbs"),
            food("Potato", "vegetables"),
            food("Carrots", "vegetables"),
            food("Onion", "vegetables"),
            food("Apple", "fruits"),
            food("Greek Yogurt", "dairy")
        )

        val plan = AiMenuPlanner.generateMealPlan(
            foods = foods,
            categories = categories,
            goal = AiMenuPlanner.PlanningGoal.ROTATION_HELPER,
            daysCount = 7
        )

        for (day in plan.days) {
            for (meal in listOf(day.breakfast, day.lunch, day.dinner)) {
                val names = meal.split(" with ", " and ").map { it.trim() }
                assertEquals("Meal repeats a food: '$meal'", names.size, names.distinct().size)
            }
        }
    }

    @Test
    fun `custom instructions exclude unwanted foods`() {
        val filtered = AiMenuPlanner.applyCustomInstructions(pool, "no yogurt, no chicken")

        assertTrue(filtered.none { it.name == "Greek Yogurt" })
        assertTrue(filtered.none { it.name == "Chicken Breast" })
        assertTrue(filtered.any { it.name == "Banana" })
    }

    @Test
    fun generateMealPlan_prioritizesHighRatedSafeFoods() {
        val foods = listOf(
            FoodItem(categoryId = "c1", name = "Whole Milk", rating = 5),
            FoodItem(categoryId = "c2", name = "Brown Rice", rating = 5),
            FoodItem(categoryId = "c3", name = "Chicken Breast", rating = 4),
            FoodItem(categoryId = "c4", name = "Unsafe Dish", rating = 1)
        )

        val plan = AiMenuPlanner.generateMealPlan(
            foods = foods,
            goal = AiMenuPlanner.PlanningGoal.SAFE_ONLY,
            daysCount = 3
        )

        assertEquals(3, plan.days.size)
        for (day in plan.days) {
            assertTrue(!day.breakfast.contains("Unsafe Dish"))
            assertTrue(!day.lunch.contains("Unsafe Dish"))
            assertTrue(!day.dinner.contains("Unsafe Dish"))
        }
    }

    @Test
    fun extractIngredientsFromDay_splitsIngredientsCorrectly() {
        val day = MealPlanDay(
            dayName = "Monday",
            breakfast = "Whole Milk & Apple",
            lunch = "Chicken Breast, Brown Rice",
            dinner = "Salmon & Broccoli",
            snacks = "Greek Yogurt and Banana"
        )

        val ingredients = AiMenuPlanner.extractIngredientsFromDay(day)

        assertTrue(ingredients.contains("Whole Milk"))
        assertTrue(ingredients.contains("Apple"))
        assertTrue(ingredients.contains("Chicken Breast"))
        assertTrue(ingredients.contains("Brown Rice"))
        assertTrue(ingredients.contains("Salmon"))
        assertTrue(ingredients.contains("Broccoli"))
        assertTrue(ingredients.contains("Greek Yogurt"))
        assertTrue(ingredients.contains("Banana"))
    }
}
