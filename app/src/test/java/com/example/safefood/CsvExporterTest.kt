package com.example.safefood

import com.example.safefood.data.export.CsvExporter
import com.example.safefood.data.model.FoodCategory
import com.example.safefood.data.model.FoodItem
import com.example.safefood.data.model.NutrientType
import com.example.safefood.data.model.ShoppingItem
import com.example.safefood.data.model.ShoppingItemStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    @Test
    fun exportFoodsToCsv_containsHeaderAndFoodDetails() {
        val categories = listOf(
            FoodCategory(id = "cat_1", name = "Dairy")
        )
        val foods = listOf(
            FoodItem(
                id = "item_1",
                categoryId = "cat_1",
                name = "Whole Milk",
                rating = 5,
                notes = "Wholesome and safe",
                nutrientTags = listOf(NutrientType.PROTEIN)
            )
        )

        val csv = CsvExporter.exportFoodsToCsv(foods, categories)

        assertTrue(csv.contains("Food Name,Category,Category ID,Rating"))
        assertTrue(csv.contains("Whole Milk"))
        assertTrue(csv.contains("Dairy"))
        assertTrue(csv.contains(",5,"))
        assertTrue(csv.contains("Wholesome and safe"))
        assertTrue(csv.contains("Proteins"))
    }

    @Test
    fun exportShoppingListToCsv_containsItemStatus() {
        val list = listOf(
            ShoppingItem(id = "shop_1", name = "White Rice", status = ShoppingItemStatus.ALREADY_HAVE),
            ShoppingItem(id = "shop_2", name = "Applesauce", status = ShoppingItemStatus.IN_CART)
        )

        val csv = CsvExporter.exportShoppingListToCsv(list)

        assertTrue(csv.contains("ID,Item Name,Category,Status,Source Meal"))
        assertTrue(csv.contains("White Rice"))
        assertTrue(csv.contains("ALREADY_HAVE"))
        assertTrue(csv.contains("Applesauce"))
        assertTrue(csv.contains("IN_CART"))
    }
}
