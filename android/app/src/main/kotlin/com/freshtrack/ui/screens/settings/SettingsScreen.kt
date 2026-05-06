package com.freshtrack.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.freshtrack.data.auth.AuthState
import com.freshtrack.domain.model.Member
import com.freshtrack.ui.components.OfflineBanner
import com.freshtrack.ui.navigation.Routes
import com.freshtrack.ui.screens.auth.AuthViewModel
import com.freshtrack.ui.screens.household.HouseholdSettingsViewModel
import com.freshtrack.ui.theme.AccentColor
import com.freshtrack.ui.theme.AppearanceViewModel
import com.freshtrack.ui.theme.Density
import com.freshtrack.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val accentColorMap: Map<AccentColor, Color> = mapOf(
    AccentColor.GREEN to Color(0xFF388E3C),
    AccentColor.BLUE to Color(0xFF1976D2),
    AccentColor.VIOLET to Color(0xFF7B1FA2),
    AccentColor.ORANGE to Color(0xFFE65100),
    AccentColor.ROSE to Color(0xFFC2185B),
    AccentColor.CYAN to Color(0xFF0097A7),
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
    val household by householdVm.household.collectAsState()
    val members by householdVm.members.collectAsState()
    val householdUi by householdVm.ui.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showSignOutConfirm by remember { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<Member?>(null) }
    var editedName by remember { mutableStateOf("") }
    var householdName by remember(household?.name) { mutableStateOf(household?.name.orEmpty()) }
    var copiedInvite by remember { mutableStateOf(false) }

    val auth = authState as? AuthState.Authenticated
    val myMember = members.firstOrNull { it.userId == auth?.userId }
    val displayName = myMember?.displayName ?: auth?.displayName ?: "Invité"
    val isOwner = household?.createdBy == auth?.userId

    LaunchedEffect(myMember?.displayName, auth?.displayName) {
        editedName = myMember?.displayName ?: auth?.displayName.orEmpty()
    }

    LaunchedEffect(householdUi.successMessage, householdUi.error) {
        val message = householdUi.successMessage ?: householdUi.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        householdVm.clearMessages()
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) appearanceVm.setNotifEnabled(true) }

    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val hh = household ?: run {
            householdVm.reportError("Impossible d'envoyer l'avatar : foyer introuvable.")
            return@rememberLauncherForActivityResult
        }
        val userId = auth?.userId ?: run {
            householdVm.reportError("Impossible d'envoyer l'avatar : utilisateur introuvable.")
            return@rememberLauncherForActivityResult
        }
        val mimeType = context.contentResolver.getType(uri)
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: run {
                householdVm.reportError("Impossible de lire l'image sélectionnée.")
                return@launch
            }
            householdVm.uploadAvatar(hh.id, userId, bytes, ext)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            OfflineBanner()
            Column(
                Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            ProfileHeader(
                displayName = displayName,
                subtitle = when (authState) {
                    is AuthState.Guest -> "Mode invité · Frigo local"
                    is AuthState.Authenticated -> "${auth?.email.orEmpty()} · ${household?.name.orEmpty()}"
                    else -> ""
                },
                avatarUrl = householdUi.avatarUrlOverride ?: myMember?.avatarUrl,
                isLoading = householdUi.isLoading,
                onAvatarClick = { if (auth != null) avatarLauncher.launch(arrayOf("image/*")) }
            )

            SectionCard(title = "Profil", icon = Icons.Default.Person) {
                if (authState is AuthState.Guest) {
                    Text("Vos produits restent sur cet appareil.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { authVm.leaveGuestMode() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Login, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Créer un compte ou se connecter")
                    }
                } else {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it.take(30) },
                        label = { Text("Prénom / pseudo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (editedName.isNotBlank() && editedName != displayName) {
                                IconButton(onClick = { myMember?.let { householdVm.updateDisplayName(it.id, editedName.trim()) } }) {
                                    Icon(Icons.Default.Check, contentDescription = "Sauvegarder")
                                }
                            }
                        }
                    )
                }
            }

            SectionCard(title = "Apparence", icon = Icons.Default.Palette) {
                Text("Thème", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = appearance.themeMode == mode,
                            onClick = { appearanceVm.setThemeMode(mode) },
                            label = {
                                Text(when (mode) {
                                    ThemeMode.SYSTEM -> "Système"
                                    ThemeMode.LIGHT -> "Clair"
                                    ThemeMode.DARK -> "Sombre"
                                })
                            }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Couleur d'accent", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccentColor.entries.forEach { color ->
                        AccentSwatch(color, appearance.accentColor == color) { appearanceVm.setAccentColor(color) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Densité", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Density.entries.forEach { density ->
                        FilterChip(
                            selected = appearance.density == density,
                            onClick = { appearanceVm.setDensity(density) },
                            label = { Text(density.label) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                SettingsSwitchRow(
                    title = "Réduire les animations",
                    subtitle = "Désactive les transitions sensibles au mouvement",
                    checked = appearance.reduceMotion,
                    onCheckedChange = appearanceVm::setReduceMotion
                )
            }

            SectionCard(title = "Notifications", icon = Icons.Default.Notifications) {
                SettingsSwitchRow(
                    title = "Alertes d'expiration",
                    subtitle = "Recevoir une alerte avant la date limite",
                    checked = notifEnabled,
                    onCheckedChange = { checked ->
                        if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            appearanceVm.setNotifEnabled(checked)
                        }
                    }
                )
                if (notifEnabled) {
                    Spacer(Modifier.height(10.dp))
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
            }

            if (auth != null && household != null) {
                SectionCard(title = "Foyer", icon = Icons.Default.Home) {
                    OutlinedTextField(
                        value = householdName,
                        onValueChange = { householdName = it.take(40) },
                        label = { Text("Nom du foyer") },
                        enabled = isOwner,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (isOwner && householdName.isNotBlank() && householdName != household?.name) {
                                IconButton(onClick = { household?.let { householdVm.updateHouseholdName(it.id, householdName.trim()) } }) {
                                    Icon(Icons.Default.Check, contentDescription = "Renommer")
                                }
                            }
                        }
                    )
                    if (!isOwner) {
                        Text("Seul le propriétaire peut renommer le foyer.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(12.dp))
                    InviteCodeRow(
                        inviteCode = household!!.inviteCode,
                        copied = copiedInvite,
                        onCopy = {
                            clipboard.setText(AnnotatedString(household!!.inviteCode))
                            copiedInvite = true
                        },
                        onShare = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Rejoins mon frigo sur FreshTrack avec le code : ${household!!.inviteCode}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Partager le code d'invitation"))
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Membres (${members.size})", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    members.forEach { member ->
                        MemberRow(
                            member = member,
                            isMe = member.userId == auth.userId,
                            canRemove = isOwner && member.userId != auth.userId,
                            onRemove = { memberToRemove = member }
                        )
                    }
                }
            }

            Card(onClick = { navController.navigate(Routes.CREDITS) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(12.dp))
                    Text("Crédits & licences", fontWeight = FontWeight.SemiBold)
                }
            }

            if (auth != null) {
                Button(
                    onClick = { showSignOutConfirm = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Se déconnecter")
                }
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
            dismissButton = { TextButton(onClick = { showSignOutConfirm = false }) { Text("Annuler") } }
        )
    }

    memberToRemove?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text("Retirer ce membre ?") },
            text = { Text("${member.displayName} pourra rejoindre à nouveau avec le code d'invitation.") },
            confirmButton = {
                Button(
                    onClick = {
                        memberToRemove = null
                        householdVm.removeMember(member.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Retirer") }
            },
            dismissButton = { TextButton(onClick = { memberToRemove = null }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun ProfileHeader(
    displayName: String,
    subtitle: String,
    avatarUrl: String?,
    isLoading: Boolean,
    onAvatarClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(avatarUrl, displayName, Modifier.size(64.dp).clickable(onClick = onAvatarClick))
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Paramètres", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                }
                Text(displayName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (isLoading) {
                Text("…", style = MaterialTheme.typography.titleLarge)
            } else {
                Icon(Icons.Default.CameraAlt, contentDescription = "Changer l'avatar")
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.size(10.dp))
                Text(title, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun AccentSwatch(color: AccentColor, selected: Boolean, onClick: () -> Unit) {
    val accentC = accentColorMap[color] ?: Color.Gray
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(accentC)
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape) else Modifier)
            .clickable(onClick = onClick)
    ) {
        if (selected) Icon(Icons.Default.Check, contentDescription = "Sélectionné", tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun InviteCodeRow(inviteCode: String, copied: Boolean, onCopy: () -> Unit, onShare: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            inviteCode,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )
        IconButton(onClick = onCopy) {
            Icon(if (copied) Icons.Default.Check else Icons.Default.ContentCopy, contentDescription = "Copier")
        }
        IconButton(onClick = onShare) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Partager")
        }
    }
}

@Composable
private fun MemberRow(member: Member, isMe: Boolean, canRemove: Boolean, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(member.avatarUrl, member.displayName, Modifier.size(40.dp))
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(member.displayName, fontWeight = FontWeight.SemiBold)
            if (isMe) Text("Moi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        if (canRemove) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.PersonRemove, "Retirer", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun Avatar(avatarUrl: String?, name: String, modifier: Modifier = Modifier) {
    if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = name,
            modifier = modifier.clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1).uppercase().ifBlank { "?" }, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black)
        }
    }
}
