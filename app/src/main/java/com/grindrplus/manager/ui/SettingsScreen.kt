package com.grindrplus.manager.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.grindrplus.manager.settings.*
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Info
import com.grindrplus.core.Config
import com.grindrplus.manager.utils.FileOperationHandler
import com.grindrplus.manager.utils.uploadAndShare
import org.json.JSONObject
import kotlin.toString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    paddingValues: PaddingValues,
    viewModel: SettingsViewModel = rememberViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val settingGroups by viewModel.settingGroups.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAboutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false },
            onViewSourceCode = {
                val intent =
                    Intent(Intent.ACTION_VIEW, "https://github.com/R0rt1z2/GrindrPlus".toUri())
                context.startActivity(intent)
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                actions = {
                    Box {
                        var expanded by remember { mutableStateOf(false) }

                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export settings") },
                                onClick = {
                                    expanded = false
                                    scope.launch {
                                        val result = Config.readRemoteConfig()
                                        FileOperationHandler.exportFile(
                                            "grindrplus_settings.json",
                                            result.toString()
                                        )
                                        snackbarHostState.showSnackbar("Settings exported successfully")
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Import settings") },
                                onClick = {
                                    expanded = false
                                    try {
                                        FileOperationHandler.importFile(
                                            arrayOf("application/json")
                                        ) {
                                            Config.writeRemoteConfig(JSONObject(it))
                                            viewModel.loadSettings()

                                            scope.launch {
                                                snackbarHostState.showSnackbar("Settings imported successfully")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Failed to import settings: ${e.message}")
                                        }
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("About") },
                                onClick = {
                                    expanded = false
                                    showAboutDialog = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 100.dp
                )
            ) {
                settingGroups.forEach { group ->
                    item {
                        SettingGroupSection(
                            group = group,
                            onSettingChanged = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Setting updated")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AboutDialog(
    onDismiss: () -> Unit,
    onViewSourceCode: () -> Unit,
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(1f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GrindrPlus",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "App Icon",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Made with ❤️ by",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Row {
                            Text(
                                text = "R0rt1z2",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.clickable {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        "https://github.com/R0rt1z2".toUri()
                                    )
                                    context.startActivity(intent)
                                }
                            )

                            Text(
                                text = " and ",
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Text(
                                text = "Rattly",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.clickable {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        "https://github.com/Rattly".toUri()
                                    )
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            "Close",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Button(
                        onClick = onViewSourceCode,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Text(
                            "Source",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingGroupSection(
    group: SettingGroup,
    onSettingChanged: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = group.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp
        ) {
            Column {
                group.settings.forEachIndexed { index, setting ->
                    ImprovedSettingItem(setting, onSettingChanged)

                    if (index < group.settings.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImprovedSettingItem(
    setting: Setting,
    onSettingChanged: () -> Unit,
) {
    when (setting) {
        is SwitchSetting -> ImprovedSwitchSetting(setting) { onSettingChanged() }
        is TextSetting -> ImprovedTextSetting(setting) { onSettingChanged() }
        is ButtonSetting -> ImprovedButtonSetting(setting)
    }
}

@Composable
fun ImprovedSwitchSetting(
    setting: SwitchSetting,
    onChanged: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = setting.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            setting.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Switch(
            checked = setting.isChecked,
            onCheckedChange = {
                setting.onCheckedChange(it)
                onChanged()
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImprovedTextSetting(
    setting: TextSetting,
    onChanged: () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(setting.value) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = setting.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                setting.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            IconButton(onClick = { isExpanded = !isExpanded }) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Edit setting",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(if (isExpanded) 90f else 0f)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
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
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error
                            )
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
                                isExpanded = false
                                onChanged()
                            }
                        }
                    ),
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (errorMessage == null) {
                                    setting.onValueChange(text)
                                    isExpanded = false
                                    onChanged()
                                }
                            },
                            enabled = errorMessage == null
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Save",
                                tint = if (errorMessage == null)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ImprovedButtonSetting(setting: ButtonSetting) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = setting.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Button(
            onClick = setting.onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.padding(start = 16.dp)
        ) {
            Text("Open")
        }
    }
}