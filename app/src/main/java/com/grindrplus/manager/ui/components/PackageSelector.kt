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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageSelector(
    selectedPackage: String,
    onPackageSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    var packages by remember(Config.readRemoteConfig()) {
        mutableStateOf(Config.getAvailablePackages(context))
    }

    LaunchedEffect(selectedPackage) {
        packages = Config.getAvailablePackages(context)
    }

    if (packages.size <= 1) {
        return
    }

    fun formatPackageName(packageName: String): String {
        if (packageName == Constants.GRINDR_PACKAGE_NAME)
            return "Main Grindr App"

        val app = packages.firstOrNull { it.packageName == packageName }
        return app?.let {
            val name = "Clone: ${it.appName}"
            if (!it.isInstalled) {
                "$name (Not installed)"
            } else if (it.needsUpdate) {
                "$name (Needs update)"
            } else {
                name
            }
        } ?: packageName
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
            packages.forEach { app ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = formatPackageName(app.packageName),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        expanded = false
                        Config.setCurrentPackage(app.packageName)
                        onPackageSelected(app.packageName)
                    }
                )
            }
        }
    }
}