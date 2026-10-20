package com.example.safefood.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun StarRatingBar(
    rating: Int, // 0 to 5
    onRatingSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 24.dp,
    readOnly: Boolean = false,
    activeColor: Color = Color(0xFFFFB300), // Warm amber gold
    inactiveColor: Color = Color(0xFFC4C4C4)
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (starIndex in 1..5) {
            val isFilled = starIndex <= rating
            val icon = if (isFilled) Icons.Filled.Star else Icons.Outlined.Star
            val tint = if (isFilled) activeColor else inactiveColor

            Icon(
                imageVector = icon,
                contentDescription = "$starIndex Star",
                tint = tint,
                modifier = Modifier
                    .size(starSize)
                    .padding(1.dp)
                    .then(
                        if (!readOnly) {
                            Modifier.clickable {
                                // Tapping the current rating resets to 0 (unrated), otherwise sets rating
                                val newRating = if (rating == starIndex) 0 else starIndex
                                onRatingSelected(newRating)
                            }
                        } else Modifier
                    )
            )
        }
    }
}
