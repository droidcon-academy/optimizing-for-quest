package com.droidcon.soundbox.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.droidcon.soundbox.ui.theme.SoundBoxTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SoundBoxTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}
