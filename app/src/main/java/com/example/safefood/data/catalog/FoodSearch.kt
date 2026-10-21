package com.example.safefood.data.catalog

import com.example.safefood.data.model.FoodItem

/**
 * Token-basierte Suche fuer Lebensmittel.
 *
 * Die Reihenfolge der Woerter ist egal und Singular/Plural werden vereinheitlicht:
 * "red onion" findet also "Onion (Red)", "tomatoes" findet "Tomato".
 * Ein Suchwort muss als ganzes Wort vorkommen ("milk" findet nicht "buttermilk").
 */
object FoodSearch {

    fun matches(food: FoodItem, query: String): Boolean {
        val queryTokens = tokens(query)
        if (queryTokens.isEmpty()) return true
        val haystack = tokens(food.name + " " + food.notes)
        return queryTokens.all { it in haystack }
    }

    internal fun tokens(value: String): Set<String> {
        return value.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(' ')
            .filter { it.isNotBlank() }
            .map(::singular)
            .toSet()
    }

    internal fun singular(word: String): String {
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
}
