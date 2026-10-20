package com.example.safefood.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SensoryTagChip(
    tag: String,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val (bgColor, textColor) = getTagColors(tag, isSelected)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = tag,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

private fun getTagColors(tag: String, isSelected: Boolean): Pair<Color, Color> {
    if (isSelected) {
        return Pair(Color(0xFF2E7D32), Color.White)
    }
    return when (tag.lowercase()) {
        "crunchy", "crispy" -> Pair(Color(0xFFFFF3E0), Color(0xFFE65100))
        "smooth", "soft" -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        "bland", "plain" -> Pair(Color(0xFFF5F5F5), Color(0xFF616161))
        "warm" -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
        "cold" -> Pair(Color(0xFFE1F5FE), Color(0xFF0277BD))
        "salty" -> Pair(Color(0xFFEFEBE9), Color(0xFF4E342E))
        "sweet" -> Pair(Color(0xFFFCE4EC), Color(0xFFAD1457))
        else -> Pair(Color(0xFFEDE7F6), Color(0xFF4527A0))
    }
}
