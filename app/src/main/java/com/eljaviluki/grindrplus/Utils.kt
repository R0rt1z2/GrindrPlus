package com.eljaviluki.grindrplus

import java.text.SimpleDateFormat
import java.util.*

object Utils {
    fun toReadableDate(timestamp: Long): String = SimpleDateFormat.getDateTimeInstance().format(Date(timestamp))
}