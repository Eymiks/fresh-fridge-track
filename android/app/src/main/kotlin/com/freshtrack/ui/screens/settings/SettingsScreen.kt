package com.freshtrack.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.freshtrack.data.auth.AuthState
import com.freshtrack.ui.navigation.Routes
import com.freshtrack.ui.screens.auth.AuthViewModel
import com.freshtrack.ui.screens.household.HouseholdSettingsViewModel
import com.freshtrack.ui.theme.AccentColor
import com.freshtrack.ui.theme.AppearanceViewModel
import com.freshtrack.ui.theme.Density
import com.freshtrack.ui.theme.ThemeMode

private val accentColorMap: Map<AccentColor, Color> = mapOf(
    AccentColor.GREEN  to Color(0xFF388E3C),
    AccentColor.BLUE   to Color(0xFF1976D2),
    AccentColor.VIOLET to Color(0xFF7B1FA2),
    AccentColor.ORANGE to Color(0xFFE65100),
    AccentColor.ROSE   to Color(0xFFC2185B),
    AccentColor.CYAN   to Color(0xFF0097A7),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    appearanceVm: AppearanceViewModel = hiltViewModel(),
    authVm: AuthViewModel = hiltViewModel(),
    householdVm: HouseholdSettingsViewModel = hiltViewModel()
) {
    val appearance by appearanceVm.appearance.collectAsState()
    val notifEnabled by appearanceVm.notifEnabled.collectAsState()
    val notifDays by appearanceVm.notifDays.collectAsState()
    val authState by authVm.authState.collectAsState()
    val members by householdVm.members.collectAsState()
    var showSignOutConfirm by remember { mutableStateOf(false) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) appearanceVm.setNotifEnabled(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── Mon profil ─────────────────────────────────────────────────────
            if (authState is AuthState.Authenticated) {
                val auth = authState as AuthState.Authenticated
                val myMember = members.firstOrNull { it.userId == auth.userId }
                var editedName by remember(myMember?.displayName, auth.displayName) {
                    mutableStateOf(myMember?.displayName ?: auth.displayName ?: "")
                }
                val originalName = myMember?.displayName ?: auth.displayName ?: ""

                Text("Mon profil", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    auth.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Prénom / pseudo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (editedName != originalName && editedName.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    myMember?.let { householdVm.updateDisplayName(it.id, editedName) }
                                }
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Sauvegarder")
                            }
                        }
                    }
                )

                HorizontalDivider(Modifier.padding(vertical = 16.dp))
            }

            // ── Thème ──────────────────────────────────────────────────────────
            Text("Thème", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = appearance.themeMode == mode,
                        onClick = { appearanceVm.setThemeMode(mode) },
                        label = {
                            Text(when (mode) {
                                ThemeMode.SYSTEM -> "Système"
                                ThemeMode.LIGHT  -> "Clair"
                                ThemeMode.DARK   -> "Sombre"
                            })
                        }
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            // ── Couleur d'accent (pastilles) ───────────────────────────────────
            Text("Couleur d'accent", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccentColor.entries.forEach { color ->
                    val accentC = accentColorMap[color] ?: Color.Gray
                    val isSelected = appearance.accentColor == color
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentC)
                            .then(
                                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                else Modifier
                            )
                            .clickable { appearanceVm.setAccentColor(color) }
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Sélectionné",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            // ── Densité ────────────────────────────────────────────────────────
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

            // ── Réduire les animations ─────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Réduire les animations", fontWeight = FontWeight.Medium)
                    Text(
                        "Pour les personnes sensibles au mouvement",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = appearance.reduceMotion,
                    onCheckedChange = { appearanceVm.setReduceMotion(it) }
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            // ── Notifications ──────────────────────────────────────────────────
            Text("Notifications", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Alertes d'expiration", fontWeight = FontWeight.Medium)
                    Text(
                        "Recevoir une alerte avant la date limite",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = notifEnabled,
                    onCheckedChange = { checked ->
                        if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            appearanceVm.setNotifEnabled(checked)
                        }
                    }
                )
            }
            if (notifEnabled) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Rappel avant expiration",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 3, 7).forEach { option ->
                        FilterChip(
                            selected = notifDays == option,
                            onClick = { appearanceVm.setNotifDays(option) },
                            label = { Text(if (option == 1) "1 jour" else "$option jours") }
                        )
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            // ── Foyer & compte ─────────────────────────────────────────────────
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

            // ── Déconnexion ────────────────────────────────────────────────────
            if (authState is AuthState.Authenticated) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showSignOutConfirm = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Se déconnecter")
                }
            }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Se déconnecter ?") },
            text = { Text("Vous serez redirigé vers l'écran de connexion.") },
            confirmButton = {
                Button(
                    onClick = { showSignOutConfirm = false; authVm.signOut() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Déconnexion") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text("Annuler") }
            }
        )
    }
}
