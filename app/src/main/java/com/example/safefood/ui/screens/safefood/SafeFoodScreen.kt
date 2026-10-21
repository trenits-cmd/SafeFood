package com.example.safefood.ui.screens.safefood

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safefood.data.model.FoodItem
import com.example.safefood.ui.SafeFoodViewModel
import com.example.safefood.ui.components.FoodDetailDialog
import com.example.safefood.ui.components.FoodThumbnail
import com.example.safefood.ui.components.NutrientBadgeChip
import com.example.safefood.ui.components.NutrientFilterRow
import com.example.safefood.ui.components.PotentialMealButton
import com.example.safefood.ui.components.StarRatingBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SafeFoodScreen(viewModel: SafeFoodViewModel) {
    val bestRatedFoods by viewModel.bestRatedFoods.collectAsState()
    val recentlyRatedFoods by viewModel.recentlyRatedFoods.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val allFoods by viewModel.foods.collectAsState()
    val nutrientFilters by viewModel.selectedNutrientFilters.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var foodDetailId by remember { mutableStateOf<String?>(null) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val categoryMap = remember(categories) { categories.associate { it.id to it.name } }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy 'at' HH:mm", Locale.getDefault()) }

    val fiveStarCount = remember(allFoods) { allFoods.count { it.rating == 5 } }
    val fourStarCount = remember(allFoods) { allFoods.count { it.rating == 4 } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Quick Stats Summary Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatBadgeCard(
                count = fiveStarCount,
                label = "😋 Yummy (5★)",
                icon = Icons.Default.Verified,
                bgColor = Color(0xFFE8F5E9),
                contentColor = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )
            StatBadgeCard(
                count = fourStarCount,
                label = "🙂 Good (4★)",
                icon = Icons.Default.Star,
                bgColor = Color(0xFFFFF8E1),
                contentColor = Color(0xFFF57F17),
                modifier = Modifier.weight(1f)
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search safe foods...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Nutrient filter (replaces the former Nutrients tab)
        NutrientFilterRow(
            selected = nutrientFilters,
            onToggle = viewModel::toggleNutrientFilter,
            onClear = viewModel::clearNutrientFilters,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tabs: "Best Rated (4-5★)" vs "Latest Rated"
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFFFB300))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Best Rated (${bestRatedFoods.size})", fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Latest Rated (${recentlyRatedFoods.size})", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val currentList = (if (selectedTabIndex == 0) bestRatedFoods else recentlyRatedFoods)
            .filter { food ->
                searchQuery.isBlank() ||
                    food.name.contains(searchQuery, ignoreCase = true) ||
                    food.notes.contains(searchQuery, ignoreCase = true)
            }
            .filter { food ->
                nutrientFilters.isEmpty() || food.nutrientTags.any { it in nutrientFilters }
            }

        if (currentList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedTabIndex == 0) "No 4 or 5-star safe foods yet. Rate items in Categories!" else "No recently rated foods found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(currentList, key = { it.id }) { food ->
                    SafeFoodHighlightCard(
                        food = food,
                        categoryName = categoryMap[food.categoryId] ?: "General",
                        dateFormat = dateFormat,
                        onRatingChanged = { newRating ->
                            viewModel.rateFood(food.id, newRating)
                        },
                        onTogglePotentialMeal = { viewModel.togglePotentialMeal(food.id) },
                        onImageClick = { foodDetailId = food.id }
                    )
                }
            }
        }
    }

    // Dialog: grosses Foto mit direktem Rating
    foodDetailId?.let { id ->
        val food = allFoods.find { it.id == id }
        if (food != null) {
            FoodDetailDialog(
                food = food,
                categoryName = categoryMap[food.categoryId] ?: "General",
                onRatingSelected = { newRating -> viewModel.rateFood(food.id, newRating) },
                onTogglePotentialMeal = { viewModel.togglePotentialMeal(food.id) },
                onDismiss = { foodDetailId = null }
            )
        }
    }
}

@Composable
fun StatBadgeCard(
    count: Int,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "$count",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = contentColor
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = contentColor.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SafeFoodHighlightCard(
    food: FoodItem,
    categoryName: String,
    dateFormat: SimpleDateFormat,
    onRatingChanged: (Int) -> Unit,
    onTogglePotentialMeal: () -> Unit = {},
    onImageClick: () -> Unit = {}
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
                    cornerRadius = 10.dp,
                    modifier = Modifier.clickable(
                        onClickLabel = "Enlarge photo"
                    ) { onImageClick() }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = food.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (food.rating >= 4) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Safe Food Verified",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

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

            if (food.nutrientTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    food.nutrientTags.forEach { nutrient ->
                        NutrientBadgeChip(nutrient = nutrient)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val dateText = food.lastRatedTimestamp?.let {
                "Last rated: ${dateFormat.format(Date(it))}"
            } ?: "Not rated yet"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateText,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                PotentialMealButton(
                    selected = food.isPotentialMeal,
                    onClick = onTogglePotentialMeal
                )
            }
        }
    }
}
