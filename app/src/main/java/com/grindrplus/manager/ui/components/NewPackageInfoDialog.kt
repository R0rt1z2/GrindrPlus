package com.grindrplus.manager.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.grindrplus.manager.utils.AppCloneUtils
import com.grindrplus.manager.utils.numberToWords

@Composable
fun NewPackageInfoDialog(
    context: Context,
    onConfirm: (AppCloneUtils.AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            val nextCloneNumber = remember { AppCloneUtils.getNextCloneNumber(context) }

            var appName by remember { mutableStateOf("Grindr ${numberToWords(nextCloneNumber)}") }
            var isError by remember { mutableStateOf(false) }
            var errorText by remember { mutableStateOf("") }

            val packagePrefix = AppCloneUtils.GRINDR_PACKAGE_PREFIX
            val packageSuffix = numberToWords(nextCloneNumber).lowercase()

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

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "New clone app",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = packageSuffix,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Package Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text(errorText, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Automatic suffix assignment")
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

                            if (packageSuffix.any { it.isDigit() }) {
                                isError = true
                                errorText = "Package name must not contain numbers"
                                return@Button
                            }

                            if (AppCloneUtils.findApp(fullPackageName)
                                    ?.isInstalled ?: false
                            ) {
                                isError = true
                                errorText = "A clone with this package name already exists"
                                return@Button
                            }

                            onConfirm(
                                AppCloneUtils.AppInfo(
                                    fullPackageName,
                                    appName,
                                    isClone = true,
                                    isInstalled = false
                                )
                            )
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}
