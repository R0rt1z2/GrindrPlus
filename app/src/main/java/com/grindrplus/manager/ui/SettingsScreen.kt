package com.grindrplus.manager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.grindrplus.manager.settings.ButtonSetting
import com.grindrplus.manager.settings.KeyboardType
import com.grindrplus.manager.settings.Setting
import com.grindrplus.manager.settings.SettingGroup
import com.grindrplus.manager.settings.SettingsViewModel
import com.grindrplus.manager.settings.SwitchSetting
import com.grindrplus.manager.settings.TextSetting
import com.grindrplus.manager.settings.rememberViewModel

// Compose UI components
@Composable
fun SettingsScreen(
    paddingValues: PaddingValues,
    viewModel: SettingsViewModel = rememberViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val settingGroups by viewModel.settingGroups.collectAsState()

    if (isLoading) {
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        SettingsContent(
            settingGroups = settingGroups,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun SettingsContent(
    settingGroups: List<SettingGroup>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        settingGroups.forEach { group ->
            item {
                SettingGroupCard(group)
            }
        }
    }
}

@Composable
fun SettingGroupCard(group: SettingGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = group.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            group.settings.forEach { setting ->
                SettingItem(setting)
                if (setting != group.settings.last()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun SettingItem(setting: Setting) {
    when (setting) {
        is SwitchSetting -> SwitchSettingItem(setting)
        is TextSetting -> TextSettingItem(setting)
        is ButtonSetting -> ButtonSettingItem(setting)
    }
}

@Composable
fun SwitchSettingItem(setting: SwitchSetting) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = setting.title,
                style = MaterialTheme.typography.titleMedium
            )
            if (setting.description != null) {
                Text(
                    text = setting.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = setting.isChecked,
            onCheckedChange = setting.onCheckedChange
        )
    }
}

@Composable
fun TextSettingItem(setting: TextSetting) {
    var text by remember { mutableStateOf(setting.value) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = setting.title,
            style = MaterialTheme.typography.titleMedium
        )

        if (setting.description != null) {
            Text(
                text = setting.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = { value ->
                text = value
                errorMessage = setting.validator?.invoke(value)
            },
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage != null,
            supportingText = {
                if (errorMessage != null) {
                    Text(text = errorMessage!!)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = when (setting.keyboardType) {
                    KeyboardType.Number -> androidx.compose.ui.text.input.KeyboardType.Number
                    KeyboardType.Email -> androidx.compose.ui.text.input.KeyboardType.Email
                    KeyboardType.Password -> androidx.compose.ui.text.input.KeyboardType.Password
                    KeyboardType.Phone -> androidx.compose.ui.text.input.KeyboardType.Phone
                    else -> androidx.compose.ui.text.input.KeyboardType.Text
                },
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (errorMessage == null) {
                        setting.onValueChange(text)
                    }
                }
            ),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = {
                    if (errorMessage == null) {
                        setting.onValueChange(text)
                    }
                }) {
                    Icon(Icons.Default.Done, contentDescription = "Save")
                }
            }
        )
    }
}

@Composable
fun ButtonSettingItem(setting: ButtonSetting) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = setting.title,
            style = MaterialTheme.typography.titleMedium
        )

        Button(onClick = setting.onClick) {
            Text("Open")
        }
    }
}