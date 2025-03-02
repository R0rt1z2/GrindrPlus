package com.grindrplus.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.grindrplus.GrindrPlus
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object Utils {
    fun getId(name: String, defType: String, context: Context): Int {
        return context.resources.getIdentifier(name, defType, context.packageName)
    }

    fun getFont(name: String, context: Context): Typeface? {
        return ResourcesCompat.getFont(context, getId(name, "font", context))
    }

    fun getDrawable(name: String, context: Context): Drawable? {
        return ResourcesCompat.getDrawable(
            context.resources,
            getId(name, "drawable", context), null
        )
    }

    fun dpToPx(dp: Float, context: Context): Float {
        val scale = context.resources.displayMetrics.density
        return dp * scale
    }

    fun spToPx(sp: Float, context: Context): Float {
        val scale = context.resources.displayMetrics.scaledDensity
        return sp * scale
    }

    fun ptToPx(pt: Float, context: Context): Float {
        return pt * (1.0f / 0.75f)
    }

    fun inToPx(inch: Float, context: Context): Float {
        val dpi = context.resources.displayMetrics.densityDpi
        return inch * dpi
    }

    fun mmToPx(mm: Float, context: Context): Float {
        val inch = mm / 25.4f
        return inToPx(inch, context)
    }

    fun createButtonDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = 12f
        }
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = GrindrPlus.context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        GrindrPlus.showToast(Toast.LENGTH_LONG, "$label copied to clipboard.")
    }

    fun formatEpochSeconds(epochSec: Long): String {
        val instant = Instant.ofEpochSecond(epochSec)
        val dt = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd '≈'")
        return dt.format(fmt)
    }
}