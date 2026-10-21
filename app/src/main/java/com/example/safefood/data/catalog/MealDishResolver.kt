package com.example.safefood.data.catalog

import java.util.Locale

/** Titel und Foto eines fertig gekochten Gerichts fuer einen Menueplan-Eintrag. */
data class MealDish(
    val title: String,
    val imageUrl: String
)

/**
 * Macht aus der Zutatenliste eines Menueplans ein erkennbares Gericht
 * ("Chicken with Rice and Carrot" -> "Chicken Rice Bowl") und waehlt ein
 * passendes Foto des gekochten Gerichts.
 *
 * Die Zuordnung ist bewusst tolerant: fehlt ein spezielles Foto, gibt es
 * immer ein neutrales Gerichtsfoto statt einer leeren Flaeche.
 */
object MealDishResolver {

    private const val ASSET_PREFIX = "file:///android_asset/food_images/"

    fun resolve(label: String, meal: String): MealDish? {
        val text = meal.lowercase(Locale.US).trim()
        if (text.isBlank()) return null
        if (text.startsWith("no ") || text.startsWith("add ") || text.startsWith("rate ")) return null

        return when {
            label.contains("breakfast", ignoreCase = true) -> breakfast(text)
            label.contains("snack", ignoreCase = true) -> snack(text)
            else -> main(text)
        }
    }

    private fun breakfast(text: String): MealDish {
        val title = when {
            has(text, "pancake", "pancakes") -> "Pancakes"
            has(text, "egg", "eggs") && has(text, "toast", "bread") -> "Egg Toast"
            has(text, "egg", "eggs") -> "Egg Breakfast"
            has(text, "oat", "oats", "porridge", "muesli", "cereal") -> "Oatmeal Bowl"
            has(text, "granola") -> "Granola Bowl"
            has(text, "yogurt", "yoghurt", "skyr", "quark") -> {
                if (hasFruit(text)) "Yogurt & Fruit Bowl" else "Yogurt Bowl"
            }
            has(text, "toast", "bread", "bagel", "croissant", "bun") -> "Breakfast Toast"
            hasFruit(text) -> "Fruit Plate"
            has(text, "cheese", "cheddar", "feta", "mozzarella", "emmental") -> "Cheese Breakfast"
            else -> "Breakfast Plate"
        }
        val image = when (title) {
            "Pancakes" -> "_dish_pancakes"
            "Egg Toast", "Egg Breakfast" -> "_dish_egg_toast"
            "Oatmeal Bowl", "Granola Bowl", "Breakfast Plate" -> "_dish_oatmeal"
            "Yogurt & Fruit Bowl", "Yogurt Bowl" -> "_dish_yogurt_fruit"
            "Breakfast Toast" -> "_dish_toast"
            "Cheese Breakfast" -> "_dish_cheese_snack"
            else -> "_dish_fruit_plate"
        }
        return MealDish(title, asset(image))
    }

    private fun snack(text: String): MealDish {
        val title = when {
            has(text, "yogurt", "yoghurt", "skyr", "quark") -> "Yogurt Bowl"
            has(text, "cheese", "cheddar", "feta", "mozzarella", "emmental", "gruyere",
                "halloumi", "manchego", "gouda", "brie", "camembert") -> "Cheese Snack"
            has(text, "bean", "beans", "lentil", "lentils", "chickpea", "chickpeas", "pea", "peas") ->
                "Bean Snack"
            hasFruit(text) -> "Fruit Plate"
            has(text, "carrot", "carrots", "cucumber", "radish", "pepper", "tomato", "turnip", "beetroot") ->
                "Veggie Sticks"
            else -> "Snack Plate"
        }
        val image = when (title) {
            "Yogurt Bowl" -> "_dish_yogurt_fruit"
            "Cheese Snack" -> "_dish_cheese_snack"
            "Bean Snack" -> "_dish_bean_snack"
            "Veggie Sticks" -> "_dish_veggie_sticks"
            else -> "_dish_fruit_plate"
        }
        return MealDish(title, asset(image))
    }

    private fun main(text: String): MealDish {
        val protein = protein(text)
        val grain = grainName(text)
        val base = when {
            has(text, "rice") -> "rice"
            has(text, "potato", "potatoes") -> "potato"
            has(text, "pasta", "noodle", "noodles", "spaghetti", "macaroni", "lasagna") -> "pasta"
            grain != null -> "grain"
            has(text, "bread", "toast", "tortilla", "wrap", "bagel") -> "bread"
            else -> "plate"
        }

        val title = when (base) {
            "rice" -> if (protein != null) "$protein Rice Bowl" else "Vegetable Rice Bowl"
            "potato" -> if (protein != null) "$protein & Potato Plate" else "Potato & Vegetable Plate"
            "pasta" -> if (protein != null) "$protein Pasta" else "Vegetable Pasta"
            "grain" -> {
                val grainLabel = grain ?: "Grain"
                if (protein != null) "$protein & $grainLabel Bowl" else "$grainLabel Bowl"
            }
            "bread" -> if (protein != null) "$protein Toast" else "Toast Plate"
            else -> if (protein != null) "$protein Plate" else "Cooked Meal"
        }

        val image = when (base) {
            "rice" -> when (protein) {
                "Chicken" -> "_dish_chicken_rice"
                "Turkey" -> "_dish_turkey_rice"
                "Beef" -> "_dish_beef_rice"
                "Pork" -> "_dish_pork_rice"
                "Fish" -> "_dish_salmon_rice"
                "Bean" -> "_dish_bean_rice"
                else -> "_dish_veg_rice"
            }
            "potato" -> if (protein != null) "_dish_meat_potato" else "_dish_veg_potato"
            "pasta" -> "_dish_pasta"
            "grain" -> "_dish_grain_bowl"
            "bread" -> "_dish_toast"
            else -> "_dish_cooked_meal"
        }
        return MealDish(title, asset(image))
    }

    private fun grainName(text: String): String? = when {
        has(text, "quinoa") -> "Quinoa"
        has(text, "bulgur") -> "Bulgur"
        has(text, "couscous") -> "Couscous"
        has(text, "barley") -> "Barley"
        has(text, "farro") -> "Farro"
        has(text, "millet") -> "Millet"
        has(text, "buckwheat") -> "Buckwheat"
        else -> null
    }

    private fun protein(text: String): String? = when {
        has(text, "chicken") -> "Chicken"
        has(text, "turkey") -> "Turkey"
        has(text, "beef", "steak", "veal") -> "Beef"
        has(text, "pork", "ham", "bacon", "sausage") -> "Pork"
        has(text, "salmon", "fish", "tuna", "cod", "trout", "mackerel", "sardine", "herring") -> "Fish"
        has(text, "bean", "beans", "lentil", "lentils", "chickpea", "chickpeas", "pea", "peas") -> "Bean"
        has(text, "egg", "eggs") -> "Egg"
        has(text, "tofu") -> "Tofu"
        has(text, "tempeh") -> "Tempeh"
        else -> null
    }

    private fun hasFruit(text: String): Boolean = has(
        text,
        "apple", "apples", "pear", "pears", "banana", "bananas", "berry", "berries",
        "orange", "oranges", "grape", "grapes", "peach", "peaches", "plum", "plums",
        "cherry", "cherries", "mango", "mangoes", "pineapple", "melon", "watermelon",
        "kiwi", "lemon", "lime", "fig", "figs", "date", "dates", "raisin", "raisins"
    )

    private fun has(text: String, vararg words: String): Boolean {
        return words.any { word ->
            Regex("(^|[^a-z0-9])" + Regex.escape(word) + "([^a-z0-9]|$)").containsMatchIn(text)
        }
    }

    private fun asset(name: String): String = ASSET_PREFIX + name + ".jpg"
}
