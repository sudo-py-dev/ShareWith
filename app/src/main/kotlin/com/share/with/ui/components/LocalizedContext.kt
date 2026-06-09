package com.share.with.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
fun LocalizedContext(
    localeCode: String,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val locale =
        when (localeCode) {
            "he" -> Locale.forLanguageTag("he")
            else -> Locale.forLanguageTag(localeCode)
        }

    val resources = context.resources
    val configuration = resources.configuration

    val localizedContext =
        remember(localeCode) {
            val config = android.content.res.Configuration(configuration)
            config.setLocale(locale)
            context.createConfigurationContext(config)
        }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        content = content,
    )
}
