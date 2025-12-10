package com.grindrplus.manager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grindrplus.manager.blocks.BlockEvent

enum class FilterTimeRange {
    ALL_TIME,
    LAST_24H,
    LAST_WEEK,
    LAST_MONTH
}

data class BlockLogFilters(
    val showBlocks: Boolean = true,
    val showUnblocks: Boolean = true,
    val timeRange: FilterTimeRange = FilterTimeRange.ALL_TIME,
    val nameFilter: String = "",
    val packageNameFilter: Set<String> = emptySet(),
    val isActive: Boolean = false
)

@Composable
fun FilterPanel(
    filters: BlockLogFilters,
    availablePackages: List<String>,
    onFiltersChanged: (BlockLogFilters) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f
    )

    val activeFiltersCount = remember(filters) {
        var count = 0
        if (!filters.showBlocks || !filters.showUnblocks) count++
        if (filters.timeRange != FilterTimeRange.ALL_TIME) count++
        if (filters.nameFilter.isNotEmpty()) count++
        if (filters.packageNameFilter.isNotEmpty()) count++
        count
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.FilterAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (activeFiltersCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = activeFiltersCount.toString(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.rotate(rotationState)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Event Type",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filters.showBlocks,
                            onClick = {
                                onFiltersChanged(filters.copy(
                                    showBlocks = !filters.showBlocks,
                                    isActive = true
                                ))
                            },
                            label = { Text("Blocks") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )

                        FilterChip(
                            selected = filters.showUnblocks,
                            onClick = {
                                onFiltersChanged(filters.copy(
                                    showUnblocks = !filters.showUnblocks,
                                    isActive = true
                                ))
                            },
                            label = { Text("Unblocks") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.RestoreFromTrash,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }

                    if (availablePackages.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "App Instance",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        PackageFilterSelector(
                            availablePackages = availablePackages,
                            selectedPackages = filters.packageNameFilter,
                            onPackageFilterChanged = { selectedPackages ->
                                onFiltersChanged(filters.copy(
                                    packageNameFilter = selectedPackages,
                                    isActive = true
                                ))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Time Range",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TimeRangeFilter(
                        selectedRange = filters.timeRange,
                        onRangeSelected = { range ->
                            onFiltersChanged(filters.copy(
                                timeRange = range,
                                isActive = true
                            ))
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Filter by Name",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = filters.nameFilter,
                        onValueChange = { name ->
                            onFiltersChanged(filters.copy(
                                nameFilter = name,
                                isActive = true
                            ))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter display name") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onFiltersChanged(BlockLogFilters())
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reset Filters")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PackageFilterSelector(
    availablePackages: List<String>,
    selectedPackages: Set<String>,
    onPackageFilterChanged: (Set<String>) -> Unit
) {
    val displayNames = remember(availablePackages) {
        availablePackages.associateWith { packageName ->
            when {
                packageName == "com.grindrapp.android" -> "Original Grindr"
                packageName.startsWith("com.grindrapp.android.") -> {
                    val cloneName = packageName.removePrefix("com.grindrapp.android.")
                    "Clone ${cloneName.replaceFirstChar { it.uppercase() }}"
                }
                else -> packageName.substringAfterLast('.')
            }
        }
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        availablePackages.forEach { packageName ->
            FilterChip(
                selected = selectedPackages.contains(packageName),
                onClick = {
                    val newSelectedPackages = selectedPackages.toMutableSet()
                    if (selectedPackages.contains(packageName)) {
                        newSelectedPackages.remove(packageName)
                    } else {
                        newSelectedPackages.add(packageName)
                    }
                    onPackageFilterChanged(newSelectedPackages)
                },
                label = { Text(displayNames[packageName] ?: packageName) },
                leadingIcon = {
                    if (selectedPackages.contains(packageName)) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Android,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRangeFilter(
    selectedRange: FilterTimeRange,
    onRangeSelected: (FilterTimeRange) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedRange == FilterTimeRange.ALL_TIME,
            onClick = { onRangeSelected(FilterTimeRange.ALL_TIME) },
            label = { Text("All Time") },
            leadingIcon = if (selectedRange == FilterTimeRange.ALL_TIME) {
                { Icon(Icons.Default.Check, contentDescription = null) }
            } else null
        )

        FilterChip(
            selected = selectedRange == FilterTimeRange.LAST_24H,
            onClick = { onRangeSelected(FilterTimeRange.LAST_24H) },
            label = { Text("24h") },
            leadingIcon = if (selectedRange == FilterTimeRange.LAST_24H) {
                { Icon(Icons.Default.Check, contentDescription = null) }
            } else null
        )

        FilterChip(
            selected = selectedRange == FilterTimeRange.LAST_WEEK,
            onClick = { onRangeSelected(FilterTimeRange.LAST_WEEK) },
            label = { Text("Week") },
            leadingIcon = if (selectedRange == FilterTimeRange.LAST_WEEK) {
                { Icon(Icons.Default.Check, contentDescription = null) }
            } else null
        )

        FilterChip(
            selected = selectedRange == FilterTimeRange.LAST_MONTH,
            onClick = { onRangeSelected(FilterTimeRange.LAST_MONTH) },
            label = { Text("Month") },
            leadingIcon = if (selectedRange == FilterTimeRange.LAST_MONTH) {
                { Icon(Icons.Default.Check, contentDescription = null) }
            } else null
        )
    }
}

fun List<BlockEvent>.applyTimeFilter(timeRange: FilterTimeRange): List<BlockEvent> {
    if (timeRange == FilterTimeRange.ALL_TIME) return this

    val currentTime = System.currentTimeMillis()
    val cutoffTime = when (timeRange) {
        FilterTimeRange.LAST_24H -> currentTime - (24 * 60 * 60 * 1000)
        FilterTimeRange.LAST_WEEK -> currentTime - (7 * 24 * 60 * 60 * 1000)
        FilterTimeRange.LAST_MONTH -> currentTime - (30L * 24 * 60 * 60 * 1000)
        else -> 0
    }

    return this.filter { it.timestamp >= cutoffTime }
}