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
            FoodItem(categoryId = "c1", name = "Saltine Crackers", rating = 5, textureTags = listOf("Crunchy")),
            FoodItem(categoryId = "c2", name = "Plain White Rice", rating = 5, textureTags = listOf("Warm")),
            FoodItem(categoryId = "c3", name = "Chicken Nuggets", rating = 4, textureTags = listOf("Warm")),
            FoodItem(categoryId = "c4", name = "Unsafe Soup", rating = 1, textureTags = listOf("Lumpy"))
        )

        val plan = AiMenuPlanner.generateMealPlan(
            foods = foods,
            goal = AiMenuPlanner.PlanningGoal.SAFE_ONLY,
            daysCount = 3
        )

        assertEquals(3, plan.days.size)
        // Ensure none of the unsafe 1-star foods are chosen when 4-5 star foods exist
        for (day in plan.days) {
            assertTrue(!day.breakfast.contains("Unsafe Soup"))
            assertTrue(!day.lunch.contains("Unsafe Soup"))
            assertTrue(!day.dinner.contains("Unsafe Soup"))
        }
    }

    @Test
    fun extractIngredientsFromDay_splitsIngredientsCorrectly() {
        val day = MealPlanDay(
            dayName = "Monday",
            breakfast = "Toast & Applesauce",
            lunch = "Rice, Chicken Nuggets",
            dinner = "Butter Noodles",
            snacks = "Pretzels and Water"
        )

        val ingredients = AiMenuPlanner.extractIngredientsFromDay(day)

        assertTrue(ingredients.contains("Toast"))
        assertTrue(ingredients.contains("Applesauce"))
        assertTrue(ingredients.contains("Rice"))
        assertTrue(ingredients.contains("Chicken Nuggets"))
        assertTrue(ingredients.contains("Butter Noodles"))
        assertTrue(ingredients.contains("Pretzels"))
        assertTrue(ingredients.contains("Water"))
    }
}
