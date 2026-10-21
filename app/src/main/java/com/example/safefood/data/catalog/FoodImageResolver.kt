package com.example.safefood.data.catalog

/**
 * Liefert zu jedem Lebensmittel ein konkretes, offline verfuegbares Foto.
 *
 * Reihenfolge der Aufloesung:
 *  1. [FoodImageCatalog.images] - exakter Treffer fuer Katalog- und Zusatz-Lebensmittel
 *     (Quelle: Wikimedia Commons / Wikipedia, Lizenzangaben in `assets/food_image_credits.json`).
 *  2. unscharfer Treffer ("Cheddar" -> "cheddar cheese", "Cherry Tomatoes" -> "cherry tomato").
 *  3. neutrales Kategorie-Foto (Milchprodukte, Obst, Huelsenfruechte, Fleisch, Gemuese, Kohlenhydrate),
 *     damit auch selbst angelegte Lebensmittel nie ein leeres oder falsches Netz-Bild bekommen.
 *
 * Es werden bewusst keine externen URLs mehr verwendet: alle Fotos liegen in `assets/food_images/`.
 */
object FoodImageResolver {

    fun urlFor(name: String, category: String): String {
        val key = normalize(name)

        FoodImageCatalog.images[key]?.let { return it }
        bestFuzzyMatch(key)?.let { return it }

        val categoryKey = fallbackKeyFor(category)
        return FoodImageCatalog.categoryFallbacks[categoryKey]
            ?: FoodImageCatalog.categoryFallbacks.getValue("carbohydrates")
    }

    /** Normalisiert Namen so, dass Katalog, Fotos und Nutzereingaben zusammenfinden. */
    fun normalize(name: String): String {
        return name.lowercase()
            .replace(Regex("\\(.*?\\)"), " ")
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Sucht das spezifischste Katalogfoto, dessen Name alle aussagekraeftigen Woerter
     * der Eingabe enthaelt. So wird "Cheddar" zu "cheddar cheese", aber "Pear" nie zu "peas".
     */
    private fun bestFuzzyMatch(key: String): String? {
        val queryTokens = tokens(key) - STOP_WORDS
        if (queryTokens.isEmpty()) return null

        var bestUrl: String? = null
        var bestScore = Double.NEGATIVE_INFINITY

        for ((candidate, url) in FoodImageCatalog.images) {
            val candidateTokens = tokens(candidate) - STOP_WORDS
            if (candidateTokens.isEmpty()) continue
            if (!candidateTokens.containsAll(queryTokens)) continue

            val extraTokens = candidateTokens.size - queryTokens.size
            // Wenige Zusatzwoerter und eine lange Uebereinstimmung sind besser.
            var score = queryTokens.size * 2.0 - extraTokens * 0.5
            if (candidate.startsWith(key)) score += 1.0
            if (candidate == key) score += 5.0

            if (score > bestScore) {
                bestScore = score
                bestUrl = url
            }
        }
        return bestUrl
    }

    private fun tokens(value: String): Set<String> {
        return value.split(' ')
            .filter { it.isNotBlank() }
            .map(::singular)
            .toSet()
    }

    private fun singular(word: String): String {
        return when {
            word.length > 4 && word.endsWith("ies") -> word.dropLast(3) + "y"
            word.length > 4 && word.endsWith("oes") -> word.dropLast(2)
            word.length > 3 && word.endsWith("es") &&
                (word.endsWith("ches") || word.endsWith("shes") || word.endsWith("sses")) ->
                word.dropLast(2)
            word.length > 3 && word.endsWith("s") && !word.endsWith("ss") -> word.dropLast(1)
            else -> word
        }
    }

    private fun fallbackKeyFor(category: String): String {
        val c = category.lowercase()
        return when {
            listOf("dairy", "milk", "cheese", "milch", "kase", "käse").any { c.contains(it) } -> "dairy"
            listOf("fruit", "obst").any { c.contains(it) } -> "fruits"
            listOf("legume", "bean", "lentil", "hülsen", "huelsen").any { c.contains(it) } -> "legumes"
            listOf("meat", "fish", "poultry", "fleisch", "fisch", "geflügel").any { c.contains(it) } -> "meat"
            listOf("vegetable", "gemüse", "gemuse", "salad", "salat").any { c.contains(it) } -> "vegetables"
            else -> "carbohydrates"
        }
    }

    private val STOP_WORDS = setOf(
        "raw", "cooked", "fresh", "dried", "whole", "organic", "natural", "plain",
        "sliced", "chopped", "ground", "free", "range", "the", "and", "with", "of"
    )
}
