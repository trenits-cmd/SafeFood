package com.example.safefood.ai

import com.example.safefood.data.catalog.MealDishResolver
import java.util.Locale

/**
 * Turns a generated meal line such as
 * "Chicken Drumstick with Bulgur and Asparagus" into a usable recipe.
 *
 * The meal planner stores loose ingredient names, not structured recipe data. This
 * generator therefore splits the line into individual foods, assigns realistic
 * amounts, and writes instructions that match the actual dish family instead of
 * using one generic text for every meal.
 */
object RecipeGenerator {

    fun generateRecipe(label: String, mealText: String, portions: Int = 2): MealRecipe {
        val servings = portions.coerceIn(1, 12)
        val components = splitComponents(mealText)
        val dish = MealDishResolver.resolve(label, mealText)
        val title = dish?.title ?: fallbackTitle(label, components)
        val isSnack = label.contains("snack", ignoreCase = true)
        val isBreakfast = label.contains("breakfast", ignoreCase = true)

        if (components.isEmpty()) {
            return MealRecipe(
                title = title,
                servings = servings,
                ingredients = emptyList(),
                instructions = listOf(
                    "Rate a few safe foods for this meal, then generate the menu again to get a complete recipe.",
                    "Open the meal after it contains ingredients to see amounts and cooking steps."
                ),
                description = "This meal does not contain enough rated foods for a recipe yet."
            )
        }

        val ingredients = components.map { name ->
            RecipeIngredient(name = name, amount = amountFor(name, servings))
        }

        val instructions = buildInstructions(
            label = label,
            mealText = mealText,
            title = title,
            components = components,
            isBreakfast = isBreakfast,
            isSnack = isSnack
        )

        val times = timesFor(mealText, isBreakfast, isSnack)
        return MealRecipe(
            title = title,
            servings = servings,
            prepTime = times.first,
            cookTime = times.second,
            ingredients = ingredients,
            instructions = instructions,
            description = descriptionFor(title, components)
        )
    }

    // ------------------------------------------------------------------ ingredient parsing

    private fun splitComponents(mealText: String): List<String> {
        val cleaned = mealText
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleaned.isBlank()) return emptyList()
        val lower = cleaned.lowercase(Locale.US)
        if (lower.startsWith("no matching") || lower.startsWith("add ") || lower.startsWith("rate ")) {
            return emptyList()
        }

        // Planner meals use "X with Y and Z". Split those connectors as well as commas,
        // so every food gets its own amount and appears in the instructions.
        val parts = cleaned
            .split(Regex("""\s*(?:,|;|\+|&|\bwith\b|\band\b|\bplus\b)\s*""", RegexOption.IGNORE_CASE))
            .map { it.trim().trim('.', '-', '–', '—') }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.US) }

        // Keep very short fragments only when they are real foods; this avoids leftovers
        // from connectors such as a trailing "and".
        return parts.filter { it.length >= 2 }
    }

    private fun amountFor(name: String, portions: Int): String {
        val n = name.lowercase(Locale.US)
        val baseAmount = when {
            isSpice(n) -> "to taste"
            n.contains("drumstick") || n.contains("wing") -> "2 pieces"
            n.contains("eggplant") -> "250 g"
            n.contains("egg") -> "2 eggs"
            n.contains("bagel") -> "2 bagels"
            n.contains("tortilla") || n.contains("wrap") -> "2 wraps"
            n.contains("bread") || n.contains("toast") -> "4 slices"
            n.contains("potato") -> "250 g"
            isPasta(n) -> "100 g"
            isGrain(n) -> "100 g"
            isProtein(n) -> "300 g"
            n.contains("yogurt") || n.contains("yoghurt") || n.contains("skyr") || n.contains("quark") -> "300 g"
            n.contains("milk") || n.contains("kefir") -> "250 ml"
            isCheese(n) -> "80 g"
            n.contains("bean") || n.contains("lentil") || n.contains("chickpea") -> "200 g"
            n.contains("oat") || n.contains("porridge") || n.contains("muesli") || n.contains("granola") || n.contains("cereal") -> "100 g"
            n.contains("nut") || n.contains("almond") || n.contains("cashew") || n.contains("walnut") ||
                n.contains("peanut") || n.contains("seed") -> "30 g"
            n.contains("oil") || n.contains("butter") -> "1 tbsp"
            n.contains("honey") || n.contains("syrup") || n.contains("jam") -> "1 tbsp"
            isFruit(n) -> "200 g"
            isVegetable(n) -> "200 g"
            else -> "150 g"
        }
        return scaleAmount(baseAmount, portions)
    }

    private fun scaleAmount(amount: String, portions: Int): String {
        if (amount == "to taste") return amount
        val factor = portions.toDouble() / 2.0
        val numberRegex = Regex("(\\d+(?:\\.\\d+)?)")
        val match = numberRegex.find(amount) ?: return amount
        val number = match.value.toDouble()
        val scaled = number * factor
        val numberText = if (scaled % 1.0 == 0.0) scaled.toInt().toString() else String.format(Locale.US, "%.1f", scaled)

        val unit = amount.substring(match.range.last + 1).trim()
        val countUnits = setOf(
            "piece", "slice", "egg", "bagel", "wrap", "cup", "clove",
            "lemon", "apple", "banana", "potato", "tomato", "carrot"
        )
        val baseUnit = unit.removeSuffix("s")
        val displayUnit = if (baseUnit in countUnits && baseUnit.isNotEmpty()) {
            if (scaled == 1.0) baseUnit else baseUnit + "s"
        } else {
            unit
        }
        return listOf(numberText, displayUnit).filter { it.isNotBlank() }.joinToString(" ")
    }

    // ------------------------------------------------------------------ instructions

    private fun buildInstructions(
        label: String,
        mealText: String,
        title: String,
        components: List<String>,
        isBreakfast: Boolean,
        isSnack: Boolean
    ): List<String> {
        val lower = mealText.lowercase(Locale.US)
        val protein = components.firstOrNull { isProtein(it) && !isVegetable(it) }
        val starch = components.firstOrNull { isGrain(it) || isPasta(it) || it.contains("potato", true) }
        val vegetables = components.filter {
            isVegetable(it) && it != starch && it != protein
        }
        val fruits = components.filter { isFruit(it) }
        val dairy = components.filter { isDairy(it) }
        val yogurt = dairy.firstOrNull { isYogurt(it) }
        val cheese = dairy.firstOrNull { isCheese(it) }
        val nuts = components.filter { isNut(it) }
        val oats = components.firstOrNull { isOats(it) }
        val bread = components.firstOrNull { isBread(it) }
        val pasta = components.firstOrNull { isPasta(it) }

        val style = when {
            isSnack && cheese != null -> Style.CHEESE_SNACK
            isSnack && (lower.contains("bean") || lower.contains("lentil") || lower.contains("chickpea")) -> Style.BEAN_SNACK
            isSnack && vegetables.isNotEmpty() && fruits.isEmpty() -> Style.VEGGIE_STICKS
            isSnack && yogurt != null && fruits.isNotEmpty() -> Style.YOGURT
            isSnack -> Style.SNACK_PLATE
            oats != null -> Style.OATMEAL
            lower.contains("pancake") -> Style.PANCAKES
            yogurt != null && fruits.isNotEmpty() && starch == null && bread == null -> Style.YOGURT
            bread != null && protein != null && protein.contains("egg", true) -> Style.EGG_TOAST
            bread != null -> Style.TOAST
            pasta != null -> Style.PASTA
            lower.contains("potato") -> Style.POTATO
            starch != null -> Style.GRAIN_BOWL
            protein != null -> Style.SKILLET
            else -> if (isBreakfast) Style.BREAKFAST_PLATE else Style.SKILLET
        }

        return when (style) {
            Style.OATMEAL -> oatmealInstructions(oats ?: "oats", fruits, nuts, yogurt, title)
            Style.PANCAKES -> pancakeInstructions(fruits, yogurt, title)
            Style.YOGURT -> yogurtInstructions(yogurt ?: "yogurt", fruits, nuts, title)
            Style.EGG_TOAST -> eggToastInstructions(protein ?: "eggs", bread ?: "bread", vegetables, cheese, title)
            Style.TOAST -> toastInstructions(bread ?: "bread", protein, vegetables, cheese, title)
            Style.PASTA -> pastaInstructions(pasta ?: "pasta", protein, vegetables, cheese, title)
            Style.POTATO -> potatoInstructions(starch ?: "potatoes", protein, vegetables, yogurt, title)
            Style.GRAIN_BOWL -> grainBowlInstructions(starch ?: "rice", protein, vegetables, cheese, yogurt, title)
            Style.CHEESE_SNACK -> cheeseSnackInstructions(cheese ?: "cheese", fruits, vegetables, nuts, title)
            Style.BEAN_SNACK -> beanSnackInstructions(protein ?: "beans", vegetables, bread, title)
            Style.VEGGIE_STICKS -> veggieStickInstructions(vegetables, yogurt, title)
            Style.SNACK_PLATE -> snackPlateInstructions(components, title)
            Style.BREAKFAST_PLATE -> breakfastPlateInstructions(components, fruits, yogurt, title)
            Style.SKILLET -> skilletInstructions(protein, vegetables, starch, cheese, title)
        }
    }

    private fun oatmealInstructions(
        oats: String,
        fruits: List<String>,
        nuts: List<String>,
        yogurt: String?,
        title: String
    ): List<String> {
        val fruitText = fruits.joinToString(" and ")
        return buildList {
            add("Bring the milk or water from the ingredient list to a gentle simmer in a small pot. Stir in the $oats and a pinch of salt.")
            add("Cook for 4–5 minutes over low heat, stirring now and then, until the oats are creamy. Add a splash more liquid if it gets too thick.")
            if (fruitText.isNotBlank()) {
                add("Wash and cut the $fruitText into bite-size pieces. Keep a few pieces aside for the top.")
            }
            if (nuts.isNotEmpty()) {
                add("Toast the ${nuts.joinToString(" and ")} in a dry pan for 1–2 minutes until fragrant (optional, but adds crunch).")
            }
            add("Divide the oatmeal between bowls and top with ${listOfNotNull(fruitText.ifBlank { null }, nuts.joinToString(" and ").ifBlank { null }, yogurt).joinToString(", ").ifBlank { "your toppings" }}.")
            add("Serve warm. Add honey or cinnamon only if you already tolerate them.")
        }
    }

    private fun pancakeInstructions(fruits: List<String>, yogurt: String?, title: String): List<String> {
        val topping = listOfNotNull(fruits.joinToString(" and ").ifBlank { null }, yogurt)
            .joinToString(" and ")
            .ifBlank { "fruit or yogurt" }
        return listOf(
            "Whisk the flour, eggs and milk from the ingredient list with a pinch of salt until smooth. Rest the batter for 5 minutes.",
            "Heat a non-stick pan over medium heat and brush it with a little butter or oil.",
            "Pour small ladles of batter into the pan. Cook for about 2 minutes, until bubbles appear on top, then flip and cook for 1 minute more.",
            "Keep the pancakes warm while you cook the rest. Stack them on plates.",
            "Top with $topping and serve warm."
        )
    }

    private fun yogurtInstructions(
        yogurt: String,
        fruits: List<String>,
        nuts: List<String>,
        title: String
    ): List<String> {
        val fruitText = fruits.joinToString(" and ").ifBlank { "fresh fruit" }
        return buildList {
            add("Spoon the $yogurt into serving bowls and smooth the top.")
            add("Wash the $fruitText, remove any stones or cores, and cut it into bite-size pieces.")
            if (nuts.isNotEmpty()) {
                add("Roughly chop the ${nuts.joinToString(" and ")} and toast them for 1 minute in a dry pan if you like extra crunch.")
            }
            add("Arrange the $fruitText over the yogurt${if (nuts.isNotEmpty()) " and scatter over the ${nuts.joinToString(" and ")}" else ""}.")
            add("Serve immediately. Keep it plain or add honey/cinnamon only if they are safe for you.")
        }
    }

    private fun eggToastInstructions(
        egg: String,
        bread: String,
        vegetables: List<String>,
        cheese: String?,
        title: String
    ): List<String> {
        val vegText = vegetables.joinToString(" and ")
        return buildList {
            add("Toast the $bread until golden and crisp.")
            add("Whisk the $egg with a pinch of salt and pepper in a small bowl.")
            add("Melt a little butter in a non-stick pan over low heat. Pour in the eggs and stir gently for 2–3 minutes, until just set.")
            if (vegText.isNotBlank()) {
                add("Wash and thinly slice the $vegText. Add it to the pan for the last minute, or serve it fresh on the side.")
            }
            add("Pile the eggs onto the toast${if (cheese != null) " and finish with the $cheese" else ""}${if (vegText.isNotBlank()) ", then add the $vegText" else ""}.")
            add("Season with a little black pepper and serve while the toast is still crisp.")
        }
    }

    private fun toastInstructions(
        bread: String,
        protein: String?,
        vegetables: List<String>,
        cheese: String?,
        title: String
    ): List<String> {
        val vegText = vegetables.joinToString(" and ")
        return buildList {
            add("Toast the $bread until golden.")
            if (protein != null) {
                add("Pat the $protein dry, season with salt and pepper, and cook it in a lightly oiled pan over medium heat for 6–8 minutes, turning once.")
                add("Rest the $protein for 3 minutes, then slice it into thin strips.")
            }
            if (vegText.isNotBlank()) {
                add("Wash and slice the $vegText. If you prefer it softer, sauté it for 2–3 minutes in the same pan.")
            }
            add("Layer the toast with ${listOfNotNull(protein, vegText.ifBlank { null }, cheese).joinToString(", ").ifBlank { "your toppings" }}.")
            add("Add a little olive oil, salt and pepper, and serve immediately.")
        }
    }

    private fun pastaInstructions(
        pasta: String,
        protein: String?,
        vegetables: List<String>,
        cheese: String?,
        title: String
    ): List<String> {
        val vegText = vegetables.joinToString(" and ")
        return buildList {
            add("Bring a large pot of salted water to a boil. Add the $pasta and cook until al dente, about 8–10 minutes. Reserve a cup of pasta water, then drain.")
            if (protein != null) {
                add("Cut the $protein into bite-size pieces and season with salt, pepper and a little paprika. Brown it in a wide pan with 1 tbsp olive oil for 6–8 minutes.")
            } else {
                add("Heat 1 tbsp olive oil in a wide pan over medium heat.")
            }
            if (vegText.isNotBlank()) {
                add("Wash and cut the $vegText. Add it to the pan and cook for 3–4 minutes, until crisp-tender.")
            }
            add("Add the drained $pasta and a splash of pasta water. Toss over medium heat for 1–2 minutes, until the sauce coats the pasta.")
            add("Finish with ${cheese ?: "a little olive oil"}, black pepper and fresh herbs if tolerated. Serve hot.")
        }
    }

    private fun potatoInstructions(
        potato: String,
        protein: String?,
        vegetables: List<String>,
        yogurt: String?,
        title: String
    ): List<String> {
        val vegText = vegetables.joinToString(" and ")
        return buildList {
            add("Preheat the oven to 200 °C. Wash and cut the $potato into wedges or bite-size pieces.")
            add("Toss the $potato with 1 tbsp olive oil, salt, pepper and a pinch of paprika. Spread it on a tray in one layer.")
            add("Roast for 25–30 minutes, turning halfway, until golden and crisp.")
            if (protein != null) {
                add("Season the $protein with salt and pepper. Pan-sear it over medium-high heat for 6–8 minutes, turning once, then rest for 3 minutes and slice.")
            }
            if (vegText.isNotBlank()) {
                add("Add the $vegText to the tray for the last 10–12 minutes, or steam it separately until just tender.")
            }
            add("Plate the $potato with ${listOfNotNull(protein, vegText.ifBlank { null }).joinToString(" and ").ifBlank { "the vegetables" }}${if (yogurt != null) ". Serve with a spoon of $yogurt" else ""}.")
        }
    }

    private fun grainBowlInstructions(
        grain: String,
        protein: String?,
        vegetables: List<String>,
        cheese: String?,
        yogurt: String?,
        title: String
    ): List<String> {
        val vegText = vegetables.joinToString(" and ")
        val cookTime = when {
            grain.contains("couscous", true) -> "5 minutes off the heat"
            grain.contains("bulgur", true) -> "12–15 minutes"
            grain.contains("quinoa", true) -> "15 minutes"
            grain.contains("barley", true) -> "25–30 minutes"
            else -> "15–18 minutes"
        }
        return buildList {
            add("Rinse the $grain under cold water. Add it to a pot with about twice its volume in water and a pinch of salt, then cover and cook for $cookTime. Fluff with a fork.")
            if (protein != null) {
                add("Pat the $protein dry and cut it into bite-size pieces. Season with salt, pepper and ½ tsp paprika.")
                add("Heat 1 tbsp olive oil in a wide pan over medium-high heat. Sear the $protein for 4–5 minutes, turn, and cook for 3–4 minutes more until browned and cooked through. Set aside.")
            }
            if (vegText.isNotBlank()) {
                add("In the same pan, add the $vegText and cook for 3–4 minutes, until crisp-tender. Season lightly.")
            }
            add("Divide the $grain between bowls and top with ${listOfNotNull(protein, vegText.ifBlank { null }, cheese).joinToString(" and ").ifBlank { "the vegetables" }}.")
            add("Spoon over the pan juices${if (yogurt != null) " and add a spoon of $yogurt" else ""}. Serve warm.")
        }
    }

    private fun cheeseSnackInstructions(
        cheese: String,
        fruits: List<String>,
        vegetables: List<String>,
        nuts: List<String>,
        title: String
    ): List<String> {
        val extras = listOfNotNull(
            fruits.joinToString(" and ").ifBlank { null },
            vegetables.joinToString(" and ").ifBlank { null },
            nuts.joinToString(" and ").ifBlank { null }
        ).joinToString(", ")
        return listOf(
            "Take the $cheese out of the fridge 10 minutes before serving so the flavour opens up.",
            "Slice or cube the $cheese into bite-size pieces.",
            if (extras.isNotBlank()) "Wash and cut the $extras. Arrange everything on a board or plate." else "Arrange the cheese on a plate.",
            "Add a few plain crackers or bread if you tolerate them.",
            "Serve at room temperature. Keep portions small if strong cheese feels overwhelming."
        )
    }

    private fun beanSnackInstructions(
        beans: String,
        vegetables: List<String>,
        bread: String?,
        title: String
    ): List<String> {
        val vegText = vegetables.joinToString(" and ")
        return buildList {
            add("Rinse the $beans under cold water and drain well. Pat them dry.")
            add("Mash the beans roughly with a fork, or leave them whole if you prefer more texture.")
            add("Mix with 1 tbsp olive oil, a squeeze of lemon, salt, pepper and a pinch of cumin. Taste and adjust.")
            if (vegText.isNotBlank()) {
                add("Wash and cut the $vegText into sticks for dipping.")
            }
            add("Spoon the bean dip into a bowl and serve with ${vegText.ifBlank { bread ?: "vegetable sticks" }}.")
        }
    }

    private fun veggieStickInstructions(
        vegetables: List<String>,
        yogurt: String?,
        title: String
    ): List<String> {
        val vegText = vegetables.joinToString(" and ").ifBlank { "vegetables" }
        return buildList {
            add("Wash the $vegText well. Peel only if needed, then cut into even sticks.")
            if (yogurt != null) {
                add("For the dip, mix the $yogurt with a squeeze of lemon, salt, pepper and a little chopped herb if tolerated.")
            }
            add("Arrange the sticks on a plate${if (yogurt != null) " with the yogurt dip in a small bowl" else ""}.")
            add("Serve fresh and crisp.")
        }
    }

    private fun snackPlateInstructions(components: List<String>, title: String): List<String> {
        val names = components.joinToString(", ")
        return listOf(
            "Wash and cut the $names as needed.",
            "Arrange everything on a small plate or board, keeping wet and dry items separate.",
            "Serve fresh. Add a plain cracker or a spoon of yogurt only if it is on your safe list."
        )
    }

    private fun breakfastPlateInstructions(
        components: List<String>,
        fruits: List<String>,
        yogurt: String?,
        title: String
    ): List<String> {
        val fruitText = fruits.joinToString(" and ")
        return buildList {
            add("Prepare the ${components.joinToString(", ")}: wash, peel and cut as needed.")
            if (fruitText.isNotBlank()) {
                add("Arrange the $fruitText on a plate or in a bowl.")
            }
            if (yogurt != null) {
                add("Add the $yogurt next to the fruit, or spoon it over the top.")
            }
            add("Keep the plate simple and serve fresh.")
        }
    }

    private fun skilletInstructions(
        protein: String?,
        vegetables: List<String>,
        starch: String?,
        cheese: String?,
        title: String
    ): List<String> {
        val vegText = vegetables.joinToString(" and ")
        return buildList {
            if (starch != null) {
                add("Cook the $starch according to the package instructions, then drain and keep warm.")
            }
            if (protein != null) {
                add("Pat the $protein dry and season with salt and pepper. Heat 1 tbsp olive oil in a pan over medium-high heat.")
                add("Cook the $protein for 6–8 minutes, turning once, until browned and cooked through. Rest for 3 minutes.")
            } else {
                add("Heat 1 tbsp olive oil in a wide pan over medium heat.")
            }
            if (vegText.isNotBlank()) {
                add("Wash and cut the $vegText. Add it to the pan and cook for 3–4 minutes, until crisp-tender.")
            }
            add("Combine ${listOfNotNull(protein, vegText.ifBlank { null }, starch, cheese).joinToString(", ").ifBlank { "the ingredients" }} in the pan and toss gently.")
            add("Taste, adjust salt and pepper, and serve warm.")
        }
    }

    // ------------------------------------------------------------------ small helpers

    private fun fallbackTitle(label: String, components: List<String>): String {
        return when {
            label.contains("snack", true) -> "Safe Snack Plate"
            label.contains("breakfast", true) -> "Safe Breakfast"
            components.isNotEmpty() -> components.first() + " Plate"
            else -> "Safe Meal"
        }
    }

    private fun descriptionFor(title: String, components: List<String>): String {
        val main = components.take(3).joinToString(", ")
        return if (main.isBlank()) {
            "A simple meal built from your safe foods."
        } else {
            "A ${title.lowercase(Locale.US)} made with $main. Amounts are for the selected number of portions."
        }
    }

    private fun timesFor(mealText: String, isBreakfast: Boolean, isSnack: Boolean): Pair<String, String> {
        val text = mealText.lowercase(Locale.US)
        return when {
            isSnack -> "5 min" to "0 min"
            text.contains("oat") || text.contains("porridge") -> "5 min" to "10 min"
            text.contains("pancake") -> "10 min" to "15 min"
            text.contains("yogurt") && !text.contains("rice") -> "5 min" to "0 min"
            text.contains("egg") && text.contains("toast") -> "5 min" to "8 min"
            text.contains("potato") -> "10 min" to "30 min"
            text.contains("pasta") -> "10 min" to "20 min"
            isBreakfast -> "5 min" to "10 min"
            else -> "10 min" to "20 min"
        }
    }

    // ------------------------------------------------------------------ food matching

    private fun isProtein(name: String): Boolean {
        val n = name.lowercase(Locale.US)
        if (n.contains("eggplant") || n.contains("aubergine")) return false
        return listOf(
            "chicken", "turkey", "beef", "steak", "veal", "pork", "ham", "bacon", "sausage",
            "salmon", "fish", "tuna", "cod", "trout", "mackerel", "sardine", "herring", "shrimp",
            "prawn", "egg", "tofu", "tempeh", "seitan", "bean", "lentil", "chickpea", "lupin"
        ).any { n.contains(it) }
    }

    private fun isGrain(name: String): Boolean {
        val n = name.lowercase(Locale.US)
        return listOf(
            "rice", "quinoa", "bulgur", "couscous", "barley", "farro", "millet", "buckwheat",
            "polenta", "corn", "spelt"
        ).any { n.contains(it) }
    }

    private fun isPasta(name: String): Boolean {
        val n = name.lowercase(Locale.US)
        return listOf("pasta", "noodle", "spaghetti", "macaroni", "lasagna", "penne", "fusilli")
            .any { n.contains(it) }
    }

    private fun isBread(name: String): Boolean {
        val n = name.lowercase(Locale.US)
        return listOf("bread", "toast", "bagel", "croissant", "bun", "tortilla", "wrap")
            .any { n.contains(it) }
    }

    private fun isOats(name: String): Boolean {
        val n = name.lowercase(Locale.US)
        return listOf("oat", "porridge", "muesli", "granola", "cereal").any { n.contains(it) }
    }

    private fun isDairy(name: String): Boolean {
        val n = name.lowercase(Locale.US)
        return listOf(
            "yogurt", "yoghurt", "skyr", "quark", "kefir", "milk", "cream", "buttermilk",
            "cheese", "cheddar", "mozzarella", "feta", "parmesan", "ricotta", "halloumi",
            "gouda", "brie", "camembert", "emmental", "gruyere", "manchego"
        ).any { n.contains(it) }
    }

    private fun isYogurt(name: String): Boolean {
        val n = name.lowercase(Locale.US)
        return listOf("yogurt", "yoghurt", "skyr", "quark", "kefir").any { n.contains(it) }
    }

    private fun isCheese(name: String): Boolean {
        val n = name.lowercase(Locale.US)
        return listOf(
            "cheese", "cheddar", "mozzarella", "feta", "parmesan", "ricotta", "halloumi",
            "gouda", "brie", "camembert", "emmental", "gruyere", "manchego", "provolone"
        ).any { n.contains(it) }
    }

    private fun isFruit(name: String): Boolean {
        val n = name.lowercase(Locale.US)
        return listOf(
            "apple", "banana", "berry", "berries", "orange", "grape", "pear", "peach", "apricot",
            "plum", "cherry", "mango", "pineapple", "melon", "watermelon", "kiwi", "lemon", "lime",
            "fig", "date", "raisin", "avocado", "coconut", "pomegranate", "papaya", "blueberry",
            "raspberry", "strawberry", "currant", "cranberry", "nectarine", "mandarin", "guava"
        ).any { n.contains(it) }
    }

    private fun isVegetable(name: String): Boolean {
        val n = name.lowercase(Locale.US)
        if (isFruit(n)) return false
        return listOf(
            "broccoli", "carrot", "spinach", "kale", "lettuce", "cabbage", "cauliflower",
            "cucumber", "zucchini", "courgette", "pepper", "onion", "garlic", "leek", "celery",
            "asparagus", "artichoke", "bean", "pea", "beet", "radish", "turnip", "pumpkin",
            "squash", "mushroom", "eggplant", "aubergine", "tomato", "fennel", "chard", "sprout",
            "potato", "yam", "corn"
        ).any { n.contains(it) }
    }

    private fun isNut(name: String): Boolean {
        val n = name.lowercase(Locale.US)
        return listOf("nut", "almond", "cashew", "walnut", "peanut", "pistachio", "hazelnut", "seed")
            .any { n.contains(it) }
    }

    private fun isSpice(name: String): Boolean {
        val n = name.lowercase(Locale.US)
        return n in setOf(
            "salt", "pepper", "black pepper", "white pepper", "paprika", "cumin", "cinnamon",
            "oregano", "basil", "thyme", "rosemary", "parsley", "dill", "turmeric", "ginger",
            "chili flakes", "curry powder", "garlic powder", "onion powder", "nutmeg", "bay leaf",
            "bay leaves", "vanilla", "cocoa powder"
        ) || n.endsWith(" spice") || n.endsWith(" seasoning") || n.endsWith(" salt")
    }

    private enum class Style {
        OATMEAL,
        PANCAKES,
        YOGURT,
        EGG_TOAST,
        TOAST,
        PASTA,
        POTATO,
        GRAIN_BOWL,
        CHEESE_SNACK,
        BEAN_SNACK,
        VEGGIE_STICKS,
        SNACK_PLATE,
        BREAKFAST_PLATE,
        SKILLET
    }
}
