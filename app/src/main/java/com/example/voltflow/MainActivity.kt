package com.example.voltflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.voltflow.ui.theme.VoltflowTheme
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            VoltflowTheme {
                VoltflowApp()
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Pay : Screen("pay")
    object History : Screen("history")
    object Profile : Screen("profile")
    object Notifications : Screen("notifications")
    object Wallet : Screen("wallet")
}

@Composable
fun VoltflowApp() {
    val navigator = rememberAppNavigator()
    val currentScreen = navigator.currentScreen
    val navItems = listOf(
        AppDestinations.HOME to Screen.Home,
        AppDestinations.PAY to Screen.Pay,
        AppDestinations.HISTORY to Screen.History,
        AppDestinations.PROFILE to Screen.Profile
    )

    if (currentScreen !in listOf(Screen.Home, Screen.Pay, Screen.History, Screen.Profile)) {
        BackHandler {
            navigator.navigate(Screen.Home) // Simplified back logic
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val showNavBar = currentScreen in listOf(Screen.Home, Screen.Pay, Screen.History, Screen.Profile)

        NavigationSuiteScaffold(
            navigationSuiteItems = {
                if (showNavBar) {
                    navItems.forEach { (dest, screen) ->
                        val isSelected = currentScreen == screen
                        item(
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label
                                )
                            },
                            label = { Text(dest.label) },
                            selected = isSelected,
                            onClick = { navigator.navigate(screen) }
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    Screen.Home -> HomeScreen(
                        onNotifyClick = { navigator.navigate(Screen.Notifications) },
                        onWalletClick = { navigator.navigate(Screen.Wallet) }
                    )
                    Screen.Pay -> PayScreen(onPaySuccess = { navigator.navigate(Screen.Home) })
                    Screen.History -> HistoryScreen()
                    Screen.Profile -> ProfileScreen(onEditProfileClick = {}, onPaymentMethodsClick = {})
                    Screen.Notifications -> NotificationsScreen(onBack = { navigator.navigate(Screen.Home) })
                    Screen.Wallet -> WalletScreen(onBack = { navigator.navigate(Screen.Home) })
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    PAY("Pay", Icons.Filled.Payment, Icons.Outlined.Payment),
    HISTORY("History", Icons.Filled.History, Icons.Outlined.History),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person),
}


@Composable
fun HomeScreen(onNotifyClick: () -> Unit, onWalletClick: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = WindowInsets.systemBars.asPaddingValues()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Good morning", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Alex", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onNotifyClick) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        item { BillSummaryCard() }
        item { Spacer(Modifier.height(24.dp)) }
        item { WalletInfoSection(onWalletClick = onWalletClick) }
        item { Spacer(Modifier.height(24.dp)) }
        item { Text("Quick Actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item { Spacer(Modifier.height(80.dp)) } // Spacer for bottom nav
    }
}

@Composable
fun BillSummaryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = "Power", tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("City Power & Light", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Text("**** **** **** 4829", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                Surface(color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(12.dp)) {
                    Text("MTR-2847561", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("Current Balance", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$84.32", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                }
                Text("Due Jan 15, 2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { 0.68f },
                    modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.background
                )
                Spacer(Modifier.width(12.dp))
                Text("68%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Text("Usage this cycle", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Pay Now", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
fun WalletInfoSection(onWalletClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        InfoCard(
            modifier = Modifier.weight(1f),
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            title = "Last Payment",
            amount = "$127.45",
            subtitle = "Dec 28, 2025",
            iconBackgroundColor = Color(0xFF2E7D32).copy(alpha = 0.1f),
            iconColor = Color(0xFF2E7D32)
        )
        InfoCard(
            modifier = Modifier.weight(1f).clickable(onClick = onWalletClick),
            icon = Icons.Outlined.Wallet,
            title = "Wallet Balance",
            amount = "$250.00",
            subtitle = "Add funds",
            iconBackgroundColor = Color(0xFFF59E0B).copy(alpha = 0.1f),
            iconColor = Color(0xFFF59E0B)
        )
    }
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    amount: String,
    subtitle: String,
    iconBackgroundColor: Color,
    iconColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(40.dp).background(iconBackgroundColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconColor)
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(amount, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

data class PaymentMethod(val name: String, val details: String, val icon: ImageVector)

@Composable
fun PayScreen(onPaySuccess: () -> Unit) {
    val paymentMethods = listOf(
        PaymentMethod("Credit Card", "•••• 4829", Icons.Outlined.CreditCard),
        PaymentMethod("Bank Transfer", "Chase •••• 1234", Icons.Outlined.AccountBalance),
        PaymentMethod("VoltFlow Wallet", "$250.00 available", Icons.Outlined.Wallet)
    )
    var selectedMethod by rememberSaveable { mutableStateOf(paymentMethods.first()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = WindowInsets.systemBars.asPaddingValues()
    ) {
        item {
            Column(modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)) {
                Text("Make Payment", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("City Power & Light", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Amount to pay", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 4.dp))
                        Text("84.32", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.ExtraBold)
                    }
                    Text("Current balance: $84.32 • Due Jan 15", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
                listOf("$50", "$100", "$150", "$200").forEach { amount ->
                    OutlinedButton(
                        onClick = { /* TODO */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    ) { Text(amount) }
                }
            }
        }
        item { Text("Payment Method", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp)) }
        items(paymentMethods) { method ->
            PaymentMethodItem(method = method, isSelected = method == selectedMethod, onClick = { selectedMethod = method })
        }
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
fun PaymentMethodItem(method: PaymentMethod, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(method.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(method.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(method.details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isSelected) {
                Box(modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

data class Transaction(val icon: ImageVector, val type: String, val date: String, val amount: String, val status: String, val amountColor: Color)

@Composable
fun HistoryScreen() {
    val transactions = listOf(
        Transaction(Icons.Outlined.ArrowDownward, "Payment", "Jan 10, 2026 • 2:34 PM", "-$127.45", "Completed", Color(0xFFC8E6C9)),
        Transaction(Icons.Outlined.Description, "Bill Generated", "Jan 1, 2026 • 12:00 AM", "+$127.45", "", Color.Transparent),
        Transaction(Icons.Outlined.ArrowDownward, "Payment", "Dec 28, 2025 • 10:15 AM", "-$115.20", "Completed", Color(0xFFC8E6C9)),
        Transaction(Icons.Outlined.Description, "Bill Generated", "Dec 15, 2025 • 12:00 AM", "+$115.20", "", Color.Transparent),
        Transaction(Icons.Outlined.ArrowDownward, "Payment", "Nov 28, 2025 • 3:22 PM", "-$98.75", "Completed", Color(0xFFC8E6C9)),
        Transaction(Icons.Outlined.ArrowDownward, "Payment", "Oct 28, 2025 • 11:45 AM", "-$142.30", "Completed", Color(0xFFC8E6C9)),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = WindowInsets.systemBars.asPaddingValues()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("History", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("Your payment records", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(
                    onClick = { /* TODO */ },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("All")
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
        }
        items(transactions) { transaction ->
            TransactionItem(transaction)
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp)
                    .background(MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(transaction.icon, contentDescription = null, tint = if (transaction.type == "Payment") Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.type, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(transaction.date, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(transaction.amount, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = if (transaction.amount.startsWith("-")) Color.Unspecified else Color(0xFF4CAF50))
                if (transaction.status.isNotEmpty()) {
                    Text(transaction.status, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4CAF50))
                }
            }
        }
    }
}

data class WalletTransaction(val type: String, val source: String, val date: String, val amount: String, val icon: ImageVector, val amountColor: Color)

@Composable
fun WalletScreen(onBack: () -> Unit) {
    val walletTransactions = listOf(
        WalletTransaction("Deposit", "Credit Card •••• 4829", "Jan 10, 2026", "+$100.00", Icons.Outlined.ArrowDownward, Color(0xFF4CAF50)),
        WalletTransaction("Bill Payment", "City Power & Light", "Jan 10, 2026", "-$84.32", Icons.Outlined.ArrowUpward, Color.Unspecified),
        WalletTransaction("Deposit", "Bank Transfer", "Dec 28, 2025", "+$200.00", Icons.Outlined.ArrowDownward, Color(0xFF4CAF50)),
        WalletTransaction("Bill Payment", "City Power & Light", "Dec 28, 2025", "-$127.45", Icons.Outlined.ArrowUpward, Color.Unspecified),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = WindowInsets.systemBars.asPaddingValues()
    ) {
        item {
            Box(contentAlignment = Alignment.TopStart) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(56.dp)) // For back button
                    Text("Wallet", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("Manage your funds", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                    WalletBalanceCard()
                    Spacer(Modifier.height(24.dp))
                    Text("Transaction History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                }
            }
        }
        items(walletTransactions) { item ->
            WalletTransactionItem(item, modifier = Modifier.padding(horizontal = 16.dp))
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun WalletBalanceCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
    ) {
        Box(
            modifier = Modifier.background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF007AFF), Color(0xFF5AC8FA)),
                )
            )
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Available Balance", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.titleMedium)
                Text("$250.00", color = Color.White, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(vertical = 8.dp))
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { /* TODO */ },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.2f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Funds", tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Funds", color = Color.White)
                    }
                    Button(
                        onClick = { /* TODO */ },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.NorthEast, contentDescription = "Withdraw", tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Withdraw", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun WalletTransactionItem(transaction: WalletTransaction, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(48.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(transaction.icon, contentDescription = null, tint = transaction.amountColor)
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.type, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text(transaction.source, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(transaction.amount, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = transaction.amountColor)
            Text(transaction.date, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


@Composable fun ProfileScreen(onPaymentMethodsClick: () -> Unit, onEditProfileClick: () -> Unit) { Column(modifier = Modifier.fillMaxSize().padding(16.dp)) { Text("Profile Screen") } }
@Composable fun NotificationsScreen(onBack: () -> Unit) { Column(modifier = Modifier.fillMaxSize().padding(16.dp)) { Button(onClick = onBack){Text("Back")} ; Text("Notifications Screen") } }

// --- App Navigator ---
@Composable
fun rememberAppNavigator(): AppNavigator {
    val navController = remember { mutableStateOf<Screen>(Screen.Home) }
    return remember { AppNavigator(navController) }
}
class AppNavigator(private val controller: MutableState<Screen>) {
    val currentScreen: Screen by controller

    fun navigate(destination: Screen) {
        controller.value = destination
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
fun DefaultPreview() {
    VoltflowTheme {
        VoltflowApp()
    }
}
