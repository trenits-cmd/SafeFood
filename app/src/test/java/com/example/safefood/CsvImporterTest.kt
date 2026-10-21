package com.example.safefood

import com.example.safefood.data.export.CsvImporter
import com.example.safefood.data.model.FoodCategory
import com.example.safefood.data.model.FoodItem
import com.example.safefood.data.model.NutrientType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvImporterTest {

    private val categories = listOf(
        FoodCategory(id = "cat_dairy", name = "Dairy"),
        FoodCategory(id = "cat_blacklist", name = "Blacklist")
    )
    private val existingFoods = listOf(
        FoodItem(id = "food_1", categoryId = "cat_dairy", name = "Whole Milk", rating = 3)
    )

    @Test
    fun `imports metadata, quoted notes and blacklist flag`() {
        val csv = """
            ID,Food Name,Category,Category ID,Rating (0-5),Last Rated Timestamp,Last Rated Date,Notes,Nutrients,Season,Image URL,Is Potential Meal,Is Blacklisted,Previous Category ID
            food_1,Whole Milk,Dairy,cat_dairy,5,1700000000000,2023-11-14,"creamy, cold",Proteins;Healthy Fats,All Year,file:///milk.jpg,true,false,
            food_2,Blue Cheese,Dairy,cat_dairy,1,1700000000001,2023-11-14,strong,"Proteins,Healthy Fats",All Year,file:///cheese.jpg,false,true,cat_dairy
        """.trimIndent()

        val result = CsvImporter.parseFoodsCsv(csv, categories, existingFoods)

        assertEquals(2, result.foodsToImport.size)
        assertTrue(result.errors.isEmpty())

        val milk = result.foodsToImport.first { it.name == "Whole Milk" }
        assertEquals("food_1", milk.id)
        assertEquals(5, milk.rating)
        assertEquals("creamy, cold", milk.notes)
        assertEquals(listOf(NutrientType.PROTEIN, NutrientType.HEALTHY_FAT), milk.nutrientTags)
        assertTrue(milk.isPotentialMeal)

        val cheese = result.foodsToImport.first { it.name == "Blue Cheese" }
        assertEquals("cat_blacklist", cheese.categoryId)
        assertEquals("cat_dairy", cheese.previousCategoryId)
        assertEquals("strong", cheese.notes)
    }

    @Test
    fun `creates missing category and reuses existing id by name`() {
        val csv = """
            Food Name,Category,Category ID,Rating
            Lentil Stew,Legumes,,4
            Whole Milk,Dairy,cat_other_id,5
        """.trimIndent()

        val result = CsvImporter.parseFoodsCsv(csv, categories, existingFoods)

        assertEquals(1, result.newCategories.size)
        assertEquals("Legumes", result.newCategories.first().name)
        val milk = result.foodsToImport.first { it.name == "Whole Milk" }
        assertEquals("cat_dairy", milk.categoryId)
        assertEquals(1, result.foodsToImport.count { it.name == "Whole Milk" })
    }
}
