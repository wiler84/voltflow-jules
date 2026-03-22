package com.example.voltflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voltflow.data.MockPaymentProcessor
import com.example.voltflow.data.UserPreferencesStore
import com.example.voltflow.data.VoltflowRepository
import com.example.voltflow.ui.VoltflowApp
import com.example.voltflow.ui.theme.VoltflowTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = VoltflowRepository(applicationContext, MockPaymentProcessor())

        setContent {
            val context = LocalContext.current
            val prefs = remember { UserPreferencesStore(context) }
            val scope = rememberCoroutineScope()
            val darkModePref by prefs.darkModeFlow.collectAsState(initial = null)
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModel.factory(repository)
            )

            VoltflowTheme(darkTheme = darkModePref ?: androidx.compose.foundation.isSystemInDarkTheme()) {
                VoltflowApp(
                    viewModel = mainViewModel,
                    darkModeEnabled = darkModePref ?: androidx.compose.foundation.isSystemInDarkTheme(),
                    onToggleDarkMode = { enabled -> scope.launch { prefs.setDarkMode(enabled) } },
                )
            }
        }
    }
}
