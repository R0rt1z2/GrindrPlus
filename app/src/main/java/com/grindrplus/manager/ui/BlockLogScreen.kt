package com.grindrplus.manager.ui

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grindrplus.manager.blocks.BlockEvent
import com.grindrplus.manager.blocks.BlockLogViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.grindrplus.manager.ui.components.BlockLogFilters
import com.grindrplus.manager.ui.components.FilterPanel
import com.grindrplus.manager.utils.AppCloneUtils

@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BlockLogScreen(innerPadding: PaddingValues) {
    val activity = LocalContext.current as ComponentActivity
    val viewModel: BlockLogViewModel = viewModel()
    val events by viewModel.events.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val filteredEvents by viewModel.filteredEvents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val availablePackages by viewModel.availablePackages.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadEvents()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Clear all events") },
            text = { Text("Are you sure you want to clear all block/unblock events? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearEvents()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Block Log") },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.exportEventsToFile(activity)
                            scope.launch {
                                snackbarHostState.showSnackbar("Saving block events JSON file...")
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export JSON"
                        )
                    }

                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear all"
                        )
                    }
                },
                modifier = Modifier.padding(top = 0.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            FilterPanel(
                filters = filters,
                availablePackages = availablePackages,
                onFiltersChanged = { newFilters ->
                    viewModel.updateFilters(newFilters)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (filters.isActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtered: ${filteredEvents.size} of ${events.size} events",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Box(
                modifier = Modifier.weight(1f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (filteredEvents.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "No events",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (filters.isActive && events.isNotEmpty())
                                "No events match your filters"
                            else
                                "No block/unblock events found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (filters.isActive && events.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.updateFilters(BlockLogFilters()) }
                            ) {
                                Text("Reset Filters")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                        val grouped = filteredEvents.groupBy {
                            dateFormat.format(Date(it.timestamp))
                        }

                        grouped.forEach { (date, dateEvents) ->
                            stickyHeader {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.background)
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = date,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            items(dateEvents) { event ->
                                BlockEventItem(
                                    event = event,
                                    onCopyId = {
                                        clipboardManager.setText(AnnotatedString(event.profileId))
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Profile ID copied to clipboard")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BlockEventItem(
    event: BlockEvent,
    onCopyId: () -> Unit
) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val time = timeFormat.format(Date(event.timestamp))

    val formattedPackage = remember(event.packageName) {
        when (val pkg = event.packageName) {
            null, "com.grindrapp.android" -> null
            else -> when {
                pkg.startsWith(AppCloneUtils.GRINDR_PACKAGE_PREFIX) -> {
                    val cloneName = pkg.removePrefix(AppCloneUtils.GRINDR_PACKAGE_PREFIX)
                    "Clone ${cloneName.replaceFirstChar { it.uppercase() }}"
                }
                else -> pkg.substringAfterLast('.')
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = if (event.eventType == "block")
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.size(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = event.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = if (event.eventType == "block")
                            "Blocked you"
                        else
                            "Unblocked you",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (formattedPackage != null) {
                        Text(
                            text = formattedPackage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: ${event.profileId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onCopyId,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy ID",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}