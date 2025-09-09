package com.teamflux.fluxai.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Use system monospace font as default
val AppFontFamily: FontFamily = FontFamily.Monospace

// Material3 Typography using FontFamily.Monospace (bold for headings)
val Typography = Typography(
    displayLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),

    headlineLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),

    titleLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),

    bodyLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),

    labelLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp)
)