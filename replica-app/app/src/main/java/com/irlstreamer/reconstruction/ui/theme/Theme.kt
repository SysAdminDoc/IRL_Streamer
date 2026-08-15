package com.irlstreamer.reconstruction.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AuditColors {
    val SettingsBackground = Color(0xFF303030)
    val AppBar = Color(0xFF212121)
    val Dialog = Color(0xFF424242)
    val QuickPanel = Color(0xFF020202)
    val Accent = Color(0xFF80CBC4)
    val PrimaryText = Color.White
    val SecondaryText = Color(0xFFC7C7C7)
    val DisabledText = Color(0xFF7A7A7A)
    val Divider = Color(0xFF4A4A4A)
    val AudioActive = Color(0xFF1F8B4D)
    val AudioMuted = Color(0xFF7F8C8D)
    val Timestamp = Color(0xFF66FF66)
    val SafeMargin = Color.Red
}

private val AuditDarkScheme = darkColorScheme(
    primary = AuditColors.Accent,
    onPrimary = Color.Black,
    secondary = AuditColors.Accent,
    background = AuditColors.SettingsBackground,
    onBackground = AuditColors.PrimaryText,
    surface = AuditColors.SettingsBackground,
    onSurface = AuditColors.PrimaryText,
    surfaceVariant = AuditColors.Dialog,
    onSurfaceVariant = AuditColors.SecondaryText,
    outline = AuditColors.Divider,
)

@Composable
fun IrlStreamerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AuditDarkScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
