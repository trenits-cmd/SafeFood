package com.example.safefood.ui.screens.menuplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safefood.data.model.FoodItem
import com.example.safefood.ui.SafeFoodViewModel
import com.example.safefood.ui.components.FoodPhoto
import java.util.Locale

/** Step-by-Step Meal Planner - guided meal building one category at a time */
@Composable
fun StepByStepPlannerScreen(viewModel: SafeFoodViewModel) {
    val foods by viewModel.foods.collectAsState()

    // Step state machine: 0=GRAIN, 1=PROTEIN, 2=VEGETABLE, 3=SUMMARY
    var currentStep by remember { mutableIntStateOf(0) }
    var selectedGrain by remember { mutableStateOf<FoodItem?>(null) }
    var selectedProtein by remember { mutableStateOf<FoodItem?>(null) }
    var selectedVegetable by remember { mutableStateOf<FoodItem?>(null) }

    // Helper functions for category matching
    fun isGrain(food: FoodItem): Boolean {
        val name = food.name.lowercase(Locale.US)
        return listOf("rice", "quinoa", "bulgur", "couscous", "barley", "noodle", "pasta", "bread", "toast", "bagel", "wrap", "corn", "potato", "sweet potato", "yam", "oats").any { name.contains(it) }
    }

    fun isProtein(food: FoodItem): Boolean {
        val name = food.name.lowercase(Locale.US)
        return listOf("chicken", "turkey", "beef", "pork", "lamb", "fish", "salmon", "tuna", "egg", "tofu", "bean", "lentil", "chickpea", "peas", "shrimp", "prawn", "steak").any { name.contains(it) }
    }

    fun isVegetable(food: FoodItem): Boolean {
        val name = food.name.lowercase(Locale.US)
        return listOf("broccoli", "carrot", "spinach", "kale", "lettuce", "cabbage", "cauliflower", "cucumber", "zucchini", "pepper", "onion", "garlic", "celery", "asparagus", "artichoke", "bean", "pea", "beet", "radish", "turnip", "pumpkin", "squash", "mushroom", "tomato", "fennel", "chard").any { name.contains(it) }
    }

    val availableFoods = remember(currentStep, foods, selectedGrain) {
        when (currentStep) {
            0 -> foods.filter { isGrain(it) }
            1 -> foods.filter { isProtein(it) && it.id != selectedGrain?.id }
            2 -> foods.filter { isVegetable(it) }
            else -> emptyList()
        }
    }

    val stepTitles = arrayOf(
        "Step 1: Choose a Grain / Starch",
        "Step 2: Choose a Protein",
        "Step 3: Choose Vegetables",
        "Step 4: Meal Summary"
    )

    val stepDescriptions = arrayOf(
        "Select a grain or starch base for your meal energy.",
        "Select a main protein source to balance your meal.",
        "Select fresh vegetables to add fiber and nutrients.",
        "Review your step-by-step custom meal composition below."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step progress header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stepTitles[currentStep],
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (0..3).forEach { i ->
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i == currentStep) MaterialTheme.colorScheme.primary
                                        else if (i < currentStep) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }
                }

                Text(
                    text = stepDescriptions[currentStep],
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Main step list or summary
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep < 3) {
                    Text(
                        text = "Available Items (${availableFoods.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    if (availableFoods.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No items found for this step.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableFoods, key = { it.id }) { food ->
                                val isSelected = when (currentStep) {
                                    0 -> selectedGrain?.id == food.id
                                    1 -> selectedProtein?.id == food.id
                                    2 -> selectedVegetable?.id == food.id
                                    else -> false
                                }
                                FoodSelectionCard(
                                    food = food,
                                    isSelected = isSelected,
                                    onSelect = {
                                        when (currentStep) {
                                            0 -> selectedGrain = food
                                            1 -> selectedProtein = food
                                            2 -> selectedVegetable = food
                                        }
                                        if (currentStep < 2) currentStep++
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Summary Step
                    Text(
                        text = "Your Meal Composition",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MealSummaryRow(label = "Grain / Starch", item = selectedGrain)
                        MealSummaryRow(label = "Protein", item = selectedProtein)
                        MealSummaryRow(label = "Vegetable", item = selectedVegetable)
                    }
                }
            }
        }

        // Navigation Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = { currentStep-- },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Back")
                }
            }

            if (currentStep < 3) {
                Button(
                    onClick = { currentStep++ },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Next")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", modifier = Modifier.size(16.dp))
                }
            } else {
                Button(
                    onClick = {
                        viewModel.showMessage("Custom Step-by-Step Meal Created!")
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Meal")
                }
            }
        }
    }
}

@Composable
private fun FoodSelectionCard(
    food: FoodItem,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FoodPhoto(
                food = food,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp)),
                cornerRadius = 8.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "🌱 ${food.season}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MealSummaryRow(
    label: String,
    item: FoodItem?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = item?.name ?: "Not selected",
                    fontSize = 14.sp,
                    fontWeight = if (item != null) FontWeight.Bold else FontWeight.Normal,
                    color = if (item != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (item != null) {
                FoodPhoto(
                    food = item,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    cornerRadius = 6.dp
                )
            }
        }
    }
}
