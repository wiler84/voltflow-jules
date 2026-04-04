package com.example.voltflow.ui

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import android.util.Patterns
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voltflow.BuildConfig
import com.example.voltflow.MainViewModel
import com.example.voltflow.Screen
import com.example.voltflow.data.*
import com.example.voltflow.rememberAppNavigator
import com.example.voltflow.screenFromRoute
import com.example.voltflow.ui.screens.*
import com.example.voltflow.R
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.fragment.app.FragmentActivity
import com.example.voltflow.notifications.NotificationHelper
import com.example.voltflow.notifications.VoltflowNotificationCenter
import com.google.firebase.messaging.FirebaseMessaging
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.coroutines.resume

object VoltflowAppUtils {
    fun shortId(value: String?): String {
        if (value.isNullOrBlank()) return "65e95c38...62cd"
        return if (value.length <= 10) value else "${value.take(8)}…${value.takeLast(4)}"
    }
}

@Composable
fun VoltflowApp(
    viewModel: MainViewModel,
    darkModeEnabled: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateCompat()
    val navigator = rememberAppNavigator(if (state.isAuthenticated) Screen.Home else Screen.Auth)
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val view = LocalView.current
    val navStateStore = remember { NavStateStore(context) }
    val notificationHelper = remember(context) { NotificationHelper(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val hazeState = rememberHazeState()
    val userId = state.dashboard.profile?.userId
    val lifecycleOwner = LocalLifecycleOwner.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }
    var pendingDeviceCredentialCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val deviceCredentialLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        pendingDeviceCredentialCallback?.invoke(result.resultCode == Activity.RESULT_OK)
        pendingDeviceCredentialCallback = null
    }
    var hasPin by remember { mutableStateOf(false) }
    var showPinFallback by remember { mutableStateOf(false) }
    var appLocked by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showReloadDialog by remember { mutableStateOf(false) }
    var authUntilEpochMillis by remember { mutableStateOf(0L) }
    var pinAttemptsRemaining by remember { mutableStateOf(5) }
    var pinLockRemainingMillis by remember { mutableStateOf(0L) }
    var lockInlineError by remember { mutableStateOf<String?>(null) }
    var showSecuritySetup by remember { mutableStateOf(false) }
    var hasAppliedInitialAppLock by remember(userId) { mutableStateOf(false) }
    val lockScope = LockScope.fromRaw(state.dashboard.securitySettings?.lockScope)
    val appLockEnabled = lockScope == LockScope.TRANSACTIONS_AND_APP
    val biometricManager = remember { BiometricManager.from(context) }
    val biometricAvailable = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    val biometricEnabled = state.dashboard.securitySettings?.biometricEnabled == true
    val keyguardManager = remember(context) { ContextCompat.getSystemService(context, KeyguardManager::class.java) }
    val launchDeviceCredentialPrompt: suspend (String, String) -> Boolean = { title, description ->
        suspendCancellableCoroutine { continuation ->
            val credentialIntent = keyguardManager?.createConfirmDeviceCredentialIntent(title, description)
            if (credentialIntent == null) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            pendingDeviceCredentialCallback = { success ->
                if (continuation.isActive) {
                    continuation.resume(success)
                }
            }

            deviceCredentialLauncher.launch(credentialIntent)

            continuation.invokeOnCancellation {
                pendingDeviceCredentialCallback = null
            }
        }
    }
    val requireTransactionAuth: (String, () -> Unit) -> Unit = { actionLabel, action ->
        val transactionAuthConfigured = (biometricEnabled && biometricAvailable) || hasPin
        if (!transactionAuthConfigured || System.currentTimeMillis() < authUntilEpochMillis) {
            action()
        } else {
            val activity = context as? FragmentActivity
            pendingAction = action
            if (biometricEnabled && biometricAvailable && activity != null) {
                scope.launch {
                    val biometricSuccess = authenticateWithTransactionBiometric(
                        activity = activity,
                        title = actionLabel,
                        subtitle = "Use biometrics to continue",
                    )
                    if (biometricSuccess) {
                        pendingAction = null
                        authUntilEpochMillis = System.currentTimeMillis() + 5 * 60_000L
                        action()
                    } else if (hasPin) {
                        showPinFallback = true
                    } else {
                        pendingAction = null
                    }
                }
            } else if (hasPin) {
                showPinFallback = true
            } else {
                pendingAction = null
                action()
            }
        }
    }

    LaunchedEffect(state.isAuthenticated, userId) {
        if (!state.isAuthenticated) {
            navigator.replaceRoot(Screen.Auth)
            return@LaunchedEffect
        }
        userId?.let {
            val restoredRoute = navStateStore.readRoute(it)
            val restored = screenFromRoute(restoredRoute)?.takeIf { s -> s != Screen.Auth } ?: Screen.Home
            navigator.replaceRoot(restored)
        }
    }

    LaunchedEffect(state.transientMessage) {
        state.transientMessage?.let { snackbarHostState.showSnackbar(it); viewModel.consumeMessage() }
    }
    LaunchedEffect(state.activeError) {
        state.activeError?.let { snackbarHostState.showSnackbar(it); viewModel.consumeError() }
    }
    LaunchedEffect(Unit) {
        notificationHelper.ensureChannels()
    }
    LaunchedEffect(Unit) {
        VoltflowNotificationCenter.events.collect { event ->
            snackbarHostState.showSnackbar(event.title.ifBlank { event.body })
        }
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(state.dashboard.notifications) {
        val latestUnread = state.dashboard.notifications
            .filter { !it.isRead }
            .maxByOrNull { it.createdAt ?: "" }
        latestUnread?.let { notification ->
            notificationHelper.dispatch(
                title = notification.title,
                body = notification.body,
                type = notification.type,
            )
        }
    }
    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) {
            runCatching { FirebaseMessaging.getInstance().token }
                .getOrNull()
                ?.addOnSuccessListener { token ->
                    if (token.isNotBlank()) {
                        viewModel.updatePushToken(token)
                    }
                }
        }
    }
    LaunchedEffect(appLocked, state.isAuthenticated, appLockEnabled) {
        if (!state.isAuthenticated || !appLockEnabled || !appLocked) return@LaunchedEffect
        val activity = context as? FragmentActivity ?: return@LaunchedEffect
        while (appLocked && state.isAuthenticated && appLockEnabled) {
            val unlocked = authenticateWithSystemAppUnlock(
                activity = activity,
                biometricAvailable = biometricAvailable,
                launchDeviceCredentialPrompt = launchDeviceCredentialPrompt,
            )
            if (unlocked) {
                appLocked = false
            } else {
                kotlinx.coroutines.delay(350)
            }
        }
    }

    BackHandler(enabled = showSecuritySetup || appLocked) { }
    BackHandler(enabled = showPinFallback) {
        showPinFallback = false
        pendingAction = null
    }
    BackHandler(enabled = navigator.canPop && !showSecuritySetup && !showPinFallback && !appLocked) {
        navigator.pop()
        if (!userId.isNullOrBlank()) {
            scope.launch { navStateStore.saveRoute(userId, navigator.currentScreen.route) }
        }
    }

    LaunchedEffect(state.dashboard.securitySettings?.pinHash, state.isAuthenticated, state.isLoading) {
        hasPin = !state.dashboard.securitySettings?.pinHash.isNullOrBlank()
        showSecuritySetup = state.isAuthenticated && !state.isLoading && !hasPin
    }
    LaunchedEffect(state.isAuthenticated) {
        if (!state.isAuthenticated) {
            showPinFallback = false
            pendingAction = null
            authUntilEpochMillis = 0L
            appLocked = false
            hasAppliedInitialAppLock = false
        }
    }

    LaunchedEffect(pinLockRemainingMillis) {
        if (pinLockRemainingMillis > 0L) {
            while (pinLockRemainingMillis > 0L) {
                kotlinx.coroutines.delay(1_000L)
                pinLockRemainingMillis = (pinLockRemainingMillis - 1_000L).coerceAtLeast(0L)
            }
        }
    }

    LaunchedEffect(state.isLoading) {
        if (state.isLoading) {
            kotlinx.coroutines.delay(10_000)
            if (state.isLoading) {
                showReloadDialog = true
            }
        } else {
            showReloadDialog = false
        }
    }
    LaunchedEffect(state.isAuthenticated, state.dashboard.securitySettings?.lockScope) {
        if (!state.isAuthenticated) {
            hasAppliedInitialAppLock = false
            appLocked = false
            return@LaunchedEffect
        }

        val settings = state.dashboard.securitySettings ?: return@LaunchedEffect
        if (!hasAppliedInitialAppLock) {
            hasAppliedInitialAppLock = true
            appLocked = LockScope.fromRaw(settings.lockScope) == LockScope.TRANSACTIONS_AND_APP
        } else if (LockScope.fromRaw(settings.lockScope) != LockScope.TRANSACTIONS_AND_APP) {
            appLocked = false
        }
    }

    DisposableEffect(lifecycleOwner, state.dashboard.securitySettings, appLockEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && state.isAuthenticated) {
                pendingAction = null
                showPinFallback = false
                if (appLockEnabled) {
                    appLocked = true
                }
            }
            if (event == Lifecycle.Event.ON_RESUME) {
                if (appLockEnabled) {
                    appLocked = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = state.isAuthenticated && navigator.currentScreen.isMainTab(),
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            ) {
                VoltflowBottomNav(
                    current = navigator.currentScreen,
                    hasUnread = state.dashboard.notifications.any { !it.isRead },
                    hazeState = hazeState,
                ) { screen ->
                    performPlatformHaptic(view, android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                    navigator.switchTab(screen)
                    if (!userId.isNullOrBlank()) {
                        scope.launch { navStateStore.saveRoute(userId, screen.route) }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(VoltflowDesign.BgTop, VoltflowDesign.BgBottom)))
                .hazeSource(state = hazeState)
        ) {
            if (state.isOffline) {
                OfflineBanner(modifier = Modifier.align(Alignment.TopCenter))
            }
            if (navigator.currentScreen == Screen.Auth) {
                AuthBloom()
            }
            AnimatedContent(
                targetState = navigator.currentScreen,
                transitionSpec = {
                    fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                },
                label = "voltflow-screen"
            ) { screen ->
                when (screen) {
                    Screen.Auth -> AuthScreen(
                        state,
                        darkModeEnabled,
                        onToggleDarkMode,
                        viewModel::signIn,
                        viewModel::signUp
                    )
                    Screen.Home -> HomeScreen(
                        state = state,
                        innerPadding = innerPadding,
                        onOpenNotifications = { navigator.navigate(Screen.Notifications) },
                        onOpenUsage = { navigator.navigate(Screen.Usage) },
                        onOpenPay = { navigator.navigate(Screen.Pay) },
                        onOpenHistory = { navigator.navigate(Screen.History) },
                        onOpenAutopay = { navigator.navigate(Screen.AutoPay) },
                        onOpenMethods = { navigator.navigate(Screen.PaymentMethods) },
                        onOpenWallet = { navigator.navigate(Screen.Wallet) },
                        onOpenSupport = { navigator.navigate(Screen.HelpCenter) },
                        onRefresh = viewModel::refresh,
                    )
                    Screen.Pay -> PayScreen(
                        state = state,
                        innerPadding = innerPadding,
                        onPay = { utility, amount, meterNumber, method, useWallet, onSuccess ->
                            requireTransactionAuth("Confirm payment") {
                                viewModel.payUtility(utility, amount, meterNumber, method, useWallet)
                                onSuccess()
                            }
                        },
                        onBack = { navigator.pop() },
                    )
                    Screen.Wallet -> WalletScreen(
                        state = state,
                        innerPadding = innerPadding,
                        onBack = { navigator.pop() },
                        onFund = { amount ->
                            requireTransactionAuth("Confirm add funds") {
                                viewModel.fundWallet(amount)
                            }
                        },
                        onWithdraw = { amount, onDone ->
                            requireTransactionAuth("Confirm withdrawal") {
                                viewModel.withdrawWallet(amount)
                                onDone()
                            }
                        },
                    )
                    Screen.History -> HistoryScreen(state, innerPadding, onBack = { navigator.pop() })
                    Screen.Notifications -> NotificationsScreen(
                        state,
                        innerPadding,
                        onBack = { navigator.pop() },
                        onMarkRead = viewModel::markNotificationRead,
                    )
                    Screen.Usage -> UsageScreen(state, innerPadding, onBack = { navigator.pop() })
                    Screen.AutoPay -> AutoPayScreen(
                        state = state,
                        innerPadding = innerPadding,
                        onSetAutopay = { enabled, paymentMethodId, amountLimit, billingCycle, paymentDay, meterNumber ->
                            requireTransactionAuth("Confirm AutoPay") {
                                viewModel.setAutopay(enabled, paymentMethodId, amountLimit, billingCycle, paymentDay, meterNumber)
                            }
                        },
                        onBack = { navigator.pop() }
                    )
                    Screen.PaymentMethods -> PaymentMethodsScreen(
                        state,
                        innerPadding,
                        onBack = { navigator.pop() },
                        onEnableMfa = viewModel::setMfaEnabled,
                        onAdd = viewModel::addPaymentMethod,
                    )
                    Screen.ConnectedDevices -> ConnectedDevicesScreen(
                        state = state,
                        innerPadding = innerPadding,
                        onBack = { navigator.pop() },
                        onRevoke = viewModel::revokeDevice,
                        onRefreshLocation = viewModel::syncCurrentDeviceLocation,
                    )
                    Screen.Profile -> ProfileHubScreen(
                        state = state,
                        innerPadding = innerPadding,
                        darkModeEnabled = darkModeEnabled,
                        onToggleDarkMode = onToggleDarkMode,
                        onOpenProfileOptions = { navigator.navigate(Screen.ProfileOptions) },
                        onOpenPaymentMethods = { navigator.navigate(Screen.PaymentMethods) },
                        onOpenNotifications = { navigator.navigate(Screen.Notifications) },
                        onOpenSecurity = { navigator.navigate(Screen.SecuritySettings) },
                        onOpenConnectedDevices = { navigator.navigate(Screen.ConnectedDevices) },
                        onOpenHelpCenter = { navigator.navigate(Screen.HelpCenter) },
                        onOpenTerms = { navigator.navigate(Screen.Terms) },
                        onOpenContact = { navigator.navigate(Screen.Contact) },
                        onSignOut = { viewModel.signOut() },
                    )
                    Screen.ProfileOptions -> ProfileOptionsScreen(state = state, innerPadding = innerPadding, onBack = { navigator.pop() }, onSaveProfile = viewModel::saveProfile)
                    Screen.SecuritySettings -> SecuritySettingsScreen(
                        state = state,
                        innerPadding = innerPadding,
                        onBack = { navigator.pop() },
                        onToggleBiometric = viewModel::setBiometricEnabled,
                        onToggleMfa = viewModel::setMfaEnabled,
                        onSetPin = viewModel::setPin,
                        onSetLockScope = viewModel::setLockScope,
                        onRequestPinReset = { viewModel.requestPinResetToken() },
                        onVerifyPinReset = { token -> viewModel.verifyPinResetToken(token) },
                        onCompletePinReset = { token, pin -> viewModel.completePinReset(token, pin) }
                    )
                    Screen.HelpCenter -> HelpCenterScreen(innerPadding = innerPadding, onBack = { navigator.pop() })
                    Screen.Terms -> TermsScreen(innerPadding = innerPadding, onBack = { navigator.pop() })
                    Screen.Contact -> ContactScreen(innerPadding = innerPadding, onBack = { navigator.pop() })
                }
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VoltflowDesign.BlueAccent, strokeWidth = 3.dp)
                }
            }

            if (showReloadDialog) {
                VoltflowReloadDialog(
                    onReload = {
                        showReloadDialog = false
                        viewModel.restoreSession()
                    },
                    onCancel = {
                        showReloadDialog = false
                        if (!state.isAuthenticated) {
                            navigator.replaceRoot(Screen.Auth)
                        }
                    }
                )
            }

            if (showPinFallback) {
                VoltflowPinFallbackOverlay(
                    lockoutRemainingMillis = pinLockRemainingMillis,
                    attemptsRemaining = pinAttemptsRemaining,
                    inlineError = lockInlineError,
                    onVerifyPin = { pin -> viewModel.verifyPin(pin) },
                    onAuthenticated = {
                        showPinFallback = false
                        val action = pendingAction
                        pendingAction = null
                        lockInlineError = null
                        pinAttemptsRemaining = 5
                        pinLockRemainingMillis = 0L
                        authUntilEpochMillis = System.currentTimeMillis() + 5 * 60_000L
                        action?.invoke()
                    },
                    onDismiss = {
                        showPinFallback = false
                        pendingAction = null
                    },
                    onAuthState = { attempts, remainingMillis, message ->
                        pinAttemptsRemaining = attempts
                        pinLockRemainingMillis = remainingMillis
                        lockInlineError = message
                    },
                )
            }
            if (showSecuritySetup) {
                SecuritySetupOverlay(
                    biometricAvailable = biometricAvailable,
                    onComplete = { pin, enableBiometric, scopeChoice ->
                        viewModel.saveSecuritySetup(pin, enableBiometric, scopeChoice)
                    }
                )
            }
            if (appLocked && state.isAuthenticated && appLockEnabled) {
                DeviceUnlockBlockingOverlay()
            }
        }
    }
}

@Composable
private fun <T> StateFlow<T>.collectAsStateCompat() = collectAsState()

@Composable
private fun AuthBloom() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 150.dp)
                .size(500.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(VoltflowDesign.BlueAccent.copy(alpha = 0.2f), Color.Transparent)))
                .blur(100.dp)
        )
    }
}

private fun Screen.isMainTab(): Boolean = this in listOf(Screen.Home, Screen.Pay, Screen.History, Screen.Profile)

private suspend fun authenticateWithCombinedSystemAuth(activity: FragmentActivity): Boolean {
    return suspendCancellableCoroutine { continuation ->
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (continuation.isActive) continuation.resume(false)
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Voltflow")
            .setSubtitle("Use your device security to continue")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        prompt.authenticate(info)
        continuation.invokeOnCancellation { prompt.cancelAuthentication() }
    }
}

private suspend fun authenticateWithBiometricOnly(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    negativeButtonText: String,
): Boolean {
    return suspendCancellableCoroutine { continuation ->
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (continuation.isActive) continuation.resume(false)
                }

                override fun onAuthenticationFailed() {
                    // Keep prompt open for additional attempts; final outcome is emitted via onAuthenticationError.
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(info)
        continuation.invokeOnCancellation { prompt.cancelAuthentication() }
    }
}

private suspend fun authenticateWithTransactionBiometric(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
): Boolean = authenticateWithBiometricOnly(
    activity = activity,
    title = title,
    subtitle = subtitle,
    negativeButtonText = "Use PIN",
)

private suspend fun authenticateWithSystemAppUnlock(
    activity: FragmentActivity,
    biometricAvailable: Boolean,
    launchDeviceCredentialPrompt: suspend (String, String) -> Boolean,
): Boolean {
    val title = "Unlock Voltflow"
    val description = "Use your device PIN, pattern, password, or biometrics to continue."
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
            authenticateWithCombinedSystemAuth(activity)
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
            if (biometricAvailable) {
                authenticateWithBiometricOnly(
                    activity = activity,
                    title = title,
                    subtitle = "Use biometrics to unlock Voltflow",
                    negativeButtonText = "Use device PIN",
                ) || launchDeviceCredentialPrompt(title, description)
            } else {
                launchDeviceCredentialPrompt(title, description)
            }
        }
        else -> launchDeviceCredentialPrompt(title, description)
    }
}

@Composable
private fun DeviceUnlockBlockingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            Text("Unlocking Voltflow", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            Text(
                "Use your device security to continue.",
                color = VoltflowDesign.GrayText,
                textAlign = TextAlign.Center,
                fontFamily = VoltflowDesign.ManropeFont
            )
            CircularProgressIndicator(color = VoltflowDesign.BlueAccent, strokeWidth = 3.dp)
        }
    }
}

@Composable
private fun SecuritySetupOverlay(
    biometricAvailable: Boolean,
    onComplete: suspend (pin: String, enableBiometric: Boolean, scope: LockScope) -> Boolean
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var enableBiometric by rememberSaveable { mutableStateOf(false) }
    var scopeChoice by rememberSaveable { mutableStateOf(LockScope.TRANSACTIONS_ONLY) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(20.dp),
            shape = RoundedCornerShape(24.dp),
            color = VoltflowDesign.ModalBg,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Secure your account", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp, fontFamily = VoltflowDesign.SoraFont)
                Text("Create a 6-digit PIN for transaction approval.", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont)
                VoltflowInput(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                    placeholder = "Enter PIN",
                )
                VoltflowInput(
                    value = confirmPin,
                    onValueChange = { confirmPin = it.filter(Char::isDigit).take(6) },
                    placeholder = "Re-enter PIN",
                )
                if (biometricAvailable) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Use biometrics for faster transaction approval", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontFamily = VoltflowDesign.ManropeFont)
                        Switch(
                            checked = enableBiometric,
                            onCheckedChange = { enableBiometric = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = VoltflowDesign.BlueAccent),
                            enabled = !isSaving,
                        )
                    }
                }
                Text("Lock scope", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontFamily = VoltflowDesign.SoraFont)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = scopeChoice == LockScope.TRANSACTIONS_ONLY,
                        onClick = { scopeChoice = LockScope.TRANSACTIONS_ONLY },
                        label = { Text("Transactions only") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VoltflowDesign.BlueAccent.copy(alpha = 0.18f),
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        enabled = !isSaving,
                    )
                    FilterChip(
                        selected = scopeChoice == LockScope.TRANSACTIONS_AND_APP,
                        onClick = { scopeChoice = LockScope.TRANSACTIONS_AND_APP },
                        label = { Text("Transactions + app lock") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VoltflowDesign.BlueAccent.copy(alpha = 0.18f),
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        enabled = !isSaving,
                    )
                }
                error?.let {
                    Text(it, color = VoltflowDesign.DestructiveRed, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
                }
                Button(
                    onClick = {
                        val weakPins = setOf("000000", "111111", "123456", "654321", "121212", "112233")
                        when {
                            pin.length != 6 || confirmPin.length != 6 -> error = "PIN must be exactly 6 digits."
                            pin != confirmPin -> error = "PINs don't match. Try again."
                            pin in weakPins -> error = "Choose a stronger PIN."
                            else -> {
                                error = null
                                scope.launch {
                                    isSaving = true
                                    val saved = onComplete(pin, enableBiometric, scopeChoice)
                                    if (!saved) {
                                        error = "Unable to save your security settings right now."
                                    }
                                    isSaving = false
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.BlueAccent),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !isSaving,
                ) {
                    Text(if (isSaving) "Saving..." else "Save security settings", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                }
            }
        }
    }
}

@Composable
private fun VoltflowBottomNav(
    current: Screen,
    hasUnread: Boolean,
    hazeState: dev.chrisbanes.haze.HazeState,
    onNavigate: (Screen) -> Unit
) {
    val view = LocalView.current
    val items = listOf(
        NavItem("Home", Screen.Home, Icons.Outlined.Home),
        NavItem("Pay", Screen.Pay, Icons.Outlined.CreditCard),
        NavItem("History", Screen.History, Icons.Outlined.History),
        NavItem("Profile", Screen.Profile, Icons.Outlined.Person),
    )
    
    val selectedIndex = items.indexOfFirst { it.screen == current }.coerceAtLeast(0)
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        val navWidth = maxWidth * 0.85f
        Surface(
            modifier = Modifier
                .width(navWidth)
                .height(72.dp),
            shape = RoundedCornerShape(36.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            shadowElevation = 18.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(36.dp))
                        .hazeEffect(
                            state = hazeState,
                            style = HazeMaterials.ultraThin(),
                        )
                        .background(VoltflowDesign.BottomNavBg.copy(alpha = 0.35f))
                )
                val itemWidth = navWidth / items.size
                val indicatorOffset by animateDpAsState(
                    targetValue = itemWidth * selectedIndex + (itemWidth / 2) - 2.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "nav-indicator"
                )

                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset, y = 12.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(VoltflowDesign.BlueAccent)
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        val selected = index == selectedIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    performPlatformHaptic(view, android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                                    onNavigate(item.screen)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        tint = if (selected) VoltflowDesign.BlueAccent else VoltflowDesign.GrayText,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    if (hasUnread && item.screen == Screen.Profile) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(VoltflowDesign.BlueAccent)
                                                .align(Alignment.TopEnd)
                                                .offset(x = 4.dp, y = (-2).dp)
                                        )
                                    }
                                }
                                Text(
                                    item.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) VoltflowDesign.BlueAccent else VoltflowDesign.GrayText,
                                    fontFamily = VoltflowDesign.BodyFont
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthScreen(
    state: UiState,
    darkModeEnabled: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String, String, String) -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var firstName by rememberSaveable { mutableStateOf("") } // Added for signup
    var lastName by rememberSaveable { mutableStateOf("") } // Added for signup
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var isSignup by rememberSaveable { mutableStateOf(false) }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val textColor = MaterialTheme.colorScheme.onSurface
    val logoGradient = VoltflowDesign.LogoGradient

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 32.dp).imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.size(72.dp).padding(bottom = 12.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    moveTo(size.width * 0.55f, 0f)
                    lineTo(size.width * 0.2f, size.height * 0.55f)
                    lineTo(size.width * 0.45f, size.height * 0.55f)
                    lineTo(size.width * 0.35f, size.height)
                    lineTo(size.width * 0.85f, size.height * 0.4f)
                    lineTo(size.width * 0.55f, size.height * 0.4f)
                    close()
                }
                drawPath(path, brush = logoGradient)
            }
        }
        Text(
            "VOLTFLOW",
            color = textColor,
            letterSpacing = 8.sp,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            fontFamily = VoltflowDesign.SoraFont
        )
        Spacer(Modifier.height(48.dp))

        Box {
            // Blurred background for the card to keep the content sharp
            Surface(
                modifier = Modifier.matchParentSize().blur(24.dp).offset(y = 4.dp),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.12f)
            ) {}

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = VoltflowDesign.CardBg,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(28.dp)) {
                    Text(
                        if (isSignup) "Create account" else "Welcome back",
                        color = textColor,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = VoltflowDesign.SoraFont
                    )
                    Text(
                        if (isSignup) "Get started in minutes" else "Sign in to continue",
                        color = VoltflowDesign.GrayText,
                        fontSize = 15.sp,
                        fontFamily = VoltflowDesign.ManropeFont
                    )
                    Spacer(Modifier.height(36.dp))

                    if (isSignup) {
                        VoltflowInput(value = firstName, onValueChange = { firstName = it }, placeholder = "First Name")
                        Spacer(Modifier.height(18.dp))
                        VoltflowInput(value = lastName, onValueChange = { lastName = it }, placeholder = "Last Name")
                        Spacer(Modifier.height(18.dp))
                    }

                    VoltflowInput(value = email, onValueChange = { email = it }, placeholder = "Email")
                    Spacer(Modifier.height(18.dp))
                    VoltflowInput(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Password",
                        isPassword = true,
                        showPassword = showPassword,
                        onTogglePassword = { showPassword = !showPassword }
                    )
                    if (isSignup) {
                        Spacer(Modifier.height(18.dp))
                        VoltflowInput(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            placeholder = "Confirm Password",
                            isPassword = true,
                            showPassword = showPassword,
                            onTogglePassword = { showPassword = !showPassword }
                        )
                    }
                    Spacer(Modifier.height(36.dp))

                    VoltflowButton(text = if (isSignup) "Create account" else "Sign in") {
                        val trimmedEmail = email.trim()
                        val trimmedPassword = password.trim()
                        val validEmail = Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()
                        val passwordValid = trimmedPassword.length >= 8

                        val validationError = when {
                            trimmedEmail.isBlank() || trimmedPassword.isBlank() -> "Email and password are required."
                            !validEmail -> "Enter a valid email address."
                            !passwordValid -> "Password must be at least 8 characters."
                            isSignup && firstName.trim().isBlank() -> "First name is required."
                            isSignup && lastName.trim().isBlank() -> "Last name is required."
                            isSignup && confirmPassword.trim() != trimmedPassword -> "Passwords do not match."
                            else -> null
                        }

                        if (validationError != null) {
                            errorMessage = validationError
                            return@VoltflowButton
                        }
                        errorMessage = null
                        if (isSignup) {
                            onSignUp(trimmedEmail, trimmedPassword, firstName.trim(), lastName.trim())
                        } else {
                            onSignIn(trimmedEmail, trimmedPassword)
                        }
                    }
                    errorMessage?.let {
                        Spacer(Modifier.height(16.dp))
                        Text(it, color = VoltflowDesign.WarningAmber, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = if (isSignup) "Already have an account? Sign in" else "New here? Create an account",
                        color = VoltflowDesign.BlueAccent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = VoltflowDesign.ManropeFont,
                        modifier = Modifier.clickable {
                            isSignup = !isSignup
                            errorMessage = null
                        }.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(80.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Dark mode", color = VoltflowDesign.GrayText, fontSize = 14.sp, fontFamily = VoltflowDesign.ManropeFont)
            Spacer(Modifier.width(16.dp))
            Switch(
                checked = darkModeEnabled,
                onCheckedChange = onToggleDarkMode,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    checkedTrackColor = VoltflowDesign.BlueAccent,
                    uncheckedTrackColor = VoltflowDesign.IconCircleBg
                )
            )
        }
    }
}

@Composable
private fun ProfileHubScreen(
    state: UiState,
    innerPadding: PaddingValues,
    darkModeEnabled: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onOpenProfileOptions: () -> Unit,
    onOpenPaymentMethods: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenConnectedDevices: () -> Unit,
    onOpenHelpCenter: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenContact: () -> Unit,
    onSignOut: () -> Unit,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val profile = state.dashboard.profile
    val displayName = profile?.firstName?.let { "$it ${profile.lastName}" }?.trim()?.ifBlank { "Alex Johnson" } ?: "Alex Johnson"
    val textColor = MaterialTheme.colorScheme.onSurface

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = innerPadding.calculateTopPadding() + 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Profile", color = textColor, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
        }
        item {
            VoltflowCard(modifier = Modifier.fillMaxWidth().clickable { onOpenProfileOptions() }) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = VoltflowDesign.BlueAccent) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.background(VoltflowDesign.PrimaryGradient)) {
                            Text(displayName.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(displayName, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                        Text(profile?.email ?: "alex.johnson@email.com", color = VoltflowDesign.GrayText, fontSize = 14.sp, fontFamily = VoltflowDesign.ManropeFont)
                        Text(profile?.accountStatus ?: "Pending", color = VoltflowDesign.WarningAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.ManropeFont)
                    }
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = VoltflowDesign.GrayText)
                }
            }
        }

        item { SectionLabel("SECURITY") }
        item {
            VoltflowCard {
                Column {
                    VoltflowRow(icon = Icons.Outlined.Security, title = "Security Settings", hasChevron = true, onClick = onOpenSecurity)
                    VoltflowDivider()
                    VoltflowRow(icon = Icons.Outlined.Fingerprint, title = "Biometric Login", hasChevron = true, onClick = onOpenSecurity)
                    VoltflowDivider()
                    VoltflowRow(icon = Icons.Outlined.Devices, title = "Connected Devices", hasChevron = true, onClick = onOpenConnectedDevices)
                }
            }
        }

        item { SectionLabel("SUPPORT") }
        item {
            VoltflowCard {
                Column {
                    VoltflowRow(icon = Icons.Outlined.HelpOutline, title = "Help Center", hasChevron = true, onClick = onOpenHelpCenter)
                    VoltflowDivider()
                    VoltflowRow(icon = Icons.Outlined.Description, title = "Terms of Service", hasChevron = true, onClick = onOpenTerms)
                    VoltflowDivider()
                    VoltflowRow(icon = Icons.Outlined.Email, title = "Contact Us", hasChevron = true, onClick = onOpenContact)
                }
            }
        }

        item {
            VoltflowCard {
                Row(modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = textColor)
                    Spacer(Modifier.width(16.dp))
                    Text("Dark Mode", color = textColor, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, fontFamily = VoltflowDesign.ManropeFont)
                    Switch(checked = darkModeEnabled, onCheckedChange = onToggleDarkMode, colors = SwitchDefaults.colors(checkedTrackColor = VoltflowDesign.BlueAccent))
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(20.dp)).clickable { showLogoutDialog = true },
                color = VoltflowDesign.DestructiveBg
            ) {
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = VoltflowDesign.DestructiveRed)
                    Spacer(Modifier.width(12.dp))
                    Text("Log Out", color = VoltflowDesign.DestructiveRed, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("VoltFlow v1.0.0", color = VoltflowDesign.GrayText, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontFamily = VoltflowDesign.ManropeFont)
        }
    }

    if (showLogoutDialog) {
        VoltflowLogoutDialog(onDismiss = { showLogoutDialog = false }, onConfirm = { onSignOut(); showLogoutDialog = false })
    }
}

// Design Components

@Composable
fun VoltflowCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        color = VoltflowDesign.CardBg,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        content()
    }
}

@Composable
fun VoltflowRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    hasChevron: Boolean = false,
    hasToggle: Boolean = false,
    checked: Boolean = false,
    onToggle: (Boolean) -> Unit = {},
    actionText: String? = null,
    onAction: () -> Unit = {},
    isDropdown: Boolean = false
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth().height(72.dp).clickable(enabled = onClick != null) { onClick?.invoke() }.padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = VoltflowDesign.SoraFont)
            if (subtitle != null) Text(subtitle, color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
        }
        if (hasToggle) {
            Switch(checked = checked, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedTrackColor = VoltflowDesign.BlueAccent))
        } else if (actionText != null) {
            Text(actionText, color = VoltflowDesign.BlueAccent, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.ManropeFont, modifier = Modifier.clickable { onAction() })
        } else if (isDropdown) {
            Surface(shape = RoundedCornerShape(12.dp), color = VoltflowDesign.IconCircleBg) {
                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(description ?: "", color = textColor, fontSize = 14.sp, fontFamily = VoltflowDesign.ManropeFont)
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = VoltflowDesign.GrayText, modifier = Modifier.size(18.dp))
                }
            }
        } else if (hasChevron || onClick != null) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = VoltflowDesign.GrayText)
        }
    }
}

@Composable
fun VoltflowDivider() {
    HorizontalDivider(color = VoltflowDesign.DividerColor, thickness = 1.dp, modifier = Modifier.padding(horizontal = 20.dp))
}

@Composable
fun VoltflowHeader(title: String, subtitle: String, onBack: () -> Unit) {
    val textColor = MaterialTheme.colorScheme.onSurface
    Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(44.dp).clip(CircleShape).background(VoltflowDesign.HeaderCircle)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = textColor)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            Text(subtitle, color = VoltflowDesign.GrayText, fontSize = 14.sp, fontFamily = VoltflowDesign.ManropeFont)
        }
    }
}

@Composable
fun VoltflowButton(text: String, icon: ImageVector? = null, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(VoltflowDesign.PrimaryGradient), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp, fontFamily = VoltflowDesign.SoraFont)
                if (icon != null) {
                    Spacer(Modifier.width(10.dp))
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun VoltflowOutlinedButton(text: String, icon: ImageVector? = null, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, VoltflowDesign.BlueAccent),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = VoltflowDesign.BlueAccent)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(text, fontWeight = FontWeight.Bold, fontSize = 17.sp, fontFamily = VoltflowDesign.SoraFont)
        }
    }
}

@Composable
fun VoltflowInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: () -> Unit = {},
    leadingIcon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(16.dp)),
        placeholder = { Text(placeholder, color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = VoltflowDesign.InputBg,
            unfocusedContainerColor = VoltflowDesign.InputBg,
            focusedIndicatorColor = VoltflowDesign.BlueAccent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = VoltflowDesign.BlueAccent
        ),
        textStyle = LocalTextStyle.current.copy(fontFamily = VoltflowDesign.ManropeFont, fontSize = 16.sp),
        visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        trailingIcon = {
            if (isPassword) {
                Text("Show", color = VoltflowDesign.BlueAccent, modifier = Modifier.padding(end = 16.dp).clickable { onTogglePassword() }, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.ManropeFont)
            }
        },
        leadingIcon = if (leadingIcon != null) { { Icon(leadingIcon, contentDescription = null, tint = VoltflowDesign.GrayText) } } else null,
    )
}

@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    val textColor = MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(16.dp)),
        color = VoltflowDesign.CardBgSolid,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.SignalWifiOff, contentDescription = null, tint = VoltflowDesign.WarningAmber, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("You're offline", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = VoltflowDesign.ManropeFont)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoltflowReloadDialog(onReload: () -> Unit, onCancel: () -> Unit) {
    val textColor = MaterialTheme.colorScheme.onSurface
    ModalBottomSheet(
        onDismissRequest = onCancel,
        containerColor = VoltflowDesign.ModalBg,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Session taking longer",
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontFamily = VoltflowDesign.SoraFont,
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = VoltflowDesign.GrayText,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onCancel() },
                )
            }
            Text(
                "We're taking longer than expected. Reload session?",
                color = VoltflowDesign.GrayText,
                fontFamily = VoltflowDesign.ManropeFont
            )
            Button(
                onClick = onReload,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.BlueAccent)
            ) {
                Text("Reload", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            }
            TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Cancel", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun VoltflowPinFallbackOverlay(
    lockoutRemainingMillis: Long,
    attemptsRemaining: Int,
    inlineError: String?,
    onVerifyPin: suspend (String) -> PinVerificationResult,
    onAuthenticated: () -> Unit,
    onDismiss: () -> Unit,
    onAuthState: (attemptsRemaining: Int, lockoutRemainingMillis: Long, message: String?) -> Unit,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            shape = RoundedCornerShape(28.dp),
            color = VoltflowDesign.ModalBg,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = textColor, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Enter PIN", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                Spacer(Modifier.height(8.dp))
                Text("Enter your 6-digit PIN to continue", color = VoltflowDesign.GrayText, fontSize = 14.sp, textAlign = TextAlign.Center, fontFamily = VoltflowDesign.ManropeFont)
                Spacer(Modifier.height(16.dp))

                if (lockoutRemainingMillis > 0L) {
                    Text(
                        "Too many attempts. Try again in ${formatLockout(lockoutRemainingMillis)}",
                        color = VoltflowDesign.DestructiveRed,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontFamily = VoltflowDesign.ManropeFont
                    )
                } else {
                    BasicTextField(
                        value = pin,
                        onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(6) },
                        textStyle = LocalTextStyle.current.copy(color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont),
                        visualTransformation = PasswordVisualTransformation(mask = '•'),
                        cursorBrush = Brush.verticalGradient(listOf(VoltflowDesign.BlueAccent, VoltflowDesign.BlueAccent)),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(VoltflowDesign.InputBg)
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (pin.isBlank()) {
                                    Text("Enter PIN", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont)
                                }
                                inner()
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$attemptsRemaining attempts remaining",
                        color = VoltflowDesign.GrayText,
                        fontSize = 12.sp,
                        fontFamily = VoltflowDesign.ManropeFont
                    )
                }

                (inlineError ?: localError)?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = VoltflowDesign.DestructiveRed, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
                }

                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Text("Cancel", color = textColor, fontFamily = VoltflowDesign.SoraFont)
                    }
                    Button(
                        onClick = {
                            if (lockoutRemainingMillis > 0L) return@Button
                            scope.launch {
                                when (val result = onVerifyPin(pin)) {
                                    is PinVerificationResult.Success -> onAuthenticated()
                                    is PinVerificationResult.Failed -> {
                                        onAuthState(result.attemptsRemaining, 0L, "Incorrect PIN")
                                        localError = "Incorrect PIN"
                                    }
                                    is PinVerificationResult.Locked -> {
                                        onAuthState(0, result.remainingMillis, "Too many attempts. Try again in ${formatLockout(result.remainingMillis)}")
                                        localError = "Too many attempts"
                                    }
                                    is PinVerificationResult.Error -> {
                                        onAuthState(attemptsRemaining, 0L, result.message)
                                        localError = result.message
                                    }
                                }
                                pin = ""
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.BlueAccent)
                    ) {
                        Text("Continue", color = MaterialTheme.colorScheme.onPrimary, fontFamily = VoltflowDesign.SoraFont)
                    }
                }
            }
        }
    }
}

private fun formatLockout(remainingMillis: Long): String {
    val total = (remainingMillis / 1000L).coerceAtLeast(0L)
    val min = total / 60
    val sec = total % 60
    return String.format("%d:%02d", min, sec)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoltflowLogoutDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val textColor = MaterialTheme.colorScheme.onSurface
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VoltflowDesign.ModalBg,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = VoltflowDesign.GrayText,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onDismiss() },
                )
            }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(VoltflowDesign.DestructiveBg),
                contentAlignment = Alignment.Center
            ) {
                Text("!", color = VoltflowDesign.DestructiveRed, fontSize = 36.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont)
            }
            Text("Logout", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            Text(
                "Are you sure you want to logout?",
                color = VoltflowDesign.GrayText,
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontFamily = VoltflowDesign.ManropeFont
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.IconCircleBg)
                ) {
                    Text("Cancel", color = textColor, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.DestructiveRed)
                ) {
                    Text("Logout", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        color = VoltflowDesign.GrayText,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.5.sp,
        fontFamily = VoltflowDesign.SoraFont,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun ActionChip(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val textColor = MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier.height(60.dp).clickable { onClick() },
        color = VoltflowDesign.CardBg,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = VoltflowDesign.BlueAccent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = VoltflowDesign.ManropeFont)
        }
    }
}

private data class NavItem(val label: String, val screen: Screen, val icon: ImageVector)

private fun performPlatformHaptic(view: android.view.View, constant: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        view.performHapticFeedback(constant)
    } else {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }
}



