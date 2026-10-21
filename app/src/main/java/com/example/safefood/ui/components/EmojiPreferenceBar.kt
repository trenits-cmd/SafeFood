package com.example.safefood.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class FoodPreferenceOption(
    val rating: Int,
    val emoji: String,
    val label: String,
    val color: Color
)

val foodPreferences = listOf(
    FoodPreferenceOption(1, "👎", "Dislike", Color(0xFFFFEBEE)),
    FoodPreferenceOption(2, "😐", "Neutral", Color(0xFFFFFDE7)),
    FoodPreferenceOption(3, "👍", "Like", Color(0xFFE8F5E9)),
    FoodPreferenceOption(4, "❤️", "Love", Color(0xFFFCE4EC))
)

@Composable
fun EmojiPreferenceBar(
    rating: Int, // 0 = unrated, 1 = Dislike, 2 = Neutral, 3 = Like, 4 = Love
    onRatingSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        foodPreferences.forEach { pref ->
            val isSelected = rating == pref.rating
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) pref.color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                label = "prefBg"
            )
            val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (!readOnly) {
                            Modifier.clickable {
                                val newRating = if (rating == pref.rating) 0 else pref.rating
                                onRatingSelected(newRating)
                            }
                        } else Modifier
                    )
                    .border(width = if (isSelected) 2.dp else 0.dp, color = borderColor, shape = RoundedCornerShape(12.dp)),
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pref.emoji,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = pref.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
