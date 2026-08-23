package com.englishbondhu.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.englishbondhu.app.ui.EnglishBondhuApp
import com.englishbondhu.app.ui.theme.EnglishBondhuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EnglishBondhuTheme { EnglishBondhuApp() }
        }
    }
}
