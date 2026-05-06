package com.freshtrack.ui.screens.household

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.webkit.MimeTypeMap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import android.content.Intent
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.freshtrack.domain.model.Member
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdSettingsScreen(
    navController: NavController,
    vm: HouseholdSettingsViewModel = hiltViewModel()
) {
    val household by vm.household.collectAsState()
    val members by vm.members.collectAsState()
    val ui by vm.ui.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val myMember = members.firstOrNull { it.userId == vm.currentUserId }
    var myName by remember(myMember?.displayName) { mutableStateOf(myMember?.displayName ?: "") }
    var householdName by remember(household?.name) { mutableStateOf(household?.name ?: "") }
    var memberToRemove by remember { mutableStateOf<Member?>(null) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    val isOwner = household?.createdBy == vm.currentUserId

    LaunchedEffect(ui.successMessage, ui.error) {
        val message = ui.successMessage ?: ui.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        vm.clearMessages()
    }

    val avatarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri)
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: run {
                vm.reportError("Impossible de lire l'image sélectionnée.")
                return@launch
            }
            vm.uploadCurrentUserAvatar(bytes, ext)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mon foyer") },
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
            // ── Mon profil ────────────────────────────────────────────────────
            Text("Mon profil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            // Avatar cliquable
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .size(80.dp)
                    .clickable {
                        avatarLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
            ) {
                if (myMember?.avatarUrl != null) {
                    AsyncImage(
                        model = myMember.avatarUrl,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
                if (ui.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 3.dp
                    )
                } else {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Changer la photo",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = myName,
                onValueChange = { myName = it },
                label = { Text("Prénom / pseudo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { myMember?.let { vm.updateDisplayName(it.id, myName) } },
                enabled = myMember != null && myName.isNotBlank() && !ui.isLoading
            ) { Text("Mettre à jour") }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            // ── Nom du foyer ──────────────────────────────────────────────────
            Text("Foyer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = householdName,
                onValueChange = { householdName = it },
                label = { Text("Nom du foyer") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = isOwner
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { household?.let { vm.updateHouseholdName(it.id, householdName) } },
                enabled = isOwner && !ui.isLoading
            ) { Text("Renommer") }

            household?.let { hh ->
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Code d'invitation : ", fontWeight = FontWeight.Medium)
                    Text(
                        hh.inviteCode,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { clipboard.setText(AnnotatedString(hh.inviteCode)) }) {
                        Icon(Icons.Default.ContentCopy, "Copier le code")
                    }
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Rejoins mon foyer FreshTrack avec le code : ${hh.inviteCode}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Partager le code d'invitation"))
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Partager le code")
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            // ── Membres ───────────────────────────────────────────────────────
            Text(
                "Membres (${members.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            members.forEach { member ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MemberAvatar(member, Modifier.size(36.dp))
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(member.displayName, fontWeight = FontWeight.SemiBold)
                        if (member.userId == vm.currentUserId) {
                            Text("Moi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (isOwner && member.userId != vm.currentUserId) {
                        IconButton(onClick = { memberToRemove = member }) {
                            Icon(Icons.Default.PersonRemove, "Retirer",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Button(
                onClick = { showSignOutConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
                enabled = !ui.isLoading
            ) { Text("Se déconnecter") }
        }
    }

    memberToRemove?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text("Retirer ce membre ?") },
            text = { Text("${member.displayName} pourra rejoindre le foyer à nouveau avec le code d'invitation.") },
            confirmButton = {
                Button(
                    onClick = {
                        memberToRemove = null
                        vm.removeMember(member.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Retirer") }
            },
            dismissButton = { TextButton(onClick = { memberToRemove = null }) { Text("Annuler") } }
        )
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Se déconnecter ?") },
            text = { Text("Vous serez redirigé vers l'écran de connexion.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        vm.signOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Déconnexion") }
            },
            dismissButton = { TextButton(onClick = { showSignOutConfirm = false }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun MemberAvatar(member: Member, modifier: Modifier = Modifier) {
    if (!member.avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = member.avatarUrl,
            contentDescription = member.displayName,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(CircleShape)
        )
    } else {
        Box(
            modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                member.displayName.take(1).uppercase().ifBlank { "?" },
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Black
            )
        }
    }
}
