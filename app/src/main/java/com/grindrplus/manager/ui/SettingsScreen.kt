package com.grindrplus.manager.ui

import android.content.Context
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grindrplus.core.Config
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val context: Context,
) : ViewModel() {

    private val _settingGroups = MutableStateFlow<List<SettingGroup>>(emptyList())
    val settingGroups: StateFlow<List<SettingGroup>> = _settingGroups

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _isLoading.value = true
            Config.initialize(null)

            try {
                // Get hooks from Config
                val hooks = Config.getHooksSettings()
                val hookSettings = hooks.filter {
                    it.key != "Mod settings" &&
                            it.key != "Persistent incognito" &&
                            it.key != "Unlimited albums"
                }.map { (hookName, pair) ->
                    SwitchSetting(
                        id = hookName,
                        title = hookName,
                        description = pair.first,
                        isChecked = pair.second,
                        onCheckedChange = {
                            viewModelScope.launch {
                                Config.setHookEnabled(hookName, it)
                                loadSettings()
                            }
                        }
                    )
                }

                // Create other settings
                val otherSettings = listOf(
                    TextSetting(
                        id = "command_prefix",
                        title = "Command Prefix",
                        description = "Change the command prefix (default: /)",
                        value = Config.get("command_prefix", "/") as String,
                        onValueChange = {
                            viewModelScope.launch {
                                Config.put("command_prefix", it)
                                loadSettings()
                            }
                        },
                        validator = { input ->
                            when {
                                input.isBlank() -> "Invalid command prefix"
                                input.length > 1 -> "Command prefix must be a single character"
                                !input.matches(Regex("[^a-zA-Z0-9]")) -> "Command prefix must be a special character"
                                else -> null
                            }
                        }
                    ),
                    TextSetting(
                        id = "online_indicator",
                        title = "Online indicator duration (mins)",
                        description = "Control when the green dot disappears after inactivity",
                        value = (Config.get("online_indicator", 5) as Number).toString(),
                        onValueChange = {
                            val value = it.toIntOrNull() ?: 5
                            viewModelScope.launch {
                                Config.put("online_indicator", value)
                                loadSettings()
                            }
                        },
                        keyboardType = KeyboardType.Number,
                        validator = { input ->
                            val value = input.toIntOrNull()
                            if (value == null || value <= 0) "Duration must be a positive number" else null
                        }
                    ),
                    // More text settings...

                    // Toggle settings
                    SwitchSetting(
                        id = "force_old_anti_block_behavior",
                        title = "Force old AntiBlock behavior",
                        description = "Use the old AntiBlock behavior (don't use this, required for testing)",
                        isChecked = Config.get("force_old_anti_block_behavior", false) as Boolean,
                        onCheckedChange = {
                            viewModelScope.launch {
                                Config.put("force_old_anti_block_behavior", it)
                                loadSettings()
                            }
                        }
                    ),
                    SwitchSetting(
                        id = "anti_block_use_toasts",
                        title = "Use toasts for AntiBlock hook",
                        description = "Instead of receiving Android notifications, use toasts for block/unblock notifications",
                        isChecked = Config.get("anti_block_use_toasts", false) as Boolean,
                        onCheckedChange = {
                            viewModelScope.launch {
                                Config.put("anti_block_use_toasts", it)
                                loadSettings()
                            }
                        }
                    ),
                    // More switch settings...
                )

                // Experimental settings
                val experimentalSettings = listOf(
                    TextSetting(
                        id = "block_import_threshold",
                        title = "Import Blocks Threshold",
                        description = "Set the time to wait between each block import (in milliseconds)",
                        value = (Config.get("block_import_threshold", 500) as Number).toString(),
                        onValueChange = {
                            val value = it.toIntOrNull() ?: 500
                            viewModelScope.launch {
                                Config.put("block_import_threshold", value)
                                loadSettings()
                            }
                        },
                        keyboardType = KeyboardType.Number,
                        validator = { input ->
                            val value = input.toIntOrNull()
                            if (value == null || value <= 0) "Threshold must be a positive number" else null
                        }
                    ),
                )

                // Create setting groups
                _settingGroups.value = listOf(
                    SettingGroup(
                        id = "hooks",
                        title = "Manage Hooks",
                        settings = hookSettings
                    ),
                    SettingGroup(
                        id = "other",
                        title = "Other Settings",
                        settings = otherSettings
                    ),
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// Setting types
sealed class Setting(open val id: String, open val title: String)

data class SwitchSetting(
    override val id: String,
    override val title: String,
    val description: String? = null,
    val isChecked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
) : Setting(id, title)

data class TextSetting(
    override val id: String,
    override val title: String,
    val description: String? = null,
    val value: String,
    val onValueChange: (String) -> Unit,
    val keyboardType: KeyboardType = KeyboardType.Text,
    val validator: ((String) -> String?)? = null,
) : Setting(id, title)

data class ButtonSetting(
    override val id: String,
    override val title: String,
    val onClick: () -> Unit,
) : Setting(id, title)

// Represents a group of settings
data class SettingGroup(
    val id: String,
    val title: String,
    val settings: List<Setting>,
)

// Keyboard type enum
enum class KeyboardType {
    Text, Number, Email, Password, Phone
}

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

@Composable
fun rememberViewModel(): SettingsViewModel {
    val context = LocalContext.current
    val factory = remember(context) { SettingsViewModelFactory(context) }
    return viewModel(factory = factory)
}

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
                }
            ),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = {
                    if (errorMessage == null) {
                        setting.onValueChange(text)
                    }
                }) {
                    Icons.Default.Done
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