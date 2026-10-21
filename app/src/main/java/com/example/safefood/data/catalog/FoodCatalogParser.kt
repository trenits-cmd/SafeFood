package com.example.safefood.data.catalog

import com.example.safefood.data.model.FoodCategory
import com.example.safefood.data.model.FoodItem
import com.example.safefood.data.model.FoodRatings
import com.example.safefood.data.model.NutrientType

data class CatalogFoodRow(
    val name: String,
    val categoryName: String
)

object FoodCatalogParser {

    val defaultCategories: List<FoodCategory> = listOf(
        FoodCategory(id = "cat_dairy", name = "Dairy", description = "Milk, cheeses, yogurt & butter"),
        FoodCategory(id = "cat_fruits", name = "Fruits", description = "Fresh, dried & citrus fruits"),
        FoodCategory(id = "cat_legumes", name = "Legumes", description = "Beans, lentils, peas & soy"),
        FoodCategory(id = "cat_meat", name = "Meat", description = "Poultry, beef, pork, lamb & cuts"),
        FoodCategory(id = "cat_vegetables", name = "Vegetables", description = "Leafy greens, roots & squash"),
        FoodCategory(id = "cat_carbs", name = "Carbohydrates", description = "Grains, rices, oats & pastas")
    )

    fun parseCsv(csv: String): List<CatalogFoodRow> {
        return csv.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .dropWhile { line ->
                val header = line.lowercase()
                header.startsWith("food item") || header.startsWith("name")
            }
            .mapNotNull { line ->
                val comma = line.indexOf(',')
                if (comma <= 0 || comma == line.lastIndex) return@mapNotNull null
                val name = line.substring(0, comma).trim().trim('"')
                val category = line.substring(comma + 1).trim().trim('"')
                if (name.isEmpty() || category.isEmpty()) null
                else CatalogFoodRow(name = name, categoryName = category)
            }
            .distinctBy { it.name.lowercase() }
            .toList()
    }

    fun toFoodItems(
        rows: List<CatalogFoodRow>,
        categories: List<FoodCategory> = defaultCategories
    ): List<FoodItem> {
        val categoryMap = categories.associate { it.name.lowercase() to it.id }
        return rows.map { row ->
            val categoryId = categoryMap[row.categoryName.lowercase()] ?: categories.first().id
            FoodItem(
                id = stableFoodId(row.name),
                categoryId = categoryId,
                name = row.name,
                rating = FoodRatings.UNRATED,
                lastRatedTimestamp = null,
                notes = "",
                nutrientTags = nutrientsFor(row.categoryName),
                season = seasonFor(row.name, row.categoryName),
                imageUrl = FoodImageResolver.urlFor(row.name, row.categoryName)
            )
        }
    }

    fun stableFoodId(name: String): String {
        val slug = name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        return "food_$slug"
    }

    fun nutrientsFor(categoryName: String): List<NutrientType> {
        return when (categoryName.lowercase()) {
            "dairy" -> listOf(NutrientType.PROTEIN, NutrientType.HEALTHY_FAT)
            "fruits" -> listOf(NutrientType.VITAMINS_MINERALS, NutrientType.FIBER)
            "legumes" -> listOf(NutrientType.PROTEIN, NutrientType.FIBER)
            "meat" -> listOf(NutrientType.PROTEIN, NutrientType.HEALTHY_FAT)
            "vegetables" -> listOf(NutrientType.VITAMINS_MINERALS, NutrientType.FIBER)
            "carbohydrates" -> listOf(NutrientType.CARBOHYDRATE, NutrientType.FIBER)
            else -> listOf(NutrientType.VITAMINS_MINERALS)
        }
    }

    fun seasonFor(name: String, categoryName: String): String {
        val key = name.lowercase()
        return when {
            categoryName.equals("Dairy", true) ||
                categoryName.equals("Legumes", true) ||
                categoryName.equals("Meat", true) ||
                categoryName.equals("Carbohydrates", true) -> "All Year"
            listOf("strawberry", "cherry", "asparagus", "artichoke", "pea", "arugula", "radish")
                .any { key.contains(it) } -> "Spring"
            listOf("peach", "plum", "berry", "melon", "watermelon", "tomato", "cucumber", "zucchini", "corn")
                .any { key.contains(it) } -> "Summer"
            listOf("apple", "pear", "pumpkin", "squash", "cranberry", "grape", "pomegranate", "fig", "sweet potato")
                .any { key.contains(it) } -> "Autumn"
            listOf("orange", "clementine", "mandarin", "grapefruit", "lemon", "lime", "kale", "brussels", "cabbage", "leek")
                .any { key.contains(it) } -> "Winter"
            else -> "All Year"
        }
    }
}
