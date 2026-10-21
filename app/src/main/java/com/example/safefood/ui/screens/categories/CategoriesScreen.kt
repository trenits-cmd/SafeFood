package com.example.safefood.ui.screens.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safefood.data.catalog.FoodSearch
import com.example.safefood.data.model.FoodCategory
import com.example.safefood.data.model.FoodItem
import com.example.safefood.data.model.NutrientType
import com.example.safefood.ui.SafeFoodViewModel
import com.example.safefood.ui.components.FoodDetailDialog
import com.example.safefood.ui.components.FoodThumbnail
import com.example.safefood.ui.components.NutrientFilterRow
import com.example.safefood.ui.components.PotentialMealButton
import com.example.safefood.ui.components.StarRatingBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoriesScreen(viewModel: SafeFoodViewModel) {
    val categories by viewModel.categories.collectAsState()
    val foods by viewModel.foods.collectAsState()
    val nutrientFilters by viewModel.selectedNutrientFilters.collectAsState()
    val selectedSeasonFilter by viewModel.selectedSeasonFilter.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    var selectedSubTabCategoryId by remember { mutableStateOf<String?>(null) } // null = All Food Groups
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddFoodDialog by remember { mutableStateOf(false) }
    var foodToEdit by remember { mutableStateOf<FoodItem?>(null) }
    var foodDetailId by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy 'at' HH:mm", Locale.getDefault()) }

    // Search, season and nutrient filters are combined (nutrients match any selected tag).
    fun matchesFilters(food: FoodItem): Boolean {
        val matchesSearch = FoodSearch.matches(food, searchQuery)
        val matchesSeason = selectedSeasonFilter == "All Seasons" ||
            food.season.equals(selectedSeasonFilter, ignoreCase = true)
        val matchesNutrients = nutrientFilters.isEmpty() ||
            food.nutrientTags.any { tag -> tag in nutrientFilters }
        return matchesSearch && matchesSeason && matchesNutrients
    }

    val filtersActive = searchQuery.isNotBlank() ||
        selectedSeasonFilter != "All Seasons" ||
        nutrientFilters.isNotEmpty()
    val visibleFoods = foods.filter(::matchesFilters)

    val filteredCategories = if (selectedSubTabCategoryId == null) {
        categories.filter { !it.name.equals("Blacklist", ignoreCase = true) }
    } else {
        if (selectedSubTabCategoryId == "cat_blacklist") {
            categories.filter { it.name.equals("Blacklist", ignoreCase = true) }
        } else {
            categories.filter { it.id == selectedSubTabCategoryId && !it.name.equals("Blacklist", ignoreCase = true) }
        }
    }.filter { category -> !filtersActive || visibleFoods.any { it.categoryId == category.id } }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search & Action Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search safe ingredients...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { viewModel.startRandomRating() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Rate")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Season Filter Chips Row
            val seasons = listOf("All Seasons", "Spring", "Summer", "Autumn", "Winter", "All Year")
            ScrollableTabRow(
                selectedTabIndex = seasons.indexOf(selectedSeasonFilter).coerceAtLeast(0),
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                seasons.forEach { season ->
                    Tab(
                        selected = selectedSeasonFilter == season,
                        onClick = { viewModel.setSeasonFilter(season) },
                        text = { Text(season, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Nutrient filter (replaces the former Nutrients tab)
            NutrientFilterRow(
                selected = nutrientFilters,
                onToggle = viewModel::toggleNutrientFilter,
                onClear = viewModel::clearNutrientFilters,
                modifier = Modifier.fillMaxWidth()
            )

            if (filtersActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${visibleFoods.size} of ${foods.size} foods match",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        searchQuery = ""
                        viewModel.setSeasonFilter("All Seasons")
                        viewModel.clearNutrientFilters()
                    }) {
                        Text("Reset filters", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Food Group Sub-Tabs (Sub tab in Categories: All, Dairy, Fruits, Legumes, Meat, Vegetables, Carbohydrates)
            ScrollableTabRow(
                selectedTabIndex = run {
                    if (selectedSubTabCategoryId == null) 0
                    else {
                        val idx = categories.filter { !it.name.equals("Blacklist", ignoreCase = true) }.indexOfFirst { it.id == selectedSubTabCategoryId }
                        if (idx >= 0) idx + 1 else {
                            val blacklist = categories.find { it.name.equals("Blacklist", ignoreCase = true) }
                            if (blacklist?.id == selectedSubTabCategoryId) categories.filter { !it.name.equals("Blacklist", ignoreCase = true) }.size + 1 else 0
                        }
                    }
                },
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedSubTabCategoryId == null,
                    onClick = { selectedSubTabCategoryId = null },
                    text = { Text("All Groups (${categories.count { !it.name.equals("Blacklist", ignoreCase = true) }})", fontWeight = FontWeight.Bold) }
                )

                categories.filter { !it.name.equals("Blacklist", ignoreCase = true) }.forEach { category ->
                    val itemCount = visibleFoods.count { it.categoryId == category.id }
                    Tab(
                        selected = selectedSubTabCategoryId == category.id,
                        onClick = { selectedSubTabCategoryId = category.id },
                        text = { Text("${category.name} ($itemCount)", fontWeight = FontWeight.Bold) }
                    )
                }
                
                categories.find { it.name.equals("Blacklist", ignoreCase = true) }?.let { blacklist ->
                    val itemCount = foods.count { it.categoryId == blacklist.id }
                    Tab(
                        selected = selectedSubTabCategoryId == blacklist.id,
                        onClick = { selectedSubTabCategoryId = blacklist.id },
                        text = { Text("${blacklist.name} ($itemCount)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredCategories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (filtersActive) {
                            "No foods match the active filters."
                        } else {
                            "No food groups found. Tap '+' to create one!"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    filteredCategories.forEach { category ->
                        val categoryFoods = foods.filter { it.categoryId == category.id }
                            .filter(::matchesFilters)
                        val isExpanded = category.isExpanded || filtersActive

                        item(key = "header-${category.id}") {
                        val rotationAngle by animateFloatAsState(
                            targetValue = if (isExpanded) 180f else 0f,
                            label = "expandRotation"
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Category Header (Clickable to fold/unfold)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleCategoryExpanded(category.id) }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.RestaurantMenu,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = category.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (category.description.isNotBlank()) {
                                            Text(
                                                text = category.description,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = if (filtersActive) {
                                                "${categoryFoods.size} matching item${if (categoryFoods.size == 1) "" else "s"}"
                                            } else {
                                                "${categoryFoods.size} items • ${if (category.isExpanded) "Expanded" else "Folded"}"
                                            },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    if (!category.name.equals("Blacklist", ignoreCase = true) &&
                                        category.name != "Dairy" && category.name != "Fruits" && 
                                        category.name != "Legumes" && category.name != "Meat" && 
                                        category.name != "Vegetables" && category.name != "Carbohydrates" &&
                                        category.name != "Pantry & Spices") {
                                        IconButton(
                                            onClick = { viewModel.deleteCategory(category.id) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Group",
                                                tint = Color.Gray.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.toggleCategoryExpanded(category.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ExpandMore,
                                            contentDescription = "Toggle Fold/Unfold",
                                            modifier = Modifier.rotate(rotationAngle)
                                        )
                                    }
                                }

                            }
                        }
                        }
                        if (isExpanded) {
                            if (categoryFoods.isEmpty()) {
                                item(key = "empty-${category.id}") {
                                    Text(
                                        text = if (filtersActive) {
                                            "No foods in this group match the active filters."
                                        } else {
                                            "No food items in this group yet."
                                        },
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            } else {
                                items(categoryFoods, key = { it.id }) { food ->
                                    FoodItemCard(
                                        food = food,
                                        dateFormat = dateFormat,
                                        onRatingChanged = { newRating ->
                                            viewModel.rateFood(food.id, newRating)
                                        },
                                        onEdit = { foodToEdit = food },
                                        onDelete = { viewModel.deleteFoodItem(food.id) },
                                        onTogglePotentialMeal = { viewModel.togglePotentialMeal(food.id) },
                                        onImageClick = { foodDetailId = food.id }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Floating Action Button to Add Food Item
        FloatingActionButton(
            onClick = { showAddFoodDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Food")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Food", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Dialog: Add Category / Food Group
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name, desc ->
                viewModel.addCategory(name, desc)
                showAddCategoryDialog = false
            }
        )
    }

    // Dialog: Add Food Item
    if (showAddFoodDialog) {
        AddOrEditFoodDialog(
            categories = categories,
            initialFood = null,
            onDismiss = { showAddFoodDialog = false },
            onSave = { categoryId, name, notes, nutrientTags, rating ->
                viewModel.addFoodItem(categoryId, name, notes, nutrientTags, rating)
                showAddFoodDialog = false
            }
        )
    }

    // Dialog: Edit Food Item
    foodToEdit?.let { food ->
        AddOrEditFoodDialog(
            categories = categories,
            initialFood = food,
            onDismiss = { foodToEdit = null },
            onSave = { categoryId, name, notes, nutrientTags, rating ->
                viewModel.updateFoodItem(
                    food.copy(
                        categoryId = categoryId,
                        name = name,
                        notes = notes,
                        nutrientTags = nutrientTags,
                        rating = rating,
                        lastRatedTimestamp = if (rating > 0) System.currentTimeMillis() else food.lastRatedTimestamp
                    )
                )
                foodToEdit = null
            }
        )
    }

    // Dialog: grosses Foto mit direktem Rating
    foodDetailId?.let { id ->
        val food = foods.find { it.id == id }
        if (food != null) {
            FoodDetailDialog(
                food = food,
                categoryName = categories.find { it.id == food.categoryId }?.name ?: "General",
                onRatingSelected = { newRating -> viewModel.rateFood(food.id, newRating) },
                onTogglePotentialMeal = { viewModel.togglePotentialMeal(food.id) },
                onDismiss = { foodDetailId = null }
            )
        }
    }
}

@Composable
fun FoodItemCard(
    food: FoodItem,
    dateFormat: SimpleDateFormat,
    onRatingChanged: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePotentialMeal: () -> Unit = {},
    onImageClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FoodThumbnail(
                food = food,
                size = 42.dp,
                cornerRadius = 8.dp,
                modifier = Modifier.clickable(
                    onClickLabel = "Enlarge photo"
                ) { onImageClick() }
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = food.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = food.season,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                StarRatingBar(
                    rating = food.rating,
                    onRatingSelected = onRatingChanged,
                    starSize = 14.dp,
                    showEmoji = true,
                    showLabel = false
                )

                Spacer(modifier = Modifier.height(3.dp))

                PotentialMealButton(
                    selected = food.isPotentialMeal,
                    onClick = onTogglePotentialMeal
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Food Group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food Group Name (e.g. Dairy, Fruits)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddOrEditFoodDialog(
    categories: List<FoodCategory>,
    initialFood: FoodItem?,
    onDismiss: () -> Unit,
    onSave: (categoryId: String, name: String, notes: String, nutrientTags: List<NutrientType>, rating: Int) -> Unit
) {
    var name by remember { mutableStateOf(initialFood?.name ?: "") }
    var notes by remember { mutableStateOf(initialFood?.notes ?: "") }
    var rating by remember { mutableStateOf(initialFood?.rating ?: 0) }
    var selectedCategoryId by remember { mutableStateOf(initialFood?.categoryId ?: categories.firstOrNull()?.id ?: "") }

    var selectedNutrientTags by remember { mutableStateOf(initialFood?.nutrientTags?.toSet() ?: emptySet()) }

    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialFood == null) "Add Food Item" else "Edit Food Item") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category Picker
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                ) {
                    val currentCategoryName = categories.find { it.id == selectedCategoryId }?.name ?: "Select Food Group"
                    OutlinedTextField(
                        value = currentCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Food Group") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name (e.g. Whole Milk, Broccoli)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Preparation Preference") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Food Rating (1-5 Emoji Stars):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                StarRatingBar(
                    rating = rating,
                    onRatingSelected = { newRating -> rating = newRating },
                    starSize = 24.dp,
                    showEmoji = true,
                    showLabel = true
                )

                Text("Nutrients Classification:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NutrientType.entries.forEach { nutrient ->
                        val isSelected = selectedNutrientTags.contains(nutrient)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedNutrientTags = if (isSelected) {
                                    selectedNutrientTags - nutrient
                                } else {
                                    selectedNutrientTags + nutrient
                                }
                            },
                            label = { Text(nutrient.displayName, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(selectedCategoryId, name, notes, selectedNutrientTags.toList(), rating)
                },
                enabled = name.isNotBlank() && selectedCategoryId.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
