package com.example.safefood

import com.example.safefood.data.catalog.MealDishResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MealDishResolverTest {

    @Test
    fun `main meal becomes a named dish with a cooked dish photo`() {
        val dish = MealDishResolver.resolve("Lunch", "Turkey with Rice and Carrot")

        assertEquals("Turkey Rice Bowl", dish?.title)
        assertTrue(dish!!.imageUrl.endsWith("_dish_turkey_rice.jpg"))
    }

    @Test
    fun `bean and rice becomes a bean rice bowl`() {
        val dish = MealDishResolver.resolve("Dinner", "Pinto Bean with Jasmine Rice and Turnip")

        assertEquals("Bean Rice Bowl", dish?.title)
        assertTrue(dish!!.imageUrl.endsWith("_dish_bean_rice.jpg"))
    }

    @Test
    fun `grain dish names the actual grain`() {
        val dish = MealDishResolver.resolve("Lunch", "Chicken Drumstick with Bulgur and Asparagus")

        assertEquals("Chicken & Bulgur Bowl", dish?.title)
        assertTrue(dish!!.imageUrl.endsWith("_dish_grain_bowl.jpg"))
    }

    @Test
    fun `breakfast gets a breakfast dish photo`() {
        val dish = MealDishResolver.resolve("Breakfast", "Rolled Oats with Apple and Yogurt")

        assertEquals("Oatmeal Bowl", dish?.title)
        assertTrue(dish!!.imageUrl.endsWith("_dish_oatmeal.jpg"))
    }

    @Test
    fun `snack gets a matching snack photo`() {
        val cheese = MealDishResolver.resolve("Snacks", "Mozzarella & Emmental")
        val veggie = MealDishResolver.resolve("Snacks", "Carrot, Carrot")
        val bean = MealDishResolver.resolve("Snacks", "Cannellini Bean & Arugula")

        assertEquals("Cheese Snack", cheese?.title)
        assertEquals("Veggie Sticks", veggie?.title)
        assertEquals("Bean Snack", bean?.title)
        assertTrue(veggie!!.imageUrl.endsWith("_dish_veggie_sticks.jpg"))
        assertTrue(bean!!.imageUrl.endsWith("_dish_bean_snack.jpg"))
    }

    @Test
    fun `plain notes do not pretend to be a dish`() {
        assertNull(MealDishResolver.resolve("Lunch", "Rate a protein, a vegetable and a side to build lunch."))
        assertNull(MealDishResolver.resolve("Breakfast", "Add breakfast foods to your safe list."))
        assertNull(MealDishResolver.resolve("Dinner", "No matching foods yet."))
    }
}
