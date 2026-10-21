package com.example.safefood.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safefood.data.model.NutrientType

/**
 * Multi-select nutrient filter used by the Categories and SafeFood tabs.
 * An empty selection means "show everything".
 */
@Composable
fun NutrientFilterRow(
    selected: Set<NutrientType>,
    onToggle: (NutrientType) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        item(key = "all") {
            FilterChip(
                selected = selected.isEmpty(),
                onClick = onClear,
                label = { Text("All nutrients", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                leadingIcon = if (selected.isEmpty()) {
                    { Icon(Icons.Default.Spa, contentDescription = null, modifier = Modifier.size(14.dp)) }
                } else null
            )
        }
        items(NutrientType.entries.toList(), key = { it.name }) { nutrient ->
            val isSelected = nutrient in selected
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(nutrient) },
                label = { Text(nutrient.displayName, fontSize = 11.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = getNutrientIcon(nutrient),
                        contentDescription = null,
                        tint = getNutrientColor(nutrient),
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = getNutrientColor(nutrient).copy(alpha = 0.18f),
                    selectedLabelColor = getNutrientColor(nutrient)
                )
            )
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
        NutrientType.PROTEIN -> Color(0xFFD32F2F)
        NutrientType.FIBER -> Color(0xFF388E3C)
        NutrientType.CARBOHYDRATE -> Color(0xFFF57C00)
        NutrientType.HEALTHY_FAT -> Color(0xFF7B1FA2)
        NutrientType.VITAMINS_MINERALS -> Color(0xFF0288D1)
    }
}

fun getNutrientIcon(nutrient: NutrientType): ImageVector {
    return when (nutrient) {
        NutrientType.PROTEIN -> Icons.Default.FitnessCenter
        NutrientType.FIBER -> Icons.Default.Grass
        NutrientType.CARBOHYDRATE -> Icons.Default.Grain
        NutrientType.HEALTHY_FAT -> Icons.Default.WaterDrop
        NutrientType.VITAMINS_MINERALS -> Icons.Default.Spa
    }
}
