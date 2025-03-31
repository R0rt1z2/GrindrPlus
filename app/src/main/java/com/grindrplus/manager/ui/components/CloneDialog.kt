package com.grindrplus.manager.ui.components

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.grindrplus.core.Constants.GRINDR_PACKAGE_NAME
import com.grindrplus.manager.installation.steps.numberToWords
import com.grindrplus.manager.utils.AppCloneUtils

@Composable
fun CloneDialog(
    context: Context,
    onDismiss: () -> Unit,
    onStartCloning: (packageName: String, appName: String, debuggable: Boolean) -> Unit,
) {
    val nextCloneNumber = remember { AppCloneUtils.getNextCloneNumber(context) }
    var appName by remember { mutableStateOf("Grindr $nextCloneNumber") }
    var debuggable by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }
    var packageName by remember {
        mutableStateOf("$GRINDR_PACKAGE_NAME.${numberToWords(nextCloneNumber).lowercase()}")
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Clone Grindr",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = packageName,
                    onValueChange = {
                        packageName = it
                        isError = false
                    },
                    label = { Text("Package Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text(errorText, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Package name must be unique and have no numbers in it")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("App Name") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Debuggable")
                    Switch(
                        checked = debuggable,
                        onCheckedChange = { debuggable = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (packageName.isBlank() || !packageName.contains(".")) {
                                isError = true
                                errorText = "Please enter a valid package name"
                                return@Button
                            }

                            if (AppCloneUtils.getExistingClones(context)
                                    .contains(packageName)
                            ) {
                                isError = true
                                errorText = "This package name already exists"
                                return@Button
                            }

                            if (packageName.any { it.isDigit() }) {
                                isError = true
                                errorText = "Package name must not contain numbers"
                                return@Button
                            }

                            onStartCloning(packageName, appName, debuggable)
                        }
                    ) {
                        Text("Clone")
                    }
                }
            }
        }
    }
}