package com.freshtrack.ui.screens.household

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Composable
fun HouseholdSetupScreen(
    navController: NavController,
    vm: HouseholdSetupViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var householdName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Votre foyer", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Créez ou rejoignez un foyer pour partager votre frigo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(24.dp))

            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Créer") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Rejoindre") })
            }

            Spacer(Modifier.height(16.dp))

            if (tab == 0) {
                OutlinedTextField(
                    value = householdName, onValueChange = { householdName = it },
                    label = { Text("Nom du foyer") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = displayName, onValueChange = { displayName = it },
                    label = { Text("Votre prénom") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { vm.createHousehold(householdName, displayName) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !ui.isLoading && householdName.isNotBlank()
                ) {
                    if (ui.isLoading) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    else Text("Créer le foyer")
                }
            } else {
                OutlinedTextField(
                    value = inviteCode, onValueChange = { inviteCode = it.uppercase() },
                    label = { Text("Code d'invitation") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = displayName, onValueChange = { displayName = it },
                    label = { Text("Votre prénom") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { vm.joinByCode(inviteCode, displayName) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !ui.isLoading && inviteCode.length == 8 && displayName.isNotBlank()
                ) {
                    if (ui.isLoading) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    else Text("Rejoindre")
                }
            }

            if (ui.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(ui.error!!, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
