package com.parsaapps.nichmarket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.parsaapps.nichmarket.ui.navigation.AppRoot
import com.parsaapps.nichmarket.ui.theme.NichMarketTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NichMarketTheme {
                AppRoot()
            }
        }
    }
}
