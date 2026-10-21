package com.example.safefood.ui.screens.nutrients

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safefood.data.model.FoodItem
import com.example.safefood.data.model.NutrientType
import com.example.safefood.ui.SafeFoodViewModel
import com.example.safefood.ui.components.FoodThumbnail
import com.example.safefood.ui.components.StarRatingBar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NutrientsScreen(viewModel: SafeFoodViewModel) {
    val foods by viewModel.foods.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    var selectedNutrientFilter by remember { mutableStateOf<NutrientType?>(null) } // null = All
    val categoryMap = remember(categories) { categories.associate { it.id to it.name } }

    val filteredFoods = remember(foods, selectedNutrientFilter, searchQuery) {
        foods
            .filter { item ->
                if (selectedNutrientFilter == null) true
                else item.nutrientTags.contains(selectedNutrientFilter)
            }
            .filter { item ->
                searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true) || item.notes.contains(searchQuery, ignoreCase = true)
            }
            .sortedByDescending { it.rating }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search ingredients by nutrient...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Nutrient Category Tabs
        ScrollableTabRow(
            selectedTabIndex = if (selectedNutrientFilter == null) 0 else NutrientType.values().indexOf(selectedNutrientFilter) + 1,
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedNutrientFilter == null,
                onClick = { selectedNutrientFilter = null },
                text = { Text("All Foods (${foods.size})", fontWeight = FontWeight.Bold) }
            )

            NutrientType.values().forEach { nutrient ->
                val count = foods.count { it.nutrientTags.contains(nutrient) }
                Tab(
                    selected = selectedNutrientFilter == nutrient,
                    onClick = { selectedNutrientFilter = nutrient },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${nutrient.displayName} ($count)", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Nutrient Info Banner Card
        selectedNutrientFilter?.let { nutrient ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = getNutrientColor(nutrient).copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = getNutrientColor(nutrient),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = getNutrientIcon(nutrient),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = nutrient.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = getNutrientColor(nutrient)
                        )
                        Text(
                            text = nutrient.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Food List Sorted By Selected Nutrient
        if (filteredFoods.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No ingredients found for this nutrient category.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredFoods, key = { it.id }) { food ->
                    NutrientFoodCard(
                        food = food,
                        categoryName = categoryMap[food.categoryId] ?: "General",
                        onRatingChanged = { newRating ->
                            viewModel.rateFood(food.id, newRating)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NutrientFoodCard(
    food: FoodItem,
    categoryName: String,
    onRatingChanged: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FoodThumbnail(
                    food = food,
                    size = 52.dp,
                    cornerRadius = 10.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = food.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Category: $categoryName",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    if (food.notes.isNotBlank()) {
                        Text(
                            text = food.notes,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }

                StarRatingBar(
                    rating = food.rating,
                    onRatingSelected = onRatingChanged,
                    starSize = 16.dp
                )
            }

            // Nutrient Badges
            if (food.nutrientTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    food.nutrientTags.forEach { nutrient ->
                        NutrientBadgeChip(nutrient = nutrient)
                    }
                }
            }
        }
    }
}

@Composable
fun NutrientBadgeChip(nutrient: NutrientType) {
    val color = getNutrientColor(nutrient)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.padding(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getNutrientIcon(nutrient),
                contentDescription = nutrient.displayName,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = nutrient.displayName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

fun getNutrientColor(nutrient: NutrientType): Color {
    return when (nutrient) {
        NutrientType.PROTEIN -> Color(0xFFD32F2F)       // Red/Coral
        NutrientType.FIBER -> Color(0xFF388E3C)         // Green
        NutrientType.CARBOHYDRATE -> Color(0xFFF57C00)  // Orange
        NutrientType.HEALTHY_FAT -> Color(0xFF7B1FA2)   // Purple
        NutrientType.VITAMINS_MINERALS -> Color(0xFF0288D1) // Blue
    }
}

fun getNutrientIcon(nutrient: NutrientType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (nutrient) {
        NutrientType.PROTEIN -> Icons.Default.FitnessCenter
        NutrientType.FIBER -> Icons.Default.Grass
        NutrientType.CARBOHYDRATE -> Icons.Default.Grain
        NutrientType.HEALTHY_FAT -> Icons.Default.WaterDrop
        NutrientType.VITAMINS_MINERALS -> Icons.Default.Spa
    }
}
