package com.example.safefood

import com.example.safefood.data.catalog.FoodCatalogParser
import com.example.safefood.data.catalog.FoodImageCatalog
import com.example.safefood.data.catalog.FoodImageResolver
import com.example.safefood.data.catalog.FoodSearch
import com.example.safefood.data.model.FoodItem
import com.example.safefood.data.model.FoodRatings
import com.example.safefood.data.model.NutrientType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        // 542 CSV-Zeilen plus 102 eigene Lebensmittel und der Red-Onion-Alias.
        assertEquals("Der Katalog muss alle Lebensmittel abdecken", 641, images.size)
        assertEquals(
            "Alle Bilder muessen offline eingebettet sein",
            641,
            images.values.count { it.startsWith("file:///android_asset/food_images/") }
        )
        assertEquals(
            "Fuer jede Kategorie gibt es ein neutrales Ersatzfoto",
            7,
            FoodImageCatalog.categoryFallbacks.size
        )
    }

    @Test
    fun `resolver returns the generated photo for a catalog item`() {
        val resolved = FoodImageResolver.urlFor("Cheddar Cheese", "Dairy")

        assertEquals(FoodImageCatalog.images[FoodImageResolver.normalize("Cheddar Cheese")], resolved)
        assertNotEquals(FoodImageResolver.urlFor("Feta Cheese", "Dairy"), resolved)
    }

    @Test
    fun `resolver finds an offline photo for custom foods and never links to the internet`() {
        // Selbst angelegte Namen, die genau im Zusatzkatalog liegen.
        val cheddar = FoodImageResolver.urlFor("Cheddar", "Dairy")
        assertEquals(FoodImageCatalog.images["cheddar"], cheddar)
        assertTrue(cheddar.startsWith("file:///android_asset/food_images/"))

        // Unbekannter Name: neutrales Kategorie-Foto statt externer URL.
        val unknown = FoodImageResolver.urlFor("Zorblax Deluxe", "Fruits")
        assertEquals(FoodImageCatalog.categoryFallbacks["fruits"], unknown)
        assertFalse(unknown.startsWith("http"))

        // Unscharfer Treffer: "Cherry Tomatoes" findet "cherry tomato".
        val cherry = FoodImageResolver.urlFor("Cherry Tomatoes", "Vegetables")
        assertEquals(FoodImageCatalog.images["cherry tomato"], cherry)
    }

    @Test
    fun `red onion has its own photo`() {
        val resolved = FoodImageResolver.urlFor("Onion (Red)", "Vegetables")

        assertEquals(FoodImageCatalog.images["onion red"], resolved)
        assertNotEquals(FoodImageResolver.urlFor("Onion (Yellow)", "Vegetables"), resolved)
    }

    @Test
    fun `pantry items get the neutral pantry fallback`() {
        val resolved = FoodImageResolver.urlFor("Zorblax Seasoning", "Pantry & Spices")

        assertEquals(FoodImageCatalog.categoryFallbacks["pantry"], resolved)
    }

    @Test
    fun `parses the pantry category`() {
        val items = FoodCatalogParser.toFoodItems(
            FoodCatalogParser.parseCsv("Food Item,Category\nSalt,Pantry & Spices\n")
        )

        assertEquals("cat_pantry", items.single().categoryId)
        assertEquals("All Year", items.single().season)
    }

    @Test
    fun `search is token based and ignores word order`() {
        val redOnion = FoodItem(categoryId = "cat_vegetables", name = "Onion (Red)")

        assertTrue(FoodSearch.matches(redOnion, "red onion"))
        assertTrue(FoodSearch.matches(redOnion, "onion"))
        assertTrue(FoodSearch.matches(redOnion, "RED"))
        assertFalse(FoodSearch.matches(redOnion, "white onion"))
    }

    @Test
    fun `search normalizes plural forms`() {
        val tomato = FoodItem(categoryId = "cat_vegetables", name = "Tomato")

        assertTrue(FoodSearch.matches(tomato, "tomatoes"))
        assertFalse(FoodSearch.matches(tomato, "cherry tomatoes"))
    }
}
