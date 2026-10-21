package com.example.safefood

import com.example.safefood.ai.RecipeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeGeneratorTest {

    @Test
    fun `meal line is split into real ingredients with amounts`() {
        val recipe = RecipeGenerator.generateRecipe(
            label = "Lunch",
            mealText = "Chicken Drumstick with Bulgur and Asparagus",
            portions = 2
        )

        val names = recipe.ingredients.map { it.name }
        assertTrue(names.contains("Chicken Drumstick"))
        assertTrue(names.contains("Bulgur"))
        assertTrue(names.contains("Asparagus"))
        assertEquals(3, recipe.ingredients.size)
        assertEquals("2 pieces", recipe.ingredients.first { it.name == "Chicken Drumstick" }.amount)
        assertEquals("100 g", recipe.ingredients.first { it.name == "Bulgur" }.amount)
    }

    @Test
    fun `amounts scale with portions`() {
        val two = RecipeGenerator.generateRecipe("Lunch", "Chicken with Rice and Carrot", 2)
        val four = RecipeGenerator.generateRecipe("Lunch", "Chicken with Rice and Carrot", 4)

        assertEquals("100 g", two.ingredients.first { it.name == "Rice" }.amount)
        assertEquals("200 g", four.ingredients.first { it.name == "Rice" }.amount)
        assertEquals("300 g", two.ingredients.first { it.name == "Chicken" }.amount)
        assertEquals("600 g", four.ingredients.first { it.name == "Chicken" }.amount)
    }

    @Test
    fun `different dishes get different instructions`() {
        val bowl = RecipeGenerator.generateRecipe("Lunch", "Chicken with Rice and Carrot", 2)
        val pasta = RecipeGenerator.generateRecipe("Dinner", "Turkey with Pasta and Tomato", 2)
        val oatmeal = RecipeGenerator.generateRecipe("Breakfast", "Rolled Oats with Banana and Yogurt", 2)

        assertNotEquals(bowl.instructions, pasta.instructions)
        assertNotEquals(bowl.instructions, oatmeal.instructions)

        assertTrue(bowl.instructions.any { it.contains("Rice") })
        assertTrue(bowl.instructions.any { it.contains("Chicken") })
        assertTrue(pasta.instructions.any { it.contains("Pasta") })
        assertTrue(oatmeal.instructions.any { it.contains("Oats") || it.contains("oats") })
    }

    @Test
    fun `notes do not produce a fake recipe`() {
        val recipe = RecipeGenerator.generateRecipe(
            "Lunch",
            "Rate a protein, a vegetable and a side to build lunch.",
            2
        )

        assertTrue(recipe.ingredients.isEmpty())
        assertTrue(recipe.instructions.first().isNotBlank())
    }
}
