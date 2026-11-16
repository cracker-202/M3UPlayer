package com.example.m3uplayer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val Bg = Color(0xFF0B0D12)
val SurfaceDark = Color(0xFF161A22)
val SurfaceInset = Color(0xFF10131A)
val Accent = Color(0xFF7C5CFF)
val Gold = Color(0xFFFFC857)
val TextPrimary = Color(0xFFF2F4F8)
val TextSecondary = Color(0xFF9AA3B2)
val ErrorColor = Color(0xFFFF6B6B)

@Composable
fun M3UTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = Accent,
        onPrimary = Color.White,
        background = Bg,
        onBackground = TextPrimary,
        surface = SurfaceDark,
        onSurface = TextPrimary,
        surfaceVariant = Color(0xFF222733),
        onSurfaceVariant = TextSecondary,
        error = ErrorColor,
    )
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalContentColor provides TextPrimary, content = content)
    }
}
