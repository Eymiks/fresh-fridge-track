package com.freshtrack.ui.screens.credits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.freshtrack.ui.theme.FreshTextStyles

@Composable
fun CreditsScreen(navController: NavController) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 12.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Eco,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Crédits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Technologies utilisées",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.70f)
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding()
            ) {
                Spacer(Modifier.height(20.dp))

                CreditCard(
                    icon = Icons.Default.Search,
                    title = "Données produits",
                    name = "Open Food Facts",
                    detail = "openfoodfacts.org · Licence ODbL"
                )
                CreditCard(
                    icon = Icons.Default.Cloud,
                    title = "Backend & Auth",
                    name = "Supabase",
                    detail = "supabase.com · Licence Apache 2.0"
                )
                CreditCard(
                    icon = Icons.Default.Camera,
                    title = "OCR date de péremption",
                    name = "Google ML Kit Text Recognition",
                    detail = "Gemini API utilisé en fallback cloud"
                )
                CreditCard(
                    icon = Icons.Default.QrCodeScanner,
                    title = "Scan code-barres",
                    name = "Google ML Kit Barcode Scanning",
                    detail = "API Google LLC"
                )
                CreditCard(
                    icon = Icons.Default.Code,
                    title = "Framework Android",
                    name = "Jetpack Compose · Material 3",
                    detail = "Google LLC · Licence Apache 2.0"
                )
                CreditCard(
                    icon = Icons.Default.Image,
                    title = "Chargement d'images",
                    name = "Coil",
                    detail = "github.com/coil-kt/coil · Licence Apache 2.0"
                )
                CreditCard(
                    icon = Icons.Default.BarChart,
                    title = "Graphiques",
                    name = "Vico",
                    detail = "github.com/patrykandpatrick/vico · Licence Apache 2.0"
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "FreshTrack — Réduire le gaspillage alimentaire",
                    style = FreshTextStyles.FormHelper,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CreditCard(
    icon: ImageVector,
    title: String,
    name: String,
    detail: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = FreshTextStyles.FormHelper,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = name,
                    style = FreshTextStyles.FormSectionTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = detail,
                    style = FreshTextStyles.FormHelper,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }
    }
}
