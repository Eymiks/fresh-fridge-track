package com.freshtrack.ui.screens.household

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.freshtrack.ui.theme.FreshTextStyles

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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Eco,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Votre foyer",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Créez ou rejoignez un foyer pour partager votre frigo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(28.dp))

                // Segmented control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Créer" to 0, "Rejoindre" to 1).forEach { (label, index) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    if (tab == index) MaterialTheme.colorScheme.surface
                                    else androidx.compose.ui.graphics.Color.Transparent
                                )
                                .clickable { tab = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (tab == index) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                if (tab == 0) {
                    HouseholdField(
                        value = householdName,
                        onValueChange = { householdName = it },
                        label = "Nom du foyer",
                        icon = Icons.Default.Home,
                        imeAction = ImeAction.Next,
                        capitalization = KeyboardCapitalization.Words
                    )
                    Spacer(Modifier.height(10.dp))
                    HouseholdField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = "Votre prénom",
                        icon = Icons.Default.Person,
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Words
                    )
                } else {
                    HouseholdField(
                        value = inviteCode,
                        onValueChange = { inviteCode = it.uppercase() },
                        label = "Code d'invitation",
                        icon = Icons.Default.Group,
                        imeAction = ImeAction.Next,
                        capitalization = KeyboardCapitalization.Characters
                    )
                    Spacer(Modifier.height(10.dp))
                    HouseholdField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = "Votre prénom",
                        icon = Icons.Default.Person,
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Words
                    )
                }

                AnimatedVisibility(visible = ui.error != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = ui.error.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (tab == 0) {
                    Button(
                        onClick = { vm.createHousehold(householdName, displayName) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !ui.isLoading && householdName.isNotBlank(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (ui.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Créer le foyer",
                                style = FreshTextStyles.ButtonLabel
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { vm.joinByCode(inviteCode, displayName) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !ui.isLoading && inviteCode.length == 8 && displayName.isNotBlank(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (ui.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Rejoindre le foyer",
                                style = FreshTextStyles.ButtonLabel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HouseholdField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    imeAction: ImeAction,
    capitalization: KeyboardCapitalization
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            imeAction = imeAction
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}
