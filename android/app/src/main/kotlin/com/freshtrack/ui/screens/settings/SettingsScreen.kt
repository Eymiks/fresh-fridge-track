package com.freshtrack.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
import com.freshtrack.ui.theme.HamburgerSide
import com.freshtrack.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ALLOWED_IMAGE_MIME = setOf("image/jpeg", "image/png", "image/webp")
private const val MAX_IMAGE_BYTES = 5 * 1024 * 1024

private val accentColorMap: Map<AccentColor, Color> = mapOf(
    AccentColor.GREEN to Color(0xFF27AA83),
    AccentColor.BLUE to Color(0xFF2F80ED),
    AccentColor.VIOLET to Color(0xFF8A4BE3),
    AccentColor.ORANGE to Color(0xFFF97316),
    AccentColor.ROSE to Color(0xFFE84B78),
    AccentColor.CYAN to Color(0xFF20A7B8),
)

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
    val notifPermissionRequested by appearanceVm.notifPermissionRequested.collectAsState()
    val authState by authVm.authState.collectAsState()
    val household by householdVm.household.collectAsState()
    val members by householdVm.members.collectAsState()
    val householdUi by householdVm.ui.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = authState as? AuthState.Authenticated
    val effectiveHousehold = household ?: auth?.household
    val effectiveMembers = if (members.isNotEmpty()) members else auth?.members.orEmpty()

    var showSignOutConfirm by remember { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<Member?>(null) }
    var editingDisplayName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var editingHouseholdName by remember { mutableStateOf(false) }
    var householdName by remember(effectiveHousehold?.name) { mutableStateOf(effectiveHousehold?.name.orEmpty()) }
    var copiedInvite by remember { mutableStateOf(false) }
    LaunchedEffect(copiedInvite) {
        if (copiedInvite) {
            delay(2_000L)
            copiedInvite = false
        }
    }

    val myMember = effectiveMembers.firstOrNull { it.userId == auth?.userId }
    val avatarUrl = householdUi.avatarUrlOverride ?: myMember?.avatarUrl
    val displayName = myMember?.displayName ?: auth?.displayName ?: "Invité"
    val isGuest = authState is AuthState.Guest
    val isOwner = effectiveHousehold?.createdBy == auth?.userId
    val headerSubtitle = when {
        isGuest -> "Mode invité · Frigo local"
        effectiveHousehold != null -> "$displayName · ${effectiveHousehold.name}"
        else -> displayName
    }
    val notificationPermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

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
    ) { granted ->
        appearanceVm.markNotifPermissionRequested()
        appearanceVm.setNotifEnabled(granted)
    }

    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType !in ALLOWED_IMAGE_MIME) {
            scope.launch { householdVm.reportError("Format non supporté. Utilisez JPEG, PNG ou WebP.") }
            return@rememberLauncherForActivityResult
        }
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val b = stream.readBytes()
                    if (b.size > MAX_IMAGE_BYTES) null else b
                }
            } ?: run {
                householdVm.reportError("Image trop volumineuse (max 5 Mo) ou illisible.")
                return@launch
            }
            householdVm.uploadCurrentUserAvatar(bytes, ext)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            SettingsHeader(
                displayName = displayName,
                subtitle = headerSubtitle,
                avatarUrl = avatarUrl,
                onBack = { navController.popBackStack() },
                onMenu = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Menu disponible depuis l'accueil")
                    }
                }
            )
            OfflineBanner()
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SectionCard(title = "Profil", icon = Icons.Default.Person) {
                    if (isGuest) {
                        GuestProfileContent(onLogin = { authVm.leaveGuestMode() })
                    } else {
                        ProfileContent(
                            displayName = displayName,
                            email = auth?.email ?: "Email non disponible",
                            avatarUrl = avatarUrl,
                            isLoading = householdUi.isLoading,
                            editing = editingDisplayName,
                            editedName = editedName,
                            onEditedNameChange = { editedName = it.take(30) },
                            onStartEdit = {
                                editedName = displayName
                                editingDisplayName = true
                            },
                            onCancelEdit = {
                                editedName = displayName
                                editingDisplayName = false
                            },
                            onSave = {
                                val trimmed = editedName.trim()
                                if (trimmed.isNotBlank() && trimmed != displayName) {
                                    myMember?.let { householdVm.updateDisplayName(it.id, trimmed) }
                                }
                                editingDisplayName = false
                            },
                            onAvatarClick = { if (auth != null) avatarLauncher.launch(arrayOf("image/*")) }
                        )
                    }
                }

                SectionCard(title = "Apparence", icon = Icons.Default.Palette) {
                    Label("THÈME")
                    SegmentedRow(
                        entries = listOf(
                            SegmentItem(ThemeMode.LIGHT, "Clair", Icons.Default.LightMode),
                            SegmentItem(ThemeMode.DARK, "Sombre", Icons.Default.DarkMode),
                            SegmentItem(ThemeMode.SYSTEM, "Système", Icons.Default.Computer),
                        ),
                        selected = appearance.themeMode,
                        onSelected = appearanceVm::setThemeMode
                    )
                    Text(
                        "Préférence conservée sur cet appareil.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Label("COULEUR D'ACCENT")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AccentColor.entries.forEach { color ->
                            AccentSwatch(color, appearance.accentColor == color) { appearanceVm.setAccentColor(color) }
                        }
                    }
                    SettingsSwitchRow(
                        title = "Réduire les animations",
                        subtitle = "Désactive les transitions de page.",
                        checked = appearance.reduceMotion,
                        onCheckedChange = appearanceVm::setReduceMotion
                    )
                    Label("DENSITÉ D'AFFICHAGE")
                    SegmentedRow(
                        entries = listOf(
                            SegmentItem(Density.COMPACT, "Compact", null),
                            SegmentItem(Density.NORMAL, "Normal", null),
                            SegmentItem(Density.SPACIOUS, "Aéré", null),
                        ),
                        selected = appearance.density,
                        onSelected = appearanceVm::setDensity
                    )
                    Label("MENU HAMBURGER")
                    SegmentedRow(
                        entries = listOf(
                            SegmentItem(HamburgerSide.LEFT, "Gauche", null),
                            SegmentItem(HamburgerSide.RIGHT, "Droite", null),
                        ),
                        selected = appearance.hamburgerSide,
                        onSelected = appearanceVm::setHamburgerSide
                    )
                }

                SectionCard(title = "Notifications", icon = Icons.Default.Notifications) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Notifications push", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                            Text(
                                "Alertes pour les produits bientôt expirés.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (notificationPermissionGranted) {
                            Switch(checked = notifEnabled, onCheckedChange = appearanceVm::setNotifEnabled)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    when {
                        notifPermissionRequested && !notificationPermissionGranted -> PermissionBlockedCard()
                        !notificationPermissionGranted -> Button(
                            onClick = {
                                appearanceVm.markNotifPermissionRequested()
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Activer les notifications", fontWeight = FontWeight.ExtraBold)
                        }
                        notifEnabled -> SegmentedRow(
                            entries = listOf(
                                SegmentItem(1, "1 jour", null),
                                SegmentItem(3, "3 jours", null),
                                SegmentItem(7, "7 jours", null),
                            ),
                            selected = notifDays,
                            onSelected = appearanceVm::setNotifDays
                        )
                    }
                }

                if (auth != null && effectiveHousehold != null) {
                    SectionCard(title = "Foyer", icon = Icons.Default.Home) {
                        HouseholdSummaryRow(
                            name = effectiveHousehold.name,
                            memberCount = effectiveMembers.size,
                            editing = editingHouseholdName,
                            value = householdName,
                            isOwner = isOwner,
                            onValueChange = { householdName = it.take(40) },
                            onStartEdit = {
                                householdName = effectiveHousehold.name
                                editingHouseholdName = true
                            },
                            onCancelEdit = {
                                householdName = effectiveHousehold.name
                                editingHouseholdName = false
                            },
                            onSave = {
                                val trimmed = householdName.trim()
                                if (trimmed.isNotBlank() && trimmed != effectiveHousehold.name) {
                                    householdVm.updateHouseholdName(effectiveHousehold.id, trimmed)
                                }
                                editingHouseholdName = false
                            }
                        )
                        Spacer(Modifier.height(16.dp))
                        Label("CODE D'INVITATION")
                        InviteCodeRow(
                            inviteCode = effectiveHousehold.inviteCode,
                            copied = copiedInvite,
                            onCopy = {
                                clipboard.setText(AnnotatedString(effectiveHousehold.inviteCode))
                                copiedInvite = true
                            },
                            onShare = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Rejoins mon frigo sur FreshTrack avec le code : ${effectiveHousehold.inviteCode}")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Partager le code d'invitation"))
                            }
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SupervisorAccount, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Label("MEMBRES")
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            effectiveMembers.forEach { member ->
                                MemberRow(
                                    member = member,
                                    isMe = member.userId == auth.userId,
                                    canRemove = isOwner && member.userId != auth.userId,
                                    onRemove = { memberToRemove = member }
                                )
                            }
                        }
                    }
                }

                SectionCard(title = "Application", icon = Icons.Default.Info) {
                    ApplicationRow(onClick = { navController.navigate(Routes.CREDITS) })
                    if (isGuest) {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { authVm.leaveGuestMode() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Créer un compte / Se connecter", fontWeight = FontWeight.ExtraBold)
                        }
                    } else {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { showSignOutConfirm = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.22f)),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Se déconnecter", fontWeight = FontWeight.ExtraBold)
                        }
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
private fun SettingsHeader(
    displayName: String,
    subtitle: String,
    avatarUrl: String?,
    onBack: () -> Unit,
    onMenu: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clearAndSetSemantics {
                        role = Role.Button
                        contentDescription = "Retour"
                    }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
            }
            Avatar(avatarUrl, displayName, Modifier.size(44.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Paramètres",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                Surface(
                    onClick = onMenu,
                    modifier = Modifier
                        .size(44.dp)
                        .clearAndSetSemantics {
                            role = Role.Button
                            contentDescription = "Ouvrir le menu"
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Menu, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
            }
            content()
        }
    }
}

@Composable
private fun ProfileContent(
    displayName: String,
    email: String,
    avatarUrl: String?,
    isLoading: Boolean,
    editing: Boolean,
    editedName: String,
    onEditedNameChange: (String) -> Unit,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSave: () -> Unit,
    onAvatarClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(70.dp), contentAlignment = Alignment.BottomEnd) {
            Avatar(
                avatarUrl = avatarUrl,
                name = displayName,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .clearAndSetSemantics {
                        role = Role.Button
                        contentDescription = "Changer l'avatar"
                    }
                    .clickable(onClick = onAvatarClick)
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Changer l'avatar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (editing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = onEditedNameChange,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    IconButton(
                        onClick = onSave,
                        enabled = editedName.trim().isNotBlank(),
                        modifier = Modifier.clearAndSetSemantics {
                            role = Role.Button
                            contentDescription = "Sauvegarder le profil"
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = onCancelEdit,
                        modifier = Modifier.clearAndSetSemantics {
                            role = Role.Button
                            contentDescription = "Annuler la modification du profil"
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    IconButton(
                        onClick = onStartEdit,
                        modifier = Modifier
                            .size(40.dp)
                            .clearAndSetSemantics {
                                role = Role.Button
                                contentDescription = "Modifier le profil"
                            }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Text(
                if (isLoading) "Mise à jour..." else email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GuestProfileContent(onLogin: () -> Unit) {
    Text("Vos produits restent sur cet appareil.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Button(onClick = onLogin, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Créer un compte / Se connecter", fontWeight = FontWeight.ExtraBold)
    }
}

private data class SegmentItem<T>(val value: T, val label: String, val icon: ImageVector?)

@Composable
private fun <T> SegmentedRow(
    entries: List<SegmentItem<T>>,
    selected: T,
    onSelected: (T) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        entries.forEach { entry ->
            val isSelected = entry.value == selected
            Surface(
                onClick = { onSelected(entry.value) },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clearAndSetSemantics {
                        role = Role.Button
                        contentDescription = if (isSelected) "${entry.label}, sélectionné" else "Choisir ${entry.label}"
                    },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    entry.icon?.let {
                        Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                    }
                    Text(
                        entry.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AccentSwatch(color: AccentColor, selected: Boolean, onClick: () -> Unit) {
    val accentC = accentColorMap[color] ?: Color.Gray
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(if (selected) Modifier.border(2.dp, accentC, CircleShape) else Modifier)
            .padding(4.dp)
            .clip(CircleShape)
            .background(accentC)
            .clearAndSetSemantics {
                role = Role.Button
                contentDescription = if (selected) "Couleur d'accent ${color.label}, sélectionnée" else "Choisir la couleur d'accent ${color.label}"
            }
            .clickable(onClick = onClick)
    ) {
        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SettingsSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.clearAndSetSemantics {
                    role = Role.Button
                    contentDescription = if (checked) "$title activé" else "$title désactivé"
                }
            )
        }
    }
}

@Composable
private fun PermissionBlockedCard() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.22f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "Notifications bloquées. Autorisez-les dans les paramètres Android.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HouseholdSummaryRow(
    name: String,
    memberCount: Int,
    editing: Boolean,
    value: String,
    isOwner: Boolean,
    onValueChange: (String) -> Unit,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            if (editing) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
                IconButton(
                    onClick = onSave,
                    enabled = value.trim().isNotBlank(),
                    modifier = Modifier.clearAndSetSemantics {
                        role = Role.Button
                        contentDescription = "Enregistrer le nom du foyer"
                    }
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = onCancelEdit,
                    modifier = Modifier.clearAndSetSemantics {
                        role = Role.Button
                        contentDescription = "Annuler le renommage du foyer"
                    }
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$memberCount membre${if (memberCount > 1) "s" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isOwner) {
                    IconButton(
                        onClick = onStartEdit,
                        modifier = Modifier.clearAndSetSemantics {
                            role = Role.Button
                            contentDescription = "Renommer le foyer"
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteCodeRow(inviteCode: String, copied: Boolean, onCopy: () -> Unit, onShare: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            inviteCode,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.80f))
                .padding(horizontal = 12.dp, vertical = 13.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            letterSpacing = MaterialTheme.typography.titleLarge.letterSpacing
        )
        IconActionButton(onClick = onCopy, contentDescription = if (copied) "Code d'invitation copié" else "Copier le code d'invitation") {
            Icon(if (copied) Icons.Default.Check else Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconActionButton(onClick = onShare, contentDescription = "Partager le code d'invitation") {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MemberRow(member: Member, isMe: Boolean, canRemove: Boolean, onRemove: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(member.avatarUrl, member.displayName, Modifier.size(40.dp).clip(CircleShape))
            Spacer(Modifier.width(12.dp))
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(member.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (isMe) {
                    Spacer(Modifier.width(8.dp))
                    Text("Moi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (canRemove) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(40.dp)
                        .clearAndSetSemantics {
                            role = Role.Button
                            contentDescription = "Retirer ${member.displayName} du foyer"
                        }
                ) {
                    Icon(Icons.Default.PersonRemove, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ApplicationRow(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                role = Role.Button
                contentDescription = "Ouvrir les crédits"
            }
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Crédits", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Attributions et bibliothèques utilisées.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun IconActionButton(onClick: () -> Unit, contentDescription: String, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.80f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .size(48.dp)
            .clearAndSetSemantics {
                role = Role.Button
                this.contentDescription = contentDescription
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun Avatar(avatarUrl: String?, name: String, modifier: Modifier = Modifier) {
    if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = name,
            modifier = modifier.clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1).uppercase().ifBlank { "?" }, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.ExtraBold)
        }
    }
}
