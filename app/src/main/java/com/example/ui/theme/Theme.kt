package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8), // Clear azure highlight in dark mode
    secondary = TealAccent,
    tertiary = CreamGold,
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF334155),
    onPrimary = Color(0xFF0F172A),
    onSecondary = Color.White,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569),
    error = RedAlert
)

private val LightColorScheme = lightColorScheme(
    primary = SlatePrimary,
    secondary = TealAccent,
    tertiary = CreamGold,
    background = SlateLightBg,
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569),
    outline = SlateBorder,
    error = RedAlert
)

object VaultTheme {
    val background: Color @Composable get() = MaterialTheme.colorScheme.background
    val cardBg: Color @Composable get() = MaterialTheme.colorScheme.surface
    val cardBorder: Color @Composable get() = MaterialTheme.colorScheme.outline
    val textPrimary: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    val textSecondary: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textMuted: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF64748B) else Color(0xFF94A3B8)
    val subCardBg: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF0B1120) else Color(0xFFF1F5F9)
    val dialogBg: Color @Composable get() = MaterialTheme.colorScheme.surface
    val isDark: Boolean @Composable get() = isSystemInDarkTheme()
}

@Composable
fun vaultTextFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = VaultTheme.cardBg,
    unfocusedContainerColor = VaultTheme.cardBg,
    focusedBorderColor = TealAccent,
    unfocusedBorderColor = VaultTheme.cardBorder,
    focusedLabelColor = TealAccent,
    unfocusedLabelColor = VaultTheme.textSecondary,
    focusedPlaceholderColor = VaultTheme.textMuted,
    unfocusedPlaceholderColor = VaultTheme.textMuted
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontTheme: VaultFontTheme = VaultFontTheme.EXECUTIVE,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val typography = getTypographyForTheme(fontTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
