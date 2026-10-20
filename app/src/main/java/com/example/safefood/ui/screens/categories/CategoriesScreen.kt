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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safefood.data.model.FoodCategory
import com.example.safefood.data.model.FoodItem
import com.example.safefood.data.model.NutrientType
import com.example.safefood.ui.SafeFoodViewModel
import com.example.safefood.ui.components.SensoryTagChip
import com.example.safefood.ui.components.StarRatingBar
import com.example.safefood.ui.screens.nutrients.NutrientBadgeChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoriesScreen(viewModel: SafeFoodViewModel) {
    val categories by viewModel.categories.collectAsState()
    val foods by viewModel.foods.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddFoodDialog by remember { mutableStateOf(false) }
    var foodToEdit by remember { mutableStateOf<FoodItem?>(null) }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy 'at' HH:mm", Locale.getDefault()) }

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
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search safe ingredients...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { showAddCategoryDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = "New Category", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Category")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (categories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No categories found. Tap '+' to create one!", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categories, key = { it.id }) { category ->
                        val categoryFoods = foods.filter { it.categoryId == category.id }
                            .filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.notes.contains(searchQuery, ignoreCase = true) }

                        val rotationAngle by animateFloatAsState(
                            targetValue = if (category.isExpanded) 180f else 0f,
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
                                            text = "${categoryFoods.size} items • ${if (category.isExpanded) "Expanded" else "Folded"}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteCategory(category.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Category",
                                            tint = Color.Gray.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
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

                                // Foldable / Expandable Food Items List
                                AnimatedVisibility(
                                    visible = category.isExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp)
                                            .padding(bottom = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (categoryFoods.isEmpty()) {
                                            Text(
                                                text = "No food items in this category yet.",
                                                fontSize = 13.sp,
                                                color = Color.Gray,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        } else {
                                            categoryFoods.forEach { food ->
                                                FoodItemCard(
                                                    food = food,
                                                    dateFormat = dateFormat,
                                                    onRatingChanged = { newRating ->
                                                        viewModel.rateFood(food.id, newRating)
                                                    },
                                                    onEdit = { foodToEdit = food },
                                                    onDelete = { viewModel.deleteFoodItem(food.id) }
                                                )
                                            }
                                        }
                                    }
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

    // Dialog: Add Category
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
            onSave = { categoryId, name, notes, textureTags, nutrientTags, rating ->
                viewModel.addFoodItem(categoryId, name, notes, textureTags, nutrientTags, rating)
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
            onSave = { categoryId, name, notes, textureTags, nutrientTags, rating ->
                viewModel.updateFoodItem(
                    food.copy(
                        categoryId = categoryId,
                        name = name,
                        notes = notes,
                        textureTags = textureTags,
                        nutrientTags = nutrientTags,
                        rating = rating,
                        lastRatedTimestamp = if (rating > 0) System.currentTimeMillis() else food.lastRatedTimestamp
                    )
                )
                foodToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodItemCard(
    food: FoodItem,
    dateFormat: SimpleDateFormat,
    onRatingChanged: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = food.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (food.notes.isNotBlank()) {
                        Text(
                            text = food.notes,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Nutrient Badges
            if (food.nutrientTags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    food.nutrientTags.forEach { nutrient ->
                        NutrientBadgeChip(nutrient = nutrient)
                    }
                }
            }

            // Texture Tags
            if (food.textureTags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    food.textureTags.forEach { tag ->
                        SensoryTagChip(tag = tag)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Rating & Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StarRatingBar(
                    rating = food.rating,
                    onRatingSelected = onRatingChanged,
                    starSize = 22.dp
                )

                val formattedDate = food.lastRatedTimestamp?.let {
                    dateFormat.format(Date(it))
                } ?: "Not rated yet"

                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Normal
                )
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
        title = { Text("New Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name (e.g. Fresh Ingredients)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Sensory Notes") },
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
    onSave: (categoryId: String, name: String, notes: String, textureTags: List<String>, nutrientTags: List<NutrientType>, rating: Int) -> Unit
) {
    var name by remember { mutableStateOf(initialFood?.name ?: "") }
    var notes by remember { mutableStateOf(initialFood?.notes ?: "") }
    var rating by remember { mutableStateOf(initialFood?.rating ?: 0) }
    var selectedCategoryId by remember { mutableStateOf(initialFood?.categoryId ?: categories.firstOrNull()?.id ?: "") }

    val availableTextureTags = listOf("Crunchy", "Smooth", "Soft", "Dry", "Bland", "Warm", "Cold", "Salty", "Sweet", "Crispy", "Fresh")
    var selectedTextureTags by remember { mutableStateOf(initialFood?.textureTags?.toSet() ?: emptySet()) }

    var selectedNutrientTags by remember { mutableStateOf(initialFood?.nutrientTags?.toSet() ?: emptySet()) }

    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialFood == null) "Add Food Ingredient" else "Edit Food Ingredient") },
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
                    val currentCategoryName = categories.find { it.id == selectedCategoryId }?.name ?: "Select Category"
                    OutlinedTextField(
                        value = currentCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
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
                    label = { Text("Ingredient Name (e.g., Salmon, Tomatoes, Brown Rice)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Cooking Preference") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Rating (1-5 Stars from Yucky to Yummy):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                StarRatingBar(
                    rating = rating,
                    onRatingSelected = { rating = it },
                    starSize = 28.dp,
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

                Text("Texture & Sensory Tags:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableTextureTags.forEach { tag ->
                        val isSelected = selectedTextureTags.contains(tag)
                        SensoryTagChip(
                            tag = tag,
                            isSelected = isSelected,
                            onClick = {
                                selectedTextureTags = if (isSelected) {
                                    selectedTextureTags - tag
                                } else {
                                    selectedTextureTags + tag
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(selectedCategoryId, name, notes, selectedTextureTags.toList(), selectedNutrientTags.toList(), rating)
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
