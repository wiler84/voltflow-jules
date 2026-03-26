package com.example.voltflow.ui

import androidx.activity.compose.BackHandler
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.input.pointer.pointerInput


object VoltflowDesign {
    val BgTop = Color(0xFF0F172A)
    val BgBottom = Color(0xFF020617)
    val CardBg = Color(0xFF1E293B).copy(alpha = 0.6f)
    val CardBgSolid = Color(0xFF1E293B)
    val IconCircleBg = Color(0xFF334155).copy(alpha = 0.5f)
    val GrayText = Color(0xFF94A3B8)
    val BlueAccent = Color(0xFF3B82F6)
    val DestructiveRed = Color(0xFFEF4444)
    val DestructiveBg = Color(0x1AEF4444)
    val HeaderCircle = Color(0xFF1E293B)
    val BottomNavBg = Color(0xFF1E293B).copy(alpha = 0.7f)
    val WarningAmber = Color(0xFFF59E0B)
    val InputBg = Color(0xFF0F172A).copy(alpha = 0.5f)
    val ModalBg = Color(0xFF0F172A)
    val DividerColor = Color(0x1FFFFFFF)

    val PrimaryGradient = Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF2563EB)))
    val LogoGradient = Brush.verticalGradient(listOf(Color(0xFF60A5FA), Color(0xFF3B82F6)))
    
    val SoraFont = FontFamily(
        Font(R.font.sora_wght, weight = FontWeight.Normal),
        Font(R.font.sora_wght, weight = FontWeight.SemiBold),
        Font(R.font.sora_wght, weight = FontWeight.Bold),
        Font(R.font.sora_wght, weight = FontWeight.Black)
    )
    
    val ManropeFont = FontFamily(
        Font(R.font.manrope_wght, weight = FontWeight.Normal),
        Font(R.font.manrope_wght, weight = FontWeight.Medium),
        Font(R.font.manrope_wght, weight = FontWeight.SemiBold),
        Font(R.font.manrope_wght, weight = FontWeight.Bold)
    )

    val AppFont = SoraFont
    val BodyFont = ManropeFont
}

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
    val navStateStore = remember { NavStateStore(context) }
    val prefs = remember { UserPreferencesStore(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val userId = state.dashboard.profile?.userId
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPin by remember { mutableStateOf(false) }
    var lastInteraction by remember { mutableStateOf(System.currentTimeMillis()) }
    var showLock by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var lockMessage by remember { mutableStateOf("Unlock Voltflow") }
    var showReloadDialog by remember { mutableStateOf(false) }
    val biometricManager = remember { BiometricManager.from(context) }
    val biometricAvailable = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    val biometricEnabled = state.dashboard.securitySettings?.biometricEnabled == true

    val requireUnlock: (String, () -> Unit) -> Unit = { message, action ->
        val lockConfigured = biometricEnabled || hasPin
        if (!lockConfigured) {
            action()
        } else {
            lockMessage = message
            pendingAction = action
            showLock = true
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

    BackHandler(enabled = navigator.canPop) {
        navigator.pop()
        if (!userId.isNullOrBlank()) {
            scope.launch { navStateStore.saveRoute(userId, navigator.currentScreen.route) }
        }
    }

    LaunchedEffect(Unit) {
        hasPin = prefs.hasPin()
        prefs.lastInteractionFlow.collect { stored ->
            stored?.let { lastInteraction = it }
        }
    }

    LaunchedEffect(state.dashboard.securitySettings?.pinEnabled) {
        hasPin = prefs.hasPin()
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

    DisposableEffect(lifecycleOwner, state.dashboard.securitySettings, hasPin) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val autoLockMinutes = state.dashboard.securitySettings?.autoLockMinutes ?: 0
                val lockEnabled = autoLockMinutes > 0 && (state.dashboard.securitySettings?.biometricEnabled == true || hasPin)
                if (lockEnabled) {
                    val now = System.currentTimeMillis()
                    val elapsed = now - lastInteraction
                    if (elapsed > autoLockMinutes * 60_000L) {
                        lockMessage = "Unlock Voltflow"
                        showLock = true
                    }
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
                    hasUnread = state.dashboard.notifications.any { !it.isRead }
                ) { screen ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                .pointerInput(Unit) { /* interaction tracking removed */ }
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
                            requireUnlock("Confirm payment") {
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
                        onFund = viewModel::fundWallet,
                        onWithdraw = { amount, onDone ->
                            requireUnlock("Confirm withdrawal") {
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
                    Screen.AutoPay -> AutoPayScreen(state, innerPadding, viewModel::setAutopay, onBack = { navigator.pop() })
                    Screen.PaymentMethods -> PaymentMethodsScreen(
                        state,
                        innerPadding,
                        onBack = { navigator.pop() },
                        onEnableMfa = viewModel::setMfaEnabled,
                        onAdd = viewModel::addPaymentMethod,
                    )
                    Screen.ConnectedDevices -> ConnectedDevicesScreen(state, innerPadding, onBack = { navigator.pop() }, onRevoke = viewModel::revokeDevice)
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
                    Screen.SecuritySettings -> SecuritySettingsScreen(state = state, innerPadding = innerPadding, onBack = { navigator.pop() }, onToggleBiometric = viewModel::setBiometricEnabled, onToggleMfa = viewModel::setMfaEnabled, onSetPin = { viewModel.setPinEnabled(true) }, onAutoLockChange = viewModel::setAutoLockMinutes)
                    Screen.HelpCenter -> HelpCenterScreen(innerPadding = innerPadding, onBack = { navigator.pop() })
                    Screen.Terms -> TermsScreen(innerPadding = innerPadding, onBack = { navigator.pop() })
                    Screen.Contact -> ContactScreen(innerPadding = innerPadding, onBack = { navigator.pop() })
                }
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
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

            if (showLock) {
                VoltflowLockOverlay(
                    message = lockMessage,
                    biometricEnabled = biometricEnabled,
                    biometricAvailable = biometricAvailable,
                    hasPin = hasPin,
                    onUnlock = {
                        showLock = false
                        val action = pendingAction
                        pendingAction = null
                        val now = System.currentTimeMillis()
                        lastInteraction = now
                        scope.launch { prefs.setLastInteraction(now) }
                        action?.invoke()
                    },
                    onCancel = {
                        showLock = false
                        pendingAction = null
                    }
                )
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

@Composable
private fun VoltflowBottomNav(current: Screen, hasUnread: Boolean, onNavigate: (Screen) -> Unit) {
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
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            shadowElevation = 18.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(36.dp))
                        .background(VoltflowDesign.BottomNavBg)
                        .blur(22.dp)
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
                                ) { onNavigate(item.screen) },
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
                drawPath(path, brush = VoltflowDesign.LogoGradient)
            }
        }
        Text(
            "VOLTFLOW",
            color = Color.White,
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
                color = Color.Black.copy(alpha = 0.2f)
            ) {}

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = VoltflowDesign.CardBg,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(28.dp)) {
                    Text(
                        if (isSignup) "Create account" else "Welcome back",
                        color = Color.White,
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
                    checkedThumbColor = Color.White,
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

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = innerPadding.calculateTopPadding() + 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Profile", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
        }
        item {
            VoltflowCard(modifier = Modifier.fillMaxWidth().clickable { onOpenProfileOptions() }) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = VoltflowDesign.BlueAccent) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.background(VoltflowDesign.PrimaryGradient)) {
                            Text(displayName.take(1).uppercase(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(displayName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
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
                    Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(16.dp))
                    Text("Dark Mode", color = Color.White, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, fontFamily = VoltflowDesign.ManropeFont)
                    Switch(checked = darkModeEnabled, onCheckedChange = onToggleDarkMode, colors = SwitchDefaults.colors(checkedTrackColor = VoltflowDesign.BlueAccent))
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(20.dp)).clickable { showLogoutDialog = true },
                color = Color(0x1AEF4444)
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
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
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
    Row(
        modifier = Modifier.fillMaxWidth().height(72.dp).clickable(enabled = onClick != null) { onClick?.invoke() }.padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = VoltflowDesign.SoraFont)
            if (subtitle != null) Text(subtitle, color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
        }
        if (hasToggle) {
            Switch(checked = checked, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedTrackColor = VoltflowDesign.BlueAccent))
        } else if (actionText != null) {
            Text(actionText, color = VoltflowDesign.BlueAccent, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.ManropeFont, modifier = Modifier.clickable { onAction() })
        } else if (isDropdown) {
            Surface(shape = RoundedCornerShape(12.dp), color = VoltflowDesign.IconCircleBg) {
                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(description ?: "", color = Color.White, fontSize = 14.sp, fontFamily = VoltflowDesign.ManropeFont)
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
    Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(44.dp).clip(CircleShape).background(VoltflowDesign.HeaderCircle)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
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
                Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, fontFamily = VoltflowDesign.SoraFont)
                if (icon != null) {
                    Spacer(Modifier.width(10.dp))
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
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
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
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
    Surface(
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(16.dp)),
        color = Color(0xFF1F2937),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.SignalWifiOff, contentDescription = null, tint = VoltflowDesign.WarningAmber, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("You're offline", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = VoltflowDesign.ManropeFont)
        }
    }
}

@Composable
private fun VoltflowReloadDialog(onReload: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = VoltflowDesign.ModalBg,
        modifier = Modifier.clip(RoundedCornerShape(28.dp)),
        title = {
            Text(
                "Session taking longer",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = VoltflowDesign.SoraFont
            )
        },
        text = {
            Text(
                "We're taking longer than expected. Reload session?",
                color = VoltflowDesign.GrayText,
                fontFamily = VoltflowDesign.ManropeFont
            )
        },
        confirmButton = {
            Button(
                onClick = onReload,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.BlueAccent)
            ) {
                Text("Reload", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont)
            }
        }
    )
}

@Composable
private fun VoltflowLockOverlay(
    message: String,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    hasPin: Boolean,
    onUnlock: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val executor = remember { ContextCompat.getMainExecutor(context) }
    val prefs = remember { UserPreferencesStore(context) }
    val scope = rememberCoroutineScope()
    var usePin by remember { mutableStateOf(!biometricEnabled || !biometricAvailable) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val prompt = remember(activity) {
        if (activity == null) null else BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlock()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    error = errString.toString()
                    usePin = true
                }

                override fun onAuthenticationFailed() {
                    error = "Authentication failed"
                    usePin = true
                }
            }
        )
    }

    LaunchedEffect(biometricEnabled, biometricAvailable, usePin) {
        if (biometricEnabled && biometricAvailable && !usePin) {
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Voltflow")
                .setSubtitle("Verify your identity")
                .setConfirmationRequired(false)
                .setNegativeButtonText("Use PIN")
                .build()
            prompt?.authenticate(info)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            shape = RoundedCornerShape(28.dp),
            color = VoltflowDesign.ModalBg,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(message, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                Spacer(Modifier.height(8.dp))
                Text("Enter your PIN to continue", color = VoltflowDesign.GrayText, fontSize = 14.sp, textAlign = TextAlign.Center, fontFamily = VoltflowDesign.ManropeFont)
                Spacer(Modifier.height(16.dp))

                if (hasPin) {
                    BasicTextField(
                        value = pin,
                        onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(6) },
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont),
                        cursorBrush = Brush.verticalGradient(listOf(VoltflowDesign.BlueAccent, VoltflowDesign.BlueAccent)),
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
                } else {
                    Text("Set up an app PIN in Security Settings.", color = VoltflowDesign.WarningAmber, fontSize = 13.sp, textAlign = TextAlign.Center, fontFamily = VoltflowDesign.ManropeFont)
                }

                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = VoltflowDesign.WarningAmber, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
                }

                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text("Cancel", color = Color.White, fontFamily = VoltflowDesign.SoraFont)
                    }
                    Button(
                        onClick = {
                            if (!hasPin) return@Button
                            scope.launch {
                                val ok = prefs.verifyPin(pin)
                                if (ok) {
                                    onUnlock()
                                } else {
                                    error = "Incorrect PIN"
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.BlueAccent)
                    ) {
                        Text("Unlock", color = Color.White, fontFamily = VoltflowDesign.SoraFont)
                    }
                }
                if (biometricEnabled && biometricAvailable) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Use biometrics",
                        color = VoltflowDesign.BlueAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = VoltflowDesign.ManropeFont,
                        modifier = Modifier.clickable {
                            error = null
                            usePin = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun VoltflowLogoutDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VoltflowDesign.ModalBg,
        modifier = Modifier.clip(RoundedCornerShape(28.dp)),
        title = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(VoltflowDesign.DestructiveBg), contentAlignment = Alignment.Center) {
                    Text("!", color = VoltflowDesign.DestructiveRed, fontSize = 36.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont)
                }
                Spacer(Modifier.height(20.dp))
                Text("Logout", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            }
        },
        text = {
            Text("Are you sure you want to logout?", color = VoltflowDesign.GrayText, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 16.sp, fontFamily = VoltflowDesign.ManropeFont)
        },
        confirmButton = {
            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(26.dp), colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.DestructiveRed)) {
                Text("Logout", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            }
        },
        dismissButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(26.dp), colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.IconCircleBg)) {
                Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            }
        }
    )
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
    Surface(
        modifier = modifier.height(60.dp).clickable { onClick() },
        color = VoltflowDesign.CardBg,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = VoltflowDesign.BlueAccent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = VoltflowDesign.ManropeFont)
        }
    }
}

private data class NavItem(val label: String, val screen: Screen, val icon: ImageVector)



