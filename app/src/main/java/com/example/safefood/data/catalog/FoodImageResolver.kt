package com.example.safefood.data.catalog

/**
 * Liefert zu jedem Lebensmittel ein konkretes Foto.
 *
 * Reihenfolge der Aufloesung:
 *  1. [FoodImageCatalog] - generierte, lebensmittelspezifische Fotos fuer alle 506 Katalogeintraege
 *     (Quelle: Wikimedia Commons / Wikipedia, Lizenzangaben in `assets/food_image_credits.json`).
 *  2. kuratierte Unsplash-Treffer fuer Namen, die nicht im Katalog stehen.
 *  3. Kategorie-Pool, damit nie ein leeres Bild entsteht.
 */
object FoodImageResolver {

    private const val FALLBACK =
        "https://images.unsplash.com/photo-1498837167922-ddd27525d352?w=500&auto=format&fit=crop&q=60"

    fun urlFor(name: String, category: String): String {
        val key = normalize(name)

        FoodImageCatalog.images[key]?.let { return it }
        exact[key]?.let { return it }

        val contained = exact.keys
            .filter { it.length >= 4 && (key.contains(it) || it.contains(key)) }
            .maxByOrNull { it.length }
        if (contained != null) return exact.getValue(contained)

        val pool = categoryPools[category.lowercase()] ?: genericPool
        return pool[Math.floorMod(key.hashCode(), pool.size)]
    }

    /** Normalisiert Namen so, dass Katalog, Fotos und Nutzereingaben zusammenfinden. */
    fun normalize(name: String): String {
        return name.lowercase()
            .replace(Regex("\\(.*?\\)"), " ")
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private val exact = mapOf(
        "whole milk" to "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=500&auto=format&fit=crop&q=60",
        "skim milk" to "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=500&auto=format&fit=crop&q=60",
        "goat milk" to "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=500&auto=format&fit=crop&q=60",
        "butter" to "https://images.unsplash.com/photo-1589985270826-4b7bb135bc9d?w=500&auto=format&fit=crop&q=60",
        "ghee" to "https://images.unsplash.com/photo-1589985270826-4b7bb135bc9d?w=500&auto=format&fit=crop&q=60",
        "greek yogurt" to "https://images.unsplash.com/photo-1488477181946-6428a0291777?w=500&auto=format&fit=crop&q=60",
        "yogurt" to "https://images.unsplash.com/photo-1488477181946-6428a0291777?w=500&auto=format&fit=crop&q=60",
        "frozen yogurt" to "https://images.unsplash.com/photo-1488477181946-6428a0291777?w=500&auto=format&fit=crop&q=60",
        "cheddar cheese" to "https://images.unsplash.com/photo-1618160702438-9b02ab6515c9?w=500&auto=format&fit=crop&q=60",
        "cheddar" to "https://images.unsplash.com/photo-1618160702438-9b02ab6515c9?w=500&auto=format&fit=crop&q=60",
        "brie cheese" to "https://images.unsplash.com/photo-1534422298391-e4f8c172dddb?w=500&auto=format&fit=crop&q=60",
        "brie" to "https://images.unsplash.com/photo-1534422298391-e4f8c172dddb?w=500&auto=format&fit=crop&q=60",
        "camembert" to "https://images.unsplash.com/photo-1534422298391-e4f8c172dddb?w=500&auto=format&fit=crop&q=60",
        "gouda cheese" to "https://images.unsplash.com/photo-1452195100486-9cc805987862?w=500&auto=format&fit=crop&q=60",
        "gouda" to "https://images.unsplash.com/photo-1452195100486-9cc805987862?w=500&auto=format&fit=crop&q=60",
        "mozzarella cheese" to "https://images.unsplash.com/photo-1584288019318-7b98f2604d70?w=500&auto=format&fit=crop&q=60",
        "mozzarella" to "https://images.unsplash.com/photo-1584288019318-7b98f2604d70?w=500&auto=format&fit=crop&q=60",
        "burrata" to "https://images.unsplash.com/photo-1584288019318-7b98f2604d70?w=500&auto=format&fit=crop&q=60",
        "feta cheese" to "https://images.unsplash.com/photo-1559561853-08451507cbe7?w=500&auto=format&fit=crop&q=60",
        "feta" to "https://images.unsplash.com/photo-1559561853-08451507cbe7?w=500&auto=format&fit=crop&q=60",
        "parmesan cheese" to "https://images.unsplash.com/photo-1554998171-894172f32997?w=500&auto=format&fit=crop&q=60",
        "parmesan" to "https://images.unsplash.com/photo-1554998171-894172f32997?w=500&auto=format&fit=crop&q=60",
        "pecorino cheese" to "https://images.unsplash.com/photo-1554998171-894172f32997?w=500&auto=format&fit=crop&q=60",
        "blue cheese" to "https://images.unsplash.com/photo-1486297678162-eb2a19b0a32d?w=500&auto=format&fit=crop&q=60",
        "roquefort cheese" to "https://images.unsplash.com/photo-1486297678162-eb2a19b0a32d?w=500&auto=format&fit=crop&q=60",
        "gorgonzola cheese" to "https://images.unsplash.com/photo-1486297678162-eb2a19b0a32d?w=500&auto=format&fit=crop&q=60",
        "ice cream" to "https://images.unsplash.com/photo-1501443762994-82bd5dace89a?w=500&auto=format&fit=crop&q=60",
        "heavy cream" to "https://images.unsplash.com/photo-1537139179934-202d08383e40?w=500&auto=format&fit=crop&q=60",
        "sour cream" to "https://images.unsplash.com/photo-1537139179934-202d08383e40?w=500&auto=format&fit=crop&q=60",
        "whipped cream" to "https://images.unsplash.com/photo-1537139179934-202d08383e40?w=500&auto=format&fit=crop&q=60",
        "cottage cheese" to "https://images.unsplash.com/photo-1628088062854-d1870b4553da?w=500&auto=format&fit=crop&q=60",
        "cream cheese" to "https://images.unsplash.com/photo-1628088062854-d1870b4553da?w=500&auto=format&fit=crop&q=60",
        "ricotta cheese" to "https://images.unsplash.com/photo-1588166524941-3bf61a9c41db?w=500&auto=format&fit=crop&q=60",
        "mascarpone" to "https://images.unsplash.com/photo-1588166524941-3bf61a9c41db?w=500&auto=format&fit=crop&q=60",
        "halloumi" to "https://images.unsplash.com/photo-1559561853-08451507cbe7?w=500&auto=format&fit=crop&q=60",
        "paneer" to "https://images.unsplash.com/photo-1567188040759-fb8a883dc6d8?w=500&auto=format&fit=crop&q=60",
        "kefir" to "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=500&auto=format&fit=crop&q=60",
        "buttermilk" to "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=500&auto=format&fit=crop&q=60",

        "apple" to "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=500&auto=format&fit=crop&q=60",
        "pear" to "https://images.unsplash.com/photo-1514733670139-4d87a1941d55?w=500&auto=format&fit=crop&q=60",
        "banana" to "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=500&auto=format&fit=crop&q=60",
        "orange" to "https://images.unsplash.com/photo-1611080626919-7cf5a9dbab5b?w=500&auto=format&fit=crop&q=60",
        "mandarin orange" to "https://images.unsplash.com/photo-1611080626919-7cf5a9dbab5b?w=500&auto=format&fit=crop&q=60",
        "clementine" to "https://images.unsplash.com/photo-1611080626919-7cf5a9dbab5b?w=500&auto=format&fit=crop&q=60",
        "tangerine" to "https://images.unsplash.com/photo-1611080626919-7cf5a9dbab5b?w=500&auto=format&fit=crop&q=60",
        "strawberry" to "https://images.unsplash.com/photo-1464965911861-746a04b4bca6?w=500&auto=format&fit=crop&q=60",
        "blueberry" to "https://images.unsplash.com/photo-1498557850523-fd3d118b962e?w=500&auto=format&fit=crop&q=60",
        "raspberry" to "https://images.unsplash.com/photo-1498557850523-fd3d118b962e?w=500&auto=format&fit=crop&q=60",
        "blackberry" to "https://images.unsplash.com/photo-1498557850523-fd3d118b962e?w=500&auto=format&fit=crop&q=60",
        "grape" to "https://images.unsplash.com/photo-1537640538966-79f369143f8f?w=500&auto=format&fit=crop&q=60",
        "watermelon" to "https://images.unsplash.com/photo-1587049352847-4a222e784d38?w=500&auto=format&fit=crop&q=60",
        "cantaloupe" to "https://images.unsplash.com/photo-1571575173700-afb9492d6ca2?w=500&auto=format&fit=crop&q=60",
        "honeydew melon" to "https://images.unsplash.com/photo-1571575173700-afb9492d6ca2?w=500&auto=format&fit=crop&q=60",
        "lemon" to "https://images.unsplash.com/photo-1538883652877-628f8f2b7473?w=500&auto=format&fit=crop&q=60",
        "lime" to "https://images.unsplash.com/photo-1544145945-f90431865c7a?w=500&auto=format&fit=crop&q=60",
        "pineapple" to "https://images.unsplash.com/photo-1550258987-190a2d41a8ba?w=500&auto=format&fit=crop&q=60",
        "mango" to "https://images.unsplash.com/photo-1553279768-865429fa0078?w=500&auto=format&fit=crop&q=60",
        "kiwi" to "https://images.unsplash.com/photo-1585059895050-66c5b2ce0b74?w=500&auto=format&fit=crop&q=60",
        "avocado" to "https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?w=500&auto=format&fit=crop&q=60",
        "peach" to "https://images.unsplash.com/photo-1629828874514-c1c530785f26?w=500&auto=format&fit=crop&q=60",
        "plum" to "https://images.unsplash.com/photo-1595855759920-86582396756a?w=500&auto=format&fit=crop&q=60",
        "cherry" to "https://images.unsplash.com/photo-1528821154947-1d85d9472f3d?w=500&auto=format&fit=crop&q=60",
        "pomegranate" to "https://images.unsplash.com/photo-1609428079875-8f62e4c0c0f4?w=500&auto=format&fit=crop&q=60",
        "fig" to "https://images.unsplash.com/photo-1577096839595-c29c61b6b3a7?w=500&auto=format&fit=crop&q=60",
        "coconut" to "https://images.unsplash.com/photo-1580984467541-4c9d1d0d0b1a?w=500&auto=format&fit=crop&q=60",
        "papaya" to "https://images.unsplash.com/photo-1517282009859-f000ec3b26fe?w=500&auto=format&fit=crop&q=60",
        "grapefruit" to "https://images.unsplash.com/photo-1457282367193-e3b79c57d4a4?w=500&auto=format&fit=crop&q=60",
        "date" to "https://images.unsplash.com/photo-1596591606975-97ee5de7167a?w=500&auto=format&fit=crop&q=60",
        "cranberry" to "https://images.unsplash.com/photo-1574217992323-0c3266c4f80c?w=500&auto=format&fit=crop&q=60",

        "lentils" to "https://images.unsplash.com/photo-1585998016460-2d2cdbf3e85a?w=500&auto=format&fit=crop&q=60",
        "red lentils" to "https://images.unsplash.com/photo-1585998016460-2d2cdbf3e85a?w=500&auto=format&fit=crop&q=60",
        "chickpeas garbanzo beans" to "https://images.unsplash.com/photo-1515543904379-3d757afe72e3?w=500&auto=format&fit=crop&q=60",
        "black beans" to "https://images.unsplash.com/photo-1585998016460-2d2cdbf3e85a?w=500&auto=format&fit=crop&q=60",
        "kidney beans" to "https://images.unsplash.com/photo-1551462147-ff29053bfc14?w=500&auto=format&fit=crop&q=60",
        "peanuts" to "https://images.unsplash.com/photo-1563227812-0ea4c22e6cc8?w=500&auto=format&fit=crop&q=60",
        "peanut butter" to "https://images.unsplash.com/photo-1621939514649-280e2ee25f60?w=500&auto=format&fit=crop&q=60",
        "tofu" to "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&auto=format&fit=crop&q=60",
        "hummus" to "https://images.unsplash.com/photo-1571068316344-75bc76f77890?w=500&auto=format&fit=crop&q=60",
        "edamame" to "https://images.unsplash.com/photo-1498654200943-1088dd73bcbd?w=500&auto=format&fit=crop&q=60",
        "green peas" to "https://images.unsplash.com/photo-1587735243615-c03f25aaff15?w=500&auto=format&fit=crop&q=60",
        "snap peas" to "https://images.unsplash.com/photo-1587735243615-c03f25aaff15?w=500&auto=format&fit=crop&q=60",

        "chicken breast" to "https://images.unsplash.com/photo-1604503468506-a8da13d82791?w=500&auto=format&fit=crop&q=60",
        "chicken thigh" to "https://images.unsplash.com/photo-1604503468506-a8da13d82791?w=500&auto=format&fit=crop&q=60",
        "whole chicken" to "https://images.unsplash.com/photo-1604503468506-a8da13d82791?w=500&auto=format&fit=crop&q=60",
        "turkey breast" to "https://images.unsplash.com/photo-1574672280600-4accfa5b6f98?w=500&auto=format&fit=crop&q=60",
        "beef steak" to "https://images.unsplash.com/photo-1603048588665-791ca8aea617?w=500&auto=format&fit=crop&q=60",
        "ribeye steak" to "https://images.unsplash.com/photo-1603048588665-791ca8aea617?w=500&auto=format&fit=crop&q=60",
        "sirloin steak" to "https://images.unsplash.com/photo-1603048588665-791ca8aea617?w=500&auto=format&fit=crop&q=60",
        "filet mignon" to "https://images.unsplash.com/photo-1603048588665-791ca8aea617?w=500&auto=format&fit=crop&q=60",
        "ground beef" to "https://images.unsplash.com/photo-1607623814075-e51df1bdc82f?w=500&auto=format&fit=crop&q=60",
        "bacon" to "https://images.unsplash.com/photo-1526367790999-0150786686a2?w=500&auto=format&fit=crop&q=60",
        "ham" to "https://images.unsplash.com/photo-1607623814075-e51df1bdc82f?w=500&auto=format&fit=crop&q=60",
        "salami" to "https://images.unsplash.com/photo-1541529086526-db283c563270?w=500&auto=format&fit=crop&q=60",
        "pork chop" to "https://images.unsplash.com/photo-1607623814075-e51df1bdc82f?w=500&auto=format&fit=crop&q=60",
        "lamb chop" to "https://images.unsplash.com/photo-1544025162-d76694265947?w=500&auto=format&fit=crop&q=60",
        "rack of lamb" to "https://images.unsplash.com/photo-1544025162-d76694265947?w=500&auto=format&fit=crop&q=60",
        "salmon" to "https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?w=500&auto=format&fit=crop&q=60",

        "broccoli" to "https://images.unsplash.com/photo-1459411621453-7b0397aeed4f?w=500&auto=format&fit=crop&q=60",
        "cauliflower" to "https://images.unsplash.com/photo-1568584711205-3277977a7f45?w=500&auto=format&fit=crop&q=60",
        "spinach" to "https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=500&auto=format&fit=crop&q=60",
        "kale" to "https://images.unsplash.com/photo-1524179091875-bf99a9b6d377?w=500&auto=format&fit=crop&q=60",
        "romaine lettuce" to "https://images.unsplash.com/photo-1622206151226-18ca2c9ab4a1?w=500&auto=format&fit=crop&q=60",
        "iceberg lettuce" to "https://images.unsplash.com/photo-1622206151226-18ca2c9ab4a1?w=500&auto=format&fit=crop&q=60",
        "carrot" to "https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?w=500&auto=format&fit=crop&q=60",
        "russet potato" to "https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=500&auto=format&fit=crop&q=60",
        "red potato" to "https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=500&auto=format&fit=crop&q=60",
        "yukon gold potato" to "https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=500&auto=format&fit=crop&q=60",
        "sweet potato" to "https://images.unsplash.com/photo-1596097635121-14b63b7a0c23?w=500&auto=format&fit=crop&q=60",
        "onion yellow" to "https://images.unsplash.com/photo-1508747703725-719777637510?w=500&auto=format&fit=crop&q=60",
        "onion red" to "https://images.unsplash.com/photo-1508747703725-719777637510?w=500&auto=format&fit=crop&q=60",
        "garlic" to "https://images.unsplash.com/photo-1540148426995-1770efd4fb54?w=500&auto=format&fit=crop&q=60",
        "tomato" to "https://images.unsplash.com/photo-1592924357228-91a4daadcfea?w=500&auto=format&fit=crop&q=60",
        "cherry tomato" to "https://images.unsplash.com/photo-1592924357228-91a4daadcfea?w=500&auto=format&fit=crop&q=60",
        "cucumber" to "https://images.unsplash.com/photo-1449300079323-02e209d9d3a6?w=500&auto=format&fit=crop&q=60",
        "zucchini" to "https://images.unsplash.com/photo-1563252722-6434563a985d?w=500&auto=format&fit=crop&q=60",
        "eggplant" to "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?w=500&auto=format&fit=crop&q=60",
        "bell pepper red" to "https://images.unsplash.com/photo-1563565375-f3fdfdbefa83?w=500&auto=format&fit=crop&q=60",
        "bell pepper green" to "https://images.unsplash.com/photo-1563565375-f3fdfdbefa83?w=500&auto=format&fit=crop&q=60",
        "pumpkin" to "https://images.unsplash.com/photo-1506843107748-2b10f545163f?w=500&auto=format&fit=crop&q=60",
        "butternut squash" to "https://images.unsplash.com/photo-1570586437263-038629a0a0c0?w=500&auto=format&fit=crop&q=60",
        "asparagus" to "https://images.unsplash.com/photo-1519996529931-28324d5a630e?w=500&auto=format&fit=crop&q=60",
        "cabbage" to "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=500&auto=format&fit=crop&q=60",
        "beetroot" to "https://images.unsplash.com/photo-1593105544546-120d5652f62e?w=500&auto=format&fit=crop&q=60",
        "radish" to "https://images.unsplash.com/photo-1592394533824-f5bc14a35017?w=500&auto=format&fit=crop&q=60",
        "arugula" to "https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=500&auto=format&fit=crop&q=60",
        "corn" to "https://images.unsplash.com/photo-1551754655-cd27e38d2076?w=500&auto=format&fit=crop&q=60",

        "white rice" to "https://images.unsplash.com/photo-1516684732162-798406f4be23?w=500&auto=format&fit=crop&q=60",
        "brown rice" to "https://images.unsplash.com/photo-1536304993881-ff69e48df8b8?w=500&auto=format&fit=crop&q=60",
        "jasmine rice" to "https://images.unsplash.com/photo-1516684732162-798406f4be23?w=500&auto=format&fit=crop&q=60",
        "basmati rice" to "https://images.unsplash.com/photo-1516684732162-798406f4be23?w=500&auto=format&fit=crop&q=60",
        "quinoa" to "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=500&auto=format&fit=crop&q=60",
        "rolled oats" to "https://images.unsplash.com/photo-1517673132405-a56a62b18caf?w=500&auto=format&fit=crop&q=60",
        "steel cut oats" to "https://images.unsplash.com/photo-1517673132405-a56a62b18caf?w=500&auto=format&fit=crop&q=60",
        "spaghetti" to "https://images.unsplash.com/photo-1551183053-bf91a1d81141?w=500&auto=format&fit=crop&q=60",
        "penne" to "https://images.unsplash.com/photo-1621996346565-e3d5d6281298?w=500&auto=format&fit=crop&q=60",
        "macaroni" to "https://images.unsplash.com/photo-1621996346565-e3d5d6281298?w=500&auto=format&fit=crop&q=60",
        "couscous" to "https://images.unsplash.com/photo-1541529086526-db283c563270?w=500&auto=format&fit=crop&q=60",
        "barley" to "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?w=500&auto=format&fit=crop&q=60",
        "bread" to "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=500&auto=format&fit=crop&q=60",
        "polenta" to "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?w=500&auto=format&fit=crop&q=60",
        "ramen noodles" to "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=500&auto=format&fit=crop&q=60",
        "udon noodles" to "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=500&auto=format&fit=crop&q=60"
    )

    private val dairyPool = listOf(
        "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1486297678162-eb2a19b0a32d?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1452195100486-9cc805987862?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1488477181946-6428a0291777?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1618160702438-9b02ab6515c9?w=500&auto=format&fit=crop&q=60"
    )

    private val fruitPool = listOf(
        "https://images.unsplash.com/photo-1619566636858-adf3ef46400b?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1519996529931-28324d5a630e?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1464965911861-746a04b4bca6?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1611080626919-7cf5a9dbab5b?w=500&auto=format&fit=crop&q=60"
    )

    private val legumePool = listOf(
        "https://images.unsplash.com/photo-1585998016460-2d2cdbf3e85a?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1515543904379-3d757afe72e3?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1551462147-ff29053bfc14?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1498654200943-1088dd73bcbd?w=500&auto=format&fit=crop&q=60"
    )

    private val meatPool = listOf(
        "https://images.unsplash.com/photo-1604503468506-a8da13d82791?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1603048588665-791ca8aea617?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1607623814075-e51df1bdc82f?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1526367790999-0150786686a2?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1544025162-d76694265947?w=500&auto=format&fit=crop&q=60"
    )

    private val vegetablePool = listOf(
        "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1459411621453-7b0397aeed4f?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1563565375-f3fdfdbefa83?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=500&auto=format&fit=crop&q=60"
    )

    private val carbPool = listOf(
        "https://images.unsplash.com/photo-1516684732162-798406f4be23?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1551183053-bf91a1d81141?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1517673132405-a56a62b18caf?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=500&auto=format&fit=crop&q=60",
        "https://images.unsplash.com/photo-1621996346565-e3d5d6281298?w=500&auto=format&fit=crop&q=60"
    )

    private val genericPool = listOf(FALLBACK)

    private val categoryPools = mapOf(
        "dairy" to dairyPool,
        "fruits" to fruitPool,
        "legumes" to legumePool,
        "meat" to meatPool,
        "vegetables" to vegetablePool,
        "carbohydrates" to carbPool
    )
}
