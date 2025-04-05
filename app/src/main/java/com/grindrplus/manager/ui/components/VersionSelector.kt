package com.grindrplus.manager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.grindrplus.manager.ui.Data

@Composable
fun VersionSelector(
    versions: List<Data>,
    selectedVersion: Data?,
    onVersionSelected: (Data) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    label: String = "Select a GrindrPlus version",
    customOption: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(Size.Zero) }
    val localDensity = LocalDensity.current

    Column(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = isEnabled,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    expanded = !expanded
                }
                .onGloballyPositioned { coordinates ->
                    textFieldSize = coordinates.size.toSize()
                },
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = selectedVersion?.modVer ?: label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selectedVersion != null)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = if (selectedVersion != null) FontWeight.Normal else FontWeight.Light,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .rotate(if (expanded) 180f else 0f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(localDensity) { textFieldSize.width.toDp() })
        ) {
            if (customOption != null) {
                DropdownMenuItem(
                    text = {
                        Text(
                            customOption,
                            fontStyle = FontStyle.Italic
                        )
                    },
                    onClick = {
                        onVersionSelected(Data("custom", "", ""))
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Custom"
                        )
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (versions.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            "No versions available",
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = { expanded = false }
                )
            } else {
                versions.forEach { version ->
                    if (version.modVer != "custom") {
                        DropdownMenuItem(
                            text = { Text("Version ${version.modVer}") },
                            onClick = {
                                onVersionSelected(version)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
