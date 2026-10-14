package com.mechastron.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mechastron.studio.ui.MechastronStudioTheme
import com.mechastron.studio.ui.StudioScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MechastronStudioTheme {
                StudioScreen()
            }
        }
    }
}