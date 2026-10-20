package com.example.safefood

import com.example.safefood.data.export.CsvExporter
import com.example.safefood.data.model.FoodCategory
import com.example.safefood.data.model.FoodItem
import com.example.safefood.data.model.ShoppingItem
import com.example.safefood.data.model.ShoppingItemStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    @Test
    fun exportFoodsToCsv_containsHeaderAndFoodDetails() {
        val categories = listOf(
            FoodCategory(id = "cat_1", name = "Crunchy Snacks")
        )
        val foods = listOf(
            FoodItem(
                id = "item_1",
                categoryId = "cat_1",
                name = "Saltine Crackers",
                rating = 5,
                notes = "Crisp and safe",
                textureTags = listOf("Crunchy", "Dry")
            )
        )

        val csv = CsvExporter.exportFoodsToCsv(foods, categories)

        assertTrue(csv.contains("Food Name,Category,Rating"))
        assertTrue(csv.contains("Saltine Crackers"))
        assertTrue(csv.contains("Crunchy Snacks"))
        assertTrue(csv.contains("5 Stars"))
        assertTrue(csv.contains("Crisp and safe"))
        assertTrue(csv.contains("Crunchy; Dry"))
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
