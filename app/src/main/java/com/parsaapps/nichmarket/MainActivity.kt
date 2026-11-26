package com.parsaapps.nichmarket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.parsaapps.nichmarket.data.model.AppSettings
import com.parsaapps.nichmarket.data.model.AppTheme
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.parsaapps.nichmarket.ui.navigation.AppRoot
import com.parsaapps.nichmarket.ui.theme.NichMarketTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by Graph.userStore.settings.collectAsState(initial = AppSettings())
            val density = LocalDensity.current
            val dark = when (settings.theme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(density.density, density.fontScale * if (settings.largeText) 1.15f else 1f)
            ) {
                NichMarketTheme(darkTheme = dark) { AppRoot(reduceMotion = settings.reduceMotion) }
            }
        }
    }
}
