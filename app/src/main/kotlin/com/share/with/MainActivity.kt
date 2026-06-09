package com.share.with

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize global crash handler
        GlobalExceptionHandler.initialize(this)
        
        setContent {
            ShareWithTheme {
                AppUI()
            }
        }
    }
}
