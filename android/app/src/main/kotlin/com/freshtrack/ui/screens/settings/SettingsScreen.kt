package com.freshtrack.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.freshtrack.data.auth.AuthState
import com.freshtrack.ui.navigation.Routes
import com.freshtrack.ui.screens.auth.AuthViewModel
import com.freshtrack.ui.theme.AccentColor
import com.freshtrack.ui.theme.AppearanceViewModel
import com.freshtrack.ui.theme.Density
import com.freshtrack.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    appearanceVm: AppearanceViewModel = hiltViewModel(),
    authVm: AuthViewModel = hiltViewModel()
) {
    val appearance by appearanceVm.appearance.collectAsState()
    val authState by authVm.authState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apparence") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier.fillMaxSize().padding(innerPadding)
                .verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            // Thème
            Text("Thème", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = appearance.themeMode == mode,
                        onClick = { appearanceVm.setThemeMode(mode) },
                        label = { Text(when(mode) { ThemeMode.SYSTEM -> "Système"; ThemeMode.LIGHT -> "Clair"; ThemeMode.DARK -> "Sombre" }) }
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            // Couleur d'accent
            Text("Couleur d'accent", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccentColor.entries.forEach { color ->
                    FilterChip(
                        selected = appearance.accentColor == color,
                        onClick = { appearanceVm.setAccentColor(color) },
                        label = { Text(color.label) }
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            // Densité
            Text("Densité", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Density.entries.forEach { density ->
                    FilterChip(
                        selected = appearance.density == density,
                        onClick = { appearanceVm.setDensity(density) },
                        label = { Text(density.label) }
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            // Réduire les animations
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Réduire les animations", fontWeight = FontWeight.Medium)
                    Text("Pour les personnes sensibles au mouvement",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = appearance.reduceMotion,
                    onCheckedChange = { appearanceVm.setReduceMotion(it) }
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Text("Foyer & compte", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            if (authState is AuthState.Guest) {
                Card(
                    onClick = { authVm.leaveGuestMode() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Créer un compte ou se connecter", fontWeight = FontWeight.Medium)
                        Text(
                            "Vos produits invités resteront disponibles pour import après connexion.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Card(
                onClick = { navController.navigate(Routes.HOUSEHOLD_SETTINGS) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mon foyer", modifier = Modifier.padding(16.dp))
            }
            Spacer(Modifier.height(8.dp))
            Card(
                onClick = { navController.navigate(Routes.CREDITS) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crédits & licences", modifier = Modifier.padding(16.dp))
            }
        }
    }
}
