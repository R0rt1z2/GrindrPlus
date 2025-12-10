package com.grindrplus.manager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Error
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class BannerType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

@Composable
fun MessageBanner(
    text: String,
    modifier: Modifier = Modifier,
    type: BannerType = BannerType.INFO,
    isVisible: Boolean = true,
    isPulsating: Boolean = false,
    onDismiss: (() -> Unit)? = null
) {
    var bannerVisible by remember { mutableStateOf(isVisible) }

    val (backgroundColor, borderColor, iconTint, icon) = when (type) {
        BannerType.INFO -> Quad(
            Color(0xFF2196F3).copy(alpha = 0.15f),
            Color(0xFF2196F3).copy(alpha = 0.5f),
            Color(0xFF2196F3),
            Icons.Default.Info
        )
        BannerType.SUCCESS -> Quad(
            Color(0xFF4CAF50).copy(alpha = 0.15f),
            Color(0xFF4CAF50).copy(alpha = 0.5f),
            Color(0xFF4CAF50),
            Icons.Default.CheckCircle
        )
        BannerType.WARNING -> Quad(
            Color(0xFFFFC107).copy(alpha = 0.15f),
            Color(0xFFFFC107).copy(alpha = 0.5f),
            Color(0xFFFFC107),
            Icons.Default.Warning
        )
        BannerType.ERROR -> Quad(
            Color(0xFFF44336).copy(alpha = 0.15f),
            Color(0xFFF44336).copy(alpha = 0.5f),
            Color(0xFFF44336),
            Icons.Outlined.Error
        )
    }

    AnimatedVisibility(
        visible = bannerVisible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulsating")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )

        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .alpha(if (isPulsating && bannerVisible) alpha else 1f)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(8.dp)
                ),
            color = backgroundColor
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = type.name.lowercase(),
                    tint = iconTint,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )

                if (onDismiss != null) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable {
                                bannerVisible = false
                                onDismiss()
                            }
                    )
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)