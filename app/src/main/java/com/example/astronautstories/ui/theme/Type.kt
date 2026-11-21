package com.example.astronautstories.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.astronautstories.R

val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn, FontWeight.Normal),
    Font(R.font.vazirmatn, FontWeight.Medium),
    Font(R.font.vazirmatn, FontWeight.Bold)
)

val SpaceGroteskFontFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Bold)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

fun buildAppTypography(scaleFactor: Float = 1.0f): Typography {
    return Typography(
        displaySmall = TextStyle(
            fontFamily = VazirmatnFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (28f * scaleFactor).sp,
            lineHeight = (36f * scaleFactor).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = VazirmatnFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (22f * scaleFactor).sp,
            lineHeight = (30f * scaleFactor).sp
        ),
        titleLarge = TextStyle(
            fontFamily = VazirmatnFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (18f * scaleFactor).sp,
            lineHeight = (26f * scaleFactor).sp
        ),
        titleMedium = TextStyle(
            fontFamily = VazirmatnFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (16f * scaleFactor).sp,
            lineHeight = (22f * scaleFactor).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = VazirmatnFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (15f * scaleFactor).sp,
            lineHeight = (22f * scaleFactor).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = VazirmatnFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (13f * scaleFactor).sp,
            lineHeight = (19f * scaleFactor).sp
        ),
        labelLarge = TextStyle(
            fontFamily = VazirmatnFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (14f * scaleFactor).sp,
            lineHeight = (18f * scaleFactor).sp
        ),
        labelMedium = TextStyle(
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (12f * scaleFactor).sp,
            lineHeight = (16f * scaleFactor).sp
        )
    )
}
