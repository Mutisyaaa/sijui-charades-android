package com.sijui.charades.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sijui.charades.R

@OptIn(ExperimentalTextApi::class)
val ManlineDisplayFont = FontFamily(
    Font(
        resId = R.font.bricolage_display,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.width(100f),
        ),
    ),
    Font(
        resId = R.font.bricolage_display,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(800),
            FontVariation.width(100f),
        ),
    ),
)

val SijuiTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = ManlineDisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.8).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = ManlineDisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = ManlineDisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = ManlineDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = ManlineDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = ManlineDisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
        letterSpacing = 0.4.sp,
    ),
)
