package com.example.safefood.data.export

import com.example.safefood.data.model.FoodCategory
import com.example.safefood.data.model.FoodItem
import com.example.safefood.data.model.ShoppingItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun exportFoodsToCsv(
        foods: List<FoodItem>,
        categories: List<FoodCategory>
    ): String {
        val categoryMap = categories.associate { it.id to it.name }
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        sb.append("ID,Food Name,Category,Rating (1-5),Last Rated Date,Notes,Nutrients\n")

        for (food in foods) {
            val categoryName = categoryMap[food.categoryId] ?: "Uncategorized"
            val ratingStr = if (food.rating > 0) "${food.rating} Stars" else "Unrated"
            val dateStr = food.lastRatedTimestamp?.let { dateFormat.format(Date(it)) } ?: "Never"
            val notesStr = food.notes
            val nutrientsStr = food.nutrientTags.joinToString("; ") { it.displayName }

            sb.append(escapeCsv(food.id)).append(",")
            sb.append(escapeCsv(food.name)).append(",")
            sb.append(escapeCsv(categoryName)).append(",")
            sb.append(escapeCsv(ratingStr)).append(",")
            sb.append(escapeCsv(dateStr)).append(",")
            sb.append(escapeCsv(notesStr)).append(",")
            sb.append(escapeCsv(nutrientsStr)).append("\n")
        }

        return sb.toString()
    }

    fun exportShoppingListToCsv(shoppingList: List<ShoppingItem>): String {
        val sb = StringBuilder()
        sb.append("ID,Item Name,Category,Status,Source Meal\n")

        for (item in shoppingList) {
            sb.append(escapeCsv(item.id)).append(",")
            sb.append(escapeCsv(item.name)).append(",")
            sb.append(escapeCsv(item.category)).append(",")
            sb.append(escapeCsv(item.status.name)).append(",")
            sb.append(escapeCsv(item.sourceMeal ?: "")).append("\n")
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            val escaped = value.replace("\"", "\"\"")
            return "\"$escaped\""
        }
        return value
    }
}
