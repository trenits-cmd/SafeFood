package com.example.safefood.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.safefood.data.model.FoodItem

/**
 * Einheitliches Lebensmittel-Foto mit Crossfade und ruhigem Platzhalter.
 *
 * Der Platzhalter bleibt sichtbar, waehrend das Bild laedt oder wenn es nicht
 * geladen werden kann - so springt das Layout nie. Es wird nie eine externe
 * Ersatz-URL nachgeladen: fehlt ein Bild, bleibt der neutrale Platzhalter.
 */
@Composable
fun FoodThumbnail(
    food: FoodItem,
    size: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp
) {
    FoodPhoto(
        food = food,
        modifier = modifier.size(size),
        cornerRadius = cornerRadius
    )
}

/**
 * Grosses Lebensmittel-Foto, z. B. in Dialogen. Die Groesse kommt vom Aufrufer
 * (Breite/Hoehe), damit dasselbe Bild in Karten und Detailansichten funktioniert.
 */
@Composable
fun FoodPhoto(
    food: FoodItem,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
) {
    val context = LocalContext.current
    val imageUrl = food.imageUrl

    if (imageUrl.isBlank()) {
        ThumbnailPlaceholder(modifier = modifier.clip(RoundedCornerShape(cornerRadius)))
        return
    }

    val request = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .build()
    }

    SubcomposeAsyncImage(
        model = request,
        contentDescription = food.name,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius)),
        loading = { ThumbnailPlaceholder() },
        error = { ThumbnailPlaceholder() }
    )
}

@Composable
private fun ThumbnailPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Restaurant,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(8.dp)
        )
    }
}
