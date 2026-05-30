package com.freenet.fakesni.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.freenet.fakesni.R

val RobotoMono = FontFamily(
    Font(R.font.roboto_mono_thin,                FontWeight.Thin),
    Font(R.font.roboto_mono_thin_italic,         FontWeight.Thin,       FontStyle.Italic),
    Font(R.font.roboto_mono_extra_light,         FontWeight.ExtraLight),
    Font(R.font.roboto_mono_extra_light_italic,  FontWeight.ExtraLight, FontStyle.Italic),
    Font(R.font.roboto_mono_light,               FontWeight.Light),
    Font(R.font.roboto_mono_light_italic,        FontWeight.Light,      FontStyle.Italic),
    Font(R.font.roboto_mono_regular,             FontWeight.Normal),
    Font(R.font.roboto_mono_italic,              FontWeight.Normal,     FontStyle.Italic),
    Font(R.font.roboto_mono_medium,              FontWeight.Medium),
    Font(R.font.roboto_mono_medium_italic,       FontWeight.Medium,     FontStyle.Italic),
    Font(R.font.roboto_mono_semi_bold,           FontWeight.SemiBold),
    Font(R.font.roboto_mono_semi_bold_italic,    FontWeight.SemiBold,   FontStyle.Italic),
    Font(R.font.roboto_mono_bold,                FontWeight.Bold),
    Font(R.font.roboto_mono_bold_italic,         FontWeight.Bold,       FontStyle.Italic),
)

private val base = Typography()

val Typography = Typography(
    displayLarge   = base.displayLarge.copy(fontFamily   = RobotoMono),
    displayMedium  = base.displayMedium.copy(fontFamily  = RobotoMono),
    displaySmall   = base.displaySmall.copy(fontFamily   = RobotoMono),
    headlineLarge  = base.headlineLarge.copy(fontFamily  = RobotoMono),
    headlineMedium = base.headlineMedium.copy(fontFamily = RobotoMono),
    headlineSmall  = base.headlineSmall.copy(fontFamily  = RobotoMono),
    titleLarge     = base.titleLarge.copy(fontFamily     = RobotoMono),
    titleMedium    = base.titleMedium.copy(fontFamily    = RobotoMono),
    titleSmall     = base.titleSmall.copy(fontFamily     = RobotoMono),
    bodyLarge      = base.bodyLarge.copy(fontFamily      = RobotoMono),
    bodyMedium     = base.bodyMedium.copy(fontFamily     = RobotoMono),
    bodySmall      = base.bodySmall.copy(fontFamily      = RobotoMono),
    labelLarge     = base.labelLarge.copy(fontFamily     = RobotoMono),
    labelMedium    = base.labelMedium.copy(fontFamily    = RobotoMono),
    labelSmall     = base.labelSmall.copy(fontFamily     = RobotoMono),
)
