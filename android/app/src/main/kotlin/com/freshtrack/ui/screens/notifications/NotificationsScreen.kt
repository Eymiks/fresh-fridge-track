package com.freshtrack.ui.screens.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import kotlin.math.roundToInt

@Composable
fun NotificationsScreen(
    vm: NotificationsViewModel = hiltViewModel(),
    onProductClick: (String) -> Unit = {}
) {
    val settings by vm.settings.collectAsState()
    val expiredProducts by vm.expiredProducts.collectAsState()
    val soonProducts by vm.soonProducts.collectAsState()
    var filter by remember { mutableStateOf(AlertFilter.ALL) }
    val alertCount = expiredProducts.size + soonProducts.size

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
                Text(
                    "Alertes d'expiration",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    formatHeaderSubtitle(expiredProducts.size, soonProducts.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                HorizontalDivider()

                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Activer les notifications", fontWeight = FontWeight.Medium)
                        Text("Recevoir des alertes avant l'expiration",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                }

                HorizontalDivider()

                if (settings.enabled) {
                    Spacer(Modifier.height(16.dp))
                    Text("Délai d'alerte", fontWeight = FontWeight.Medium)
                    Text("Alerter ${settings.days} jour(s) avant l'expiration",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = settings.days.toFloat(),
                        onValueChange = { vm.setDays(it.roundToInt()) },
                        valueRange = 1f..14f,
                        steps = 12
                    )
                    Row(Modifier.fillMaxWidth()) {
                        Text("1j", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.weight(1f))
                        Text("14j", style = MaterialTheme.typography.labelSmall)
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
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Tout est sous contrôle", fontWeight = FontWeight.Bold)
                        Text(
                            "Aucun produit n'est périmé ou proche de sa date limite.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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

private fun formatHeaderSubtitle(expiredCount: Int, soonCount: Int): String = when {
    expiredCount > 0 && soonCount > 0 -> "$expiredCount périmé(s) · $soonCount bientôt"
    expiredCount > 0 -> "$expiredCount périmé(s)"
    soonCount > 0 -> "$soonCount bientôt périmé(s)"
    else -> "Aucun produit à vérifier"
}
