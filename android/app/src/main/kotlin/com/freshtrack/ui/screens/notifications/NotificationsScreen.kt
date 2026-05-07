package com.freshtrack.ui.screens.notifications

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.ContextCompat
import com.freshtrack.ui.screens.index.ProductCard

@Composable
fun NotificationsScreen(
    vm: NotificationsViewModel = hiltViewModel(),
    onProductClick: (String) -> Unit = {}
) {
    val settings by vm.settings.collectAsState()
    val permissionRequested by vm.permissionRequested.collectAsState()
    val expiredProducts by vm.expiredProducts.collectAsState()
    val soonProducts by vm.soonProducts.collectAsState()
    var filter by remember { mutableStateOf(AlertFilter.ALL) }
    var settingsOpen by remember { mutableStateOf(false) }
    val alertCount = expiredProducts.size + soonProducts.size
    val filteredCount = when (filter) {
        AlertFilter.ALL -> alertCount
        AlertFilter.EXPIRED -> expiredProducts.size
        AlertFilter.SOON -> soonProducts.size
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        vm.markPermissionRequested()
        if (granted) vm.setEnabled(true) else vm.setEnabled(false)
    }

    val context = LocalContext.current
    val activity = context as? Activity
    val notificationPermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val permPermanentlyDenied = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissionRequested &&
            !notificationPermissionGranted &&
            activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == false
    } else false

    Surface(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Alertes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Text(
                                formatHeaderSubtitle(expiredProducts.size, soonProducts.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (permPermanentlyDenied) {
                    item {
                        Card(
                            Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = MaterialTheme.shapes.large,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.24f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Block,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Notifications bloquées",
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        "Autorisez les notifications dans les paramètres système.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                TextButton(onClick = {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                }) {
                                    Text("Ouvrir", color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.size(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Notifications push", fontWeight = FontWeight.Black)
                                    Text(
                                        if (settings.enabled) "Activées · rappel ${settings.days}j" else "En pause",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = settings.enabled,
                                    onCheckedChange = { checked ->
                                        if (checked && !notificationPermissionGranted) {
                                            vm.markPermissionRequested()
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            vm.setEnabled(checked)
                                        }
                                    }
                                )
                                IconButton(onClick = { settingsOpen = !settingsOpen }) {
                                    Icon(
                                        if (settingsOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (settingsOpen) "Replier" else "Déplier"
                                    )
                                }
                            }

                            if (settingsOpen && settings.enabled) {
                                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                                Text("Rappel avant expiration", fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(1, 3, 7).forEach { option ->
                                        AlertFilterPill(
                                            label = if (option == 1) "1 jour" else "$option jours",
                                            selected = settings.days == option,
                                            color = MaterialTheme.colorScheme.primary,
                                            onClick = { vm.setDays(option) }
                                        )
                                    }
                                }
                            } else if (settingsOpen) {
                                Text(
                                    "Activez les notifications pour recevoir un rappel avant expiration.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AlertFilterPill("Tout ($alertCount)", filter == AlertFilter.ALL, MaterialTheme.colorScheme.primary) {
                            filter = AlertFilter.ALL
                        }
                        AlertFilterPill("Périmés (${expiredProducts.size})", filter == AlertFilter.EXPIRED, MaterialTheme.colorScheme.error) {
                            filter = AlertFilter.EXPIRED
                        }
                        AlertFilterPill("Bientôt (${soonProducts.size})", filter == AlertFilter.SOON, MaterialTheme.colorScheme.tertiary) {
                            filter = AlertFilter.SOON
                        }
                    }
                }

                if (alertCount == 0) {
                    item {
                        EmptyAlertState(
                            title = "Tout est sous contrôle",
                            subtitle = "Aucun produit n'est périmé ou proche de sa date limite."
                        )
                    }
                } else if (filteredCount == 0) {
                    item {
                        EmptyAlertState(
                            title = "Aucun produit pour ce filtre",
                            subtitle = "Changez de filtre ou revenez à Tout."
                        )
                    }
                }

                if ((filter == AlertFilter.ALL || filter == AlertFilter.EXPIRED) && expiredProducts.isNotEmpty()) {
                    item {
                        AlertSectionHeader("Périmés", expiredProducts.size, Icons.Default.ErrorOutline, MaterialTheme.colorScheme.error)
                    }
                    items(expiredProducts, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            isSelected = false,
                            isSelectionMode = false,
                            onClick = { onProductClick(product.id) },
                            onLongClick = {}
                        )
                    }
                }

                if ((filter == AlertFilter.ALL || filter == AlertFilter.SOON) && soonProducts.isNotEmpty()) {
                    item {
                        AlertSectionHeader("Bientôt périmés", soonProducts.size, Icons.Default.Notifications, MaterialTheme.colorScheme.tertiary)
                    }
                    items(soonProducts, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            isSelected = false,
                            isSelectionMode = false,
                            onClick = { onProductClick(product.id) },
                            onLongClick = {}
                        )
                    }
                }
            }
        }
    }
}

private enum class AlertFilter { ALL, EXPIRED, SOON }

@Composable
private fun AlertFilterPill(
    label: String,
    selected: Boolean,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            color = if (selected) color else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AlertSectionHeader(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .size(34.dp)
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = color,
            modifier = Modifier.weight(1f)
        )
        Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.12f)) {
            Text(
                count.toString(),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
private fun EmptyAlertState(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .background(
                        if (title.startsWith("Tout")) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (title.startsWith("Tout")) Icons.Default.CheckCircle else Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (title.startsWith("Tout")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(title, fontWeight = FontWeight.Black)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatHeaderSubtitle(expiredCount: Int, soonCount: Int): String = when {
    expiredCount > 0 && soonCount > 0 -> "$expiredCount périmé(s) · $soonCount bientôt"
    expiredCount > 0 -> "$expiredCount périmé(s)"
    soonCount > 0 -> "$soonCount bientôt périmé(s)"
    else -> "Aucun produit à vérifier"
}
