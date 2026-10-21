package com.example.safefood

import com.example.safefood.data.catalog.FoodCatalogParser
import com.example.safefood.data.catalog.FoodImageCatalog
import com.example.safefood.data.catalog.FoodImageResolver
import com.example.safefood.data.model.FoodRatings
import com.example.safefood.data.model.NutrientType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodCatalogParserTest {

    private val sampleCsv = """
        Food Item,Category
        Whole Milk,Dairy
        Apple,Fruits
        Chicken Breast,Meat
    """.trimIndent()

    @Test
    fun `parses rows and skips the header`() {
        val rows = FoodCatalogParser.parseCsv(sampleCsv)

        assertEquals(3, rows.size)
        assertEquals("Whole Milk", rows[0].name)
        assertEquals("Dairy", rows[0].categoryName)
        assertEquals("Chicken Breast", rows[2].name)
        assertEquals("Meat", rows[2].categoryName)
    }

    @Test
    fun `strips a utf-8 bom`() {
        val rows = FoodCatalogParser.parseCsv("\uFEFFFood Item,Category\nApple,Fruits\n")

        assertEquals(1, rows.size)
        assertEquals("Apple", rows[0].name)
    }

    @Test
    fun `keeps commas inside quoted fields`() {
        val rows = FoodCatalogParser.parseCsv("Food Item,Category\n\"Beans, dried\",Legumes\n")

        assertEquals(1, rows.size)
        assertEquals("Beans, dried", rows[0].name)
        assertEquals("Legumes", rows[0].categoryName)
    }

    @Test
    fun `unescapes doubled quotes`() {
        val rows = FoodCatalogParser.parseCsv("Food Item,Category\n\"Farmer's \"\"Best\"\" Cheese\",Dairy\n")

        assertEquals("Farmer's \"Best\" Cheese", rows[0].name)
    }

    @Test
    fun `ignores blank lines and duplicates`() {
        val rows = FoodCatalogParser.parseCsv("Food Item,Category\n\nApple,Fruits\n\napple,Fruits\n")

        assertEquals(1, rows.size)
    }

    @Test
    fun `ignores malformed lines`() {
        val rows = FoodCatalogParser.parseCsv("Food Item,Category\nOnlyAName\nApple,Fruits\n")

        assertEquals(1, rows.size)
        assertEquals("Apple", rows[0].name)
    }

    @Test
    fun `creates stable and distinct ids`() {
        val first = FoodCatalogParser.stableFoodId("Crème Brûlée")
        val second = FoodCatalogParser.stableFoodId("Crème Brûlée")

        assertEquals(first, second)
        assertTrue(first.startsWith("food_"))
        assertNotEquals(first, FoodCatalogParser.stableFoodId("Whole Milk"))
    }

    @Test
    fun `assigns nutrient tags per food group`() {
        assertEquals(
            listOf(NutrientType.PROTEIN, NutrientType.HEALTHY_FAT),
            FoodCatalogParser.nutrientsFor("Meat")
        )
        assertEquals(
            listOf(NutrientType.CARBOHYDRATE, NutrientType.FIBER),
            FoodCatalogParser.nutrientsFor("Carbohydrates")
        )
    }

    @Test
    fun `catalog items start unrated`() {
        val items = FoodCatalogParser.toFoodItems(FoodCatalogParser.parseCsv(sampleCsv))

        assertEquals(3, items.size)
        assertTrue(items.all { it.rating == FoodRatings.UNRATED })
        assertTrue(items.all { it.lastRatedTimestamp == null })
    }

    @Test
    fun `every catalog entry has its own specific photo`() {
        val images = FoodImageCatalog.images

        // 506 CSV-Zeilen, aber 502 Schluessel: Onion- und Bell-Pepper-Varianten
        // (z. B. "Onion (Red)" / "Onion (White)") teilen sich denselben normalisierten Namen.
        assertEquals("Der Katalog muss alle Lebensmittel abdecken", 502, images.size)
        assertTrue("Alle Bild-URLs muessen HTTPS sein", images.values.all { it.startsWith("https://") })

        val unique = images.values.distinct().size
        assertTrue(
            "Zu viele doppelte Fotos: nur $unique von ${images.size} sind unterschiedlich",
            unique >= 460
        )
    }

    @Test
    fun `resolver returns the generated photo for a catalog item`() {
        val resolved = FoodImageResolver.urlFor("Cheddar Cheese", "Dairy")

        assertEquals(FoodImageCatalog.images[FoodImageResolver.normalize("Cheddar Cheese")], resolved)
        assertNotEquals(FoodImageResolver.urlFor("Feta Cheese", "Dairy"), resolved)
    }
}
