package com.example.voltflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voltflow.ui.theme.VoltflowTheme

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
    object Analytics : Screen("analytics")
    object AutoPay : Screen("autopay")
    object PaymentMethods : Screen("payment_methods")
    object EditProfile : Screen("edit_profile")
    object UsageDetail : Screen("usage_detail")
    object PaymentSuccess : Screen("payment_success")
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

    // Handle Back Navigation
    if (currentScreen != Screen.Home && currentScreen != Screen.Pay && 
        currentScreen != Screen.History && currentScreen != Screen.Profile) {
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
            navigationSuiteColors = NavigationSuiteDefaults.colors(
                navigationBarContainerColor = MaterialTheme.colorScheme.surface,
                navigationBarContentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
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
                            onActionClick = { action ->
                                when(action) {
                                    "Auto-pay" -> navigator.navigate(Screen.AutoPay)
                                    "Usage Graph" -> navigator.navigate(Screen.Analytics)
                                }
                            },
                            onUsageClick = { navigator.navigate(Screen.UsageDetail) },
                            onPayClick = { navigator.navigate(Screen.Pay) },
                            onViewAll = { navigator.navigate(Screen.History) }
                        )
                        Screen.Pay -> PayScreen(onPaySuccess = { navigator.navigate(Screen.PaymentSuccess) })
                        Screen.History -> HistoryScreen()
                        Screen.Profile -> ProfileScreen(
                            onPaymentMethodsClick = { navigator.navigate(Screen.PaymentMethods) },
                            onEditProfileClick = { navigator.navigate(Screen.EditProfile) }
                        )
                        Screen.Notifications -> NotificationsScreen(onBack = { navigator.navigate(Screen.Home) })
                        Screen.Analytics -> AnalyticsScreen(onBack = { navigator.navigate(Screen.Home) })
                        Screen.AutoPay -> AutoPayScreen(onBack = { navigator.navigate(Screen.Home) })
                        Screen.PaymentMethods -> PaymentMethodsScreen(onBack = { navigator.navigate(Screen.Profile) })
                        Screen.EditProfile -> EditProfileScreen(onBack = { navigator.navigate(Screen.Profile) })
                        Screen.UsageDetail -> UsageDetailScreen(onBack = { navigator.navigate(Screen.Home) })
                        Screen.PaymentSuccess -> PaymentSuccessScreen(onDone = { navigator.navigate(Screen.Home) })
                    }
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
    PAY("Pay", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    HISTORY("History", Icons.Filled.History, Icons.Outlined.History),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person),
}

// --- SHARED COMPONENTS ---

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Back")
            }
            Spacer(Modifier.width(16.dp))
        }
        Column {
            Text(text = title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// --- SCREENS ---

@Composable
fun HomeScreen(onNotifyClick: () -> Unit, onActionClick: (String) -> Unit, onUsageClick: () -> Unit, onPayClick: () -> Unit, onViewAll: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Good morning", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Alex", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onNotifyClick, modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)) {
                    Icon(Icons.Default.NotificationsNone, contentDescription = null)
                }
            }
        }
        item { BillSummaryCard(onCardClick = onUsageClick, onPayClick = onPayClick) }
        item { QuickActionsSection(onActionClick) }
        item { RecentActivitySection(onViewAll) }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun BillSummaryCard(onCardClick: () -> Unit, onPayClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onCardClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FlashOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("City Power & Light", fontWeight = FontWeight.SemiBold)
                    Text("**** **** 4829", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text("MTR-2847561", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Current Balance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$84.32", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                }
                Text("Due Jan 15, 2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = 0.68f,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
            )
            Text("Usage this cycle: 68%", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onPayClick() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("Pay Now")
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
fun QuickActionsSection(onActionClick: (String) -> Unit) {
    Column {
        Text("Quick Actions", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionItem("Auto-pay", Icons.Default.Autorenew) { onActionClick("Auto-pay") }
            QuickActionItem("Usage Graph", Icons.Default.BarChart) { onActionClick("Usage Graph") }
            QuickActionItem("Support", Icons.Default.SupportAgent) { onActionClick("Support") }
        }
    }
}

@Composable
fun QuickActionItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.width(108.dp).clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun RecentActivitySection(onViewAll: () -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Recent Activity", fontWeight = FontWeight.Bold)
            TextButton(onClick = onViewAll) { Text("View all >", color = MaterialTheme.colorScheme.primary) }
        }
        ActivityItem("Payment", "Dec 28", "-$127.45", true)
        ActivityItem("Bill Generated", "Dec 15", "+$127.45", false)
    }
}

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("Notifications", "Stay updated on your account", onBack)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(listOf(
                NotificationData("Payment Successful", "Your payment of $127.45 was processed successfully.", "2 hours ago", Icons.Default.CheckCircle, Color(0xFF10B981)),
                NotificationData("Payment Due Soon", "Your electricity bill of $84.32 is due on Jan 15.", "1 day ago", Icons.Default.AccessTime, Color(0xFFF59E0B)),
                NotificationData("Auto-Pay Scheduled", "Auto-pay of $84.32 will be processed on Jan 15.", "2 days ago", Icons.Default.FlashOn, Color(0xFF3177C5))
            )) { data ->
                NotificationItem(data)
            }
        }
    }
}

data class NotificationData(val title: String, val body: String, val time: String, val icon: ImageVector, val color: Color)

@Composable
fun NotificationItem(data: NotificationData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.size(40.dp).background(data.color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(data.icon, contentDescription = null, tint = data.color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(data.title, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                }
                Text(data.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(data.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun AnalyticsScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("Analytics", "Spending & usage insights", onBack)
        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)).padding(4.dp)) {
            AnalyticsTab("3 Months", true, Modifier.weight(1f))
            AnalyticsTab("6 Months", false, Modifier.weight(1f))
            AnalyticsTab("1 Year", false, Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AnalyticsCard("Total Spent", "$425.72", "12% less", Icons.Default.AttachMoney, Modifier.weight(1f))
            AnalyticsCard("Units Used", "1135", "8% less", Icons.Default.FlashOn, Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
        Text("Spending Trend", fontWeight = FontWeight.Bold)
        // Simplified Bar Chart
        Row(modifier = Modifier.fillMaxWidth().height(150.dp).padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            Bar(98, "Oct")
            Bar(115, "Nov")
            Bar(127, "Dec", true)
            Bar(84, "Jan")
        }
    }
}

@Composable
fun AnalyticsTab(label: String, selected: Boolean, modifier: Modifier) {
    Box(modifier = modifier.height(40.dp).background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
        Text(label, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun AnalyticsCard(label: String, value: String, trend: String, icon: ImageVector, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(trend, style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981))
        }
    }
}

@Composable
fun Bar(height: Int, label: String, highlighted: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.width(40.dp).height(height.dp).background(if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)))
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun AutoPayScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("Auto-Pay", "Automatic bill payments", onBack)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FlashOn, contentDescription = null)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Pay", fontWeight = FontWeight.Bold)
                    Text("Not enabled", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = false, onCheckedChange = {})
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Payment Settings", fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Payment Day", fontWeight = FontWeight.SemiBold)
                        Text("Day of month to pay", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("15")
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payment, contentDescription = null)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Payment Method", fontWeight = FontWeight.SemiBold)
                        Text("Credit Card **** 4829", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Change", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun PaymentMethodsScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("Payment Methods", "Manage your payment options", onBack)
        PaymentMethodCard("Credit Card", "**** 4829", Icons.Default.CreditCard, isDefault = true)
        PaymentMethodCard("Bank Account", "Chase **** 1234", Icons.Default.AccountBalance)
        Spacer(Modifier.height(24.dp))
        Box(modifier = Modifier.fillMaxWidth().height(56.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add Payment Method")
            }
        }
    }
}

@Composable
fun PaymentMethodCard(name: String, details: String, icon: ImageVector, isDefault: Boolean = false) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.background, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, fontWeight = FontWeight.Bold)
                    if (isDefault) {
                        Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(start = 8.dp)) {
                            Text("Default", modifier = Modifier.padding(horizontal = 4.dp), fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Text(details, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = {}) { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
fun EditProfileScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ScreenHeader("Edit Profile", "Update your information", onBack)
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(modifier = Modifier.size(100.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(30.dp)), contentAlignment = Alignment.Center) {
                Text("A", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(24.dp).background(Color.White, CircleShape).padding(4.dp))
        }
        TextButton(onClick = {}) { Text("Change Photo") }
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = "Alex", onValueChange = {}, label = { Text("First Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = "Johnson", onValueChange = {}, label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = "alex.johnson@email.com", onValueChange = {}, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.weight(1f))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
            Text("Save Changes")
        }
    }
}

@Composable
fun UsageDetailScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("Usage", "Electricity consumption", onBack)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("This Month", style = MaterialTheme.typography.bodySmall)
                        Text("220 kWh", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    }
                    Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(16.dp)) {
                        Text("38% less", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color(0xFF2E7D32), fontSize = 12.sp)
                    }
                }
                Text("Based on your purchase history, you've used approximately 220 units this billing cycle.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}

@Composable
fun PaymentSuccessScreen(onDone: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(80.dp).background(Color(0xFFE8F5E9), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Payment Successful", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Your payment of $84.32 has been processed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(40.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Transaction Token", style = MaterialTheme.typography.labelSmall)
                Text("VF-2026-0111-8429", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(40.dp))
        Button(onClick = onDone, modifier = Modifier.width(200.dp), shape = RoundedCornerShape(12.dp)) {
            Text("Done")
        }
    }
}

// Reuse ActivityItem and PaymentMethodItem from previous implementation...
@Composable
fun ActivityItem(title: String, date: String, amount: String, isPayment: Boolean) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.background, CircleShape), contentAlignment = Alignment.Center) {
                Icon(if (isPayment) Icons.Default.ArrowDownward else Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (isPayment) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(amount, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PayScreen(onPaySuccess: () -> Unit) {
    val vm: PaymentViewModel = viewModel()
    val isProcessing = vm.isProcessing
    val paymentSuccess = vm.paymentSuccess

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Make Payment", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("City Power & Light", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Amount to pay", style = MaterialTheme.typography.bodySmall)
                Text("$ 84.32", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                Text("Current balance: $84.32 • Due Jan 15", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("$50", "$100", "$150", "$200").forEach { amount ->
                OutlinedButton(onClick = { /* select amount */ }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text(amount) }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Payment Method", fontWeight = FontWeight.Bold)
        PaymentMethodItem("Credit Card", "**** 4829", Icons.Default.CreditCard, true)
        PaymentMethodItem("Bank Transfer", "Chase **** 1234", Icons.Default.AccountBalance, false)
        Spacer(Modifier.weight(1f))

        if (isProcessing) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
        } else {
            Button(onClick = { vm.startPayment() }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
                Text("Pay $84.32", fontSize = 18.sp)
            }
        }

        LaunchedEffect(paymentSuccess) {
            if (paymentSuccess) {
                vm.clearSuccess()
                onPaySuccess()
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun PaymentMethodItem(name: String, details: String, icon: ImageVector, selected: Boolean) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold)
                Text(details, style = MaterialTheme.typography.bodySmall)
            }
            if (selected) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun HistoryScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Your payment records", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = {}) { Icon(Icons.Default.FilterList, contentDescription = null) }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(
                Triple("Payment", "Jan 10, 2026", "-$127.45"),
                Triple("Bill Generated", "Jan 1, 2026", "+$127.45"),
                Triple("Payment", "Dec 28, 2025", "-$115.20")
            )) { (t, d, a) -> ActivityItem(t, d, a, t == "Payment") }
        }
    }
}

@Composable
fun ProfileScreen(onPaymentMethodsClick: () -> Unit, onEditProfileClick: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Manage your account", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            Card(modifier = Modifier.fillMaxWidth().clickable { onEditProfileClick() }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(60.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) { Text("A", color = Color.White, fontSize = 24.sp) }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Alex Johnson", fontWeight = FontWeight.Bold); Text("alex.johnson@email.com", style = MaterialTheme.typography.bodySmall)
                        Text("Verified", color = Color(0xFF10B981), fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
        item { ProfileSection("ACCOUNT", listOf("Payment Methods" to Icons.Default.Payment), onPaymentMethodsClick) }
        item { ProfileSection("SECURITY", listOf("Security Settings" to Icons.Default.Security, "Biometric Login" to Icons.Default.Fingerprint), {}) }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.Red)
                    Spacer(Modifier.width(16.dp)); Text("Log Out", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun ProfileSection(title: String, items: List<Pair<String, ImageVector>>, onItemClick: () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column {
                items.forEach { (label, icon) ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onItemClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null); Spacer(Modifier.width(16.dp)); Text(label, modifier = Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    VoltflowTheme {
        VoltflowApp()
    }
}
