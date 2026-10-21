package com.example.safefood.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun getRatingEmoji(rating: Int): String {
    return when (rating) {
        5 -> "😋" // Yummy! (Safe Favorite)
        4 -> "🙂" // Good (Safe Option)
        3 -> "😐" // Okay (Neutral)
        2 -> "🙁" // Unsure (Borderline)
        1 -> "🤢" // Yucky (Not Safe / Disliked)
        else -> "❓" // Unrated
    }
}

fun getRatingLabel(rating: Int): String {
    return when (rating) {
        5 -> "5 stars: Yummy (safe favorite)"
        4 -> "4 stars: Good (safe option)"
        3 -> "3 stars: Okay (neutral)"
        2 -> "2 stars: Unsure (borderline)"
        1 -> "1 star: Yucky (not safe)"
        else -> "Unrated"
    }
}

@Composable
fun StarRatingBar(
    rating: Int, // 0 to 5
    onRatingSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 20.dp,
    showEmoji: Boolean = true,
    showLabel: Boolean = false,
    readOnly: Boolean = false,
    activeColor: Color = Color(0xFFFFB300), // Warm amber gold
    inactiveColor: Color = Color(0xFFD1D5DB)
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (showEmoji && rating > 0) {
                Text(
                    text = getRatingEmoji(rating),
                    fontSize = 18.sp,
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
                        .semantics {
                            contentDescription = "$starIndex star"
                            role = Role.Button
                        }
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
        }

        if (showLabel && rating > 0) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = getRatingLabel(rating),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = when (rating) {
                    5, 4 -> Color(0xFF2E7D32)
                    3 -> Color(0xFFF57C00)
                    else -> Color(0xFFC62828)
                }
            )
        }
    }
}
