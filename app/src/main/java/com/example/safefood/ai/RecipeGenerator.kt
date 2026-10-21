package com.example.safefood.ai

import com.example.safefood.data.catalog.MealDishResolver
import java.util.Locale

object RecipeGenerator {
    
    fun generateRecipe(label: String, mealText: String, portions: Int = 2): MealRecipe {
        val text = mealText.lowercase(Locale.US).trim()
        if (text.isBlank()) {
            return MealRecipe(
                title = "Custom Meal",
                servings = portions,
                ingredients = emptyList(),
                instructions = listOf("Add your ingredients and prepare as desired.")
            )
        }
        
        val ingredients = mutableListOf<RecipeIngredient>()
        val parts = mealText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        
        for (part in parts) {
            val baseAmount = when {
                part.lowercase().contains("rice") || part.lowercase().contains("pasta") || part.lowercase().contains("bulgur") || part.lowercase().contains("quinoa") -> "100g"
                part.lowercase().contains("chicken") || part.lowercase().contains("beef") || part.lowercase().contains("pork") || part.lowercase().contains("turkey") || part.lowercase().contains("fish") -> "150g"
                part.lowercase().contains("potato") -> "200g"
                part.lowercase().contains("bean") || part.lowercase().contains("lentil") -> "100g"
                part.lowercase().contains("cheese") -> "50g"
                part.lowercase().contains("oat") || part.lowercase().contains("granola") -> "80g"
                part.lowercase().contains("yogurt") || part.lowercase().contains("milk") -> "150ml"
                part.lowercase().contains("apple") || part.lowercase().contains("pear") || part.lowercase().contains("banana") -> "100g"
                else -> "80g"
            }
            val scaled = scaleAmount(baseAmount, portions)
            ingredients.add(RecipeIngredient(part, scaled))
        }
        
        val dish = MealDishResolver.resolve(label, mealText)
        val title = dish?.title ?: "Meal"
        val isBreakfast = label.contains("breakfast", true)
        val isSnack = label.contains("snack", true)
        
        val instructions = when {
            isBreakfast -> listOf(
                "Prepare your ingredients by washing/chopping as needed.",
                "Combine in a bowl or cook as preferred (e.g., heat oats with milk or mix yogurt with fruit).",
                "Adjust seasoning to taste and serve hot or cold.",
                "Enjoy your meal!"
            )
            isSnack -> listOf(
                "Arrange all ingredients on a plate.",
                "Enjoy fresh as a snack!"
            )
            text.contains("rice") || text.contains("bulgur") || text.contains("quinoa") -> listOf(
                "Cook the grain according to package instructions or use pre-cooked.",
                "If using meat, cook in a pan over medium-high heat for 10-12 minutes until done.",
                "Chop vegetables and add to the pan for 2-3 more minutes.",
                "Combine everything with the grain, adjust seasoning, and serve.",
                "Plate your ${title.lowercase()} and enjoy!"
            )
            text.contains("potato") -> listOf(
                "Dice potatoes and boil for 10-12 minutes until tender, or roast at 200°C for 20-25 minutes.",
                "Cook your protein in a separate pan if needed.",
                "Combine potatoes with protein and vegetables.",
                "Season to taste and serve hot."
            )
            text.contains("pasta") -> listOf(
                "Boil pasta until al dente, drain reserving a small amount of pasta water.",
                "Cook or warm protein and vegetables in a large pan.",
                "Add pasta to pan with a splash of water, toss everything together.",
                "Serve hot with your preferred seasoning."
            )
            else -> listOf(
                "Prep all ingredients by washing and cutting as needed.",
                "Cook protein first if raw, then add vegetables.",
                "Combine all ingredients in one dish, heat if desired.",
                "Adjust to taste and plate your meal.",
                "Enjoy!"
            )
        }
        
        val (prep, cook) = when {
            isBreakfast -> "5 min" to "5 min"
            isSnack -> "0-2 min" to "0 min"
            else -> "10 min" to "20 min"
        }
        
        return MealRecipe(
            title = title,
            servings = portions,
            prepTime = prep,
            cookTime = cook,
            ingredients = ingredients,
            instructions = instructions,
            description = "A balanced ${title.lowercase()} made from your safe ingredients."
        )
    }
    
    private fun scaleAmount(amount: String, portions: Int): String {
        val basePortions = 2
        val factor = portions.toDouble() / basePortions.toDouble()
        val numRegex = Regex("(\\d+(?:\\.\\d+)?)")
        val match = numRegex.find(amount)
        if (match != null) {
            val num = match.value.toDouble()
            val scaled = (num * factor).let { if (it % 1.0 == 0.0) it.toInt().toString() else String.format("%.1f", it).removeSuffix(".0") }
            return amount.replace(numRegex, scaled)
        }
        return amount
    }
}