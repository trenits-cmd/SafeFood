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
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        sb.append("ID,Food Name,Category,Category ID,Rating (0-5),Last Rated Timestamp,Last Rated Date,Notes,Nutrients,Season,Image URL,Is Potential Meal,Is Blacklisted\n")

        for (food in foods) {
            val categoryName = categoryMap[food.categoryId] ?: "Uncategorized"
            val dateStr = food.lastRatedTimestamp?.let { dateFormat.format(Date(it)) } ?: ""
            val nutrientsStr = food.nutrientTags.joinToString(";") { it.name }
            val isBlacklisted = categoryName.equals("Blacklist", ignoreCase = true)

            sb.append(escapeCsv(food.id)).append(",")
            sb.append(escapeCsv(food.name)).append(",")
            sb.append(escapeCsv(categoryName)).append(",")
            sb.append(escapeCsv(food.categoryId)).append(",")
            sb.append(escapeCsv(food.rating.toString())).append(",")
            sb.append(escapeCsv(food.lastRatedTimestamp?.toString() ?: "")).append(",")
            sb.append(escapeCsv(dateStr)).append(",")
            sb.append(escapeCsv(food.notes)).append(",")
            sb.append(escapeCsv(nutrientsStr)).append(",")
            sb.append(escapeCsv(food.season)).append(",")
            sb.append(escapeCsv(food.imageUrl)).append(",")
            sb.append(escapeCsv(if (food.isPotentialMeal) "true" else "false")).append(",")
            sb.append(escapeCsv(if (isBlacklisted) "true" else "false")).append("\n")
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
