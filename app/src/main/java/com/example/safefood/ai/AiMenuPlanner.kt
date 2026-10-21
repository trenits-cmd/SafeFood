package com.example.safefood.ai

import com.example.safefood.data.model.FoodCategory
import com.example.safefood.data.model.FoodItem
import com.example.safefood.data.model.FoodRatings
import com.example.safefood.data.model.MealPlan
import com.example.safefood.data.model.MealPlanDay
import java.util.Calendar
import java.util.Locale
import java.util.UUID

/**
 * Builds meal plans from rated foods.
 *
 * Instead of picking random items per slot, every food is classified into a meal role
 * (grain, protein, dairy, fruit, vegetable, fat) and each meal is composed from roles:
 * breakfast needs a grain/fruit/dairy base, lunch and dinner need protein + starch + vegetable.
 * This prevents combinations like "breakfast: carrots".
 */
object AiMenuPlanner {

    enum class PlanningGoal(val label: String, val description: String) {
        SAFE_ONLY("Strict Safe Foods (4-5 Stars)", "Use only foods rated Good or Yummy"),
        BALANCED("Balanced Meals", "Combine protein, produce, and carbs from rated-okay-or-better foods"),
        LOW_PREPARATION("Low Energy / Fast Prep", "Prefer simple, low-effort safe items"),
        ROTATION_HELPER("Safe Food Rotation", "Avoid repeating the same item across days")
    }

    private enum class MealRole { GRAIN, PROTEIN, DAIRY, FRUIT, VEGETABLE, FAT }

    private class Classified(val food: FoodItem, val roles: Set<MealRole>)

    fun generateMealPlan(
        foods: List<FoodItem>,
        categories: List<FoodCategory> = emptyList(),
        goal: PlanningGoal = PlanningGoal.SAFE_ONLY,
        customInstructions: String = "",
        daysCount: Int = 3,
        preferredIds: Set<String> = emptySet(),
        onlyPreferred: Boolean = false
    ): MealPlan {
        val minRating = when (goal) {
            PlanningGoal.SAFE_ONLY, PlanningGoal.ROTATION_HELPER, PlanningGoal.LOW_PREPARATION -> FoodRatings.SAFE_MIN
            PlanningGoal.BALANCED -> FoodRatings.OKAY
        }
        val categoryNames = categories.associate { it.id to it.name }

        var pool = foods.filter { it.rating >= minRating }
        if (onlyPreferred) pool = pool.filter { it.id in preferredIds }
        pool = applyCustomInstructions(pool, customInstructions)

        val classified = pool.map { food ->
            Classified(food, rolesOf(food, categoryNames[food.categoryId] ?: ""))
        }

        val emptyMessage = when {
            onlyPreferred && foods.none { it.isPotentialMeal && it.rating >= minRating } ->
                "No 'potential meal' foods are rated ${FoodRatings.label(minRating)} or higher yet."
            foods.none { it.rating >= minRating } ->
                "No matching foods yet. Rate more items first."
            else ->
                "No foods match these instructions. Try a shorter prompt."
        }

        val count = daysCount.coerceIn(1, 14)
        val dayNames = rotatingDayNames(count)
        // Rotation memory across the whole plan (soft, only used by ROTATION_HELPER).
        val usedAcrossPlan = mutableSetOf<String>()

        val daysList = dayNames.mapIndexed { index, dayName ->
            if (classified.isEmpty()) {
                MealPlanDay(
                    dayName = dayName,
                    breakfast = emptyMessage,
                    lunch = "",
                    dinner = "",
                    snacks = ""
                )
            } else {
                buildDay(dayName, classified, goal, preferredIds, usedAcrossPlan, index)
            }
        }

        val suffix = if (onlyPreferred) " • Potential meal foods" else ""
        return MealPlan(
            id = UUID.randomUUID().toString(),
            title = "${goal.label} ($count ${if (count == 1) "Day" else "Days"})$suffix",
            days = daysList
        )
    }

    fun extractIngredientsFromDay(day: MealPlanDay): List<String> {
        val result = linkedSetOf<String>()
        val raw = listOf(day.breakfast, day.lunch, day.dinner, day.snacks)
        for (item in raw) {
            if (item.isBlank()) continue
            if (isNote(item)) continue
            val splitItems = item.split(",", "&", " and ", " with ", "+")
            for (s in splitItems) {
                val cleaned = s.trim()
                if (cleaned.isNotEmpty()) result.add(cleaned)
            }
        }
        return result.toList()
    }

    private fun isNote(text: String): Boolean {
        val lower = text.lowercase(Locale.US)
        return lower.startsWith("no matching") ||
            lower.startsWith("no foods match") ||
            lower.startsWith("no 'potential") ||
            lower.startsWith("add ")
    }

    internal fun applyCustomInstructions(pool: List<FoodItem>, instructions: String): List<FoodItem> {
        val text = instructions.lowercase(Locale.US).trim()
        if (text.isBlank()) return pool

        val excluded = Regex("""(?:no|without|exclude|avoid)\s+([a-z0-9][a-z0-9\s-]{1,40})""")
            .findAll(text)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotEmpty() }
            .toList()

        var result = pool
        if (excluded.isNotEmpty()) {
            result = result.filter { food ->
                excluded.none { token -> food.name.contains(token, ignoreCase = true) }
            }
        }

        val preferred = result.filter { food -> text.contains(food.name.lowercase(Locale.US)) }
        if (preferred.isNotEmpty()) return preferred
        return result
    }

    // ------------------------------------------------------------------ day / meal composition

    private fun buildDay(
        dayName: String,
        pool: List<Classified>,
        goal: PlanningGoal,
        preferredIds: Set<String>,
        usedAcrossPlan: MutableSet<String>,
        dayIndex: Int
    ): MealPlanDay {
        val usedToday = mutableSetOf<String>()

        val breakfast = composeBreakfast(pool, preferredIds, usedToday, usedAcrossPlan, goal)
        val lunch = composeMain(pool, preferredIds, usedToday, usedAcrossPlan, goal, dayIndex)
        val dinner = composeMain(pool, preferredIds, usedToday, usedAcrossPlan, goal, dayIndex + 1)
        val snacks = composeSnacks(pool, preferredIds, usedToday, usedAcrossPlan, goal)

        return MealPlanDay(
            dayName = dayName,
            breakfast = if (breakfast.isEmpty()) {
                "Add breakfast foods to your safe list (oats, bread, yogurt, fruit or eggs)."
            } else {
                formatMeal(breakfast)
            },
            lunch = if (lunch.isEmpty()) "Rate a protein, a vegetable and a side to build lunch." else formatMeal(lunch),
            dinner = if (dinner.isEmpty()) "Rate a protein, a vegetable and a side to build dinner." else formatMeal(dinner),
            snacks = snacks.joinToString(" & ") { it.name }
        )
    }

    private fun composeBreakfast(
        pool: List<Classified>,
        preferredIds: Set<String>,
        usedToday: MutableSet<String>,
        usedAcrossPlan: MutableSet<String>,
        goal: PlanningGoal
    ): List<FoodItem> {
        fun pickRole(roles: Set<MealRole>, predicate: (FoodItem) -> Boolean = { true }): FoodItem? =
            pick(pool, roles, preferredIds, usedToday, usedAcrossPlan, goal, predicate)

        val grain = pickRole(setOf(MealRole.GRAIN)) { isBreakfastGrain(it) }
        val fruit = pickRole(setOf(MealRole.FRUIT))
        val dairy = pickRole(setOf(MealRole.DAIRY)) { isYogurt(it) }
            ?: pickRole(setOf(MealRole.DAIRY)) { isBreakfastDairy(it) }
            ?: pickRole(setOf(MealRole.DAIRY)) { !isCheeseOnly(it) }
            ?: pickRole(setOf(MealRole.DAIRY))
        val egg = pickRole(setOf(MealRole.PROTEIN)) { isBreakfastProtein(it) }
        val spread = pickRole(setOf(MealRole.FAT)) { isSpread(it) }

        val chosen = mutableListOf<FoodItem>()
        when {
            // Full breakfast: a breakfast grain plus fruit and/or dairy.
            grain != null -> {
                chosen += grain
                (fruit ?: egg ?: spread)?.let { chosen += it }
                (dairy ?: egg)?.let { if (chosen.none { c -> c.id == it.id }) chosen += it }
            }
            // No bread/oats available: a fruit + dairy plate (e.g. "Yogurt with Apple"),
            // never a plain dinner side such as rice or potato.
            dairy != null && fruit != null -> {
                chosen += dairy
                chosen += fruit
            }
            fruit != null && spread != null -> {
                chosen += fruit
                chosen += spread
            }
            egg != null && fruit != null -> {
                chosen += egg
                chosen += fruit
            }
            fruit != null -> chosen += fruit
            dairy != null -> chosen += dairy
            egg != null -> chosen += egg
        }

        if (chosen.isEmpty()) {
            // Last resort: keep it light, never a plain vegetable.
            val lightRoles = setOf(MealRole.FRUIT, MealRole.DAIRY, MealRole.FAT)
            pickRole(lightRoles)?.let { chosen += it }
            pickRole(lightRoles)?.let { if (chosen.none { c -> c.id == it.id }) chosen += it }
        }
        return chosen.take(3)
    }

    private fun composeMain(
        pool: List<Classified>,
        preferredIds: Set<String>,
        usedToday: MutableSet<String>,
        usedAcrossPlan: MutableSet<String>,
        goal: PlanningGoal,
        variance: Int
    ): List<FoodItem> {
        fun pickRole(roles: Set<MealRole>, predicate: (FoodItem) -> Boolean = { true }): FoodItem? =
            pick(pool, roles, preferredIds, usedToday, usedAcrossPlan, goal, predicate)

        val protein = pickRole(setOf(MealRole.PROTEIN))
        val grain = pickRole(setOf(MealRole.GRAIN)) { isMainGrain(it) }
            ?: pickRole(setOf(MealRole.GRAIN))
        // Prefer a "real" vegetable (carrot, broccoli, ...) over an aromatic cooking base like onion.
        val vegetable = pickRole(setOf(MealRole.VEGETABLE)) { !isAromatic(it) }
            ?: pickRole(setOf(MealRole.VEGETABLE))

        val chosen = mutableListOf<FoodItem>()
        protein?.let { chosen += it }
        grain?.let { chosen += it }
        vegetable?.let { chosen += it }

        // If a role is missing on a sparse list, add a second vegetable or protein instead.
        if (chosen.size < 2) {
            val filler = pickRole(setOf(MealRole.VEGETABLE, MealRole.PROTEIN, MealRole.FAT))
            if (filler != null && chosen.none { it.id == filler.id }) chosen += filler
        }
        // Slight variation between lunch and dinner: if there is a second vegetable, use it sometimes.
        if (variance % 2 == 1 && chosen.size < 3) {
            pickRole(setOf(MealRole.VEGETABLE))?.let { chosen += it }
        }
        return chosen.distinctBy { it.id }.take(3)
    }

    private fun composeSnacks(
        pool: List<Classified>,
        preferredIds: Set<String>,
        usedToday: MutableSet<String>,
        usedAcrossPlan: MutableSet<String>,
        goal: PlanningGoal
    ): List<FoodItem> {
        fun pickRole(roles: Set<MealRole>, predicate: (FoodItem) -> Boolean = { true }): FoodItem? =
            pick(pool, roles, preferredIds, usedToday, usedAcrossPlan, goal, predicate)

        val chosen = mutableListOf<FoodItem>()
        // Snacks are fruit/dairy/vegetable based; fruit first, then a light dairy or a vegetable.
        val primary = pickRole(setOf(MealRole.FRUIT))
            ?: pickRole(setOf(MealRole.DAIRY)) { !isCheeseOnly(it) }
            ?: pickRole(setOf(MealRole.VEGETABLE))
        primary?.let { chosen += it }

        if (chosen.size < 2) {
            val secondary = pickRole(setOf(MealRole.DAIRY)) { isYogurt(it) }
                ?: pickRole(setOf(MealRole.VEGETABLE)) { chosen.none { c -> c.id == it.id } }
                ?: pickRole(setOf(MealRole.FAT)) { isSpread(it) }
                ?: pickRole(setOf(MealRole.DAIRY)) { chosen.none { c -> c.id == it.id } && !isCheeseOnly(it) }
            secondary?.let { chosen += it }
        }
        return chosen.distinctBy { it.id }
    }

    // ------------------------------------------------------------------ selection helpers

    private fun pick(
        pool: List<Classified>,
        roles: Set<MealRole>,
        preferredIds: Set<String>,
        usedToday: MutableSet<String>,
        usedAcrossPlan: MutableSet<String>,
        goal: PlanningGoal,
        predicate: (FoodItem) -> Boolean = { true }
    ): FoodItem? {
        val candidates = pool.filter { c ->
            c.roles.any { it in roles } && predicate(c.food) && !isDrink(c.food) && !isCondiment(c.food)
        }
        if (candidates.isEmpty()) return null

        val notToday = candidates.filter { it.food.id !in usedToday }
        val base = when {
            notToday.isNotEmpty() -> notToday
            else -> candidates
        }
        val rotated = if (goal == PlanningGoal.ROTATION_HELPER) {
            base.filter { it.food.id !in usedAcrossPlan }.ifEmpty { base }
        } else {
            base
        }

        // Shuffle first, then a stable preference sort: ties stay varied between days.
        val ranked = rotated.shuffled().sortedWith(
            if (goal == PlanningGoal.LOW_PREPARATION) {
                compareByDescending<Classified> { it.food.id in preferredIds }
                    .thenBy { lowPrepScore(it.food) }
                    .thenByDescending { it.food.rating }
            } else {
                compareByDescending<Classified> { it.food.id in preferredIds }
                    .thenByDescending { it.food.rating }
            }
        )

        val chosen = ranked.firstOrNull() ?: return null
        usedToday += chosen.food.id
        usedAcrossPlan += chosen.food.id
        return chosen.food
    }

    private fun formatMeal(items: List<FoodItem>): String {
        val names = items.map { it.name }
        return when (names.size) {
            0 -> ""
            1 -> names[0]
            2 -> "${names[0]} with ${names[1]}"
            else -> "${names[0]} with ${names[1]} and ${names[2]}"
        }
    }

    private fun lowPrepScore(food: FoodItem): Int {
        val name = food.name.lowercase(Locale.US)
        val easy = listOf(
            "yogurt", "yoghurt", "cheese", "milk", "skyr", "banana", "apple", "berry",
            "bread", "oat", "muesli", "cereal", "cracker", "hummus", "avocado", "carrot",
            "cucumber", "tomato", "egg", "nuts", "almond"
        )
        return if (easy.any { name.contains(it) }) 0 else 1
    }

    private fun isBreakfastGrain(food: FoodItem): Boolean {
        val n = food.name.lowercase(Locale.US)
        return listOf(
            "oat", "porridge", "muesli", "granola", "cereal", "bread", "toast", "bagel",
            "croissant", "pancake", "bun", "wrap", "rye", "spelt"
        ).any { n.contains(it) }
    }

    private fun isMainGrain(food: FoodItem): Boolean {
        val n = food.name.lowercase(Locale.US)
        return listOf(
            "rice", "potato", "pasta", "quinoa", "couscous", "bulgur", "barley", "noodle",
            "corn", "tortilla", "polenta", "millet", "buckwheat", "spelt"
        ).any { n.contains(it) }
    }

    private fun isBreakfastProtein(food: FoodItem): Boolean {        val n = food.name.lowercase(Locale.US)
        return listOf("egg", "cheese", "ham", "bacon", "sausage", "salmon", "nut", "seed").any { n.contains(it) }
    }

    private fun isSpread(food: FoodItem): Boolean {
        val n = food.name.lowercase(Locale.US)
        if (n.contains("oil")) return false
        return listOf("butter", "avocado", "nut", "almond", "peanut", "cashew", "seed", "tahini", "hummus")
            .any { n.contains(it) }
    }

    private fun isCheeseOnly(food: FoodItem): Boolean {
        val n = food.name.lowercase(Locale.US)
        if (n.contains("cheese")) return true
        val cheeses = listOf(
            "cheddar", "brie", "camembert", "gouda", "mozzarella", "feta", "parmesan",
            "pecorino", "roquefort", "gorgonzola", "gruyere", "emmental", "havarti",
            "jarlsberg", "manchego", "munster", "taleggio", "provolone", "edam", "ricotta",
            "mascarpone", "halloumi", "paneer", "burrata", "neufchatel"
        )
        return cheeses.any { n.contains(it) }
    }

    private fun isBreakfastDairy(food: FoodItem): Boolean {
        val n = food.name.lowercase(Locale.US)
        return listOf("yogurt", "yoghurt", "skyr", "quark", "kefir", "milk", "cream", "buttermilk")
            .any { n.contains(it) }
    }

    private fun isYogurt(food: FoodItem): Boolean {
        val n = food.name.lowercase(Locale.US)
        return listOf("yogurt", "yoghurt", "skyr", "quark", "kefir").any { n.contains(it) }
    }

    private fun isAromatic(food: FoodItem): Boolean {
        val n = food.name.lowercase(Locale.US)
        return listOf("onion", "garlic", "leek", "shallot", "scallion", "chive")
            .any { n.contains(it) }
    }

    private fun isDrink(food: FoodItem): Boolean {
        val n = food.name.lowercase(Locale.US)
        return listOf(
            "water", "coffee", "tea", "wine", "beer", "soda", "cola", "lemonade",
            "smoothie", "milkshake", "juice"
        ).any { n.contains(it) }
    }

    private fun isCondiment(food: FoodItem): Boolean {
        val n = food.name.lowercase(Locale.US)
        val exact = setOf(
            "salt", "sugar", "pepper", "black pepper", "white pepper", "cinnamon", "oregano",
            "basil", "thyme", "rosemary", "parsley", "cumin", "paprika", "turmeric", "ginger",
            "vinegar", "ketchup", "mustard", "mayonnaise", "soy sauce", "stock", "broth",
            "baking powder", "baking soda", "yeast", "gelatin", "cocoa powder", "vanilla"
        )
        return n in exact || n.endsWith(" oil") || n == "oil" || n.endsWith(" spice") ||
            n.endsWith(" seasoning") || n.endsWith(" salt")
    }

    // ------------------------------------------------------------------ food classification

    private fun rolesOf(food: FoodItem, categoryName: String): Set<MealRole> {
        val name = food.name.lowercase(Locale.US)
        val category = categoryName.lowercase(Locale.US)
        if (isDrink(food) || isCondiment(food)) return emptySet()

        val roles = mutableSetOf<MealRole>()

        when {
            category.contains("fruit") -> roles += MealRole.FRUIT
            category.contains("vegetable") -> roles += MealRole.VEGETABLE
            category.contains("dairy") -> roles += MealRole.DAIRY
            category.contains("meat") || category.contains("fish") || category.contains("poultry") ->
                roles += MealRole.PROTEIN
            category.contains("legume") -> roles += MealRole.PROTEIN
            category.contains("carb") || category.contains("grain") -> roles += MealRole.GRAIN
        }

        if (GRAIN_KEYS.any { name.contains(it) }) roles += MealRole.GRAIN
        if (DAIRY_KEYS.any { name.contains(it) }) roles += MealRole.DAIRY
        if (PROTEIN_KEYS.any { name.contains(it) }) roles += MealRole.PROTEIN
        if (FRUIT_KEYS.any { name.contains(it) }) roles += MealRole.FRUIT
        if (VEGETABLE_KEYS.any { name.contains(it) }) roles += MealRole.VEGETABLE
        if (FAT_KEYS.any { name.contains(it) }) roles += MealRole.FAT

        // "Eggplant" is a vegetable, not an egg; "peach"/"pear" are fruit, not peas.
        if (name.contains("eggplant") || name.contains("aubergine")) {
            roles -= MealRole.PROTEIN
            roles += MealRole.VEGETABLE
        }
        if (name.contains("peach") || name.contains("pear")) {
            roles -= MealRole.PROTEIN
            roles -= MealRole.VEGETABLE
            roles += MealRole.FRUIT
        }

        // "Bell pepper" is a vegetable, not a fruit, even though the keyword list matches.
        if (name.contains("pepper") && !name.contains("black pepper") && !name.contains("white pepper")) {
            roles -= MealRole.FRUIT
            roles += MealRole.VEGETABLE
        }
        if (name.contains("tomato")) {
            roles -= MealRole.FRUIT
            roles += MealRole.VEGETABLE
        }
        if (name.contains("avocado") && MealRole.FRUIT in roles) {
            roles += MealRole.FAT
        }
        return roles
    }

    private val GRAIN_KEYS = listOf(
        "oat", "porridge", "muesli", "granola", "cereal", "bread", "toast", "bagel", "croissant",
        "pancake", "flour", "pasta", "noodle", "rice", "quinoa", "couscous", "bulgur", "barley",
        "potato", "corn", "tortilla", "wrap", "bun", "cracker", "rye", "spelt", "wheat",
        "semolina", "polenta", "buckwheat", "millet"
    )

    private val DAIRY_KEYS = listOf(
        "milk", "yogurt", "yoghurt", "kefir", "quark", "skyr", "cream", "buttermilk",
        "cheese", "curd", "mozzarella", "cheddar", "feta", "parmesan", "ricotta", "ghee"
    )

    private val PROTEIN_KEYS = listOf(
        "chicken", "beef", "pork", "lamb", "turkey", "duck", "veal", "goat", "fish", "salmon",
        "tuna", "cod", "trout", "sardine", "mackerel", "herring", "shrimp", "prawn", "egg",
        "tofu", "tempeh", "seitan", "bean", "lentil", "chickpea", "peas", "lupin", "soy", "ham",
        "bacon", "sausage", "salami", "steak", "mince", "fillet", "nut", "almond", "cashew",
        "walnut", "pistachio", "hazelnut", "peanut", "seed", "quinoa"
    )

    private val FRUIT_KEYS = listOf(
        "apple", "banana", "berry", "berries", "orange", "grape", "pear", "peach", "apricot",
        "plum", "cherry", "mango", "pineapple", "melon", "kiwi", "lemon", "lime", "grapefruit",
        "fig", "date", "raisin", "avocado", "coconut", "pomegranate", "papaya", "passion fruit",
        "blueberry", "raspberry", "strawberry", "currant", "cranberry", "nectarine", "mandarin",
        "tangerine", "lychee", "guava"
    )

    private val VEGETABLE_KEYS = listOf(
        "broccoli", "carrot", "spinach", "kale", "lettuce", "cabbage", "cauliflower",
        "cucumber", "zucchini", "courgette", "bell pepper", "onion", "garlic", "leek",
        "celery", "asparagus", "artichoke", "bean", "peas", "green pea", "beet", "radish", "turnip",
        "pumpkin", "squash", "mushroom", "eggplant", "aubergine", "tomato", "fennel",
        "chard", "sprout", "potato", "sweet potato", "yam"
    )

    private val FAT_KEYS = listOf(
        "oil", "butter", "margarine", "avocado", "nut", "almond", "cashew", "walnut",
        "peanut", "seed", "olive", "tahini", "mayonnaise", "ghee"
    )

    private fun rotatingDayNames(daysCount: Int): List<String> {
        val names = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val calendar = Calendar.getInstance()
        val mondayBased = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
        return (0 until daysCount).map { offset ->
            names[(mondayBased + offset) % 7]
        }
    }
}
