package com.example.safefood.ai

data class RecipeIngredient(
    val name: String,
    val amount: String
)

data class MealRecipe(
    val title: String,
    val servings: Int = 2,
    val prepTime: String = "5 min",
    val cookTime: String = "15 min",
    val ingredients: List<RecipeIngredient>,
    val instructions: List<String>,
    val description: String = ""
)
