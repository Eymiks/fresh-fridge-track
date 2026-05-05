package com.freshtrack.ui.screens.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.freshtrack.ui.screens.index.ProductCard

@Composable
fun NotificationsScreen(
    vm: NotificationsViewModel = hiltViewModel(),
    onProductClick: (String) -> Unit = {}
) {
    val settings by vm.settings.collectAsState()
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
    ) { granted -> if (granted) vm.setEnabled(true) }

    Surface(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Alertes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            formatHeaderSubtitle(expiredProducts.size, soonProducts.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Notifications push", fontWeight = FontWeight.Bold)
                                Text(
                                    if (settings.enabled) "Activées · rappel ${settings.days}j" else "En pause",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.enabled,
                                onCheckedChange = { checked ->
                                    if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
                            Text("Rappel avant expiration", fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(1, 3, 7).forEach { option ->
                                    FilterChip(
                                        selected = settings.days == option,
                                        onClick = { vm.setDays(option) },
                                        label = { Text(if (option == 1) "1 jour" else "$option jours") }
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filter == AlertFilter.ALL,
                        onClick = { filter = AlertFilter.ALL },
                        label = { Text("Tout ($alertCount)") }
                    )
                    FilterChip(
                        selected = filter == AlertFilter.EXPIRED,
                        onClick = { filter = AlertFilter.EXPIRED },
                        label = { Text("Périmés (${expiredProducts.size})") }
                    )
                    FilterChip(
                        selected = filter == AlertFilter.SOON,
                        onClick = { filter = AlertFilter.SOON },
                        label = { Text("Bientôt (${soonProducts.size})") }
                    )
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
                    Text("Périmés", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error)
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
                    Text("Bientôt périmés", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
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

private enum class AlertFilter { ALL, EXPIRED, SOON }

@Composable
private fun EmptyAlertState(title: String, subtitle: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            if (title.startsWith("Tout")) Icons.Default.CheckCircle else Icons.Default.Notifications,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(title, fontWeight = FontWeight.Bold)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatHeaderSubtitle(expiredCount: Int, soonCount: Int): String = when {
    expiredCount > 0 && soonCount > 0 -> "$expiredCount périmé(s) · $soonCount bientôt"
    expiredCount > 0 -> "$expiredCount périmé(s)"
    soonCount > 0 -> "$soonCount bientôt périmé(s)"
    else -> "Aucun produit à vérifier"
}
