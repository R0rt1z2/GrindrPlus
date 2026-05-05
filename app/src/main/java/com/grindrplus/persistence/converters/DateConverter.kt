package com.grindrplus.persistence.converters

import androidx.room.TypeConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DateConverter {
    companion object {
        // SimpleDateFormat is not thread-safe. Room can call converters from
        // any worker thread, so each thread gets its own instance.
        private val dateFormat = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
        }
    }

    @TypeConverter
    fun fromTimestamp(value: String?): Date? {
        return value?.let {
            try {
                dateFormat.get()!!.parse(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): String? {
        return date?.let { dateFormat.get()!!.format(it) }
    }
}