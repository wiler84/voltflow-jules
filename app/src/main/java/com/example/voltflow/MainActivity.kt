package com.example.voltflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import android.os.SystemClock

class MainActivity : ComponentActivity() {
    private var repository: VoltflowRepository? = null

    private fun getRepository(): VoltflowRepository {
        return repository ?: VoltflowRepository(
            applicationContext,
            MockPaymentProcessor()
        ).also {
            repository = it
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashStart = SystemClock.elapsedRealtime()
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            val elapsed = SystemClock.elapsedRealtime() - splashStart
            val repo = getRepository()
            // Splash persists for minimum 1s, or while loading (max 10s)
            elapsed < 1_000 || (repo.uiState.value.isLoading && elapsed < 10_000)
        }

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            val context = LocalContext.current
            val prefs = remember { UserPreferencesStore(context) }
            val scope = rememberCoroutineScope()
            val darkModePref by prefs.darkModeFlow.collectAsState(initial = null)
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModel.factory(getRepository())
            )
            val uiState by mainViewModel.uiState.collectAsState()

            val profileDarkMode = uiState.dashboard.profile?.darkMode
            val resolvedDarkMode = profileDarkMode ?: darkModePref ?: androidx.compose.foundation.isSystemInDarkTheme()

            VoltflowTheme(darkTheme = resolvedDarkMode) {
                VoltflowApp(
                    viewModel = mainViewModel,
                    darkModeEnabled = resolvedDarkMode,
                    onToggleDarkMode = { enabled ->
                        scope.launch { prefs.setDarkMode(enabled) }
                        mainViewModel.setDarkMode(enabled)
                    },
                )
            }
        }
    }
}
