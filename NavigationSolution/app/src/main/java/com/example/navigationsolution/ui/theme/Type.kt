package com.example.navigationsolution.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal var textScale: Double = 1.2

fun updateTextScale(scale: Double) {
    textScale = scale
}

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = (57 * textScale).sp,
        lineHeight = (64 * textScale).sp,
        letterSpacing = (0 * textScale).sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = (24 * textScale).sp,
        lineHeight = (32 * textScale).sp,
        letterSpacing = (0 * textScale).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = (18 * textScale).sp,
        lineHeight = (28 * textScale).sp,
        letterSpacing = (0 * textScale).sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = (16 * textScale).sp,
        lineHeight = (24 * textScale).sp,
        letterSpacing = (0.15 * textScale).sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = (14 * textScale).sp,
        lineHeight = (20 * textScale).sp,
        letterSpacing = (0.25 * textScale).sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = (12 * textScale).sp,
        lineHeight = (16 * textScale).sp,
        letterSpacing = (0.5 * textScale).sp
    )
)