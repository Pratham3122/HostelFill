package com.prathamshah.hostelfill.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Blue/navy palette matching the brief
private val Blue900   = Color(0xFF0D47A1)
private val Blue700   = Color(0xFF1565C0)
private val Blue500   = Color(0xFF1976D2)
private val Blue100   = Color(0xFFBBDEFB)
private val Navy      = Color(0xFF0A1929)
private val White     = Color(0xFFFFFFFF)

private val LightColors = lightColorScheme(
    primary            = Blue700,
    onPrimary          = White,
    primaryContainer   = Blue100,
    onPrimaryContainer = Blue900,
    secondary          = Blue500,
    onSecondary        = White,
    background         = Color(0xFFF5F7FA),
    surface            = White,
    error              = Color(0xFFB00020),
    onError            = White
)

@Composable
fun HostelFillTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography  = Typography(),
        content     = content
    )
}
