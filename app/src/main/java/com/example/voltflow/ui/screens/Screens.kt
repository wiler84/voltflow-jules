package com.example.voltflow.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voltflow.data.*
import com.example.voltflow.ui.*
import java.text.NumberFormat
import java.util.*

@Composable
fun HomeScreen(
    state: UiState,
    innerPadding: PaddingValues,
    onOpenNotifications: () -> Unit,
    onOpenUsage: () -> Unit,
    onOpenPay: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAutopay: () -> Unit,
    onOpenMethods: () -> Unit,
    onRefresh: () -> Unit
) {
    val dashboard = state.dashboard
    val profile = dashboard.profile
    val walletBalance = dashboard.wallet?.balance ?: 0.0
    val lastPayment = dashboard.transactions.firstOrNull { it.kind == TransactionKind.UTILITY_PAYMENT.name }
    val usage = dashboard.usage
    
    val accountNumber = "**** **** 4829"
    val meterId = "MTR-2847561"
    
    val currentBalance = 84.32 
    val usagePercent = ((usage?.monthlyUsage ?: 68.0) / 100.0).toFloat().coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = innerPadding.calculateTopPadding() + 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Good morning", color = VoltflowDesign.GrayText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(profile?.firstName ?: "Alex", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(VoltflowDesign.HeaderCircle)
                ) {
                    Box {
                        Icon(Icons.Outlined.Notifications, contentDescription = null, tint = Color.White)
                        if (dashboard.notifications.any { !it.isRead }) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(VoltflowDesign.BlueAccent).align(Alignment.TopEnd).offset(x = (-2).dp, y = 2.dp))
                        }
                    }
                }
            }
        }

        item {
            VoltflowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = VoltflowDesign.BlueAccent)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("City Power & Light", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(accountNumber, color = VoltflowDesign.GrayText, fontSize = 13.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Surface(shape = RoundedCornerShape(8.dp), color = VoltflowDesign.IconCircleBg) {
                            Text(meterId, color = VoltflowDesign.GrayText, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    Text("Current Balance", color = VoltflowDesign.GrayText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$", color = Color.White, fontSize = 24.sp, modifier = Modifier.padding(bottom = 6.dp))
                        Text(String.format("%.2f", currentBalance), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.weight(1f))
                        Text("Due Jan 15, 2026", color = VoltflowDesign.GrayText, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Usage this cycle", color = VoltflowDesign.GrayText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.weight(1f))
                        Text("${(usagePercent * 100).toInt()}%", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { usagePercent },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = VoltflowDesign.BlueAccent,
                        trackColor = VoltflowDesign.IconCircleBg,
                    )
                    Spacer(Modifier.height(32.dp))
                    VoltflowButton(text = "Pay Now", icon = Icons.AutoMirrored.Default.OpenInNew) { onOpenPay() }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                VoltflowCard(modifier = Modifier.weight(1f).clickable { onOpenHistory() }) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x1A4ADE80)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Last Payment", color = VoltflowDesign.GrayText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(amountFormatter(lastPayment?.amount ?: 127.45), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(formatTimestamp(lastPayment?.occurredAt ?: "2025-12-28T10:00:00Z").substringBefore(" •"), color = VoltflowDesign.GrayText, fontSize = 12.sp)
                    }
                }
                VoltflowCard(modifier = Modifier.weight(1f).clickable { onOpenPay() }) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x1AF59E0B)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Wallet Balance", color = VoltflowDesign.GrayText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(amountFormatter(walletBalance), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Add funds", color = VoltflowDesign.BlueAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("Quick Actions", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionChip("Auto-pay", Icons.Outlined.Autorenew, onOpenAutopay, modifier = Modifier.weight(1f))
                ActionChip("Usage Graph", Icons.Outlined.BarChart, onOpenUsage, modifier = Modifier.weight(1f))
                ActionChip("Support", Icons.Outlined.HeadsetMic, onOpenPay, modifier = Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Activity", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("View all >", color = VoltflowDesign.BlueAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onOpenHistory() })
            }
        }
        
        items(dashboard.recentTransactions) { transaction ->
            HistoryRow(
                icon = if (transaction.kind == TransactionKind.UTILITY_PAYMENT.name) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                title = transaction.description,
                amount = if (transaction.amount > 0 && transaction.kind != TransactionKind.WALLET_FUNDING.name) "-${amountFormatter(transaction.amount)}" else "+${amountFormatter(transaction.amount)}",
                date = formatTimestamp(transaction.occurredAt).substringBefore(" •"),
                status = transaction.status,
                isPayment = transaction.kind == TransactionKind.UTILITY_PAYMENT.name
            )
        }
    }
}

@Composable
fun WalletScreen(state: UiState, innerPadding: PaddingValues, onBack: () -> Unit, onFund: (Double) -> Unit) {
    val dashboard = state.dashboard
    var showFundDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        VoltflowHeader(title = "Wallet", subtitle = "Manage your funds", onBack = onBack)
        Spacer(Modifier.height(24.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            shape = RoundedCornerShape(32.dp),
            color = Color.Transparent
        ) {
            Box(modifier = Modifier.background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF10B981))))) {
                Column(modifier = Modifier.padding(28.dp)) {
                    Text("Available Balance", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(amountFormatter(dashboard.wallet?.balance ?: 0.0), color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { showFundDialog = true },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f))
                        ) {
                            Text("+ Add Funds", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, Color.White),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.AutoMirrored.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Withdraw", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("Transaction History", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(dashboard.transactions) { transaction ->
                TransactionHistoryRow(
                    if (transaction.kind == TransactionKind.WALLET_FUNDING.name) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    transaction.description,
                    amountFormatter(transaction.amount),
                    transaction.paymentMethod,
                    formatTimestamp(transaction.occurredAt).substringBefore(" •"),
                    transaction.kind == TransactionKind.WALLET_FUNDING.name
                )
            }
        }
    }
    
    if (showFundDialog) {
        FundWalletDialog(onDismiss = { showFundDialog = false }, onConfirm = { onFund(it); showFundDialog = false })
    }
}

@Composable
private fun FundWalletDialog(onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf("50") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VoltflowDesign.ModalBg,
        modifier = Modifier.clip(RoundedCornerShape(28.dp)),
        title = { Text("Add Funds", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Select an amount to add to your wallet", color = VoltflowDesign.GrayText)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("20", "50", "100", "200").forEach { valAmount ->
                        val selected = amount == valAmount
                        Surface(
                            modifier = Modifier.weight(1f).height(44.dp).clickable { amount = valAmount },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) VoltflowDesign.BlueAccent else VoltflowDesign.CardBg,
                            border = BorderStroke(1.dp, if (selected) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("$$valAmount", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(amount.toDoubleOrNull() ?: 0.0) }, colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.BlueAccent)) {
                Text("Confirm", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VoltflowDesign.GrayText)
            }
        }
    )
}

@Composable
private fun TransactionHistoryRow(icon: ImageVector, title: String, amount: String, source: String, date: String, isDeposit: Boolean) {
    VoltflowCard {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = if (isDeposit) Color(0xFF10B981) else VoltflowDesign.GrayText, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(source, color = VoltflowDesign.GrayText, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(amount, color = if (isDeposit) Color(0xFF10B981) else Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(date, color = VoltflowDesign.GrayText, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun UsageScreen(state: UiState, innerPadding: PaddingValues) {
    val usage = state.dashboard.usage
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        VoltflowHeader(title = "Analytics", subtitle = "Spending & usage insights", onBack = {})
        Spacer(Modifier.height(24.dp))
        
        var selectedTab by remember { mutableStateOf(0) }
        Surface(
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            color = VoltflowDesign.CardBg
        ) {
            Row(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                listOf("3 Months", "6 Months", "1 Year").forEachIndexed { index, text ->
                    val selected = selectedTab == index
                    Surface(
                        modifier = Modifier.weight(1f).fillMaxHeight().clickable { selectedTab = index },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) VoltflowDesign.BlueAccent else Color.Transparent
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text, color = if (selected) Color.White else VoltflowDesign.GrayText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AnalyticsMetricCard(Icons.Default.AttachMoney, "Total Spent", amountFormatter(usage?.totalSpent ?: 425.72), "12% less", true, modifier = Modifier.weight(1f))
            AnalyticsMetricCard(Icons.Default.Bolt, "Units Used", "${(usage?.totalSpent ?: 1135.0).toInt()}", "8% less", true, modifier = Modifier.weight(1f))
        }
        
        Spacer(Modifier.height(32.dp))
        Text("Spending Trend", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        VoltflowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.height(180.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.Bottom) {
                    BarChartItem("Oct", 0.6f, "$98.75")
                    BarChartItem("Nov", 0.7f, "$115.2")
                    BarChartItem("Dec", 0.9f, "$127.45")
                    BarChartItem("Jan", 0.5f, "$84.32")
                }
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Average monthly", color = VoltflowDesign.GrayText, fontSize = 15.sp)
                    Spacer(Modifier.weight(1f))
                    Text(amountFormatter(usage?.totalSpent?.div(4) ?: 106.43), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("Monthly Breakdown", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { BreakdownRow("Jan 2025", "220 kWh", "$84.32", "$0.383/kWh") }
            item { BreakdownRow("Dec 2025", "358 kWh", "$127.45", "$0.356/kWh") }
            item { BreakdownRow("Nov 2025", "312 kWh", "$115.20", "$0.369/kWh") }
        }
    }
}

@Composable
private fun BreakdownRow(date: String, usage: String, amount: String, rate: String) {
    VoltflowCard {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(date, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(usage, color = VoltflowDesign.GrayText, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(amount, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(rate, color = VoltflowDesign.GrayText, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AnalyticsMetricCard(icon: ImageVector, title: String, value: String, change: String, positive: Boolean, modifier: Modifier = Modifier) {
    VoltflowCard(modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = VoltflowDesign.BlueAccent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(title, color = VoltflowDesign.GrayText, fontSize = 13.sp)
            Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (positive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, contentDescription = null, tint = if (positive) Color(0xFF10B981) else Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                Text(change, color = if (positive) Color(0xFF10B981) else Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BarChartItem(label: String, heightFactor: Float, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight(heightFactor)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.verticalGradient(listOf(VoltflowDesign.BlueAccent, VoltflowDesign.BlueAccent.copy(alpha = 0.6f))))
        )
        Spacer(Modifier.height(12.dp))
        Text(label, color = VoltflowDesign.GrayText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PayScreen(state: UiState, innerPadding: PaddingValues, onPay: (UtilityType, Double, String?, Boolean) -> Unit) {
    val dashboard = state.dashboard
    val haptics = LocalHapticFeedback.current
    val currentBalance = 84.32
    var amountText by remember { mutableStateOf(String.format("%.2f", currentBalance)) }
    var selectedMethodId by remember { mutableStateOf(dashboard.paymentMethods.firstOrNull { it.isDefault }?.id) }
    var useWallet by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        VoltflowHeader(title = "Make Payment", subtitle = "City Power & Light", onBack = {})
        Spacer(Modifier.height(24.dp))
        VoltflowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Amount to pay", color = VoltflowDesign.GrayText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$", color = VoltflowDesign.GrayText, fontSize = 32.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(amountText, color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(20.dp))
                Surface(shape = CircleShape, color = VoltflowDesign.IconCircleBg) {
                    Text("Current balance: $${String.format("%.2f", currentBalance)} • Due Jan 15", color = VoltflowDesign.GrayText, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("50", "100", "150", "200").forEach { valAmount ->
                Surface(
                    modifier = Modifier.weight(1f).height(52.dp).clickable { amountText = String.format("%.2f", valAmount.toDouble()) },
                    shape = RoundedCornerShape(16.dp),
                    color = VoltflowDesign.CardBg,
                    border = BorderStroke(1.dp, if (amountText == String.format("%.2f", valAmount.toDouble()) && !useWallet) VoltflowDesign.BlueAccent else Color.Transparent)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("$$valAmount", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(40.dp))
        Text("Payment Method", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            dashboard.paymentMethods.forEach { method ->
                PaymentMethodRow(
                    icon = Icons.Default.CreditCard,
                    title = method.cardBrand,
                    subtitle = "**** ${method.cardLast4}",
                    selected = selectedMethodId == method.id && !useWallet,
                    onClick = {
                        selectedMethodId = method.id
                        useWallet = false
                    }
                )
            }
            PaymentMethodRow(
                icon = Icons.Default.Smartphone,
                title = "VoltFlow Wallet",
                subtitle = amountFormatter(dashboard.wallet?.balance ?: 0.0) + " available",
                selected = useWallet,
                onClick = {
                    useWallet = true
                    selectedMethodId = null
                }
            )
        }
        Spacer(Modifier.weight(1f))
        VoltflowButton(text = "Pay $${amountText} >", onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onPay(UtilityType.ELECTRICITY, amountText.toDoubleOrNull() ?: 0.0, selectedMethodId, useWallet)
            showSuccess = true
        })
        Spacer(Modifier.height(120.dp))
    }

    if (showSuccess) {
        SuccessDialog(amount = amountText, onDismiss = { showSuccess = false })
    }
}

@Composable
private fun SuccessDialog(amount: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val token = "VF-2026-0111-8429"
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White, // Based on image
        modifier = Modifier.clip(RoundedCornerShape(32.dp)),
        title = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFF0FDF4)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(24.dp))
                Text("Payment Successful", color = Color(0xFF0F172A), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Your payment of $$amount has been processed.", color = Color(0xFF64748B), textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Surface(color = Color(0xFFF8FAFC), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Transaction Token", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(token, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp).clickable { clipboard.setText(AnnotatedString(token)) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF0F172A))
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun PaymentMethodRow(icon: ImageVector, title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(80.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = VoltflowDesign.CardBg,
        border = BorderStroke(1.5.dp, if (selected) VoltflowDesign.BlueAccent else Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = VoltflowDesign.BlueAccent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = VoltflowDesign.GrayText, fontSize = 14.sp)
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape).border(2.dp, if (selected) VoltflowDesign.BlueAccent else VoltflowDesign.GrayText, CircleShape).padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(VoltflowDesign.BlueAccent))
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(state: UiState, innerPadding: PaddingValues) {
    val dashboard = state.dashboard
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("History", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("Your payment records", color = VoltflowDesign.GrayText, fontSize = 14.sp)
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = VoltflowDesign.CardBg,
                modifier = Modifier.height(44.dp).clickable { }
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FilterList, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("All", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = VoltflowDesign.GrayText, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            items(dashboard.transactions) { transaction ->
                HistoryRow(
                    icon = if (transaction.kind == TransactionKind.UTILITY_PAYMENT.name) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    title = transaction.description,
                    amount = if (transaction.amount > 0 && transaction.kind != TransactionKind.WALLET_FUNDING.name) "-${amountFormatter(transaction.amount)}" else "+${amountFormatter(transaction.amount)}",
                    date = formatTimestamp(transaction.occurredAt),
                    status = transaction.status,
                    isPayment = transaction.kind == TransactionKind.UTILITY_PAYMENT.name
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(icon: ImageVector, title: String, amount: String, date: String, status: String, isPayment: Boolean) {
    VoltflowCard {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = if (isPayment) Color(0xFF10B981) else Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(date, color = VoltflowDesign.GrayText, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(amount, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                if (status.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                        Spacer(Modifier.width(6.dp))
                        Text(status, color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(state: UiState, innerPadding: PaddingValues) {
    val notifications = state.dashboard.notifications
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        VoltflowHeader(title = "Notifications", subtitle = "Stay updated on your account", onBack = {})
        Spacer(Modifier.height(24.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(notifications) { notification ->
                NotificationRow(
                    icon = when(notification.type) {
                        "payment" -> Icons.Default.CheckCircle
                        "bill" -> Icons.Default.AccessTime
                        "alert" -> Icons.Default.Bolt
                        else -> Icons.Default.Notifications
                    },
                    title = notification.title,
                    body = notification.body,
                    time = formatTimestamp(notification.createdAt),
                    unread = !notification.isRead,
                    iconColor = when(notification.type) {
                        "payment" -> Color(0xFF10B981)
                        "bill" -> Color(0xFFF59E0B)
                        "alert" -> VoltflowDesign.BlueAccent
                        else -> Color.White
                    }
                )
            }
        }
    }
}

@Composable
private fun NotificationRow(icon: ImageVector, title: String, body: String, time: String, unread: Boolean, iconColor: Color) {
    VoltflowCard {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    if (unread) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(VoltflowDesign.BlueAccent))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(body, color = VoltflowDesign.GrayText, fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(8.dp))
                Text(time, color = VoltflowDesign.GrayText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// Sub-screens (Security, Payment Methods, etc.) match the simplified spec from images

@Composable
fun AutoPayScreen(state: UiState, innerPadding: PaddingValues, onSetAutopay: (Boolean, String?, Double, String) -> Unit) {
    val settings = state.dashboard.autopay ?: AutopaySettings(userId = state.dashboard.profile?.userId ?: "")
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        VoltflowHeader(title = "Auto-Pay", subtitle = "Automatic bill payments", onBack = {})
        Spacer(Modifier.height(24.dp))
        VoltflowCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Pay", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(if (settings.enabled) "Enabled" else "Not enabled", color = VoltflowDesign.GrayText, fontSize = 14.sp)
                }
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = { onSetAutopay(it, settings.paymentMethodId, settings.amountLimit, settings.billingCycle) },
                    colors = SwitchDefaults.colors(checkedTrackColor = VoltflowDesign.BlueAccent)
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("Payment Settings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        VoltflowCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                VoltflowRow(icon = Icons.Default.CalendarMonth, title = "Payment Day", subtitle = "Day of month to pay", description = "15", isDropdown = true)
                VoltflowDivider()
                VoltflowRow(icon = Icons.Default.CreditCard, title = "Payment Method", subtitle = "Credit Card **** 4829", actionText = "Change")
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("How It Works", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            HowItWorksItem("1", "Your bill is generated at the start of each month")
            HowItWorksItem("2", "Payment is automatically processed on your selected day")
            HowItWorksItem("3", "You'll receive a confirmation notification")
        }
    }
}

@Composable
private fun HowItWorksItem(number: String, text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = VoltflowDesign.CardBg,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
                Text(number, color = VoltflowDesign.BlueAccent, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Spacer(Modifier.width(16.dp))
            Text(text, color = VoltflowDesign.GrayText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ProfileOptionsScreen(state: UiState, innerPadding: PaddingValues, onBack: () -> Unit, onSaveProfile: (String, String, String) -> Unit) {
    var firstName by remember { mutableStateOf(state.dashboard.profile?.firstName ?: "") }
    var lastName by remember { mutableStateOf(state.dashboard.profile?.lastName ?: "") }
    var phone by remember { mutableStateOf(state.dashboard.profile?.phone ?: "") }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        VoltflowHeader(title = "Profile Options", subtitle = "Manage your account details", onBack = onBack)
        Spacer(Modifier.height(24.dp))
        VoltflowCard {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                VoltflowInput(value = firstName, onValueChange = { firstName = it }, placeholder = "First Name")
                VoltflowInput(value = lastName, onValueChange = { lastName = it }, placeholder = "Last Name")
                VoltflowInput(value = phone, onValueChange = { phone = it }, placeholder = "Phone Number")
                Spacer(Modifier.height(12.dp))
                VoltflowButton(text = "Save Profile") { onSaveProfile(firstName, lastName, phone) }
            }
        }
    }
}

@Composable
fun SecuritySettingsScreen(state: UiState, innerPadding: PaddingValues, onBack: () -> Unit, onToggleBiometric: (Boolean) -> Unit, onToggleMfa: (Boolean) -> Unit, onSetPin: () -> Unit, onAutoLockChange: (Int) -> Unit) {
    val security = state.dashboard.securitySettings ?: SecuritySettings(userId = state.dashboard.profile?.userId ?: "")
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        VoltflowHeader(title = "Security Settings", subtitle = "Protect your VoltFlow account", onBack = onBack)
        Spacer(Modifier.height(24.dp))
        VoltflowCard {
            Column {
                VoltflowRow(icon = Icons.Outlined.Fingerprint, title = "Biometric App Lock", subtitle = if (security.biometricEnabled) "Enabled" else "Disabled", hasToggle = true, checked = security.biometricEnabled, onToggle = onToggleBiometric)
                VoltflowDivider()
                VoltflowRow(icon = Icons.Outlined.Security, title = "Multi-factor authentication", subtitle = "Required for cross-device payment methods", actionText = if (security.mfaEnabled) "Disable" else "Enable", onAction = { onToggleMfa(!security.mfaEnabled) })
                VoltflowDivider()
                VoltflowRow(icon = Icons.Outlined.MoreHoriz, title = "App PIN", subtitle = if (security.pinEnabled) "PIN set" else "No PIN set", actionText = "Set PIN", onAction = onSetPin)
                VoltflowDivider()
                VoltflowRow(icon = Icons.Outlined.Timer, title = "Auto-lock timeout", subtitle = "Lock after ${security.autoLockMinutes} minute", description = "${security.autoLockMinutes} minute", isDropdown = true)
            }
        }
        Spacer(Modifier.height(32.dp))
        SectionLabel("ACCOUNT")
        VoltflowCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("User ID: ${VoltflowAppUtils.shortId(state.dashboard.profile?.userId)}", color = VoltflowDesign.GrayText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun PaymentMethodsScreen(state: UiState, innerPadding: PaddingValues, onBack: () -> Unit, onEnableMfa: (Boolean) -> Unit, onAdd: (String, String, Int, Int) -> Unit) {
    val methods = state.dashboard.paymentMethods
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        VoltflowHeader(title = "Payment Methods", subtitle = "Manage your payment options", onBack = onBack)
        Spacer(Modifier.height(24.dp))
        if (methods.isEmpty()) {
            VoltflowCard {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Enable MFA for cross-device cards", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Cards on other devices are hidden until MFA is enabled.", color = VoltflowDesign.GrayText, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            VoltflowCard {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("No payment methods", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Add a method after enabling MFA.", color = VoltflowDesign.GrayText, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(methods) { method ->
                    PaymentMethodRow(icon = Icons.Default.CreditCard, title = method.cardBrand, subtitle = "**** ${method.cardLast4}", selected = method.isDefault, onClick = {})
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        VoltflowOutlinedButton(text = "Add Payment Method", icon = Icons.Default.Add) { }
    }
}

@Composable
fun ConnectedDevicesScreen(state: UiState, innerPadding: PaddingValues, onBack: () -> Unit, onRevoke: (String) -> Unit) {
    val devices = state.dashboard.devices
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        VoltflowHeader(title = "Connected Devices", subtitle = "Manage active sessions", onBack = onBack)
        Spacer(Modifier.height(24.dp))
        if (devices.isEmpty()) {
            VoltflowCard {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("No devices yet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Sign in to register this device.", color = VoltflowDesign.GrayText, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(devices) { device ->
                    VoltflowCard {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Smartphone, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(device.deviceName, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(device.platform, color = VoltflowDesign.GrayText, fontSize = 12.sp)
                            }
                            if (device.deviceId != state.dashboard.currentDeviceId) {
                                Text("Revoke", color = VoltflowDesign.DestructiveRed, modifier = Modifier.clickable { onRevoke(device.deviceId) })
                            } else {
                                Text("Current", color = VoltflowDesign.BlueAccent, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HelpCenterScreen(innerPadding: PaddingValues, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        VoltflowHeader(title = "Help Center", subtitle = "Find answers fast", onBack = onBack)
        Spacer(Modifier.height(24.dp))
        VoltflowInput(value = "", onValueChange = {}, placeholder = "Search help topics", leadingIcon = Icons.Outlined.Search)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.weight(1f)) { VoltflowSupportButton(icon = Icons.Outlined.Chat, text = "Chat Support") }
            Box(modifier = Modifier.weight(1f)) { VoltflowSupportButton(icon = Icons.Outlined.Call, text = "Call Support") }
        }
        Spacer(Modifier.height(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            HelpTopic("How to make a payment")
            HelpTopic("Enable Auto-Pay")
            HelpTopic("Add funds to VoltFlow Wallet")
            HelpTopic("Secure your account")
        }
    }
}

@Composable
fun TermsScreen(innerPadding: PaddingValues, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        VoltflowHeader(title = "Terms of Service", subtitle = "Last updated Jan 1, 2026", onBack = onBack)
        Spacer(Modifier.height(24.dp))
        VoltflowCard {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                TermsSection("Summary", "VoltFlow provides utility billing services. By using the app, you agree to pay charges on time, keep your account secure, and follow applicable laws.")
                TermsSection("Billing & Payments", "Payments are processed using your selected method. Auto-Pay follows the date you choose and can be paused anytime.")
                TermsSection("Account Security", "You're responsible for keeping your login credentials secure and reviewing connected devices regularly.")
                TermsSection("Privacy", "We collect account details and payment history to provide service improvements and support.")
            }
        }
        Spacer(Modifier.height(32.dp))
        VoltflowButton(text = "Open Full Terms", icon = Icons.Outlined.OpenInNew) { }
        Spacer(Modifier.height(24.dp))
        Text("Copy Link", color = VoltflowDesign.BlueAccent, modifier = Modifier.fillMaxWidth().clickable { }, textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 15.sp)
    }
}

@Composable
fun ContactScreen(innerPadding: PaddingValues, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        VoltflowHeader(title = "Contact Us", subtitle = "We're here to help", onBack = onBack)
        Spacer(Modifier.height(24.dp))
        VoltflowCard {
            Column {
                VoltflowRow(icon = Icons.Outlined.Call, title = "Call support", subtitle = "Mon-Fri, 8am-8pm", hasChevron = true)
                VoltflowDivider()
                VoltflowRow(icon = Icons.Outlined.Email, title = "Email support", subtitle = "support@voltflow.app", hasChevron = true)
                VoltflowDivider()
                VoltflowRow(icon = Icons.Outlined.Public, title = "Visit help site", subtitle = "Browse troubleshooting guides", hasChevron = true)
                VoltflowDivider()
                VoltflowRow(icon = Icons.Outlined.BugReport, title = "Report a problem", subtitle = "Send logs and details", hasChevron = true)
            }
        }
        Spacer(Modifier.height(24.dp))
        VoltflowCard {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Place, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("VoltFlow HQ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("901 Market Street, San Francisco, CA", color = VoltflowDesign.GrayText, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun VoltflowSupportButton(icon: ImageVector, text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(60.dp).clickable { },
        color = VoltflowDesign.CardBg,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = VoltflowDesign.BlueAccent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun HelpTopic(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(64.dp).clickable { },
        color = VoltflowDesign.CardBg,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = VoltflowDesign.GrayText)
        }
    }
}

@Composable
fun TermsSection(title: String, text: String) {
    Column {
        Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
        Spacer(Modifier.height(8.dp))
        Text(text, color = VoltflowDesign.GrayText, fontSize = 15.sp, lineHeight = 22.sp)
    }
}
