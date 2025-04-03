package com.grindrplus.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.grindrplus.core.Config
import com.grindrplus.core.Constants
import com.grindrplus.manager.utils.AppCloneUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageSelector(
    onPackageSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    var packages by remember(Config.readRemoteConfig()) {
        mutableStateOf(Config.getAvailablePackages(context))
    }

    LaunchedEffect(Unit) {
        packages = Config.getAvailablePackages(context)
    }

    var selectedPackage by remember {
        mutableStateOf(Config.getCurrentPackage())
    }

    if (packages.size <= 1) {
        return
    }

    fun formatPackageName(packageName: String): String {
        val packageManager = context.packageManager

        return when {
            packageName == Constants.GRINDR_PACKAGE_NAME -> "Main Grindr App"
            packageName.startsWith(AppCloneUtils.GRINDR_PACKAGE_PREFIX) -> {
                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()

                    if (appName != packageName && appName.isNotEmpty()) {
                        "Clone: $appName"
                    } else {
                        val suffix = packageName.removePrefix(AppCloneUtils.GRINDR_PACKAGE_PREFIX)
                        "Clone: $suffix"
                    }
                } catch (_: Exception) {
                    val suffix = packageName.removePrefix(AppCloneUtils.GRINDR_PACKAGE_PREFIX)
                    "Clone: $suffix"
                }
            }
            else -> packageName
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = "Select App",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            onClick = { expanded = true },
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatPackageName(selectedPackage),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select app",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            packages.forEach { packageName ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = formatPackageName(packageName),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        selectedPackage = packageName
                        expanded = false
                        Config.setCurrentPackage(packageName)
                        onPackageSelected(packageName)
                    }
                )
            }
        }
    }
}