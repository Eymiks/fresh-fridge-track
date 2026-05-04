package com.freshtrack.ui.screens.household

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage

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

    val myMember = members.firstOrNull { it.userId == vm.currentUserId }
    var myName by remember(myMember?.displayName) { mutableStateOf(myMember?.displayName ?: "") }
    var householdName by remember(household?.name) { mutableStateOf(household?.name ?: "") }

    val avatarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val hh = household ?: return@rememberLauncherForActivityResult
        val userId = vm.currentUserId ?: return@rememberLauncherForActivityResult
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
        val ext = context.contentResolver.getType(uri)?.substringAfterLast('/') ?: "jpeg"
        vm.uploadAvatar(hh.id, userId, bytes, ext)
    }

    Scaffold(
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
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { household?.let { vm.updateHouseholdName(it.id, householdName) } },
                enabled = !ui.isLoading
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
                    Text(member.displayName, modifier = Modifier.weight(1f))
                    if (member.userId != vm.currentUserId) {
                        IconButton(onClick = { vm.removeMember(member.id) }) {
                            Icon(Icons.Default.PersonRemove, "Retirer",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Button(
                onClick = { vm.signOut() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
                enabled = !ui.isLoading
            ) { Text("Se déconnecter") }

            if (ui.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(ui.error!!, color = MaterialTheme.colorScheme.error)
            }
            if (ui.successMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(ui.successMessage!!, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
