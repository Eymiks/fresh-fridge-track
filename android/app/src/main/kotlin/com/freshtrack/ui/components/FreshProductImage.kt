package com.freshtrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.freshtrack.data.openfoodfacts.OpenFoodFactsImageSize
import com.freshtrack.data.openfoodfacts.normalizeOpenFoodFactsImageUrl

@Composable
fun FreshProductImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    imageSize: OpenFoodFactsImageSize = OpenFoodFactsImageSize.DISPLAY,
    shape: Shape = MaterialTheme.shapes.small,
    tint: Color = MaterialTheme.colorScheme.primary,
    fallbackIcon: ImageVector = Icons.Default.Restaurant
) {
    val context = LocalContext.current
    val normalizedUrl = remember(imageUrl, imageSize) {
        imageUrl
            ?.takeIf { it.isNotBlank() }
            ?.let { normalizeOpenFoodFactsImageUrl(it, imageSize) }
    }
    val imageRequest = remember(normalizedUrl, context) {
        normalizedUrl?.let { url -> ImageRequest.Builder(context).data(url).build() }
    }
    var failed by remember(normalizedUrl) { mutableStateOf(false) }

    Box(
        modifier
            .clip(shape)
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        if (imageRequest != null && !failed) {
            AsyncImage(
                model = imageRequest,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onError = { failed = true }
            )
        } else {
            Icon(
                fallbackIcon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
