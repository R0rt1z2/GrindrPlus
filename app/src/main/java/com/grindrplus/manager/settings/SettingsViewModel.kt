package com.grindrplus.manager.settings

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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
                )

                val managerSettings = mutableListOf<Setting>(
                    SwitchSetting(
                        id = "analytics",
                        title = "Opt-in analytics",
                        description = "Help improve the app by sending anonymous usage data",
                        isChecked = Config.get("analytics", true) as Boolean,
                        onCheckedChange = {
                            viewModelScope.launch {
                                Config.put("analytics", it)
                                loadSettings()
                            }
                        }
                    )
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    managerSettings += SwitchSetting(
                        id = "material_you",
                        title = "Enable dynamic colors",
                        description = "Use Material You colors for the app\nRestart the app to apply changes",
                        isChecked = Config.get("material_you", false) as Boolean,
                        onCheckedChange = {
                            viewModelScope.launch {
                                Config.put("material_you", it)
                                loadSettings()
                            }
                        }
                    )
                }


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
                    SettingGroup(
                        id = "manager",
                        title = "Manager Settings",
                        settings = managerSettings
                    ),
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
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