package com.example.voltflow.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voltflow.MainViewModel
import com.example.voltflow.Screen
import com.example.voltflow.data.*
import com.example.voltflow.rememberAppNavigator
import com.example.voltflow.screenFromRoute
import com.example.voltflow.ui.screens.*
import com.example.voltflow.R
import com.example.voltflow.bouncyClick
import com.example.voltflow.errorShake
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.voltflow.notifications.NotificationHelper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
    initialDeepLink: String? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    if (!state.isAuthReady) {
        Box(modifier = Modifier.fillMaxSize().background(VoltflowDesign.BgTop))
        return
    }
    val navigator = rememberAppNavigator(if (state.isAuthenticated) Screen.Home else Screen.Auth)
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
    var isAppInBackground by remember { mutableStateOf(false) }

    LaunchedEffect(initialDeepLink) {
        if (initialDeepLink != null) {
            navigator.navigate(Screen.ResetPassword)
            onDeepLinkConsumed()
        }
    }

    LaunchedEffect(state.isAuthenticated) {
        if (!state.isAuthenticated) {
            navigator.replaceRoot(Screen.Auth)
            return@LaunchedEffect
        }
        if (navigator.currentScreen == Screen.Auth) {
            userId?.let {
                val restoredRoute = navStateStore.readRoute(it)
                val restored = screenFromRoute(restoredRoute)?.takeIf { s -> s != Screen.Auth } ?: Screen.Home
                navigator.replaceRoot(restored)
            }
        }
    }

    LaunchedEffect(state.activeError) {
        state.activeError?.let { snackbarHostState.showSnackbar(it); viewModel.consumeError() }
    }
    LaunchedEffect(Unit) {
        notificationHelper.ensureChannels()
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler(enabled = navigator.canPop) {
        navigator.pop()
        if (!userId.isNullOrBlank()) {
            scope.launch { navStateStore.saveRoute(userId, navigator.currentScreen.route) }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && state.isAuthenticated) {
                isAppInBackground = true
            }
            if (event == Lifecycle.Event.ON_RESUME) {
                isAppInBackground = false
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
            // Point 11 & Nav Entry Fix: Hide during loading, slide up when ready
            AnimatedVisibility(
                visible = state.isAuthenticated && !state.isLoading && navigator.currentScreen.isMainTab(),
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)),
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
            } else if (state.isDegraded) {
                DegradedModeBanner(modifier = Modifier.align(Alignment.TopCenter))
            }
            if (navigator.currentScreen == Screen.Auth) {
                AuthBloom()
            }
            AnimatedContent(
                targetState = navigator.currentScreen,
                transitionSpec = {
                    if (targetState.isMainTab() && initialState.isMainTab()) {
                        val isForward = targetState.index > initialState.index
                        if (isForward) {
                            (slideInHorizontally { it } + fadeIn(tween(400)))
                                .togetherWith(slideOutHorizontally { -it } + fadeOut(tween(400)))
                        } else {
                            (slideInHorizontally { -it } + fadeIn(tween(400)))
                                .togetherWith(slideOutHorizontally { it } + fadeOut(tween(400)))
                        }
                    } else {
                        fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                    }
                },
                label = "voltflow-screen"
            ) { screen ->
                when (screen) {
                    Screen.Auth -> AuthScreen(
                        state,
                        darkModeEnabled,
                        onToggleDarkMode,
                        viewModel::signIn,
                        viewModel::signUp,
                        onSuccess = {
                            // Point 11: Always go to Home after login
                            navigator.replaceRoot(Screen.Home)
                        }
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
                        onPay = { utility, amount, meterNumber, method, useWallet, onResult ->
                            viewModel.payUtility(
                                PaymentDraft(utility, amount, meterNumber, method, useWallet),
                                onResult
                            )
                        },
                        onBack = { navigator.pop() },
                    )
                    Screen.Wallet -> WalletScreen(
                        state = state,
                        innerPadding = innerPadding,
                        hazeState = hazeState,
                        onBack = { navigator.pop() },
                        onFund = { amount ->
                            viewModel.fundWallet(amount)
                        },
                        onWithdraw = { amount, method, onDone ->
                            viewModel.withdrawWallet(amount, method)
                            onDone()
                        },
                    )
                    Screen.History -> HistoryScreen(state, innerPadding, onBack = { navigator.pop() })
                    Screen.Notifications -> NotificationsScreen(
                        state,
                        innerPadding,
                        onBack = { navigator.pop() },
                        onMarkRead = viewModel::markNotificationRead,
                    )
                    Screen.Usage -> UsageScreen(viewModel, onBack = { navigator.pop() })
                    Screen.AutoPay -> AutoPayScreen(
                        state = state,
                        innerPadding = innerPadding,
                        onSetAutopay = { enabled, paymentMethodId, amountLimit, billingCycle, paymentDay, meterNumber ->
                            viewModel.setAutopay(enabled, paymentMethodId, amountLimit, billingCycle, paymentDay, meterNumber)
                        },
                        onBack = { navigator.pop() }
                    )
                    Screen.PaymentMethods -> PaymentMethodsScreen(
                        state = state,
                        innerPadding = innerPadding,
                        onBack = { navigator.pop() },
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
                        onOpenAutopay = { navigator.navigate(Screen.AutoPay) },
                        onOpenNotifications = { navigator.navigate(Screen.Notifications) },
                        onOpenConnectedDevices = { navigator.navigate(Screen.ConnectedDevices) },
                        onOpenHelpCenter = { navigator.navigate(Screen.HelpCenter) },
                        onOpenTerms = { navigator.navigate(Screen.Terms) },
                        onOpenContact = { navigator.navigate(Screen.Contact) },
                        onSignOut = { viewModel.signOut() },
                    )
                    Screen.ProfileOptions -> ProfileOptionsScreen(state = state, innerPadding = innerPadding, onBack = { navigator.pop() }, onSaveProfile = viewModel::saveProfile)
                    Screen.HelpCenter -> HelpCenterScreen(innerPadding = innerPadding, onBack = { navigator.pop() })
                    Screen.Terms -> TermsScreen(innerPadding = innerPadding, onBack = { navigator.pop() })
                    Screen.Contact -> ContactScreen(innerPadding = innerPadding, onBack = { navigator.pop() })
                    Screen.TransactionReceipt -> TransactionReceiptScreen(state = state, innerPadding = innerPadding, onDone = { navigator.replaceRoot(Screen.Home) })
                    Screen.ResetPassword -> ResetPasswordScreen(
                        onReset = { pwd ->
                            viewModel.updatePassword(pwd)
                            navigator.replaceRoot(Screen.Auth)
                        },
                        onBack = { navigator.pop() }
                    )
                }
            }
            
            // PRO UI: Background Privacy Blur
            if (isAppInBackground) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .blur(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        VoltflowLogo(size = 80.dp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "SECURE SESSION",
                            color = Color.White.copy(alpha = 0.7f),
                            letterSpacing = 4.sp,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = VoltflowDesign.SoraFont
                        )
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
    onSuccess: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var firstName by rememberSaveable { mutableStateOf("") } 
    var lastName by rememberSaveable { mutableStateOf("") } 
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var isSignup by rememberSaveable { mutableStateOf(false) }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    
    // Point 3: Specific field error triggers
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    
    val textColor = MaterialTheme.colorScheme.onSurface

    // Point 10: Auth loading overlay
    if (state.isLoading && !state.isAuthenticated) {
        Box(modifier = Modifier.fillMaxSize().background(VoltflowDesign.BgTop), contentAlignment = Alignment.Center) {
            LogoLoadingIndicator()
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 32.dp).imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        VoltflowLogo(size = 72.dp)
        Spacer(Modifier.height(12.dp))
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

                    VoltflowInput(
                        value = email, 
                        onValueChange = { 
                            email = it
                            emailError = false
                        }, 
                        placeholder = "Email",
                        modifier = Modifier.errorShake(emailError)
                    )
                    Spacer(Modifier.height(18.dp))
                    VoltflowInput(
                        value = password,
                        onValueChange = { 
                            password = it
                            passwordError = false
                        },
                        placeholder = "Password",
                        isPassword = true,
                        showPassword = showPassword,
                        onTogglePassword = { showPassword = !showPassword },
                        modifier = Modifier.errorShake(passwordError)
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

                        emailError = trimmedEmail.isBlank() || !validEmail
                        passwordError = trimmedPassword.isBlank() || (!isSignup && !passwordValid)

                        val validationError = when {
                            emailError -> "Enter a valid email address."
                            passwordError -> "Password must be at least 8 characters."
                            isSignup && firstName.trim().isBlank() -> "First name is required."
                            isSignup && lastName.trim().isBlank() -> "Last name is required."
                            isSignup && confirmPassword.trim() != trimmedPassword -> "Passwords do not match."
                            else -> null
                        }

                        if (validationError != null) {
                            formError = validationError
                            return@VoltflowButton
                        }
                        formError = null
                        if (isSignup) {
                            onSignUp(trimmedEmail, trimmedPassword, firstName.trim(), lastName.trim())
                        } else {
                            onSignIn(trimmedEmail, trimmedPassword)
                        }
                    }
                    formError?.let {
                        Spacer(Modifier.height(16.dp))
                        Text(it, color = VoltflowDesign.WarningAmber, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont, modifier = Modifier.errorShake(formError))
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
                            formError = null
                            emailError = false
                            passwordError = false
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
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                    label = "nav-indicator"
                )

                // Neon Glow for active tab
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset - 10.dp, y = 8.dp)
                        .size(24.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(VoltflowDesign.BlueAccent.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                        .blur(6.dp)
                )

                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset, y = 12.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(VoltflowDesign.BlueAccent)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
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
                                .bouncyClick {
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
                                    // Point 13: Profile Dot removed, Unread Badge ONLY
                                    if (hasUnread && item.screen == Screen.Profile) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                                .padding(1.dp)
                                                .clip(CircleShape)
                                                .background(VoltflowDesign.BlueAccent)
                                                .align(Alignment.TopEnd)
                                        )
                                    }
                                }
                                Text(
                                    item.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) VoltflowDesign.BlueAccent else VoltflowDesign.GrayText,
                                    fontFamily = VoltflowDesign.ManropeFont
                                )
                            }
                        }
                    }
                }
            }
        }
    }
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
                    modifier = Modifier.weight(1f).height(52.dp).bouncyClick { onDismiss() },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.IconCircleBg)
                ) {
                    Text("Cancel", color = textColor, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).height(52.dp).bouncyClick { onConfirm() },
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
