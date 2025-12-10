package com.grindrplus.manager.blocks

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grindrplus.GrindrPlus
import com.grindrplus.core.LogSource
import com.grindrplus.core.Logger
import com.grindrplus.manager.ui.components.BlockLogFilters
import com.grindrplus.manager.ui.components.FilterTimeRange
import com.grindrplus.manager.ui.components.applyTimeFilter
import com.grindrplus.manager.utils.AppCloneUtils
import com.grindrplus.manager.utils.FileOperationHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BlockLogViewModel : ViewModel() {
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = MutableStateFlow<List<BlockEvent>>(emptyList())
    val events: StateFlow<List<BlockEvent>> = _events.asStateFlow()

    private val _filters = MutableStateFlow(BlockLogFilters())
    val filters: StateFlow<BlockLogFilters> = _filters.asStateFlow()

    private val _filteredEvents = MutableStateFlow<List<BlockEvent>>(emptyList())
    val filteredEvents: StateFlow<List<BlockEvent>> = _filteredEvents.asStateFlow()

    private val _availablePackages = MutableStateFlow<List<String>>(emptyList())
    val availablePackages: StateFlow<List<String>> = _availablePackages.asStateFlow()

    init {
        viewModelScope.launch {
            events.collect { applyFilters() }
        }

        viewModelScope.launch {
            filters.collect { applyFilters() }
        }
    }

    private fun applyFilters() {
        val currentFilters = _filters.value
        val allEvents = _events.value

        _filteredEvents.value = allEvents
            .filter { event ->
                when (event.eventType) {
                    "block" -> currentFilters.showBlocks
                    "unblock" -> currentFilters.showUnblocks
                    else -> true
                }
            }
            .applyTimeFilter(currentFilters.timeRange)
            .let { events ->
                if (currentFilters.nameFilter.isNotEmpty()) {
                    events.filter {
                        it.displayName.contains(currentFilters.nameFilter, ignoreCase = true)
                    }
                } else {
                    events
                }
            }
            .let { events ->
                if (currentFilters.packageNameFilter.isNotEmpty()) {
                    events.filter { event ->
                        val eventPackage = event.packageName ?: AppCloneUtils.GRINDR_PACKAGE_NAME
                        currentFilters.packageNameFilter.contains(eventPackage)
                    }
                } else {
                    events
                }
            }
    }

    fun updateFilters(newFilters: BlockLogFilters) {
        _filters.value = newFilters
    }

    fun loadEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val eventsArray = GrindrPlus.bridgeClient.getBlockEvents()
                val eventsList = mutableListOf<BlockEvent>()
                val packageSet = mutableSetOf<String>()

                packageSet.add("com.grindrapp.android")

                for (i in 0 until eventsArray.length()) {
                    val event = eventsArray.getJSONObject(i)
                    val packageName = event.optString("packageName", "com.grindrapp.android")

                    if (packageName.isNotEmpty()) {
                        packageSet.add(packageName)
                    }

                    eventsList.add(
                        BlockEvent(
                            profileId = event.getString("profileId"),
                            displayName = event.getString("displayName"),
                            eventType = event.getString("eventType"),
                            timestamp = event.getLong("timestamp"),
                            packageName = packageName
                        )
                    )
                }

                eventsList.sortByDescending { it.timestamp }
                _events.value = eventsList

                _availablePackages.value = packageSet.toList()
            } catch (e: Exception) {
                Logger.e("Failed to load block events: ${e.message}", LogSource.MANAGER)
                Logger.writeRaw(e.stackTraceToString())
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                GrindrPlus.bridgeClient.clearBlockEvents()
                loadEvents()
            } catch (e: Exception) {
                Logger.e("Failed to clear block events: ${e.message}", LogSource.MANAGER)
                Logger.writeRaw(e.stackTraceToString())
            }
        }
    }

    fun exportEventsJson(): String {
        val eventsToExport = _filteredEvents.value.ifEmpty { _events.value }
        val jsonArray = JSONArray()

        eventsToExport.forEach { event ->
            val jsonObject = JSONObject()
            jsonObject.put("profileId", event.profileId)
            jsonObject.put("displayName", event.displayName)
            jsonObject.put("eventType", event.eventType)
            jsonObject.put("timestamp", event.timestamp)
            event.packageName?.let {
                jsonObject.put("packageName", it)
            }
            jsonArray.put(jsonObject)
        }

        return jsonArray.toString(4)
    }

    fun exportEventsToFile(context: Context) {
        val jsonContent = exportEventsJson()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "block_events_$timestamp.json"
        FileOperationHandler.exportFile(filename, jsonContent)
    }
}