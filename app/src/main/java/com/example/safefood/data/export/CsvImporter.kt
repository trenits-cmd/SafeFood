package com.example.safefood.data.export

import com.example.safefood.data.model.FoodCategory
import com.example.safefood.data.model.FoodItem
import com.example.safefood.data.model.FoodRatings
import com.example.safefood.data.model.NutrientType
import java.util.UUID

object CsvImporter {
    
    data class ImportResult(
        val foodsToImport: List<FoodItem>,
        val newCategories: List<FoodCategory>,
        val errors: List<String>
    )
    
    fun parseFoodsCsv(
        csvContent: String,
        existingCategories: List<FoodCategory>,
        existingFoods: List<FoodItem>
    ): ImportResult {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return ImportResult(emptyList(), emptyList(), listOf("Empty CSV file"))
        
        val header = lines[0].split(",").map { it.trim().lowercase().replace("\"", "") }
        val errors = mutableListOf<String>()
        val newCategories = mutableListOf<FoodCategory>()
        val categoryMap = existingCategories.associateBy { it.name.lowercase() }.toMutableMap()
        
        val nameIdx = header.indexOfFirst { it.contains("name") || it.contains("food") }
        val categoryIdx = header.indexOfFirst { it.contains("category") && !it.contains("id") }
        val categoryIdIdx = header.indexOfFirst { it.contains("category") && it.contains("id") }
        val ratingIdx = header.indexOfFirst { it.contains("rating") }
        val notesIdx = header.indexOfFirst { it.contains("note") }
        val nutrientsIdx = header.indexOfFirst { it.contains("nutrient") }
        val seasonIdx = header.indexOfFirst { it.contains("season") }
        val imageIdx = header.indexOfFirst { it.contains("image") }
        val potentialIdx = header.indexOfFirst { it.contains("potential") }
        val blacklistIdx = header.indexOfFirst { it.contains("blacklist") }
        val previousCategoryIdx = header.indexOfFirst { it.contains("previous category") }
        val timestampIdx = header.indexOfFirst { it.contains("timestamp") }
        val idIdx = header.indexOfFirst { it == "id" }
        
        if (nameIdx == -1) return ImportResult(emptyList(), emptyList(), listOf("Missing 'Food Name' or 'Name' column"))
        
        val importedFoods = mutableListOf<FoodItem>()
        
        for (i in 1 until lines.size) {
            try {
                val values = parseCsvLine(lines[i])
                val name = if (nameIdx < values.size) values[nameIdx] else continue
                if (name.isBlank()) continue
                
                val categoryName = if (categoryIdx != -1 && categoryIdx < values.size) values[categoryIdx] else "Uncategorized"
                val explicitCategoryId = if (categoryIdIdx != -1 && categoryIdIdx < values.size) values[categoryIdIdx] else ""
                val categoryKey = categoryName.lowercase()
                val categoryId = when {
                    explicitCategoryId.isNotBlank() && existingCategories.any { it.id == explicitCategoryId } -> explicitCategoryId
                    categoryMap[categoryKey] != null -> categoryMap.getValue(categoryKey).id
                    else -> run {
                        val newCat = FoodCategory(
                            id = explicitCategoryId.ifBlank { UUID.randomUUID().toString() },
                            name = categoryName
                        )
                        categoryMap[categoryKey] = newCat
                        if (existingCategories.none { it.name.lowercase() == categoryKey }) {
                            newCategories.add(newCat)
                        }
                        newCat.id
                    }
                }
                
                var rating = 0
                if (ratingIdx != -1 && ratingIdx < values.size) {
                    val ratingStr = values[ratingIdx]
                    rating = ratingStr.filter { it.isDigit() }.toIntOrNull() ?: FoodRatings.coerce(ratingStr.toIntOrNull() ?: 0)
                }
                
                val notes = if (notesIdx != -1 && notesIdx < values.size) values[notesIdx] else ""
                val season = if (seasonIdx != -1 && seasonIdx < values.size) values[seasonIdx] else "All Year"
                val imageUrl = if (imageIdx != -1 && imageIdx < values.size) values[imageIdx] else ""
                val isPotentialMeal = if (potentialIdx != -1 && potentialIdx < values.size) {
                    values[potentialIdx].lowercase() == "true"
                } else false
                val isBlacklisted = if (blacklistIdx != -1 && blacklistIdx < values.size) {
                    values[blacklistIdx].lowercase() == "true"
                } else categoryName.equals("Blacklist", ignoreCase = true)
                val finalCategoryId = if (isBlacklisted) {
                    existingCategories.find { it.name.equals("Blacklist", ignoreCase = true) }?.id
                        ?: newCategories.find { it.name.equals("Blacklist", ignoreCase = true) }?.id
                        ?: run {
                            val blacklist = FoodCategory(id = "cat_blacklist", name = "Blacklist", description = "Foods rated 1 star. Hidden from recommendations.")
                            newCategories.add(blacklist)
                            blacklist.id
                        }
                } else categoryId
                val previousCategoryId = if (previousCategoryIdx != -1 && previousCategoryIdx < values.size) {
                    values[previousCategoryIdx].takeIf { it.isNotBlank() && existingCategories.any { cat -> cat.id == it } }
                } else null
                
                var timestamp: Long? = null
                if (timestampIdx != -1 && timestampIdx < values.size && values[timestampIdx].isNotBlank()) {
                    timestamp = values[timestampIdx].toLongOrNull()
                }
                
                val nutrients = mutableListOf<NutrientType>()
                if (nutrientsIdx != -1 && nutrientsIdx < values.size && values[nutrientsIdx].isNotBlank()) {
                    values[nutrientsIdx].split(";", ",").forEach { nut ->
                        val n = nut.trim().uppercase().replace(" ", "_")
                        NutrientType.values().find { it.name == n || it.displayName.equals(nut.trim(), ignoreCase = true) }
                            ?.let { nutrients.add(it) }
                    }
                }
                
                val id = when {
                    idIdx != -1 && idIdx < values.size && values[idIdx].isNotBlank() -> {
                        val importedId = values[idIdx]
                        if (existingFoods.any { it.id == importedId }) importedId
                        else existingFoods.find { it.name.equals(name, ignoreCase = true) }?.id
                            ?: importedId
                    }
                    else -> existingFoods.find { it.name.equals(name, ignoreCase = true) }?.id
                        ?: UUID.randomUUID().toString()
                }
                
                val food = FoodItem(
                    id = id,
                    categoryId = finalCategoryId,
                    name = name,
                    rating = FoodRatings.coerce(rating),
                    lastRatedTimestamp = if (FoodRatings.coerce(rating) > 0) timestamp ?: System.currentTimeMillis() else timestamp,
                    notes = notes,
                    nutrientTags = nutrients,
                    season = season,
                    imageUrl = imageUrl,
                    isPotentialMeal = isPotentialMeal,
                    previousCategoryId = previousCategoryId
                )
                importedFoods.add(food)
            } catch (e: Exception) {
                errors.add("Row $i: ${e.message}")
            }
        }
        
        return ImportResult(importedFoods, newCategories, errors)
    }
    
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (i in line.indices) {
            val char = line[i]
            when {
                char == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> {
                    current.append(char)
                }
            }
        }
        result.add(current.toString())
        return result
    }
}