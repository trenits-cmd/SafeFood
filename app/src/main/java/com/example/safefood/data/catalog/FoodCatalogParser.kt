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
        FoodCategory(id = "cat_carbs", name = "Carbohydrates", description = "Grains, rices, oats & pastas"),
        FoodCategory(id = "cat_pantry", name = "Pantry & Spices", description = "Spices, oils, baking & pantry staples"),
        FoodCategory(id = "cat_blacklist", name = "Blacklist", description = "Foods rated 1 star - not safe to include")
    )

    fun parseCsv(csv: String): List<CatalogFoodRow> {
        return csv.removePrefix("\uFEFF")
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { isHeaderLine(it) }
            .mapNotNull { line ->
                val fields = splitCsvLine(line)
                if (fields.size < 2) return@mapNotNull null
                val name = fields[0].trim()
                val category = fields[1].trim()
                if (name.isEmpty() || category.isEmpty()) null
                else CatalogFoodRow(name = name, categoryName = category)
            }
            .distinctBy { it.name.lowercase() }
            .toList()
    }

    private fun isHeaderLine(line: String): Boolean {
        val lower = line.lowercase()
        return lower.startsWith("food item") ||
            lower.startsWith("name,") ||
            lower.startsWith("name;") ||
            lower.startsWith("name\t")
    }

    /**
     * Zerlegt eine CSV-Zeile und respektiert dabei Anfuehrungszeichen,
     * doppelte Anfuehrungszeichen als Escape und Kommas innerhalb von Feldern.
     */
    internal fun splitCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && inQuotes && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index++
                }
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    fields += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
            index++
        }
        fields += current.toString()

        return fields.map { it.trim() }
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
            "pantry & spices" -> listOf(NutrientType.VITAMINS_MINERALS)
            else -> listOf(NutrientType.VITAMINS_MINERALS)
        }
    }

    fun seasonFor(name: String, categoryName: String): String {
        val key = name.lowercase()
        return when {
            categoryName.equals("Dairy", true) ||
                categoryName.equals("Legumes", true) ||
                categoryName.equals("Meat", true) ||
                categoryName.equals("Carbohydrates", true) ||
                categoryName.equals("Pantry & Spices", true) -> "All Year"
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
