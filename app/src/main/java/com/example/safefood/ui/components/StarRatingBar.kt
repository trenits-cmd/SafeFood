package com.example.safefood.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun getRatingEmoji(rating: Int): String {
    return when (rating) {
        5 -> "😋" // Yummy / Safe Favorite
        4 -> "🙂" // Good / Safe
        3 -> "😐" // Neutral
        2 -> "🙁" // Borderline / Unsure
        1 -> "🤢" // Yucky
        else -> "❓" // Unrated
    }
}

fun getRatingLabel(rating: Int): String {
    return when (rating) {
        5 -> "5★ Yummy!"
        4 -> "4★ Good"
        3 -> "3★ Okay"
        2 -> "2★ Unsure"
        1 -> "1★ Yucky"
        else -> "Unrated"
    }
}

@Composable
fun StarRatingBar(
    rating: Int, // 0 to 5
    onRatingSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 24.dp,
    showEmoji: Boolean = true,
    showLabel: Boolean = false,
    readOnly: Boolean = false,
    activeColor: Color = Color(0xFFFFB300), // Warm amber gold
    inactiveColor: Color = Color(0xFFC4C4C4)
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showEmoji && rating > 0) {
            Text(
                text = getRatingEmoji(rating),
                fontSize = (starSize.value * 0.9).sp,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

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
                                val newRating = if (rating == starIndex) 0 else starIndex
                                onRatingSelected(newRating)
                            }
                        } else Modifier
                    )
            )
        }

        if (showLabel && rating > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = getRatingLabel(rating),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (rating >= 4) Color(0xFF2E7D32) else if (rating <= 2) Color(0xFFC62828) else Color(0xFFF57C00)
            )
        }
    }
}
