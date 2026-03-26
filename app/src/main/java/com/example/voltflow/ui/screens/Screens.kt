package com.example.voltflow.ui.screens

import androidx.compose.animation.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voltflow.data.*
import com.example.voltflow.ui.*
import androidx.biometric.BiometricManager
import kotlinx.coroutines.launch
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
    onOpenWallet: () -> Unit, // Added onOpenWallet parameter
    onOpenSupport: () -> Unit,
    onRefresh: () -> Unit
) {
    val dashboard = state.dashboard
    val profile = dashboard.profile
    val walletBalance = dashboard.wallet?.balance ?: 0.0
    val lastPayment = dashboard.transactions.firstOrNull { it.kind == TransactionKind.UTILITY_PAYMENT.name }
    val usage = dashboard.usage
    val billingAccount = dashboard.billingAccounts.firstOrNull { it.isDefault } ?: dashboard.billingAccounts.firstOrNull()
    val latestBill = dashboard.bills.maxByOrNull { it.dueDate } ?: dashboard.bills.firstOrNull()

    val accountNumber = billingAccount?.accountMasked ?: "****-****-0000"
    val meterId = billingAccount?.meterNumber ?: "MTR-000000"
    val providerName = billingAccount?.providerName ?: "Electric Utility"

    val currentBalance = latestBill?.amountDue ?: 0.0
    val usagePercent = ((usage?.monthlyUsage ?: 0.0).coerceIn(0.0, 100.0) / 100.0).toFloat()
    val greeting = greetingForCurrentTime(profile?.firstName)
    val dueLabel = latestBill?.dueDate?.let { "Due ${formatBillDate(it)}" } ?: "No due date"

    LaunchedEffect(Unit) {
        onRefresh()
    }

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
                    Text(greeting.substringBefore(","), color = VoltflowDesign.GrayText, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = VoltflowDesign.ManropeFont)
                    Text(profile?.firstName ?: "Alex", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont)
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
                            Text(providerName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                            Text(accountNumber, color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
                        }
                        Spacer(Modifier.weight(1f))
                        Surface(shape = RoundedCornerShape(8.dp), color = VoltflowDesign.IconCircleBg) {
                            Text(meterId, color = VoltflowDesign.GrayText, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontFamily = VoltflowDesign.ManropeFont)
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    Text("Current Balance", color = VoltflowDesign.GrayText, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = VoltflowDesign.ManropeFont)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$", color = Color.White, fontSize = 24.sp, modifier = Modifier.padding(bottom = 6.dp), fontFamily = VoltflowDesign.SoraFont)
                        Text(String.format("%.2f", currentBalance), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont)
                        Spacer(Modifier.weight(1f))
                        Text(dueLabel, color = VoltflowDesign.GrayText, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp), fontFamily = VoltflowDesign.ManropeFont)
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Usage this cycle", color = VoltflowDesign.GrayText, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = VoltflowDesign.ManropeFont)
                        Spacer(Modifier.weight(1f))
                        Text("${(usagePercent * 100).toInt()}%", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.ManropeFont)
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { usagePercent },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = VoltflowDesign.BlueAccent,
                        trackColor = VoltflowDesign.IconCircleBg,
                    )
                    Spacer(Modifier.height(32.dp))
                    VoltflowButton(text = "Buy Units", icon = Icons.AutoMirrored.Default.OpenInNew) { onOpenPay() }
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
                        Text("Last Payment", color = VoltflowDesign.GrayText, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = VoltflowDesign.ManropeFont)
                        Text(amountFormatter(lastPayment?.amount ?: 127.45), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                        Text(formatTimestamp(lastPayment?.occurredAt ?: "2025-12-28T10:00:00Z").substringBefore(" •"), color = VoltflowDesign.GrayText, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
                    }
                }
                VoltflowCard(modifier = Modifier.weight(1f).clickable { onOpenWallet() }) { // Wallet card now navigates
                    Column(modifier = Modifier.padding(20.dp)) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x1AF59E0B)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Wallet Balance", color = VoltflowDesign.GrayText, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = VoltflowDesign.ManropeFont)
                        Text(amountFormatter(walletBalance), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                        Text("Add funds", color = VoltflowDesign.BlueAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.ManropeFont)
                    }
                }
            }
        }

        item {
            Text("Quick Actions", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item { ActionChip("Auto-pay", Icons.Outlined.Autorenew, onOpenAutopay) }
                item { ActionChip("Usage Graph", Icons.Outlined.BarChart, onOpenUsage) }
                item { ActionChip("Payment Methods", Icons.Outlined.CreditCard, onOpenMethods) }
                item { ActionChip("Support", Icons.Outlined.HeadsetMic, onOpenSupport) }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Activity", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                Text("View all >", color = VoltflowDesign.BlueAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onOpenHistory() }, fontFamily = VoltflowDesign.ManropeFont)
            }
        }
        
        if (dashboard.recentTransactions.isEmpty()) {
            item {
                VoltflowCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("No transactions yet", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                        Spacer(Modifier.height(6.dp))
                        Text("Your recent payments will appear here.", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont)
                    }
                }
            }
        } else {
            items(dashboard.recentTransactions) { transaction ->
                HistoryRow(
                    icon = if (transaction.kind == TransactionKind.UTILITY_PAYMENT.name) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    title = transaction.description.ifBlank { "Payment" },
                    amount = if (transaction.amount > 0 && transaction.kind != TransactionKind.WALLET_FUNDING.name) "-${amountFormatter(transaction.amount)}" else "+${amountFormatter(transaction.amount)}",
                    date = formatTimestamp(transaction.occurredAt).substringBefore(" •"),
                    status = transaction.status,
                    isPayment = transaction.kind == TransactionKind.UTILITY_PAYMENT.name
                )
            }
        }
    }
}

@Composable
fun WalletScreen(
    state: UiState,
    innerPadding: PaddingValues,
    onBack: () -> Unit,
    onFund: (Double) -> Unit,
    onWithdraw: (Double, () -> Unit) -> Unit,
) {
    val dashboard = state.dashboard
    var showFundDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .statusBarsPadding()
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
                    Text("Available Balance", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = VoltflowDesign.ManropeFont)
                    Text(amountFormatter(dashboard.wallet?.balance ?: 0.0), color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont)
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { showFundDialog = true },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f))
                        ) {
                            Text("+ Add Funds", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                        }
                        OutlinedButton(
                            onClick = { showWithdrawDialog = true },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, Color.White),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.AutoMirrored.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Withdraw", fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("Transaction History", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
        Spacer(Modifier.height(16.dp))
        if (dashboard.walletTransactions.isEmpty()) {
            VoltflowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("No wallet activity yet", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                    Spacer(Modifier.height(6.dp))
                    Text("Deposits, withdrawals, and payments will show here.", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(dashboard.walletTransactions) { transaction ->
                    WalletHistoryRow(transaction)
                }
            }
        }
    }

    if (showFundDialog) {
        FundWalletDialog(onDismiss = { showFundDialog = false }, onConfirm = { onFund(it); showFundDialog = false })
    }
    if (showWithdrawDialog) {
        WithdrawWalletDialog(
            onDismiss = { showWithdrawDialog = false },
            onConfirm = { amount ->
                onWithdraw(amount) {
                    showWithdrawDialog = false
                }
            }
        )
    }
}

@Composable
private fun FundWalletDialog(onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf("50") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VoltflowDesign.ModalBg,
        modifier = Modifier.clip(RoundedCornerShape(28.dp)),
        title = { Text("Add Funds", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont) },
        text = {
            Column {
                Text("Select an amount to add to your wallet", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont)
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
                                Text("$$valAmount", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(amount.toDoubleOrNull() ?: 0.0) }, colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.BlueAccent)) {
                Text("Confirm", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont)
            }
        }
    )
}

@Composable
private fun WithdrawWalletDialog(onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf("50") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VoltflowDesign.ModalBg,
        modifier = Modifier.clip(RoundedCornerShape(28.dp)),
        title = { Text("Withdraw Funds", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont) },
        text = {
            Column {
                Text("Select an amount to withdraw from your wallet", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont)
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
                                Text("$$valAmount", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(amount.toDoubleOrNull() ?: 0.0) }, colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.BlueAccent)) {
                Text("Confirm", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont)
            }
        }
    )
}

@Composable
private fun WalletHistoryRow(transaction: WalletTransaction) {
    val isDeposit = transaction.kind.lowercase(Locale.getDefault()) == "deposit"
    val isWithdraw = transaction.kind.lowercase(Locale.getDefault()) == "withdraw"
    val icon = when {
        isDeposit -> Icons.Default.ArrowDownward
        isWithdraw -> Icons.Default.ArrowUpward
        else -> Icons.Default.Bolt
    }
    val amountPrefix = if (isDeposit) "+" else "-"
    VoltflowCard {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = if (isDeposit) Color(0xFF10B981) else VoltflowDesign.GrayText, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.kind.replaceFirstChar { it.uppercase() }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                Text(transaction.methodLabel, color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$amountPrefix${amountFormatter(transaction.amount)}", color = if (isDeposit) Color(0xFF10B981) else Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                Text(formatTimestamp(transaction.occurredAt).substringBefore(" •"), color = VoltflowDesign.GrayText, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
            }
        }
    }
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
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                Text(source, color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(amount, color = if (isDeposit) Color(0xFF10B981) else Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                Text(date, color = VoltflowDesign.GrayText, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
            }
        }
    }
}

@Composable
fun UsageScreen(state: UiState, innerPadding: PaddingValues, onBack: () -> Unit) {
    val periods = state.dashboard.usagePeriods
    var rangeMonths by remember { mutableStateOf(3) }
    val filteredPeriods = remember(periods, rangeMonths) {
        val now = java.time.LocalDate.now()
        val startDate = now.minusMonths(rangeMonths.toLong())
        periods.filter { period ->
            parseLocalDate(period.periodStart)?.isAfter(startDate.minusDays(1)) ?: true
        }.sortedBy { it.periodStart }
    }
    val totalSpent = filteredPeriods.sumOf { it.amountSpent }
    val totalUnits = filteredPeriods.sumOf { it.kwhUsed }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .statusBarsPadding()
    ) {
        VoltflowHeader(title = "Analytics", subtitle = "Spending & usage insights", onBack = onBack)
        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(
                selected = rangeMonths == 3,
                onClick = { rangeMonths = 3 },
                label = { Text("3 Months") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VoltflowDesign.BlueAccent, selectedLabelColor = Color.White)
            )
            FilterChip(
                selected = rangeMonths == 6,
                onClick = { rangeMonths = 6 },
                label = { Text("6 Months") }
            )
            FilterChip(
                selected = rangeMonths == 12,
                onClick = { rangeMonths = 12 },
                label = { Text("1 Year") }
            )
        }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            VoltflowCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Total Spent", color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
                    Text(amountFormatter(totalSpent), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont)
                    Text("Across ${filteredPeriods.size} periods", color = VoltflowDesign.GrayText, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
                }
            }
            VoltflowCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Units Used", color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
                    Text(String.format("%.0f", totalUnits), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont)
                    Text("kWh total", color = VoltflowDesign.GrayText, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Text("Spending Trend", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
        Spacer(Modifier.height(16.dp))
        if (filteredPeriods.isEmpty()) {
            VoltflowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("No usage data yet", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                    Spacer(Modifier.height(6.dp))
                    Text("Data appears after your first payment.", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont)
                }
            }
        } else {
            val chartPeriods = filteredPeriods.takeLast(4)
            val maxAmount = chartPeriods.maxOfOrNull { it.amountSpent }?.takeIf { it > 0 } ?: 1.0
            VoltflowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.height(180.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.Bottom) {
                        chartPeriods.forEach { period ->
                            BarChartItem(
                                label = formatShortMonth(period.periodStart),
                                heightFactor = (period.amountSpent / maxAmount).toFloat().coerceIn(0.1f, 1f),
                                value = amountFormatter(period.amountSpent)
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Average monthly", color = VoltflowDesign.GrayText, fontSize = 15.sp, fontFamily = VoltflowDesign.ManropeFont)
                        Spacer(Modifier.weight(1f))
                        val avg = if (filteredPeriods.isNotEmpty()) totalSpent / filteredPeriods.size else 0.0
                        Text(amountFormatter(avg), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont)
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("Monthly Breakdown", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
        Spacer(Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filteredPeriods.sortedByDescending { it.periodStart }) { period ->
                VoltflowCard {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(formatMonthYear(period.periodStart), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                            Text("${String.format("%.0f", period.kwhUsed)} kWh", color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(amountFormatter(period.amountSpent), color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                            val rate = if (period.kwhUsed > 0) period.amountSpent / period.kwhUsed else 0.0
                            Text("${String.format("$%.3f", rate)}/kWh", color = VoltflowDesign.GrayText, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakdownRow(date: String, usage: String, amount: String, rate: String) {
    VoltflowCard {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(date, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                Text(usage, color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(amount, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                Text(rate, color = VoltflowDesign.GrayText, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
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
            Text(title, color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
            Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (positive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, contentDescription = null, tint = if (positive) Color(0xFF10B981) else Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                Text(change, color = if (positive) Color(0xFF10B981) else Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.ManropeFont)
            }
        }
    }
}

@Composable
private fun BarChartItem(label: String, heightFactor: Float, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.ManropeFont)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight(heightFactor)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.verticalGradient(listOf(VoltflowDesign.BlueAccent, VoltflowDesign.BlueAccent.copy(alpha = 0.6f))))
        )
        Spacer(Modifier.height(12.dp))
        Text(label, color = VoltflowDesign.GrayText, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = VoltflowDesign.ManropeFont)
    }
}

@Composable
fun PayScreen(
    state: UiState,
    innerPadding: PaddingValues,
    onPay: (UtilityType, Double, String, String?, Boolean, () -> Unit) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val dashboard = state.dashboard
    val haptics = LocalHapticFeedback.current
    val billingAccount = dashboard.billingAccounts.firstOrNull { it.isDefault } ?: dashboard.billingAccounts.firstOrNull()
    val latestBill = dashboard.bills.maxByOrNull { it.dueDate } ?: dashboard.bills.firstOrNull()
    val currentBalance = latestBill?.amountDue ?: 0.0
    val providerName = billingAccount?.providerName ?: "Electric Utility"
    val dueLabel = latestBill?.dueDate?.let { "Due ${formatBillDate(it)}" } ?: "No due date"
    var amountText by remember { mutableStateOf(String.format("%.2f", currentBalance)) }
    var selectedMethodId by remember { mutableStateOf(dashboard.paymentMethods.firstOrNull { it.isDefault }?.id) }
    var useWallet by remember { mutableStateOf(dashboard.paymentMethods.isEmpty()) }
    var showSuccess by remember { mutableStateOf(false) }
    val autopaySettings = state.dashboard.autopay ?: AutopaySettings(userId = state.dashboard.profile?.userId ?: "")
    var meterNumber by remember { mutableStateOf(autopaySettings.meterNumber ?: billingAccount?.meterNumber ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val amountValue = amountText.toDoubleOrNull() ?: 0.0
    val payLabel = if (amountValue > 0) "Pay ${amountFormatter(amountValue)}" else "Pay"

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .statusBarsPadding()
                .imePadding()
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Make Payment", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                    Text(providerName, color = VoltflowDesign.GrayText, fontSize = 14.sp, fontFamily = VoltflowDesign.ManropeFont)
                }
            }

            Spacer(Modifier.height(24.dp))
            VoltflowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Amount to pay", color = VoltflowDesign.GrayText, fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = VoltflowDesign.ManropeFont)
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$", color = VoltflowDesign.GrayText, fontSize = 32.sp, modifier = Modifier.padding(bottom = 8.dp), fontFamily = VoltflowDesign.SoraFont)
                        Spacer(Modifier.width(12.dp))
                        BasicTextField(
                            value = amountText,
                            onValueChange = { newValue ->
                                val filteredValue = newValue.filter { it.isDigit() || it == '.' }
                                if (filteredValue.count { it == '.' } <= 1) {
                                    val parts = filteredValue.split(".")
                                    val normalized = if (parts.size == 2) {
                                        "${parts[0]}.${parts[1].take(2)}"
                                    } else {
                                        filteredValue
                                    }
                                    amountText = normalized
                                }
                                val amount = amountText.toDoubleOrNull() ?: 0.0
                                if (amount > 1000.0) {
                                    amountText = "1000.00"
                                }
                            },
                            textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont),
                            cursorBrush = Brush.verticalGradient(listOf(VoltflowDesign.BlueAccent, VoltflowDesign.BlueAccent))
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Surface(shape = CircleShape, color = VoltflowDesign.IconCircleBg) {
                        Text(
                            "Current balance: ${amountFormatter(currentBalance)} • $dueLabel",
                            color = VoltflowDesign.GrayText,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            fontFamily = VoltflowDesign.ManropeFont
                        )
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
                        border = BorderStroke(1.5.dp, if (amountText == String.format("%.2f", valAmount.toDouble())) VoltflowDesign.BlueAccent else Color.Transparent)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("$$valAmount", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Meter Number", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            Spacer(Modifier.height(12.dp))
            VoltflowInput(
                value = meterNumber,
                onValueChange = { meterNumber = it },
                placeholder = "Enter meter number",
            )

            Spacer(Modifier.height(32.dp))
            Text("Payment Method", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                dashboard.paymentMethods.forEach { method ->
                    PaymentMethodRow(
                        icon = Icons.Default.CreditCard,
                        title = method.cardBrand,
                        subtitle = "•••• ${method.cardLast4}",
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
                    subtitle = "${amountFormatter(dashboard.wallet?.balance ?: 0.0)} available",
                    selected = useWallet,
                    onClick = {
                        useWallet = true
                        selectedMethodId = null
                    }
                )
            }
            errorMessage?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = VoltflowDesign.WarningAmber, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
            }
            Spacer(Modifier.weight(1f))
            VoltflowButton(text = payLabel) {
                val amt = amountText.toDoubleOrNull() ?: 0.0
                val error = when {
                    amt < 1.0 -> "Enter an amount of at least $1.00."
                    amt > 1000.0 -> "Maximum payment is $1,000.00."
                    meterNumber.isBlank() -> "Meter number is required."
                    !useWallet && selectedMethodId == null -> "Select a payment method."
                    else -> null
                }
                if (error != null) {
                    errorMessage = error
                } else {
                    errorMessage = null
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPay(UtilityType.ELECTRICITY, amt, meterNumber.trim(), selectedMethodId, useWallet) {
                        showSuccess = true
                    }
                }
            }
            Spacer(Modifier.height(120.dp))
        }

        AnimatedVisibility(
            visible = showSuccess,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                SuccessDialog(amount = amountText, onDismiss = { showSuccess = false })
            }
        }
    }
}

@Composable
private fun SuccessDialog(amount: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val token = "VF-2026-0111-8429"
    val formattedAmount = amount.toDoubleOrNull()?.let { amountFormatter(it) } ?: amount
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White, 
        modifier = Modifier.clip(RoundedCornerShape(32.dp)),
        title = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFF0FDF4)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(24.dp))
                Text("Payment Successful", color = Color(0xFF0F172A), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Your payment of $formattedAmount has been processed.", color = Color(0xFF64748B), textAlign = TextAlign.Center, fontFamily = VoltflowDesign.ManropeFont)
                Spacer(Modifier.height(24.dp))
                Surface(color = Color(0xFFF8FAFC), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Transaction Token", color = Color(0xFF94A3B8), fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(token, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = VoltflowDesign.ManropeFont)
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
                Text("Done", fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
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
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                Text(subtitle, color = VoltflowDesign.GrayText, fontSize = 14.sp, fontFamily = VoltflowDesign.ManropeFont)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(state: UiState, innerPadding: PaddingValues, onBack: () -> Unit) {
    val dashboard = state.dashboard
    var showFilters by remember { mutableStateOf(false) }
    var kindFilter by remember { mutableStateOf("All") }
    var statusFilter by remember { mutableStateOf("All") }
    var dateFilter by remember { mutableStateOf("All") }

    val filteredTransactions = remember(dashboard.transactions, kindFilter, statusFilter, dateFilter) {
        val now = java.time.Instant.now()
        val earliestAllowed = when (dateFilter) {
            "This Week" -> now.minus(java.time.Duration.ofDays(7))
            "This Month" -> now.minus(java.time.Duration.ofDays(30))
            "Last 3 Months" -> now.minus(java.time.Duration.ofDays(90))
            else -> null
        }
        dashboard.transactions.filter { item ->
            val kindMatch = when (kindFilter) {
                "Utility Payment" -> item.kind == TransactionKind.UTILITY_PAYMENT.name
                "Wallet Funding" -> item.kind == TransactionKind.WALLET_FUNDING.name
                "Autopay" -> item.kind == TransactionKind.AUTOPAY_CHARGE.name
                else -> true
            }
            val statusMatch = when (statusFilter) {
                "Completed" -> item.status.equals("succeeded", true) || item.status.equals("completed", true)
                "Processing" -> item.status.equals("processing", true)
                "Failed" -> item.status.equals("failed", true)
                else -> true
            }
            val dateMatch = earliestAllowed?.let { earliest ->
                val instant = runCatching { java.time.Instant.parse(item.occurredAt) }.getOrNull()
                instant?.isAfter(earliest) ?: true
            } ?: true
            kindMatch && statusMatch && dateMatch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("History", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont)
                Text("Your payment records", color = VoltflowDesign.GrayText, fontSize = 14.sp, fontFamily = VoltflowDesign.ManropeFont)
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = VoltflowDesign.CardBg,
                modifier = Modifier
                    .height(44.dp)
                    .clickable { showFilters = true }
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FilterList, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Filter", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = VoltflowDesign.GrayText, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        val activeFilters = listOfNotNull(
            kindFilter.takeIf { it != "All" },
            statusFilter.takeIf { it != "All" },
            dateFilter.takeIf { it != "All" }
        )
        if (activeFilters.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                activeFilters.forEach { label ->
                    AssistChip(
                        onClick = { },
                        label = { Text(label, fontFamily = VoltflowDesign.ManropeFont) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = VoltflowDesign.IconCircleBg, labelColor = Color.White)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        if (filteredTransactions.isEmpty()) {
            VoltflowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("No transactions found", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                    Spacer(Modifier.height(6.dp))
                    Text("Try adjusting your filters.", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(filteredTransactions) { transaction ->
                    val statusLabel = when (transaction.status.lowercase(Locale.getDefault())) {
                        "succeeded", "completed" -> "Completed"
                        "processing" -> "Processing"
                        "failed" -> "Failed"
                        else -> transaction.status
                    }
                    HistoryRow(
                        icon = if (transaction.kind == TransactionKind.UTILITY_PAYMENT.name) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        title = transaction.description.ifBlank { "Payment" },
                        amount = if (transaction.amount > 0 && transaction.kind != TransactionKind.WALLET_FUNDING.name) "-${amountFormatter(transaction.amount)}" else "+${amountFormatter(transaction.amount)}",
                        date = formatTimestamp(transaction.occurredAt),
                        status = statusLabel,
                        isPayment = transaction.kind == TransactionKind.UTILITY_PAYMENT.name
                    )
                }
            }
        }
    }

    if (showFilters) {
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            containerColor = VoltflowDesign.CardBgSolid
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("Filter History", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = VoltflowDesign.SoraFont)
                FilterSection(
                    title = "Type",
                    options = listOf("All", "Utility Payment", "Wallet Funding", "Autopay"),
                    selected = kindFilter,
                    onSelect = { kindFilter = it }
                )
                FilterSection(
                    title = "Status",
                    options = listOf("All", "Completed", "Processing", "Failed"),
                    selected = statusFilter,
                    onSelect = { statusFilter = it }
                )
                FilterSection(
                    title = "Date",
                    options = listOf("All", "This Week", "This Month", "Last 3 Months"),
                    selected = dateFilter,
                    onSelect = { dateFilter = it }
                )
                Button(
                    onClick = { showFilters = false },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.BlueAccent)
                ) {
                    Text("Apply Filters", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = VoltflowDesign.GrayText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = {
                        Text(option, fontFamily = VoltflowDesign.ManropeFont, fontWeight = FontWeight.Medium)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VoltflowDesign.BlueAccent,
                        selectedLabelColor = Color.White,
                        containerColor = VoltflowDesign.IconCircleBg,
                        labelColor = Color.White
                    )
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
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                Text(date, color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(amount, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                if (status.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusColor = when (status.lowercase(Locale.getDefault())) {
                            "processing" -> VoltflowDesign.WarningAmber
                            "failed" -> VoltflowDesign.DestructiveRed
                            else -> Color(0xFF10B981)
                        }
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                        Spacer(Modifier.width(6.dp))
                        Text(status.replaceFirstChar { it.uppercase() }, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.ManropeFont)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(state: UiState, innerPadding: PaddingValues, onBack: () -> Unit, onMarkRead: (String) -> Unit) {
    val notifications = state.dashboard.notifications
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .statusBarsPadding()
    ) {
        VoltflowHeader(title = "Notifications", subtitle = "Stay updated on your account", onBack = onBack)
        Spacer(Modifier.height(24.dp))
        if (notifications.isEmpty()) {
            VoltflowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("You're all caught up", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                    Spacer(Modifier.height(6.dp))
                    Text("We'll notify you when something needs attention.", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(notifications) { notification ->
                    NotificationRow(
                        title = notification.title,
                        body = notification.body,
                        time = formatTimestamp(notification.createdAt),
                        unread = !notification.isRead,
                        type = notification.type,
                        onClick = {
                            if (!notification.isRead) onMarkRead(notification.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    title: String,
    body: String,
    time: String,
    unread: Boolean,
    type: String,
    onClick: () -> Unit
) {
    val iconColor = when (type) {
        "payment" -> Color(0xFF10B981)
        "bill" -> Color(0xFFF59E0B)
        "alert" -> VoltflowDesign.BlueAccent
        "autopay" -> Color(0xFF38BDF8)
        "wallet" -> Color(0xFFF59E0B)
        else -> Color.White
    }
    VoltflowCard(modifier = Modifier.clickable { onClick() }) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
                VoltflowLogoMark(tint = iconColor)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f), fontFamily = VoltflowDesign.SoraFont)
                    if (unread) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(VoltflowDesign.BlueAccent))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(body, color = VoltflowDesign.GrayText, fontSize = 14.sp, lineHeight = 20.sp, fontFamily = VoltflowDesign.ManropeFont)
                Spacer(Modifier.height(8.dp))
                Text(time, color = VoltflowDesign.GrayText, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = VoltflowDesign.ManropeFont)
            }
        }
    }
}

// Sub-screens (Security, Payment Methods, etc.) match the simplified spec from images

@Composable
fun AutoPayScreen(
    state: UiState,
    innerPadding: PaddingValues,
    onSetAutopay: (Boolean, String?, Double, String, Int, String?) -> Unit,
    onBack: () -> Unit
) {
    val settings = state.dashboard.autopay ?: AutopaySettings(userId = state.dashboard.profile?.userId ?: "")
    val methods = state.dashboard.paymentMethods
    val billingAccount = state.dashboard.billingAccounts.firstOrNull { it.isDefault } ?: state.dashboard.billingAccounts.firstOrNull()
    var enabled by remember { mutableStateOf(settings.enabled) }
    var paymentDay by remember { mutableStateOf(settings.paymentDay) }
    var amountText by remember { mutableStateOf(if (settings.amountLimit > 0) settings.amountLimit.toString() else "0") }
    var meterNumber by remember { mutableStateOf(billingAccount?.meterNumber ?: "") }
    var selectedMethodId by remember { mutableStateOf(settings.paymentMethodId ?: methods.firstOrNull()?.id) }
    var showDayPicker by remember { mutableStateOf(false) }
    var showMethodPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).statusBarsPadding()) {
        VoltflowHeader(title = "Auto-Pay", subtitle = "Automatic bill payments", onBack = onBack)
        Spacer(Modifier.height(24.dp))
        VoltflowCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Pay", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                    Text(if (enabled) "Enabled" else "Not enabled", color = VoltflowDesign.GrayText, fontSize = 14.sp, fontFamily = VoltflowDesign.ManropeFont)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = VoltflowDesign.BlueAccent)
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("Payment Settings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
        Spacer(Modifier.height(16.dp))
        VoltflowCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                VoltflowRow(
                    icon = Icons.Default.CalendarMonth,
                    title = "Payment Day",
                    subtitle = "Day of month to pay",
                    description = paymentDay.toString(),
                    isDropdown = true,
                    onClick = { showDayPicker = true }
                )
                VoltflowDivider()
                val methodLabel = methods.firstOrNull { it.id == selectedMethodId }
                    ?.let { "${it.cardBrand} •••• ${it.cardLast4}" }
                    ?: "Select method"
                VoltflowRow(
                    icon = Icons.Default.CreditCard,
                    title = "Payment Method",
                    subtitle = methodLabel,
                    actionText = "Change",
                    onAction = { showMethodPicker = true }
                )
                VoltflowDivider()
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text("Amount Limit", color = Color.White, fontWeight = FontWeight.SemiBold, fontFamily = VoltflowDesign.SoraFont)
                    Spacer(Modifier.height(8.dp))
                    VoltflowInput(
                        value = amountText,
                        onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        placeholder = "0.00",
                    )
                }
                VoltflowDivider()
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text("Meter Number", color = Color.White, fontWeight = FontWeight.SemiBold, fontFamily = VoltflowDesign.SoraFont)
                    Spacer(Modifier.height(8.dp))
                    VoltflowInput(
                        value = meterNumber,
                        onValueChange = { meterNumber = it },
                        placeholder = "Enter meter number",
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        VoltflowButton(text = "Save Auto-Pay Settings") {
            val amountValue = amountText.toDoubleOrNull() ?: 0.0
            onSetAutopay(
                enabled,
                selectedMethodId,
                amountValue,
                "monthly",
                paymentDay,
                meterNumber.ifBlank { null }
            )
        }
        Spacer(Modifier.height(32.dp))
        Text("How It Works", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            HowItWorksItem("1", "Your bill is generated at the start of each month")
            HowItWorksItem("2", "Payment is automatically processed on your selected day")
            HowItWorksItem("3", "You'll receive a confirmation notification")
        }
    }

    if (showDayPicker) {
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(
            onDismissRequest = { showDayPicker = false },
            containerColor = VoltflowDesign.CardBgSolid
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select payment day", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items((1..31).toList()) { day ->
                        ListItem(
                            headlineContent = { Text(day.toString(), color = Color.White, fontFamily = VoltflowDesign.ManropeFont) },
                            modifier = Modifier.clickable { paymentDay = day; showDayPicker = false }
                        )
                    }
                }
            }
        }
    }

    if (showMethodPicker) {
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(
            onDismissRequest = { showMethodPicker = false },
            containerColor = VoltflowDesign.CardBgSolid
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select payment method", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                methods.forEach { method ->
                    PaymentMethodRow(
                        icon = Icons.Default.CreditCard,
                        title = method.cardBrand,
                        subtitle = "•••• ${method.cardLast4}",
                        selected = method.id == selectedMethodId,
                        onClick = { selectedMethodId = method.id; showMethodPicker = false }
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
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
                Text(number, color = VoltflowDesign.BlueAccent, fontWeight = FontWeight.Black, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
            }
            Spacer(Modifier.width(16.dp))
            Text(text, color = VoltflowDesign.GrayText, fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = VoltflowDesign.ManropeFont)
        }
    }
}

@Composable
fun ProfileOptionsScreen(state: UiState, innerPadding: PaddingValues, onBack: () -> Unit, onSaveProfile: (String, String, String) -> Unit) {
    var firstName by remember { mutableStateOf(state.dashboard.profile?.firstName ?: "") }
    var lastName by remember { mutableStateOf(state.dashboard.profile?.lastName ?: "") }
    var phone by remember { mutableStateOf(state.dashboard.profile?.phone ?: "") }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).statusBarsPadding()) {
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
    var showAutoLockPicker by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val prefs = remember { UserPreferencesStore(context) }
    val scope = rememberCoroutineScope()
    val biometricManager = BiometricManager.from(context)
    val biometricStatus = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    val biometricAvailable = biometricStatus == BiometricManager.BIOMETRIC_SUCCESS
    val biometricHelper = when (biometricStatus) {
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Biometric hardware not available"
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware unavailable"
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No biometrics enrolled"
        else -> null
    }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).statusBarsPadding()) {
        VoltflowHeader(title = "Security Settings", subtitle = "Protect your VoltFlow account", onBack = onBack)
        Spacer(Modifier.height(24.dp))
        VoltflowCard {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Fingerprint, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Biometric App Lock", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = VoltflowDesign.SoraFont)
                        Text(
                            biometricHelper ?: if (security.biometricEnabled) "Enabled" else "Disabled",
                            color = VoltflowDesign.GrayText,
                            fontSize = 13.sp,
                            fontFamily = VoltflowDesign.ManropeFont
                        )
                    }
                    Switch(
                        checked = security.biometricEnabled,
                        onCheckedChange = { if (biometricAvailable) onToggleBiometric(it) },
                        enabled = biometricAvailable,
                        colors = SwitchDefaults.colors(checkedTrackColor = VoltflowDesign.BlueAccent)
                    )
                }
                VoltflowDivider()
                VoltflowRow(icon = Icons.Outlined.Security, title = "Multi-factor authentication", subtitle = "Required for cross-device payment methods", actionText = if (security.mfaEnabled) "Disable" else "Enable", onAction = { onToggleMfa(!security.mfaEnabled) })
                VoltflowDivider()
                VoltflowRow(
                    icon = Icons.Outlined.MoreHoriz,
                    title = "App PIN",
                    subtitle = if (security.pinEnabled) "PIN set" else "No PIN set",
                    actionText = if (security.pinEnabled) "Change PIN" else "Set PIN",
                    onAction = { showPinDialog = true }
                )
                VoltflowDivider()
                VoltflowRow(
                    icon = Icons.Outlined.Timer,
                    title = "Auto-lock timeout",
                    subtitle = "Lock after ${security.autoLockMinutes} minute${if (security.autoLockMinutes != 1) "s" else ""}",
                    description = "${security.autoLockMinutes} minute",
                    isDropdown = true,
                    onClick = { showAutoLockPicker = true }
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        SectionLabel("ACCOUNT")
        VoltflowCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("User ID: ${VoltflowAppUtils.shortId(state.dashboard.profile?.userId)}", color = VoltflowDesign.GrayText, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = VoltflowDesign.ManropeFont)
            }
        }
    }

    if (showAutoLockPicker) {
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(
            onDismissRequest = { showAutoLockPicker = false },
            containerColor = VoltflowDesign.CardBgSolid
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Auto-lock timeout", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                listOf(0, 1, 2, 5, 10, 30).forEach { minutes ->
                    val label = if (minutes == 0) "Off" else "$minutes minute${if (minutes != 1) "s" else ""}"
                    ListItem(
                        headlineContent = { Text(label, color = Color.White, fontFamily = VoltflowDesign.ManropeFont) },
                        modifier = Modifier.clickable {
                            onAutoLockChange(minutes)
                            showAutoLockPicker = false
                        }
                    )
                }
            }
        }
    }

    if (showPinDialog) {
        PinSetupDialog(
            onDismiss = { showPinDialog = false },
            onConfirm = { pin ->
                scope.launch {
                    prefs.setPin(pin)
                    onSetPin()
                    showPinDialog = false
                }
            }
        )
    }
}

@Composable
private fun PinSetupDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VoltflowDesign.ModalBg,
        modifier = Modifier.clip(RoundedCornerShape(28.dp)),
        title = { Text("Set App PIN", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VoltflowInput(
                    value = pin,
                    onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(6) },
                    placeholder = "Enter PIN",
                )
                VoltflowInput(
                    value = confirm,
                    onValueChange = { confirm = it.filter { ch -> ch.isDigit() }.take(6) },
                    placeholder = "Confirm PIN",
                )
                error?.let { Text(it, color = VoltflowDesign.WarningAmber, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valid = pin.length in 4..6 && pin == confirm
                    if (!valid) {
                        error = "PINs must be (4–6 digits)."
                        return@Button
                    }
                    error = null
                    onConfirm(pin)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.BlueAccent)
            ) {
                Text("Save PIN", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont)
            }
        }
    )
}

@Composable
fun PaymentMethodsScreen(state: UiState, innerPadding: PaddingValues, onBack: () -> Unit, onEnableMfa: (Boolean) -> Unit, onAdd: (String, String, Int, Int) -> Unit) {
    val methods = state.dashboard.paymentMethods
    val mfaEnabled = state.dashboard.securitySettings?.mfaEnabled == true
    var showAddDialog by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).statusBarsPadding()) {
        VoltflowHeader(title = "Payment Methods", subtitle = "Manage your payment options", onBack = onBack)
        Spacer(Modifier.height(24.dp))
        if (methods.isEmpty()) {
            VoltflowCard {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Enable MFA for cross-device cards", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, fontFamily = VoltflowDesign.SoraFont)
                    Spacer(Modifier.height(8.dp))
                    Text("Cards on other devices are hidden until MFA is enabled.", color = VoltflowDesign.GrayText, fontSize = 14.sp, fontFamily = VoltflowDesign.ManropeFont)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onEnableMfa(!mfaEnabled) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.BlueAccent)
                    ) {
                        Text(if (mfaEnabled) "Disable MFA" else "Enable MFA", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            VoltflowCard {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("No payment methods", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, fontFamily = VoltflowDesign.SoraFont)
                    Spacer(Modifier.height(8.dp))
                    Text("Add a method after enabling MFA.", color = VoltflowDesign.GrayText, fontSize = 14.sp, fontFamily = VoltflowDesign.ManropeFont)
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
        VoltflowOutlinedButton(text = "Add Payment Method", icon = Icons.Default.Add) { showAddDialog = true }
    }

    if (showAddDialog) {
        AddPaymentMethodDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { brand, number, month, year ->
                onAdd(brand, number, month, year)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddPaymentMethodDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, Int) -> Unit
) {
    var brand by remember { mutableStateOf("Visa") }
    var number by remember { mutableStateOf("") }
    var expiryMonth by remember { mutableStateOf("") }
    var expiryYear by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VoltflowDesign.ModalBg,
        modifier = Modifier.clip(RoundedCornerShape(28.dp)),
        title = { Text("Add Payment Method", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VoltflowInput(value = brand, onValueChange = { brand = it }, placeholder = "Card brand")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        VoltflowInput(
                            value = expiryMonth,
                            onValueChange = { expiryMonth = it.filter { ch -> ch.isDigit() }.take(2) },
                            placeholder = "MM",
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        VoltflowInput(
                            value = expiryYear,
                            onValueChange = { expiryYear = it.filter { ch -> ch.isDigit() }.take(4) },
                            placeholder = "YYYY",
                        )
                    }
                }
                error?.let { Text(it, color = VoltflowDesign.WarningAmber, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val month = expiryMonth.toIntOrNull() ?: 0
                    val year = expiryYear.toIntOrNull() ?: 0
                    val valid = brand.isNotBlank() && number.length >= 12 && month in 1..12 && year >= 2026
                    if (!valid) {
                        error = "Enter valid card details."
                        return@Button
                    }
                    error = null
                    onConfirm(brand.trim(), number.trim(), month, year)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.BlueAccent)
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont)
            }
        }
    )
}

@Composable
fun ConnectedDevicesScreen(state: UiState, innerPadding: PaddingValues, onBack: () -> Unit, onRevoke: (String) -> Unit) {
    val devices = state.dashboard.devices
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).statusBarsPadding()) {
        VoltflowHeader(title = "Connected Devices", subtitle = "Manage active sessions", onBack = onBack)
        Spacer(Modifier.height(24.dp))
        if (devices.isEmpty()) {
            VoltflowCard {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("No devices yet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, fontFamily = VoltflowDesign.SoraFont)
                    Spacer(Modifier.height(8.dp))
                    Text("Sign in to register this device.", color = VoltflowDesign.GrayText, fontSize = 14.sp, fontFamily = VoltflowDesign.ManropeFont)
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
                                Text(device.deviceName, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                                // Removed device.platform as per prompt (Android version info)
                                Text(device.location ?: "Unknown location", color = VoltflowDesign.GrayText, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
                                Text("Last active: ${formatTimestamp(device.lastActive)}", color = VoltflowDesign.GrayText, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
                            }
                            if (device.deviceId != state.dashboard.currentDeviceId) {
                                Text("Revoke", color = VoltflowDesign.DestructiveRed, modifier = Modifier.clickable { onRevoke(device.deviceId) }, fontFamily = VoltflowDesign.ManropeFont)
                            } else {
                                Text("Current Device", color = VoltflowDesign.BlueAccent, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
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
    val helpTopics = remember {
        listOf(
            HelpTopic("How to make a payment", "Navigate to the Pay tab, enter your amount and meter number, then choose a payment method."),
            HelpTopic("Enable Auto-Pay", "Go to Profile → Auto-Pay and toggle on, then choose your payment day and method."),
            HelpTopic("Add funds to VoltFlow Wallet", "Open Wallet and tap Add Funds to top up your balance."),
            HelpTopic("Secure your account", "Use Security Settings to enable biometric login and set an auto-lock timeout."),
            HelpTopic("How to add a payment method", "Open Payment Methods and tap Add Payment Method."),
            HelpTopic("Payment failed, what do I do?", "Check your wallet balance or card details, then retry the payment."),
            HelpTopic("Contact support", "Use the Contact Us screen to reach VoltFlow support.")
        )
    }
    var query by remember { mutableStateOf("") }
    val results by remember(query) {
        mutableStateOf(
            helpTopics.filter {
                it.title.contains(query, ignoreCase = true) || it.body.contains(query, ignoreCase = true)
            }
        )
    }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).statusBarsPadding()) {
        VoltflowHeader(title = "Help Center", subtitle = "Find answers fast", onBack = onBack)
        Spacer(Modifier.height(24.dp))
        VoltflowInput(value = query, onValueChange = { query = it }, placeholder = "Search help topics", leadingIcon = Icons.Outlined.Search)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.weight(1f)) { VoltflowSupportButton(icon = Icons.Outlined.Chat, text = "Chat Support") { uriHandler.openUri("mailto:support@voltflow.app") } }
            Box(modifier = Modifier.weight(1f)) { VoltflowSupportButton(icon = Icons.Outlined.Call, text = "Call Support") { uriHandler.openUri("tel:+14155550111") } }
        }
        Spacer(Modifier.height(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (results.isEmpty()) {
                VoltflowCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("No results for \"$query\"", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                        Spacer(Modifier.height(6.dp))
                        Text("Try a different search term.", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont)
                    }
                }
            } else {
                results.forEach { topic ->
                    ExpandableHelpTopic(topic)
                }
            }
        }
    }
}

private data class HelpTopic(val title: String, val body: String)

@Composable
private fun ExpandableHelpTopic(topic: HelpTopic) {
    var expanded by remember { mutableStateOf(false) }
    VoltflowCard(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(topic.title, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(topic.body, color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont, lineHeight = 20.sp)
                }
            }
        }
    }
}

@Composable
fun TermsScreen(innerPadding: PaddingValues, onBack: () -> Unit) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    val termsUrl = "https://voltflow.app/terms"
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).statusBarsPadding()) {
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
        VoltflowButton(text = "Open Full Terms", icon = Icons.Outlined.OpenInNew) { uriHandler.openUri(termsUrl) }
        Spacer(Modifier.height(24.dp))
        Text(
            "Copy Link",
            color = VoltflowDesign.BlueAccent,
            modifier = Modifier.fillMaxWidth().clickable { clipboard.setText(AnnotatedString(termsUrl)) },
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            fontFamily = VoltflowDesign.SoraFont
        )
    }
}

@Composable
fun ContactScreen(innerPadding: PaddingValues, onBack: () -> Unit) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).statusBarsPadding()) {
        VoltflowHeader(title = "Contact Us", subtitle = "We're here to help", onBack = onBack)
        Spacer(Modifier.height(24.dp))
        VoltflowCard {
            Column {
                VoltflowRow(icon = Icons.Outlined.Call, title = "Call support", subtitle = "Mon-Fri, 8am-8pm", hasChevron = true, onClick = { uriHandler.openUri("tel:+14155550111") })
                VoltflowDivider()
                VoltflowRow(icon = Icons.Outlined.Email, title = "Email support", subtitle = "support@voltflow.app", hasChevron = true, onClick = { uriHandler.openUri("mailto:support@voltflow.app") })
                VoltflowDivider()
                VoltflowRow(icon = Icons.Outlined.Public, title = "Visit help site", subtitle = "Browse troubleshooting guides", hasChevron = true, onClick = { uriHandler.openUri("https://voltflow.app/help") })
                VoltflowDivider()
                VoltflowRow(icon = Icons.Outlined.BugReport, title = "Report a problem", subtitle = "Send logs and details", hasChevron = true, onClick = { uriHandler.openUri("mailto:support@voltflow.app?subject=Voltflow%20Issue") })
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
                    Text("VoltFlow HQ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, fontFamily = VoltflowDesign.SoraFont)
                    Text("901 Market Street, San Francisco, CA", color = VoltflowDesign.GrayText, fontSize = 14.sp, fontFamily = VoltflowDesign.ManropeFont)
                }
            }
        }
    }
}

@Composable
fun VoltflowSupportButton(icon: ImageVector, text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(60.dp).clickable { onClick() },
        color = VoltflowDesign.CardBg,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = VoltflowDesign.BlueAccent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = VoltflowDesign.SoraFont)
        }
    }
}


@Composable
fun TermsSection(title: String, text: String) {
    Column {
        Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp, fontFamily = VoltflowDesign.SoraFont)
        Spacer(Modifier.height(8.dp))
        Text(text, color = VoltflowDesign.GrayText, fontSize = 15.sp, lineHeight = 22.sp, fontFamily = VoltflowDesign.ManropeFont)
    }
}

@Composable
private fun VoltflowLogoMark(tint: Color) {
    Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Bolt, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

private fun parseLocalDate(value: String?): java.time.LocalDate? {
    if (value.isNullOrBlank()) return null
    return runCatching { java.time.LocalDate.parse(value) }.getOrNull()
}

private fun formatBillDate(value: String): String {
    val date = parseLocalDate(value) ?: return value
    val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")
    return date.format(formatter)
}

private fun formatMonthYear(value: String): String {
    val date = parseLocalDate(value) ?: return value
    val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")
    return date.format(formatter)
}

private fun formatShortMonth(value: String): String {
    val date = parseLocalDate(value) ?: return value.take(3)
    val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM")
    return date.format(formatter)
}


