package com.freshtrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.freshtrack.domain.format.formatInstantDate
import com.freshtrack.domain.model.ExpirationStatus
import com.freshtrack.domain.model.Product
import com.freshtrack.domain.model.ProductStatus
import com.freshtrack.domain.model.getDaysUntilExpiration
import com.freshtrack.domain.model.getExpirationStatus
import com.freshtrack.ui.theme.ColorExpired
import com.freshtrack.ui.theme.ColorFresh
import com.freshtrack.ui.theme.ColorSoon
import com.freshtrack.ui.theme.Density
import com.freshtrack.ui.theme.LocalAppearance

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProductCard(
    product: Product,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onConsume: (() -> Unit)? = null,
    onThrow: (() -> Unit)? = null
) {
    val status = remember(
        product.status,
        product.openedAt,
        product.daysAfterOpening,
        product.expirationDate,
        product.frozenUntil
    ) { product.getExpirationStatus() }
    val daysLeft = remember(
        product.status,
        product.openedAt,
        product.daysAfterOpening,
        product.expirationDate,
        product.frozenUntil
    ) { product.getDaysUntilExpiration() }
    val statusColor = status.color()
    val daysLabel = remember(daysLeft) {
        when {
            daysLeft < 0 -> "${-daysLeft}j"
            daysLeft == 0 -> "Auj."
            else -> "${daysLeft}j"
        }
    }
    val imageRequest = rememberProductImageRequest(product.imageUrl)

    val verticalPadding = when (LocalAppearance.current.density) {
        Density.COMPACT -> 2.dp
        Density.NORMAL -> 4.dp
        Density.SPACIOUS -> 8.dp
    }

    var showMenu by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { onConsume?.invoke(); onConsume != null }
                SwipeToDismissBoxValue.EndToStart -> { onThrow?.invoke(); onThrow != null }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = verticalPadding),
        enableDismissFromStartToEnd = !isSelectionMode && onConsume != null,
        enableDismissFromEndToStart = !isSelectionMode && onThrow != null,
        backgroundContent = {
            SwipeBackground(dismissState.targetValue)
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusStripe(statusColor)
                    .padding(start = 4.dp)
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Checkbox(checked = isSelected, onCheckedChange = null)
                    Spacer(Modifier.width(4.dp))
                }

                ProductImage(product = product, imageRequest = imageRequest, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))

                ProductMainText(product = product, showOpenedPill = true)

                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Pill(daysLabel, statusColor, filled = true)
                        if (!product.nutriScore.isNullOrBlank()) {
                            NutriScoreBadge(product.nutriScore)
                        }
                    }
                    if (!isSelectionMode) {
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.MoreVert, "Plus d'options", Modifier.size(16.dp))
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                if (onEdit != null) {
                                    DropdownMenuItem(
                                        text = { Text("Modifier") },
                                        onClick = { showMenu = false; onEdit() },
                                        leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) }
                                    )
                                }
                                if (onConsume != null) {
                                    DropdownMenuItem(
                                        text = { Text("Marquer consommé") },
                                        onClick = { showMenu = false; onConsume() },
                                        leadingIcon = { Icon(Icons.Default.Check, null, tint = ColorFresh, modifier = Modifier.size(18.dp)) }
                                    )
                                }
                                if (onThrow != null) {
                                    DropdownMenuItem(
                                        text = { Text("Jeter") },
                                        onClick = { showMenu = false; onThrow() },
                                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = ColorExpired, modifier = Modifier.size(18.dp)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ArchivedProductCard(
    product: Product,
    onClick: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = product.status.archiveMeta()
    val imageRequest = rememberProductImageRequest(product.imageUrl)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusStripe(status.color)
                .padding(start = 4.dp)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductImage(product = product, imageRequest = imageRequest, tint = status.color)
            Spacer(Modifier.width(10.dp))
            ProductMainText(product = product, showOpenedPill = false, subtitleOverride = statusDate(product))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill(status.label, status.color)
                Surface(
                    onClick = onRestore,
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Réactiver", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue) {
    val (bgColor, alignment) = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> ColorFresh.copy(alpha = 0.15f) to Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> ColorExpired.copy(alpha = 0.15f) to Alignment.CenterEnd
        else -> Color.Transparent to Alignment.Center
    }
    Box(
        Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.medium)
            .background(bgColor)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, null, tint = ColorFresh)
                Spacer(Modifier.width(4.dp))
                Text("Consommé", color = ColorFresh, fontWeight = FontWeight.Medium)
            }
            SwipeToDismissBoxValue.EndToStart -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Jeté", color = ColorExpired, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.Delete, null, tint = ColorExpired)
            }
            else -> {}
        }
    }
}

@Composable
private fun ProductImage(product: Product, imageRequest: ImageRequest?, tint: Color) {
    Box(contentAlignment = Alignment.TopEnd) {
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = product.name,
                modifier = Modifier.size(52.dp).clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AcUnit, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            }
        }
        if (product.frozenUntil != null) {
            Box(
                Modifier
                    .size(18.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(Color(0xFF0288D1)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AcUnit, contentDescription = "Congelé", tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun RowScope.ProductMainText(
    product: Product,
    showOpenedPill: Boolean,
    subtitleOverride: String? = null
) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            product.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            subtitleOverride ?: listOfNotNull(product.brand, product.quantity).joinToString(" · ").ifBlank { "Produit du foyer" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (showOpenedPill && product.status == ProductStatus.OPENED) {
                Pill("Ouvert", MaterialTheme.colorScheme.tertiary)
            }
        }
        if (!product.addedByName.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    "Ajouté par ${product.addedByName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun Pill(label: String, color: Color, filled: Boolean = false) {
    val bg = if (filled) color else color.copy(alpha = 0.12f)
    val fg = if (filled) Color.White else color
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NutriScoreBadge(score: String) {
    val (bg, fg) = when (score.uppercase()) {
        "A" -> Color(0xFF1B5E20) to Color.White
        "B" -> Color(0xFF558B2F) to Color.White
        "C" -> Color(0xFFF9A825) to Color.Black
        "D" -> Color(0xFFE65100) to Color.White
        "E" -> Color(0xFFB71C1C) to Color.White
        else -> return
    }
    Box(
        Modifier.size(20.dp).clip(MaterialTheme.shapes.extraSmall).background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(score.uppercase(), color = fg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun rememberProductImageRequest(imageUrl: String?): ImageRequest? {
    val context = LocalContext.current
    return remember(imageUrl) {
        imageUrl?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .build()
        }
    }
}

private fun Modifier.statusStripe(color: Color): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        size = Size(4.dp.toPx(), size.height),
        cornerRadius = CornerRadius(4.dp.toPx())
    )
}

private fun ExpirationStatus.color(): Color = when (this) {
    ExpirationStatus.EXPIRED -> ColorExpired
    ExpirationStatus.SOON -> ColorSoon
    ExpirationStatus.FRESH -> ColorFresh
}

private data class ArchiveMeta(
    val label: String,
    val color: Color,
    val icon: ImageVector
)

@Composable
private fun ProductStatus.archiveMeta(): ArchiveMeta = when (this) {
    ProductStatus.OPENED -> ArchiveMeta("Ouvert", MaterialTheme.colorScheme.primary, Icons.Default.Inventory2)
    ProductStatus.CONSUMED -> ArchiveMeta("Consommé", ColorFresh, Icons.Default.Restaurant)
    ProductStatus.THROWN -> ArchiveMeta("Jeté", MaterialTheme.colorScheme.error, Icons.Default.Delete)
    ProductStatus.ACTIVE -> ArchiveMeta("Actif", MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.Check)
}

private fun statusDate(product: Product): String {
    val instant = product.statusChangedAt ?: product.addedAt
    val date = formatInstantDate(instant).orEmpty()
    return when (product.status) {
        ProductStatus.OPENED -> "Ouvert le $date"
        ProductStatus.CONSUMED -> "Consommé le $date"
        ProductStatus.THROWN -> "Jeté le $date"
        ProductStatus.ACTIVE -> "Actif depuis le $date"
    }
}
