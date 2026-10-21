package com.example.safefood.ui.screens.menuplanner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.safefood.ai.AiMenuPlanner
import com.example.safefood.data.catalog.MealDishResolver
import com.example.safefood.data.model.MealPlan
import com.example.safefood.data.model.MealPlanDay
import com.example.safefood.ui.SafeFoodViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MenuPlannerScreen(viewModel: SafeFoodViewModel) {
    val mealPlans by viewModel.mealPlans.collectAsState()
    val foods by viewModel.foods.collectAsState()
    val potentialMealFoods by viewModel.potentialMealFoods.collectAsState()

    var selectedGoal by remember { mutableStateOf(AiMenuPlanner.PlanningGoal.SAFE_ONLY) }
    var selectedDaysCount by remember { mutableIntStateOf(3) }
    var customInstructions by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var usePotentialMealsOnly by remember { mutableStateOf(false) }

    var goalDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(potentialMealFoods.isEmpty()) {
        if (potentialMealFoods.isEmpty()) usePotentialMealsOnly = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Potential meal foods selected on food cards
        if (potentialMealFoods.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RestaurantMenu,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Potential meal foods (${potentialMealFoods.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.clearPotentialMeals() }) {
                                Text("Clear", fontSize = 12.sp)
                            }
                        }
                        Text(
                            text = "Mark foods with the 'Potential meal' button in Categories or SafeFood. " +
                                "They are preferred when a menu is generated. Tap a chip to remove it.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            potentialMealFoods.forEach { food ->
                                InputChip(
                                    selected = false,
                                    onClick = { viewModel.togglePotentialMeal(food.id) },
                                    label = { Text(food.name, fontSize = 12.sp) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove ${food.name}",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // AI Generator Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Planner",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "AI Safe Food Menu Planner",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Generates safe meal combinations based on your ratings",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Goal Selector Dropdown
                    ExposedDropdownMenuBox(
                        expanded = goalDropdownExpanded,
                        onExpandedChange = { goalDropdownExpanded = !goalDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedGoal.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Planning Strategy") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = goalDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = goalDropdownExpanded,
                            onDismissRequest = { goalDropdownExpanded = false }
                        ) {
                            AiMenuPlanner.PlanningGoal.values().forEach { goal ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(goal.label, fontWeight = FontWeight.Bold)
                                            Text(goal.description, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    },
                                    onClick = {
                                        selectedGoal = goal
                                        goalDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Days selector
                    Text("Duration:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 3, 7).forEach { days ->
                            FilterChip(
                                selected = selectedDaysCount == days,
                                onClick = { selectedDaysCount = days },
                                label = { Text("$days ${if (days == 1) "Day" else "Days"}") }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customInstructions,
                        onValueChange = { customInstructions = it },
                        label = { Text("Custom AI Prompt / Preferences (Optional)") },
                        placeholder = { Text("e.g., Only light meals, no dairy") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (potentialMealFoods.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = usePotentialMealsOnly,
                                onClick = { usePotentialMealsOnly = !usePotentialMealsOnly },
                                label = { Text("Only my potential meal foods", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.RestaurantMenu,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                        Text(
                            text = if (usePotentialMealsOnly) {
                                "Menus will use only these ${potentialMealFoods.size} foods (rated items only)."
                            } else {
                                "These ${potentialMealFoods.size} foods get priority, other safe foods fill the gaps."
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val unratedPotential = potentialMealFoods.count { it.rating == 0 }
                        if (unratedPotential > 0) {
                            Text(
                                text = "$unratedPotential of these are not rated yet. " +
                                    "Rate them in Categories or SafeFood to use them in a menu.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.generateAiMealPlan(
                                    goal = selectedGoal,
                                    customInstructions = customInstructions,
                                    daysCount = selectedDaysCount,
                                    usePotentialMealsOnly = usePotentialMealsOnly,
                                    clearExisting = true
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isGenerating
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Fresh Menu", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.clearMealPlans() },
                            modifier = Modifier.height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Clear All", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section Title
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Saved AI Meal Plans (${mealPlans.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        if (mealPlans.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No AI meal plans generated yet. Tap the button above to generate your first plan!", color = Color.Gray)
                }
            }
        } else {
            items(mealPlans, key = { it.id }) { plan ->
                MealPlanCard(
                    mealPlan = plan,
                    onAddDayIngredientsToCart = { day ->
                        viewModel.addDayIngredientsToShoppingList(day)
                    }
                )
            }
        }
    }
}

@Composable
fun MealPlanCard(
    mealPlan: MealPlan,
    onAddDayIngredientsToCart: (MealPlanDay) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = mealPlan.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${mealPlan.days.size} Days Planned",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            mealPlan.days.forEach { day ->
                DayMealRow(
                    day = day,
                    onAddIngredientsToCart = { onAddDayIngredientsToCart(day) }
                )
            }
        }
    }
}

@Composable
fun DayMealRow(
    day: MealPlanDay,
    onAddIngredientsToCart: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = day.dayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Put ingredients in shopping list button
                    Button(
                        onClick = onAddIngredientsToCart,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            Icons.Default.AddShoppingCart,
                            contentDescription = "To Shopping List",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Ingredients", fontSize = 11.sp)
                    }

                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Toggle",
                            modifier = Modifier.rotate(if (expanded) 180f else 0f)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MealSlotText(label = "Breakfast", value = day.breakfast)
                    MealSlotText(label = "Lunch", value = day.lunch)
                    MealSlotText(label = "Dinner", value = day.dinner)
                    MealSlotText(label = "Snacks", value = day.snacks)
                }
            }
        }
    }
}

@Composable
fun MealSlotText(label: String, value: String) {
    if (value.isBlank()) return
    val dish = MealDishResolver.resolve(label, value)
    var showRecipe by remember { mutableStateOf(false) }
    
    if (showRecipe && !value.startsWith("No ") && !value.startsWith("Add ") && !value.startsWith("Rate ")) {
        RecipeDialog(label = label, mealText = value, onDismiss = { showRecipe = false })
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (dish != null) {
            val context = LocalContext.current
            val request = remember(dish.imageUrl) {
                ImageRequest.Builder(context)
                    .data(dish.imageUrl)
                    .crossfade(true)
                    .build()
            }
            SubcomposeAsyncImage(
                model = request,
                contentDescription = dish.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !value.startsWith("No ") && !value.startsWith("Add ") && !value.startsWith("Rate ")) {
                        showRecipe = true
                    }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !value.startsWith("No ") && !value.startsWith("Add ") && !value.startsWith("Rate ")) {
                    showRecipe = true
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (dish != null) {
                        Text(
                            text = "  ${dish.title}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MealPhoto(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val request = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .build()
    }

    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(10.dp)),
        loading = { MealPhotoPlaceholder() },
        error = { MealPhotoPlaceholder() }
    )
}

@Composable
private fun MealPhotoPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Restaurant,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp)
        )
    }
}
