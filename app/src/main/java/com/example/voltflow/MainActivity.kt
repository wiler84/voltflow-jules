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
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private var repository: VoltflowRepository? = null
    private var initialDeepLink by mutableStateOf<String?>(null)

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
        handleIntent(intent)
        val splashStart = SystemClock.elapsedRealtime()
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            val elapsed = SystemClock.elapsedRealtime() - splashStart
            val repo = getRepository()
            // Splash persists while checking session (isAuthReady) 
            // OR for a minimum of 600ms to allow branding visibility
            !repo.uiState.value.isAuthReady || elapsed < 600
        }

        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)

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
                    initialDeepLink = initialDeepLink,
                    onDeepLinkConsumed = { initialDeepLink = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            if (uri.scheme == "voltflow" && uri.host == "reset") {
                initialDeepLink = uri.toString()
            }
        }
    }
}
