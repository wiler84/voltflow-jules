package com.example.voltflow.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    
    val AppFont = FontFamily.SansSerif
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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val userId = state.dashboard.profile?.userId

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
                VoltflowBottomNav(current = navigator.currentScreen) { screen ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    navigator.switchTab(screen)
                    if (!userId.isNullOrBlank()) {
                        scope.launch { navStateStore.saveRoute(userId, screen.route) }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(VoltflowDesign.BgTop, VoltflowDesign.BgBottom)))) {
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
                    Screen.Home -> HomeScreen(state, innerPadding, { navigator.navigate(Screen.Notifications) }, { navigator.navigate(Screen.Usage) }, { navigator.navigate(Screen.Pay) }, { navigator.navigate(Screen.History) }, { navigator.navigate(Screen.AutoPay) }, { navigator.navigate(Screen.PaymentMethods) }, viewModel::refresh)
                    Screen.Pay -> PayScreen(state, innerPadding, viewModel::payUtility)
                    Screen.Wallet -> WalletScreen(state, innerPadding, onBack = { navigator.pop() }, onFund = viewModel::fundWallet)
                    Screen.History -> HistoryScreen(state, innerPadding)
                    Screen.Notifications -> NotificationsScreen(state, innerPadding)
                    Screen.Usage -> UsageScreen(state, innerPadding)
                    Screen.AutoPay -> AutoPayScreen(state, innerPadding, viewModel::setAutopay)
                    Screen.PaymentMethods -> PaymentMethodsScreen(state, innerPadding, onBack = { navigator.pop() }, onEnableMfa = viewModel::setMfaEnabled, onAdd = viewModel::addPaymentMethod)
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
private fun VoltflowBottomNav(current: Screen, onNavigate: (Screen) -> Unit) {
    val items = listOf(
        NavItem("Home", Screen.Home, Icons.Outlined.Home),
        NavItem("Pay", Screen.Pay, Icons.Outlined.CreditCard),
        NavItem("History", Screen.History, Icons.Outlined.History),
        NavItem("Profile", Screen.Profile, Icons.Outlined.Person),
    )
    
    val selectedIndex = items.indexOfFirst { it.screen == current }.coerceAtLeast(0)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .height(72.dp),
            shape = RoundedCornerShape(36.dp),
            color = VoltflowDesign.BottomNavBg,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            shadowElevation = 16.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val itemWidth = 320.dp / items.size
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
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (selected) VoltflowDesign.BlueAccent else VoltflowDesign.GrayText,
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    item.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) VoltflowDesign.BlueAccent else VoltflowDesign.GrayText
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
    var isSignup by rememberSaveable { mutableStateOf(false) }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.size(64.dp).padding(bottom = 16.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    moveTo(size.width * 0.5f, 0f)
                    lineTo(size.width * 0.2f, size.height * 0.6f)
                    lineTo(size.width * 0.45f, size.height * 0.6f)
                    lineTo(size.width * 0.35f, size.height)
                    lineTo(size.width * 0.8f, size.height * 0.4f)
                    lineTo(size.width * 0.55f, size.height * 0.4f)
                    close()
                }
                drawPath(path, brush = VoltflowDesign.LogoGradient)
            }
        }
        Text(
            "VOLTFLOW",
            color = Color.White,
            letterSpacing = 6.sp,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            fontFamily = VoltflowDesign.AppFont
        )
        Spacer(Modifier.height(48.dp))

        Surface(
            modifier = Modifier.fillMaxWidth().blur(15.dp),
            shape = RoundedCornerShape(28.dp),
            color = VoltflowDesign.CardBg,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(28.dp)) {
                Text(
                    if (isSignup) "Create account" else "Welcome back",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = VoltflowDesign.AppFont
                )
                Text(
                    if (isSignup) "Get started in minutes" else "Sign in to continue",
                    color = VoltflowDesign.GrayText,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(32.dp))

                VoltflowInput(value = email, onValueChange = { email = it }, placeholder = "Email")
                Spacer(Modifier.height(16.dp))
                VoltflowInput(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Password",
                    isPassword = true,
                    showPassword = showPassword,
                    onTogglePassword = { showPassword = !showPassword }
                )
                Spacer(Modifier.height(32.dp))

                VoltflowButton(text = if (isSignup) "Create account" else "Sign in") {
                    if (isSignup) onSignUp(email, password, "", "") else onSignIn(email, password)
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = if (isSignup) "Already have an account? Sign in" else "New here? Create an account",
                    color = VoltflowDesign.BlueAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { isSignup = !isSignup }.align(Alignment.CenterHorizontally)
                )
            }
        }
        
        Spacer(Modifier.height(80.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Dark mode", color = VoltflowDesign.GrayText, fontSize = 14.sp)
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
            Text("Profile", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        item {
            VoltflowCard(modifier = Modifier.fillMaxWidth().clickable { onOpenProfileOptions() }) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = VoltflowDesign.BlueAccent) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.background(VoltflowDesign.PrimaryGradient)) {
                            Text(displayName.take(1).uppercase(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(displayName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(profile?.email ?: "alex.johnson@email.com", color = VoltflowDesign.GrayText, fontSize = 14.sp)
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
                    Text("Dark Mode", color = Color.White, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
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
                    Text("Log Out", color = VoltflowDesign.DestructiveRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("VoltFlow v1.0.0", color = VoltflowDesign.GrayText, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
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
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, color = VoltflowDesign.GrayText, fontSize = 13.sp)
        }
        if (hasToggle) {
            Switch(checked = checked, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedTrackColor = VoltflowDesign.BlueAccent))
        } else if (actionText != null) {
            Text(actionText, color = VoltflowDesign.BlueAccent, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onAction() })
        } else if (isDropdown) {
            Surface(shape = RoundedCornerShape(12.dp), color = VoltflowDesign.IconCircleBg) {
                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(description ?: "", color = Color.White, fontSize = 14.sp)
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
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = VoltflowDesign.GrayText, fontSize = 14.sp)
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
                Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
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
            Text(text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
    }
}

@Composable
fun VoltflowInput(value: String, onValueChange: (String) -> Unit, placeholder: String, isPassword: Boolean = false, showPassword: Boolean = false, onTogglePassword: () -> Unit = {}, leadingIcon: ImageVector? = null) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(16.dp)),
        placeholder = { Text(placeholder, color = VoltflowDesign.GrayText) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = VoltflowDesign.InputBg,
            unfocusedContainerColor = VoltflowDesign.InputBg,
            focusedIndicatorColor = VoltflowDesign.BlueAccent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = VoltflowDesign.BlueAccent
        ),
        visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        trailingIcon = {
            if (isPassword) {
                Text("Show", color = VoltflowDesign.BlueAccent, modifier = Modifier.padding(end = 16.dp).clickable { onTogglePassword() }, fontWeight = FontWeight.Bold)
            }
        },
        leadingIcon = if (leadingIcon != null) { { Icon(leadingIcon, contentDescription = null, tint = VoltflowDesign.GrayText) } } else null
    )
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
                    Text("!", color = VoltflowDesign.DestructiveRed, fontSize = 36.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(20.dp))
                Text("Logout", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text("Are you sure you want to logout?", color = VoltflowDesign.GrayText, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 16.sp)
        },
        confirmButton = {
            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(26.dp), colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.DestructiveRed)) {
                Text("Logout", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(26.dp), colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.IconCircleBg)) {
                Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold)
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
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

private data class NavItem(val label: String, val screen: Screen, val icon: ImageVector)
