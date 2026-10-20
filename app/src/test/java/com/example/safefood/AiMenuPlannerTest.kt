package com.example.safefood

import com.example.safefood.ai.AiMenuPlanner
import com.example.safefood.data.model.FoodItem
import com.example.safefood.data.model.MealPlanDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiMenuPlannerTest {

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
