package com.parsaapps.nichmarket.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.parsaapps.nichmarket.R

// Bundled for consistent offline Persian rendering. See assets/licenses/Vazirmatn-OFL.txt.
private val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

private val base = Typography()
val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Vazirmatn),
    displayMedium = base.displayMedium.copy(fontFamily = Vazirmatn),
    displaySmall = base.displaySmall.copy(fontFamily = Vazirmatn),
    headlineLarge = base.headlineLarge.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 25.sp, lineHeight = 38.sp),
    headlineSmall = base.headlineSmall.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 32.sp),
    titleMedium = base.titleMedium.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 26.sp),
    titleSmall = base.titleSmall.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontFamily = Vazirmatn, fontSize = 16.sp, lineHeight = 28.sp),
    bodyMedium = base.bodyMedium.copy(fontFamily = Vazirmatn, fontSize = 14.sp, lineHeight = 24.sp),
    bodySmall = base.bodySmall.copy(fontFamily = Vazirmatn, lineHeight = 21.sp),
    labelLarge = base.labelLarge.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    labelMedium = base.labelMedium.copy(fontFamily = Vazirmatn, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
    labelSmall = base.labelSmall.copy(fontFamily = Vazirmatn, lineHeight = 18.sp)
)
