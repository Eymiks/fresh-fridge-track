package com.freshtrack.ui.screens.credits

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crédits") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier.fillMaxSize().padding(innerPadding)
                .verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Text("FreshTrack", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Application de suivi des produits du frigo",
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            CreditSection("Données produits",
                "Open Food Facts — openfoodfacts.org\nLicence ODbL")
            CreditSection("Backend",
                "Supabase — supabase.com\nLicence Apache 2.0")
            CreditSection("OCR date",
                "Google ML Kit Text Recognition\nGoogle Gemini (fallback cloud)")
            CreditSection("Scan code-barres",
                "Google ML Kit Barcode Scanning")
            CreditSection("Framework Android",
                "Jetpack Compose + Material 3\nGoogle LLC — Licence Apache 2.0")
            CreditSection("Images",
                "Coil — github.com/coil-kt/coil\nLicence Apache 2.0")
            CreditSection("Graphiques",
                "Vico — github.com/patrykandpatrick/vico\nLicence Apache 2.0")
        }
    }
}

@Composable
private fun CreditSection(title: String, description: String) {
    Text(title, fontWeight = FontWeight.SemiBold)
    Text(description, style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(12.dp))
}
