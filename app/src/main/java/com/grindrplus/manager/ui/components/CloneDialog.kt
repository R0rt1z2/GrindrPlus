package com.grindrplus.manager.ui.components

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.grindrplus.manager.installation.steps.numberToWords
import com.grindrplus.manager.utils.AppCloneUtils

@Composable
fun CloneDialog(
    context: Context,
    onDismiss: () -> Unit,
    onStartCloning: (packageName: String, appName: String, debuggable: Boolean, embedLSPatch: Boolean) -> Unit
) {
    val hasReachedMaxClones = remember { AppCloneUtils.hasReachedMaxClones(context) }
    val nextCloneNumber = remember { AppCloneUtils.getNextCloneNumber(context) }

    if (hasReachedMaxClones) {
        MaxClonesReachedDialog(
            onDismiss = onDismiss
        )
        return
    }

    var appName by remember { mutableStateOf("Grindr ${numberToWords(nextCloneNumber)}") }
    var debuggable by remember { mutableStateOf(false) }
    var embedLSPatch by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }

    val packagePrefix = AppCloneUtils.GRINDR_PACKAGE_PREFIX
    var packageSuffix by remember { mutableStateOf(numberToWords(nextCloneNumber).lowercase()) }

    val fullPackageName = "$packagePrefix$packageSuffix"

    val prefixVisualTransformation = VisualTransformation { text ->
        val prefixedText = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Gray)) {
                append(packagePrefix)
            }
            append(text)
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return offset + packagePrefix.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                return if (offset <= packagePrefix.length) 0 else offset - packagePrefix.length
            }
        }

        TransformedText(prefixedText, offsetMapping)
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

                Text(
                    text = "${AppCloneUtils.getExistingClones(context).size}/${AppCloneUtils.MAX_CLONES} clones used",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = packageSuffix,
                    onValueChange = {
                        packageSuffix = it
                        isError = false
                    },
                    label = { Text("Package Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text(errorText, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Suffix must be unique and have no numbers in it")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    singleLine = true,
                    visualTransformation = prefixVisualTransformation
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Embed LSPatch")
                    Switch(
                        checked = embedLSPatch,
                        onCheckedChange = { embedLSPatch = it }
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
                            if (packageSuffix.isBlank()) {
                                isError = true
                                errorText = "Please enter a valid suffix"
                                return@Button
                            }

                            if (AppCloneUtils.getExistingClones(context)
                                    .contains(fullPackageName)
                            ) {
                                isError = true
                                errorText = "This package name already exists"
                                return@Button
                            }

                            if (packageSuffix.any { it.isDigit() }) {
                                isError = true
                                errorText = "Package name must not contain numbers"
                                return@Button
                            }

                            onStartCloning(fullPackageName, appName, debuggable, embedLSPatch)
                        }
                    ) {
                        Text("Clone")
                    }
                }
            }
        }
    }
}

@Composable
fun MaxClonesReachedDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Maximum Clones Reached") },
        text = {
            Text("GrindrPlus only supports up to 5 clones. Please uninstall an existing clone before creating a new one.")
        },
        confirmButton = {
            Button(
                onClick = onDismiss
            ) {
                Text("OK")
            }
        }
    )
}