package com.englishbondhu.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import com.englishbondhu.app.ui.EnglishBondhuApp
import com.englishbondhu.app.ui.theme.EnglishBondhuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // targetSdk 35+ is edge-to-edge on Android 15+; opt in everywhere so layouts and
        // status-bar icon colours behave the same on older Android versions too.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            EnglishBondhuTheme { EnglishBondhuApp() }
        }
    }
}
