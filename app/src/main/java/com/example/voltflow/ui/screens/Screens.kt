package com.example.voltflow.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.*
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.voltflow.MainViewModel
import com.example.voltflow.ui.QrCodeGenerator
import com.example.voltflow.ui.QrCodeView
import com.example.voltflow.data.UsageRange
import com.example.voltflow.data.UsageChartData
import com.example.voltflow.data.UsageChartPoint
import androidx.compose.ui.unit.sp
import com.example.voltflow.R
import com.example.voltflow.data.*
import com.example.voltflow.ui.*
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials
import com.example.voltflow.bouncyClick
import com.example.voltflow.errorShake
import androidx.compose.ui.graphics.drawscope.clipPath

import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryModelOf
import kotlin.math.roundToInt

@Composable
fun EnergyWaveProgress(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    val color = VoltflowDesign.BlueAccent
    val trackColor = VoltflowDesign.IconCircleBg

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val cornerRadiusPx = 5.dp.toPx()
        
        // Draw Track
        drawRoundRect(
            color = trackColor,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx)
        )

        // Clip to rounded corners
        val path = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    0f, 0f, width, height,
                    androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
            )
        }

        clipPath(path) {
            // Draw progress
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(color.copy(alpha = 0.7f), color)
                ),
                size = androidx.compose.ui.geometry.Size(width * progress, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx)
            )
            
            // Animated Highlight Overlay
            val highlightX = width * progress * waveOffset
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.3f), Color.Transparent)
                ),
                topLeft = androidx.compose.ui.geometry.Offset(highlightX - 20.dp.toPx(), 0f),
                size = androidx.compose.ui.geometry.Size(40.dp.toPx(), height)
            )
        }
    }
}

@Composable
fun LogoLoadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val blueAccent = VoltflowDesign.BlueAccent
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
        VoltflowCard(modifier = Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.size(120.dp).graphicsLayer { rotationZ = rotation }) {
                    drawArc(
                        color = blueAccent,
                        startAngle = 0f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_icon_voltflow),
                    contentDescription = "Loading",
                    modifier = Modifier.size(120.dp).clip(RoundedCornerShape(30.dp))
                )
            }
        }
    }
}

@Composable
fun ShimmerBox(modifier: Modifier, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)) {
    Box(
        modifier = modifier
            .shimmerLoading(isLoading = true, shape = shape)
            .background(VoltflowDesign.IconCircleBg.copy(alpha = 0.5f), shape)
    )
}

@Composable
fun HomeSkeletonLayout(innerPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = innerPadding.calculateTopPadding() + 56.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        userScrollEnabled = false
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShimmerBox(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)))
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ShimmerBox(Modifier.width(80.dp).height(14.dp))
                        ShimmerBox(Modifier.width(120.dp).height(28.dp))
                    }
                }
                ShimmerBox(Modifier.size(48.dp), shape = CircleShape)
            }
        }
        item {
            VoltflowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ShimmerBox(Modifier.size(44.dp), shape = CircleShape)
                        Spacer(Modifier.width(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ShimmerBox(Modifier.width(120.dp).height(16.dp))
                            ShimmerBox(Modifier.width(80.dp).height(12.dp))
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    ShimmerBox(Modifier.width(100.dp).height(14.dp))
                    Spacer(Modifier.height(8.dp))
                    ShimmerBox(Modifier.width(180.dp).height(42.dp), shape = RoundedCornerShape(8.dp))
                    Spacer(Modifier.height(24.dp))
                    ShimmerBox(Modifier.fillMaxWidth().height(10.dp), shape = RoundedCornerShape(5.dp))
                    Spacer(Modifier.height(32.dp))
                    ShimmerBox(Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                VoltflowCard(modifier = Modifier.weight(1f).height(140.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        ShimmerBox(Modifier.size(36.dp), shape = CircleShape)
                        Spacer(Modifier.height(16.dp))
                        ShimmerBox(Modifier.width(80.dp).height(14.dp))
                        Spacer(Modifier.height(8.dp))
                        ShimmerBox(Modifier.width(100.dp).height(20.dp))
                    }
                }
                VoltflowCard(modifier = Modifier.weight(1f).height(140.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        ShimmerBox(Modifier.size(36.dp), shape = CircleShape)
                        Spacer(Modifier.height(16.dp))
                        ShimmerBox(Modifier.width(80.dp).height(14.dp))
                        Spacer(Modifier.height(8.dp))
                        ShimmerBox(Modifier.width(100.dp).height(20.dp))
                    }
                }
            }
        }
        item {
            ShimmerBox(Modifier.width(150.dp).height(24.dp))
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) { ShimmerBox(Modifier.width(100.dp).height(56.dp), shape = RoundedCornerShape(16.dp)) }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ShimmerBox(Modifier.width(120.dp).height(24.dp))
                ShimmerBox(Modifier.width(60.dp).height(14.dp))
            }
            Spacer(Modifier.height(16.dp))
            repeat(3) {
                ShimmerBox(Modifier.fillMaxWidth().height(88.dp), shape = RoundedCornerShape(24.dp))
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ProfileSkeletonLayout(innerPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = innerPadding.calculateTopPadding() + 56.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false
    ) {
        item {
            ShimmerBox(Modifier.width(120.dp).height(32.dp))
        }
        item {
            VoltflowCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    ShimmerBox(Modifier.size(64.dp), shape = CircleShape)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ShimmerBox(Modifier.width(140.dp).height(20.dp))
                        ShimmerBox(Modifier.width(180.dp).height(14.dp))
                        ShimmerBox(Modifier.width(80.dp).height(12.dp))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)); ShimmerBox(Modifier.width(100.dp).height(14.dp)) }
        item {
            VoltflowCard {
                Column {
                    repeat(3) {
                        Row(modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                            ShimmerBox(Modifier.size(44.dp), shape = CircleShape)
                            Spacer(Modifier.width(16.dp))
                            ShimmerBox(Modifier.width(150.dp).height(16.dp))
                        }
                        if (it < 2) VoltflowDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun UsageSkeletonLayout(innerPadding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).statusBarsPadding()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(Modifier.size(44.dp), shape = CircleShape)
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ShimmerBox(Modifier.width(120.dp).height(22.dp))
                ShimmerBox(Modifier.width(180.dp).height(14.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) { ShimmerBox(Modifier.width(80.dp).height(32.dp), shape = RoundedCornerShape(16.dp)) }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            VoltflowCard(modifier = Modifier.weight(1f).height(100.dp)) { }
            VoltflowCard(modifier = Modifier.weight(1f).height(100.dp)) { }
        }
        Spacer(Modifier.height(32.dp))
        ShimmerBox(Modifier.width(160.dp).height(24.dp))
        Spacer(Modifier.height(16.dp))
        VoltflowCard(modifier = Modifier.fillMaxWidth().height(200.dp)) { }
    }
}

@Composable
fun TimeoutLoadingCard(onReload: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        VoltflowCard(modifier = Modifier.fillMaxWidth(0.85f)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = VoltflowDesign.WarningAmber, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Session taking long", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                Spacer(Modifier.height(8.dp))
                Text("The connection is unstable or the server is busy. You can wait or try reloading.", color = VoltflowDesign.GrayText, fontSize = 14.sp, textAlign = TextAlign.Center, fontFamily = VoltflowDesign.ManropeFont)
                Spacer(Modifier.height(24.dp))
                VoltflowButton(text = "Reload", icon = Icons.Default.Refresh) { onReload() }
            }
        }
    }
}

@Composable
fun HomeLoadingState(onReload: () -> Unit, innerPadding: PaddingValues) {
    var loadingPhase by remember { mutableIntStateOf(0) } // 0: Logo, 1: Skeleton, 2: Timeout

    LaunchedEffect(Unit) {
        // Reduced logo duration since unblocked sync starts immediately
        delay(1000) // 1.0s of logo
        loadingPhase = 1
        // Much longer skeleton duration before warning to avoid premature dialog
        delay(15000) // 15.0s of skeleton (16.0s total)
        loadingPhase = 2
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = loadingPhase,
            transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) },
            label = "loading_content"
        ) { phase ->
            when (phase) {
                0 -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LogoLoadingIndicator()
                    }
                }
                1 -> {
                    HomeSkeletonLayout(innerPadding)
                }
                2 -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Keep skeleton in background
                        HomeSkeletonLayout(innerPadding)
                        // Dark overlay
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
                        TimeoutLoadingCard(onReload)
                    }
                }
            }
        }
    }
}

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
    onOpenWallet: () -> Unit,
    onOpenSupport: () -> Unit,
    onRefresh: () -> Unit
) {
    if (state.isLoading) {
        HomeLoadingState(onReload = onRefresh, innerPadding = innerPadding)
        return
    }

    val dashboard = state.dashboard
    val textColor = MaterialTheme.colorScheme.onSurface
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
    var selectedTransaction by remember { mutableStateOf<TransactionRecord?>(null) }



    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = innerPadding.calculateTopPadding() + 56.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val displayName = profile?.firstName?.take(1)?.uppercase() ?: "A"
                    Surface(modifier = Modifier.size(34.dp), shape = CircleShape, color = VoltflowDesign.BlueAccent) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.background(VoltflowDesign.PrimaryGradient)) {
                            Text(displayName, color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(greeting.substringBefore(","), color = VoltflowDesign.GrayText, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = VoltflowDesign.ManropeFont)
                        Text(profile?.firstName ?: "Alex", color = textColor, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont)
                    }
                }
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(VoltflowDesign.HeaderCircle)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Notifications, contentDescription = null, tint = textColor)
                        if (dashboard.notifications.any { !it.isRead }) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 10.dp, end = 10.dp)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(VoltflowDesign.BlueAccent)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                }
            }
        }

        item {
            var isFlipped by remember { mutableStateOf(false) }
            val rotation by animateFloatAsState(
                targetValue = if (isFlipped) 180f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy)
            )
            val isBack = rotation > 90f
            val density = androidx.compose.ui.platform.LocalDensity.current.density
            
            VoltflowCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    }
                    .clickable { isFlipped = !isFlipped }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            if (isBack) {
                                rotationY = 180f
                            }
                        }
                ) {
                    if (!isBack) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg),
                                contentAlignment = Alignment.Center
                            ) {
                                VoltflowLogo(size = 28.dp)
                            }
                            Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(providerName, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
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
                                Text("$", color = textColor, fontSize = 24.sp, modifier = Modifier.padding(bottom = 6.dp), fontFamily = VoltflowDesign.SoraFont)
                                Text(String.format("%.2f", currentBalance), color = textColor, fontSize = 42.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont)
                                Spacer(Modifier.weight(1f))
                                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 8.dp)) {
                                    Text(dueLabel, color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
                                    dashboard.predictedBill?.let { pred ->
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            "Est. next: ${amountFormatter(pred)}",
                                            color = VoltflowDesign.BlueAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = VoltflowDesign.ManropeFont
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Usage this cycle", color = VoltflowDesign.GrayText, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = VoltflowDesign.ManropeFont)
                                Spacer(Modifier.weight(1f))
                                Text("${(usagePercent * 100).toInt()}%", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.ManropeFont)
                            }
                            Spacer(Modifier.height(10.dp))
                            EnergyWaveProgress(
                                progress = usagePercent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .padding(horizontal = 4.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color.Transparent, VoltflowDesign.BlueAccent.copy(alpha = 0.4f), Color.Transparent)
                                        )
                                    )
                                    .blur(4.dp)
                            )
                            Spacer(Modifier.height(32.dp))
                            VoltflowButton(text = "Pay Now", icon = Icons.AutoMirrored.Default.ArrowForward) { onOpenPay() }
                        }
                    } else {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(44.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Analytics, contentDescription = null, tint = VoltflowDesign.BlueAccent)
                                }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text("Predictive Usage Insights", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                                    Text("Smart electricity projection", color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
                                }
                                Spacer(Modifier.weight(1f))
                                Box(
                                    modifier = Modifier.size(24.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg).clickable { isFlipped = false },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = VoltflowDesign.GrayText, modifier = Modifier.size(14.dp))
                                }
                            }
                            
                            Spacer(Modifier.height(24.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Days Remaining", color = VoltflowDesign.GrayText, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
                                    Spacer(Modifier.height(4.dp))
                                    val daysLeft = if (walletBalance > 0) {
                                        (walletBalance / 2.45).toInt().coerceIn(1, 45)
                                    } else {
                                        12
                                    }
                                    val daysColor = if (daysLeft < 5) MaterialTheme.colorScheme.error else VoltflowDesign.BlueAccent
                                    Text("$daysLeft Days", color = daysColor, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont)
                                }
                                
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Daily Burn Rate", color = VoltflowDesign.GrayText, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
                                    Spacer(Modifier.height(4.dp))
                                    Text("$2.45 / day", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                                }
                            }
                            
                            Spacer(Modifier.height(20.dp))
                            
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = VoltflowDesign.IconCircleBg
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Projected Billing Forecast", color = VoltflowDesign.GrayText, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
                                    Spacer(Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Est. Month-End Total:", color = textColor, fontSize = 14.sp, fontFamily = VoltflowDesign.ManropeFont)
                                        Spacer(Modifier.weight(1f))
                                        val projectedTotal = walletBalance + 42.50
                                        Text(amountFormatter(projectedTotal), color = VoltflowDesign.BlueAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(24.dp))
                            VoltflowButton(text = "Back to Balance", icon = Icons.Default.Refresh) { isFlipped = false }
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                VoltflowCard(modifier = Modifier.weight(1f).bouncyClick { onOpenHistory() }) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Last Payment", color = VoltflowDesign.GrayText, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = VoltflowDesign.ManropeFont)
                        Text(amountFormatter(lastPayment?.amount ?: 127.45), color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                        Text(formatTimestamp(lastPayment?.occurredAt ?: "2025-12-28T10:00:00Z").substringBefore(" •"), color = VoltflowDesign.GrayText, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
                    }
                }
                VoltflowCard(modifier = Modifier.weight(1f).bouncyClick { onOpenWallet() }) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(VoltflowDesign.WarningAmber.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = VoltflowDesign.WarningAmber, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Wallet Balance", color = VoltflowDesign.GrayText, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = VoltflowDesign.ManropeFont)
                        Text(amountFormatter(walletBalance), color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                        Text("Add funds", color = VoltflowDesign.BlueAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.ManropeFont)
                    }
                }
            }
        }

        dashboard.predictedBill?.let { pred ->
            item {
                PredictedBillWidget(predictedAmount = pred)
            }
        }

        item {
            Text("Quick Actions", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
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
                Text("Recent Activity", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                Text("View all >", color = VoltflowDesign.BlueAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onOpenHistory() }, fontFamily = VoltflowDesign.ManropeFont)
            }
        }
        
        if (dashboard.recentTransactions.isEmpty()) {
            item {
                VoltflowCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("No transactions yet", color = textColor, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
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
                    isPayment = transaction.kind == TransactionKind.UTILITY_PAYMENT.name,
                    onClick = { selectedTransaction = transaction },
                    accentLink = true,
                )
            }
        }
    }

    selectedTransaction?.let { transaction ->
        TransactionDetailSheet(
            transaction = transaction,
            onDismiss = { selectedTransaction = null },
        )
    }
}

@Composable
fun WalletScreen(
    state: UiState,
    innerPadding: PaddingValues,
    hazeState: HazeState,
    onBack: () -> Unit,
    onFund: (Double) -> Unit,
    onWithdraw: (Double, String, () -> Unit) -> Unit,
) {
    val dashboard = state.dashboard
    val textColor = MaterialTheme.colorScheme.onSurface
    val onGradientText = MaterialTheme.colorScheme.onPrimary
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
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .hazeChild(state = hazeState, style = HazeMaterials.thin(VoltflowDesign.BgTop))
                .bouncyClick { },
            shape = RoundedCornerShape(32.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Box(modifier = Modifier.background(brush = VoltflowDesign.WalletGradient, alpha = 0.7f)) {
                Column(modifier = Modifier.padding(28.dp)) {
                    Text("Available Balance", color = onGradientText.copy(alpha = 0.9f), fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = VoltflowDesign.ManropeFont)
                    Text(amountFormatter(dashboard.wallet?.balance ?: 0.0), color = onGradientText, fontSize = 48.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont)
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { showFundDialog = true },
                            modifier = Modifier.weight(1f).height(52.dp).bouncyClick { showFundDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = onGradientText.copy(alpha = 0.25f))
                        ) {
                            Text("+ Add Funds", color = onGradientText, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                        }
                        Button(
                            onClick = { showWithdrawDialog = true },
                            modifier = Modifier.weight(1f).height(52.dp).bouncyClick { showWithdrawDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Icon(Icons.AutoMirrored.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp), tint = VoltflowDesign.BlueAccent)
                            Spacer(Modifier.width(8.dp))
                            Text("Withdraw", color = VoltflowDesign.BlueAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("Wallet History", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
        Spacer(Modifier.height(16.dp))
        if (dashboard.walletTransactions.isEmpty()) {
            VoltflowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("No wallet activity yet", color = textColor, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                    Spacer(Modifier.height(6.dp))
                    Text("Deposits and withdrawals will show here.", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
                items(dashboard.walletTransactions) { transaction ->
                    WalletHistoryRow(transaction)
                }
            }
        }
    }

    if (showFundDialog) {
        FundWalletSheet(
            currentBalance = dashboard.wallet?.balance ?: 0.0,
            onDismiss = { showFundDialog = false },
            onConfirm = { onFund(it); showFundDialog = false }
        )
    }
    if (showWithdrawDialog) {
        WithdrawWalletSheet(
            currentBalance = dashboard.wallet?.balance ?: 0.0,
            paymentMethods = dashboard.paymentMethods,
            onDismiss = { showWithdrawDialog = false },
            onConfirm = { amount, method ->
                onWithdraw(amount, method) {
                    showWithdrawDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundWalletSheet(currentBalance: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf("50") }
    VoltflowModalSheet(title = "Add Funds", onDismiss = onDismiss) {
        Text("Current balance: ${amountFormatter(currentBalance)}", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont)
        Spacer(Modifier.height(12.dp))
        Text("Select an amount to add to your wallet", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("20", "50", "100", "200").forEach { valAmount ->
                val selected = amount == valAmount
                Surface(
                    modifier = Modifier.weight(1f).height(44.dp).clickable { amount = valAmount },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) VoltflowDesign.BlueAccent else VoltflowDesign.CardBg,
                    border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color.Transparent)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "$$valAmount",
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            fontFamily = VoltflowDesign.SoraFont
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        VoltflowButton(text = "Confirm Add Funds") { onConfirm(amount.toDoubleOrNull() ?: 0.0) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawWalletSheet(
    currentBalance: Double, 
    paymentMethods: List<PaymentMethod>,
    onDismiss: () -> Unit, 
    onConfirm: (Double, String) -> Unit
) {
    var amount by remember { mutableStateOf("50") }
    var selectedMethodId by remember { mutableStateOf(paymentMethods.firstOrNull { it.isDefault }?.id ?: paymentMethods.firstOrNull()?.id) }
    val view = LocalView.current
    
    VoltflowModalSheet(title = "Withdraw Funds", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Current balance: ${amountFormatter(currentBalance)}", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont)
            
            Text("Amount to withdraw", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("20", "50", "100", "200").forEach { valAmount ->
                    val selected = amount == valAmount
                    Surface(
                        modifier = Modifier.weight(1f).height(44.dp).clickable { amount = valAmount },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) VoltflowDesign.DestructiveRed else VoltflowDesign.CardBg,
                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color.Transparent)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "$$valAmount",
                                color = if (selected) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                fontFamily = VoltflowDesign.SoraFont
                            )
                        }
                    }
                }
            }

            Text("Withdraw to", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            
            if (paymentMethods.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { /* Navigate to add method */ },
                    shape = RoundedCornerShape(12.dp),
                    color = VoltflowDesign.IconCircleBg
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = VoltflowDesign.WarningAmber, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("No accounts available. Add one to withdraw.", color = VoltflowDesign.GrayText, fontSize = 14.sp, fontFamily = VoltflowDesign.ManropeFont)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    paymentMethods.forEach { method ->
                        val selected = selectedMethodId == method.id
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { selectedMethodId = method.id },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) VoltflowDesign.BlueAccent.copy(alpha = 0.08f) else VoltflowDesign.CardBg,
                            border = BorderStroke(1.dp, if (selected) VoltflowDesign.BlueAccent else Color.Transparent)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = if (selected) VoltflowDesign.BlueAccent else VoltflowDesign.GrayText)
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(method.cardBrand, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = VoltflowDesign.SoraFont)
                                    Text("•••• ${method.cardLast4}", color = VoltflowDesign.GrayText, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
                                }
                                Spacer(Modifier.weight(1f))
                                if (selected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VoltflowDesign.BlueAccent, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { 
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > currentBalance) {
                        performScreenHaptic(view, android.view.HapticFeedbackConstants.REJECT)
                        return@Button
                    }
                    if (selectedMethodId == null) {
                        performScreenHaptic(view, android.view.HapticFeedbackConstants.REJECT)
                        return@Button
                    }
                    onConfirm(amt, selectedMethodId!!) 
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.DestructiveRed),
                enabled = selectedMethodId != null
            ) {
                Text("Confirm Withdrawal", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            }
        }
    }
}

@Composable
private fun WalletHistoryRow(transaction: WalletTransaction) {
    val isDeposit = transaction.kind.lowercase(Locale.getDefault()) == "deposit"
    val isWithdraw = transaction.kind.lowercase(Locale.getDefault()) == "withdraw"
    val textColor = MaterialTheme.colorScheme.onSurface
    val icon = when {
        isDeposit -> Icons.Default.ArrowDownward
        isWithdraw -> Icons.Default.ArrowUpward
        else -> Icons.Default.Bolt
    }
    val amountPrefix = if (isDeposit) "+" else "-"
    VoltflowCard {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = if (isDeposit) MaterialTheme.colorScheme.tertiary else VoltflowDesign.GrayText, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.kind.replaceFirstChar { it.uppercase() }, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                Text(transaction.methodLabel, color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$amountPrefix${amountFormatter(transaction.amount)}", color = if (isDeposit) MaterialTheme.colorScheme.tertiary else textColor, fontWeight = FontWeight.Black, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                Text(formatTimestamp(transaction.occurredAt).substringBefore(" •"), color = VoltflowDesign.GrayText, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
            }
        }
    }
}

@Composable
private fun TransactionHistoryRow(icon: ImageVector, title: String, amount: String, source: String, date: String, isDeposit: Boolean) {
    val textColor = MaterialTheme.colorScheme.onSurface
    VoltflowCard {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = if (isDeposit) MaterialTheme.colorScheme.tertiary else VoltflowDesign.GrayText, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                Text(source, color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(amount, color = if (isDeposit) MaterialTheme.colorScheme.tertiary else textColor, fontWeight = FontWeight.Black, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                Text(date, color = VoltflowDesign.GrayText, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
            }
        }
    }
}

@Composable
fun UsageScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val chartState by viewModel.usageChartState.collectAsState()
    val range = chartState.range
    val data = chartState.data
    val isLoading = chartState.isLoading
    val error = chartState.error
    val textColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .statusBarsPadding()
    ) {
        VoltflowHeader(title = "Analytics", subtitle = "Spending & usage insights", onBack = onBack)
        
        Spacer(Modifier.height(24.dp))

        // Toggles
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            val isMoneyMode = chartState.isMoneyMode
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = VoltflowDesign.IconCircleBg,
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isMoneyMode) VoltflowDesign.BlueAccent else Color.Transparent,
                        modifier = Modifier.clickable { if (!isMoneyMode) viewModel.loadChartData(range, true) }
                    ) {
                        Text("Money", color = if (isMoneyMode) Color.White else textColor.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (!isMoneyMode) VoltflowDesign.BlueAccent else Color.Transparent,
                        modifier = Modifier.clickable { if (isMoneyMode) viewModel.loadChartData(range, false) }
                    ) {
                        Text("Usage", color = if (!isMoneyMode) Color.White else textColor.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
                    }
                }
            }
        }

        // Point 1: Usage analytics horizontal selector with animated indicator
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            val ranges = UsageRange.entries
            val itemWidth = maxWidth / ranges.size
            val selectedIndex = ranges.indexOf(range).coerceAtLeast(0)
            
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "indicator"
            )

            // Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VoltflowDesign.IconCircleBg)
            )

            // Animated Indicator
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .height(44.dp)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(VoltflowDesign.BlueAccent)

                    .drawWithContent {
                        drawContent()
                    }
            )

            Row(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                ranges.forEach { r ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { viewModel.loadChartData(r) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            r.label,
                            color = if (r == range) Color.White else textColor.copy(alpha = 0.6f),
                            fontFamily = VoltflowDesign.ManropeFont,
                            fontWeight = if (r == range) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Summary Card
        VoltflowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(if (chartState.isMoneyMode) "Total Spent" else "Total Usage", color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    val formattedTotal = if (chartState.isMoneyMode) {
                        amountFormatter(data?.totalUsage ?: 0.0)
                    } else {
                        "${data?.totalUsage?.roundToInt() ?: 0} kWh"
                    }
                    Text(formattedTotal, color = textColor, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont)
                    data?.let {
                        val pct = "${"%+.1f".format(it.percentageChange)}%"
                        Text(
                            pct,
                            color = if (it.percentageChange >= 0) VoltflowDesign.WarningAmber else MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = VoltflowDesign.ManropeFont,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Text("Usage Trend", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
        Spacer(Modifier.height(16.dp))

        // Point 4: Graph switching animation (morphing data naturally)
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(top = 12.dp)) {
            
            AnimatedContent(
                targetState = chartState.isMoneyMode to isLoading,
                transitionSpec = {
                    if (targetState.first != initialState.first) {
                        // Slide horizontally when switching Money/Usage
                        val direction = if (targetState.first) 1 else -1
                        (slideInHorizontally { width -> direction * width } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> -direction * width } + fadeOut())
                    } else {
                        fadeIn() togetherWith fadeOut()
                    }
                },
                label = "graph_content"
            ) { (moneyMode, loading) ->
                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = VoltflowDesign.BlueAccent)
                    }
                } else if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                } else if (data != null && data.points.isNotEmpty()) {
                    val chartEntryModel = entryModelOf(*data.points.map { it.value.toFloat() }.toTypedArray())
                    
                    Chart(
                        chart = columnChart(
                            columns = listOf(
                                com.patrykandpatrick.vico.core.component.shape.LineComponent(
                                    color = if (moneyMode) 0xFF00C853.toInt() else 0xFF0066FF.toInt(),
                                    thicknessDp = 10f,
                                    shape = com.patrykandpatrick.vico.core.component.shape.Shapes.roundedCornerShape(allPercent = 40)
                                )
                            )
                        ),
                        model = chartEntryModel,
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No data available for this range", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Text("Detailed Periods", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(data?.points?.reversed() ?: emptyList()) { point ->
                VoltflowCard {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            val date = Instant.ofEpochMilli(point.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                            Text(date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")), color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                            val detailVal = if (chartState.isMoneyMode) amountFormatter(point.value) else "${point.value.roundToInt()} kWh"
                            Text(detailVal, color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = VoltflowDesign.GrayText)
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun BreakdownRow(date: String, usage: String, amount: String, rate: String) {
    val textColor = MaterialTheme.colorScheme.onSurface
    VoltflowCard {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(date, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                Text(usage, color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(amount, color = textColor, fontWeight = FontWeight.Black, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                Text(rate, color = VoltflowDesign.GrayText, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
            }
        }
    }
}

@Composable
private fun AnalyticsMetricCard(icon: ImageVector, title: String, value: String, change: String, positive: Boolean, modifier: Modifier = Modifier) {
    val textColor = MaterialTheme.colorScheme.onSurface
    VoltflowCard(modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = VoltflowDesign.BlueAccent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(title, color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
            Text(value, color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (positive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, contentDescription = null, tint = if (positive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                Text(change, color = if (positive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.ManropeFont)
            }
        }
    }
}

@Composable
private fun BarChartItem(label: String, heightFactor: Float, value: String) {
    val textColor = MaterialTheme.colorScheme.onSurface
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.ManropeFont)
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
    val view = LocalView.current
    val textColor = MaterialTheme.colorScheme.onSurface
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
    val latestTransaction = dashboard.transactions.firstOrNull()
    val selectedMethodLabel = if (useWallet) {
        "Voltflow Wallet • ${amountFormatter(dashboard.wallet?.balance ?: 0.0)} available"
    } else {
        dashboard.paymentMethods.firstOrNull { it.id == selectedMethodId }
            ?.let { "${it.cardBrand} •••• ${it.cardLast4}" }
            ?: "Select payment method"
    }
    val meterValid = meterNumber.length == 15
    val payScrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(payScrollState)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Make Payment", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
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
                            textStyle = LocalTextStyle.current.copy(color = textColor, fontSize = 64.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont),
                            cursorBrush = Brush.verticalGradient(listOf(VoltflowDesign.BlueAccent, VoltflowDesign.BlueAccent)),
                            modifier = Modifier.errorShake(errorMessage?.contains("Amount"))
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
                    val selected = amountText == String.format("%.2f", valAmount.toDouble())
                    Surface(
                        modifier = Modifier.weight(1f).height(52.dp).bouncyClick { amountText = String.format("%.2f", valAmount.toDouble()) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) VoltflowDesign.BlueAccent else VoltflowDesign.CardBg,
                        border = BorderStroke(1.5.dp, if (selected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("$$valAmount", color = if (selected) Color.White else textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Meter Number", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            Spacer(Modifier.height(12.dp))
            VoltflowInput(
                value = meterNumber,
                onValueChange = { meterNumber = it.filter(Char::isDigit).take(15) },
                placeholder = "Enter 15-digit meter number",
                modifier = Modifier.errorShake(errorMessage?.contains("Meter"))
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (meterNumber.isEmpty()) "Meter number must be exactly 15 digits." else if (meterValid) "Meter number looks good." else "Meter number must be exactly 15 digits.",
                color = when {
                    meterNumber.isEmpty() -> VoltflowDesign.GrayText
                    meterValid -> MaterialTheme.colorScheme.tertiary
                    else -> VoltflowDesign.DestructiveRed
                },
                fontSize = 12.sp,
                fontFamily = VoltflowDesign.ManropeFont
            )

            Spacer(Modifier.height(32.dp))
            Text("Payment Method", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            Spacer(Modifier.height(6.dp))
            Text(selectedMethodLabel, color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
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
                Text(it, color = VoltflowDesign.WarningAmber, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont, modifier = Modifier.errorShake(errorMessage))
            }
            Spacer(Modifier.height(28.dp))
            VoltflowButton(text = payLabel, icon = Icons.AutoMirrored.Default.ArrowForward) {
                val amt = amountText.toDoubleOrNull() ?: 0.0
                val error = when {
                    amt < 1.0 -> "Enter an amount of at least $1.00."
                    amt > 1000.0 -> "Maximum payment is $1,000.00."
                    meterNumber.length != 15 -> "Meter number must be exactly 15 digits."
                    !useWallet && selectedMethodId == null -> "Select a payment method."
                    else -> null
                }
                if (error != null) {
                    errorMessage = null // Reset to re-trigger effect
                    errorMessage = error
                } else {
                    errorMessage = null
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPay(UtilityType.ELECTRICITY, amt, meterNumber.trim(), selectedMethodId, useWallet) {
                        performScreenHaptic(view, android.view.HapticFeedbackConstants.CONFIRM)
                    }
                }
            }
            // Point 15: Increased bottom padding for Pay screen scroll
            Spacer(Modifier.height(240.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentSuccessSheet(
    amount: String,
    meterNumber: String,
    methodLabel: String,
    transactionId: String,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val formattedAmount = amount.toDoubleOrNull()?.let { amountFormatter(it) } ?: amount
    val token = remember(transactionId) { QrCodeGenerator.generatePrepaidToken(transactionId) }
    
    VoltflowModalSheet(title = "", onDismiss = onDismiss) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(42.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("Payment Successful", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            Spacer(Modifier.height(12.dp))
            Text("Your payment of $formattedAmount has been processed.", color = VoltflowDesign.GrayText, textAlign = TextAlign.Center, fontFamily = VoltflowDesign.ManropeFont)
            Spacer(Modifier.height(32.dp))
            
            Text("Prepaid Meter Token", color = VoltflowDesign.GrayText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(16.dp),
                color = VoltflowDesign.IconCircleBg
            ) {
                Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(token, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont, modifier = Modifier.weight(1f))
                    IconButton(onClick = { clipboard.setText(AnnotatedString(token)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = VoltflowDesign.BlueAccent, modifier = Modifier.size(20.dp))
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            Text("Scan QR Receipt", color = VoltflowDesign.GrayText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.size(160.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(4.dp, VoltflowDesign.BlueAccent)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                    QrCodeView(
                        token = token,
                        modifier = Modifier.fillMaxSize(),
                        tintColor = Color.Black
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
            VoltflowButton(text = "Done") { onDismiss() }
        }
    }
}

@Composable
private fun PaymentMethodRow(icon: ImageVector, title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val view = LocalView.current
    Surface(
        modifier = Modifier.fillMaxWidth().height(80.dp).clickable {
            performScreenHaptic(view, android.view.HapticFeedbackConstants.CONTEXT_CLICK)
            onClick()
        },
        shape = RoundedCornerShape(20.dp),
        color = VoltflowDesign.CardBg,
        border = BorderStroke(1.5.dp, if (selected) VoltflowDesign.BlueAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = VoltflowDesign.BlueAccent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(state: UiState, innerPadding: PaddingValues, onBack: () -> Unit) {
    val dashboard = state.dashboard
    val textColor = MaterialTheme.colorScheme.onSurface
    var showFilters by remember { mutableStateOf(false) }
    var kindFilter by remember { mutableStateOf("All") }
    var statusFilter by remember { mutableStateOf("All") }
    var dateFilter by remember { mutableStateOf("All") }
    var selectedTransaction by remember { mutableStateOf<TransactionRecord?>(null) }

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
                Text("History", color = textColor, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont)
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
                    Icon(Icons.Default.FilterList, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Filter", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
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
                        colors = AssistChipDefaults.assistChipColors(containerColor = VoltflowDesign.IconCircleBg, labelColor = textColor)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        if (filteredTransactions.isEmpty()) {
            VoltflowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("No transactions found", color = textColor, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
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
                        isPayment = transaction.kind == TransactionKind.UTILITY_PAYMENT.name,
                        onClick = { selectedTransaction = transaction },
                    )
                }
            }
        }
    }

    if (showFilters) {
        VoltflowModalSheet(title = "Filter History", onDismiss = { showFilters = false }) {
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
            Spacer(Modifier.height(16.dp))
            VoltflowButton(text = "Apply Filters") { showFilters = false }
        }
    }

    selectedTransaction?.let { transaction ->
        TransactionDetailSheet(
            transaction = transaction,
            onDismiss = { selectedTransaction = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoltflowModalSheet(
    title: String,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VoltflowDesign.ModalBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(VoltflowDesign.GrayText.copy(alpha = 0.4f))
            )
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (title.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        fontFamily = VoltflowDesign.SoraFont,
                    )
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onDismiss() },
                        color = VoltflowDesign.IconCircleBg,
                        shape = CircleShape,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            content()
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SheetDetailRow(
    label: String,
    value: String,
    isCopyable: Boolean = false,
    onCopy: (() -> Unit)? = null,
) {
    val view = LocalView.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = VoltflowDesign.GrayText, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
            Spacer(Modifier.height(4.dp))
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontFamily = VoltflowDesign.ManropeFont)
        }
        if (isCopyable && onCopy != null) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = VoltflowDesign.BlueAccent,
                modifier = Modifier.size(18.dp).clickable {
                    performScreenHaptic(view, android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                    onCopy()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailSheet(transaction: TransactionRecord, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    VoltflowModalSheet(title = "Transaction Details", onDismiss = onDismiss) {
        VoltflowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SheetDetailRow("Amount", amountFormatter(transaction.amount))
                SheetDetailRow("Status", transaction.status.replaceFirstChar { it.uppercase() })
                SheetDetailRow("Date", formatTimestamp(transaction.occurredAt))
                SheetDetailRow("Method", transaction.paymentMethod)
                transaction.meterNumber?.takeIf { it.isNotBlank() }?.let {
                    SheetDetailRow("Meter Number", it)
                }
                SheetDetailRow(
                    "Transaction ID",
                    transaction.id,
                    isCopyable = true,
                ) {
                    clipboard.setText(AnnotatedString(transaction.id))
                }
                transaction.processorReference?.takeIf { it.isNotBlank() }?.let {
                    SheetDetailRow("Processor Ref", it)
                }
                
                if (transaction.kind == TransactionKind.UTILITY_PAYMENT.name || transaction.meterNumber != null) {
                    val token = remember(transaction.id) { QrCodeGenerator.generatePrepaidToken(transaction.id) }
                    Spacer(Modifier.height(8.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Spacer(Modifier.height(8.dp))
                    
                    Text("Prepaid Meter Token", color = VoltflowDesign.GrayText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = VoltflowDesign.IconCircleBg
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(token, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont, modifier = Modifier.weight(1f))
                            IconButton(onClick = { clipboard.setText(AnnotatedString(token)) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = VoltflowDesign.BlueAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Surface(
                            modifier = Modifier.size(140.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(3.dp, VoltflowDesign.BlueAccent)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
                                QrCodeView(
                                    token = token,
                                    modifier = Modifier.fillMaxSize(),
                                    tintColor = Color.Black
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
private fun WheelDayPickerSheet(
    selectedDay: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val view = LocalView.current
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (selectedDay - 1).coerceAtLeast(0))
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val currentDay by remember {
        derivedStateOf {
            (listState.firstVisibleItemIndex + 2).coerceIn(1, 31)
        }
    }

    LaunchedEffect(currentDay) {
        // iPhone Alarm style: Light, crisp haptic on every tick
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
    }

    VoltflowModalSheet(title = "Select payment day", onDismiss = onDismiss) {
        Text(
            "Day $currentDay",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            fontFamily = VoltflowDesign.SoraFont,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(VoltflowDesign.IconCircleBg)
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(vertical = 84.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items((1..31).toList()) { day ->
                    Text(
                        text = day.toString(),
                        color = if (day == currentDay) MaterialTheme.colorScheme.onSurface else VoltflowDesign.GrayText,
                        fontSize = if (day == currentDay) 28.sp else 20.sp,
                        fontWeight = if (day == currentDay) FontWeight.Black else FontWeight.Medium,
                        fontFamily = VoltflowDesign.SoraFont,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        VoltflowButton(text = "Use Day $currentDay") { onSelect(currentDay) }
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
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = VoltflowDesign.IconCircleBg,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(
    icon: ImageVector,
    title: String,
    amount: String,
    date: String,
    status: String,
    isPayment: Boolean,
    onClick: (() -> Unit)? = null,
    accentLink: Boolean = false,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    VoltflowCard(modifier = Modifier.clickable(enabled = onClick != null) { onClick?.invoke() }) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = if (isPayment) MaterialTheme.colorScheme.tertiary else textColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = if (accentLink && onClick != null) VoltflowDesign.BlueAccent else textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = VoltflowDesign.SoraFont
                )
                Text(date, color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(amount, color = textColor, fontWeight = FontWeight.Black, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                if (status.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusColor = when (status.lowercase(Locale.getDefault())) {
                            "processing" -> VoltflowDesign.WarningAmber
                            "failed" -> VoltflowDesign.DestructiveRed
                            else -> MaterialTheme.colorScheme.tertiary
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(state: UiState, innerPadding: PaddingValues, onBack: () -> Unit, onMarkRead: (String) -> Unit) {
    val notifications = state.dashboard.notifications
    val textColor = MaterialTheme.colorScheme.onSurface
    var selectedNotification by remember { mutableStateOf<AppNotification?>(null) }

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
                    Text("You're all caught up", color = textColor, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
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
                            selectedNotification = notification
                            if (!notification.isRead) onMarkRead(notification.id)
                        }
                    )
                }
            }
        }
    }

    selectedNotification?.let { notification ->
        NotificationDetailSheet(
            notification = notification,
            dashboard = state.dashboard,
            onDismiss = { selectedNotification = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailSheet(
    notification: AppNotification,
    dashboard: DashboardState,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val view = LocalView.current
    
    // Find linked transaction if this is a payment notification
    // We try to match by date/amount or a more robust link in a real app
    val linkedTransaction = remember(notification) {
        if (notification.type == "payment") {
            dashboard.transactions.firstOrNull { 
                notification.body.contains(amountFormatter(it.amount)) || 
                it.occurredAt?.take(10) == notification.createdAt?.take(10)
            }
        } else null
    }

    VoltflowModalSheet(title = notification.title, onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                notification.body,
                color = VoltflowDesign.GrayText,
                fontSize = 15.sp,
                fontFamily = VoltflowDesign.ManropeFont,
                lineHeight = 22.sp
            )
            
            if (linkedTransaction != null) {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                
                VoltflowCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SheetDetailRow("Amount", amountFormatter(linkedTransaction.amount))
                        SheetDetailRow("Status", linkedTransaction.status.replaceFirstChar { it.uppercase() })
                        SheetDetailRow("Date", formatTimestamp(linkedTransaction.occurredAt))
                        SheetDetailRow("Method", linkedTransaction.paymentMethod)
                        linkedTransaction.meterNumber?.takeIf { it.isNotBlank() }?.let {
                            SheetDetailRow("Meter Number", it)
                        }
                        SheetDetailRow(
                            "Transaction ID",
                            linkedTransaction.id,
                            isCopyable = true,
                        ) {
                            clipboard.setText(AnnotatedString(linkedTransaction.id))
                        }
                        
                        val token = remember(linkedTransaction.id) { QrCodeGenerator.generatePrepaidToken(linkedTransaction.id) }
                        
                        Spacer(Modifier.height(8.dp))
                        Text("Prepaid Meter Token", color = VoltflowDesign.GrayText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = VoltflowDesign.IconCircleBg
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(token, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont, modifier = Modifier.weight(1f))
                                IconButton(onClick = { 
                                    performScreenHaptic(view, android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                                    clipboard.setText(AnnotatedString(token)) 
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = VoltflowDesign.BlueAccent, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Surface(
                                modifier = Modifier.size(140.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(3.dp, VoltflowDesign.BlueAccent)
                            ) {
                                Box(modifier = Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
                                    QrCodeView(token = token, modifier = Modifier.fillMaxSize(), tintColor = Color.Black)
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                VoltflowButton(text = "Download Receipt", icon = Icons.Default.Download) {
                    // Logic to export/download would go here
                    performScreenHaptic(view, android.view.HapticFeedbackConstants.CONFIRM)
                }
            } else {
                // For security or other alerts, show device info if available
                val linkedDevice = remember(notification) {
                    if (notification.type == "security") {
                        dashboard.devices.firstOrNull { notification.body.contains(it.deviceName, ignoreCase = true) }
                    } else null
                }
                
                if (linkedDevice != null) {
                    VoltflowCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SheetDetailRow("Device", linkedDevice.deviceName)
                            SheetDetailRow("Platform", linkedDevice.platform)
                            SheetDetailRow("Location", linkedDevice.location ?: "Unknown")
                            SheetDetailRow("Last Active", formatTimestamp(linkedDevice.lastActive))
                        }
                    }
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
    val textColor = MaterialTheme.colorScheme.onSurface
    val iconColor = when (type) {
        "payment" -> MaterialTheme.colorScheme.tertiary
        "bill" -> VoltflowDesign.WarningAmber
        "alert" -> VoltflowDesign.BlueAccent
        "autopay" -> MaterialTheme.colorScheme.primary
        "wallet" -> VoltflowDesign.WarningAmber
        "security" -> VoltflowDesign.DestructiveRed
        else -> textColor
    }
    val icon = when (type) {
        "payment" -> Icons.Default.ReceiptLong
        "wallet" -> Icons.Default.AccountBalanceWallet
        "autopay" -> Icons.Default.Autorenew
        "security" -> Icons.Default.Security
        "system" -> Icons.Default.Info
        "bill" -> Icons.Default.Description
        "login" -> Icons.Default.Person
        else -> Icons.Default.Notifications
    }
    VoltflowCard(modifier = Modifier.clickable { onClick() }) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f), fontFamily = VoltflowDesign.SoraFont)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoPayScreen(
    state: UiState,
    innerPadding: PaddingValues,
    onSetAutopay: (Boolean, String?, Double, String, Int, String?) -> Unit,
    onBack: () -> Unit
) {
    val settings = state.dashboard.autopay ?: AutopaySettings(userId = state.dashboard.profile?.userId ?: "")
    val textColor = MaterialTheme.colorScheme.onSurface
    val methods = state.dashboard.paymentMethods
    val billingAccount = state.dashboard.billingAccounts.firstOrNull { it.isDefault } ?: state.dashboard.billingAccounts.firstOrNull()
    var enabled by remember { mutableStateOf(settings.enabled) }
    var paymentDay by remember { mutableStateOf(settings.paymentDay) }
    var amountText by remember { mutableStateOf(if (settings.amountLimit > 0) settings.amountLimit.toString() else "0") }
    var meterNumber by remember { mutableStateOf(settings.meterNumber ?: billingAccount?.meterNumber ?: "") }
    var selectedMethodId by remember { mutableStateOf(settings.paymentMethodId ?: methods.firstOrNull()?.id) }
    var showDayPicker by remember { mutableStateOf(false) }
    var showMethodPicker by remember { mutableStateOf(false) }
    var confirmStep by remember { mutableStateOf(0) }
    var showOffStateDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val selectedMethodLabel = methods.firstOrNull { it.id == selectedMethodId }
        ?.let { "${it.cardBrand} •••• ${it.cardLast4}" }
        ?: "Select method"

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).statusBarsPadding().verticalScroll(rememberScrollState())) {
        VoltflowHeader(title = "Auto-Pay", subtitle = "Automatic bill payments", onBack = onBack)
        Spacer(Modifier.height(24.dp))
        VoltflowCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = textColor, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Pay", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
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
        Text("Payment Settings", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
        Spacer(Modifier.height(16.dp))
        VoltflowCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                VoltflowRow(
                    icon = Icons.Default.CalendarMonth,
                    title = "Payment Day",
                    subtitle = "Day of month to pay",
                    description = (paymentDay ?: 15).toString(),
                    isDropdown = true,
                    onClick = { showDayPicker = true }
                )
                VoltflowDivider()
                val isMethodError = errorMessage?.contains("payment method") == true
                VoltflowRow(
                    icon = Icons.Default.CreditCard,
                    title = "Payment Method",
                    subtitle = selectedMethodLabel,
                    actionText = "Change",
                    onAction = { showMethodPicker = true },
                    modifier = Modifier
                        .errorShake(isMethodError)
                        .background(if (isMethodError) VoltflowDesign.DestructiveBg else Color.Transparent)
                )
                VoltflowDivider()
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text("Amount Limit", color = textColor, fontWeight = FontWeight.SemiBold, fontFamily = VoltflowDesign.SoraFont)
                    Spacer(Modifier.height(8.dp))
                    val isAmountError = errorMessage?.contains("Amount") == true
                    VoltflowInput(
                        value = amountText,
                        onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        placeholder = "0.00",
                        isError = isAmountError,
                        modifier = Modifier.errorShake(isAmountError)
                    )
                }
                VoltflowDivider()
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text("Meter Number", color = textColor, fontWeight = FontWeight.SemiBold, fontFamily = VoltflowDesign.SoraFont)
                    Spacer(Modifier.height(8.dp))
                    val isMeterError = errorMessage?.contains("Meter") == true
                    VoltflowInput(
                        value = meterNumber,
                        onValueChange = { meterNumber = it.filter(Char::isDigit).take(15) },
                        placeholder = "Enter 15-digit meter number",
                        isError = isMeterError,
                        modifier = Modifier.errorShake(isMeterError)
                    )
                }
            }
        }
        
        errorMessage?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = VoltflowDesign.WarningAmber, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont, modifier = Modifier.errorShake(errorMessage))
        }

        Spacer(Modifier.height(20.dp))
        VoltflowButton(text = "Save Auto-Pay Settings") {
            val amountValue = amountText.toDoubleOrNull() ?: 0.0
            if (enabled) {
                val error = when {
                    amountValue <= 0 -> "Amount limit must be greater than zero."
                    meterNumber.length != 15 -> "Meter number must be exactly 15 digits."
                    selectedMethodId == null -> "Please select a payment method."
                    else -> null
                }
                
                if (error != null) {
                    errorMessage = null
                    errorMessage = error
                } else {
                    errorMessage = null
                    confirmStep = 1
                }
            } else {
                errorMessage = null
                onSetAutopay(
                    false,
                    selectedMethodId,
                    amountValue,
                    "monthly",
                    paymentDay ?: 15,
                    meterNumber.ifBlank { null }
                )
                showOffStateDialog = true
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("How It Works", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            HowItWorksItem("1", "Your bill is generated at the start of each month")
            HowItWorksItem("2", "Payment is automatically processed on your selected day")
            HowItWorksItem("3", "You'll receive a confirmation notification")
        }
        Spacer(Modifier.height(120.dp))
    }

    if (confirmStep == 1) {
        ConfirmAutopaySheet(
            enabled = enabled,
            meterNumber = meterNumber,
            methodLabel = selectedMethodLabel,
            amountLimit = amountText.toDoubleOrNull() ?: 0.0,
            onDismiss = { confirmStep = 0 },
            onConfirm = {
                val amountValue = amountText.toDoubleOrNull() ?: 0.0
                onSetAutopay(
                    enabled,
                    selectedMethodId,
                    amountValue,
                    "monthly",
                    paymentDay ?: 15,
                    meterNumber.ifBlank { null }
                )
                confirmStep = 2
            }
        )
    }

    if (confirmStep == 2) {
        VoltflowModalSheet(title = "Auto-Pay Active!", onDismiss = { confirmStep = 0 }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(VoltflowDesign.BlueAccent.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = VoltflowDesign.BlueAccent, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Auto-Pay successfully enabled!",
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = VoltflowDesign.SoraFont
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Voltflow will now automatically process payments for your meter number $meterNumber on day $paymentDay of each month.",
                    color = VoltflowDesign.GrayText,
                    textAlign = TextAlign.Center,
                    fontFamily = VoltflowDesign.ManropeFont,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(24.dp))
                VoltflowButton(text = "Awesome") {
                    confirmStep = 0
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showOffStateDialog) {
        VoltflowModalSheet(title = "Auto-Pay is Off", onDismiss = { showOffStateDialog = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    "Details added but Auto-Pay is off, turn it on?",
                    color = VoltflowDesign.GrayText,
                    fontFamily = VoltflowDesign.ManropeFont,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { showOffStateDialog = false },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.IconCircleBg)
                    ) {
                        Text("Keep Off", color = textColor, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                    }
                    Button(
                        onClick = {
                            val amountValue = amountText.toDoubleOrNull() ?: 0.0
                            onSetAutopay(
                                true,
                                selectedMethodId,
                                amountValue,
                                "monthly",
                                paymentDay ?: 15,
                                meterNumber.ifBlank { null }
                            )
                            enabled = true
                            showOffStateDialog = false
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.BlueAccent)
                    ) {
                        Text("Turn On", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showDayPicker) {
        WheelDayPickerSheet(
            selectedDay = paymentDay ?: 15,
            onDismiss = { showDayPicker = false },
            onSelect = { selected ->
                paymentDay = selected
                showDayPicker = false
            }
        )
    }

    if (showMethodPicker) {
        VoltflowModalSheet(title = "Select payment method", onDismiss = { showMethodPicker = false }) {
            methods.forEach { method ->
                PaymentMethodRow(
                    icon = Icons.Default.CreditCard,
                    title = method.cardBrand,
                    subtitle = "•••• ${method.cardLast4}",
                    selected = method.id == selectedMethodId,
                    onClick = { selectedMethodId = method.id; showMethodPicker = false }
                )
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmAutopaySheet(
    enabled: Boolean,
    meterNumber: String,
    methodLabel: String,
    amountLimit: Double,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    VoltflowModalSheet(title = "Confirm Auto-Pay Settings", onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(if (enabled) "You are about to enable automatic monthly payments." else "You are about to disable automatic payments.", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont, fontSize = 14.sp)
            Spacer(Modifier.height(24.dp))
            if (enabled) {
                VoltflowCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ReceiptRow("Meter Number", meterNumber)
                        ReceiptRow("Payment Method", methodLabel)
                        ReceiptRow("Monthly Limit", amountFormatter(amountLimit))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            VoltflowButton(text = "Confirm", icon = Icons.Default.Check) { onConfirm() }
            Spacer(Modifier.height(24.dp))
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



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreen(state: UiState, innerPadding: PaddingValues, onBack: () -> Unit, onAdd: (String, String, Int, Int) -> Unit) {
    val methods = state.dashboard.paymentMethods
    val mfaEnabled = false
    val textColor = MaterialTheme.colorScheme.onSurface
    var showAddDialog by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).statusBarsPadding()) {
        VoltflowHeader(title = "Payment Methods", subtitle = "Manage your payment options", onBack = onBack)
        Spacer(Modifier.height(24.dp))
        if (methods.isEmpty()) {
            VoltflowCard {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("No payment methods", color = textColor, fontWeight = FontWeight.Bold, fontSize = 17.sp, fontFamily = VoltflowDesign.SoraFont)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPaymentMethodDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, Int) -> Unit
) {
    var brand by remember { mutableStateOf("Unknown") }
    var number by remember { mutableStateOf("") }
    var expiryMonth by remember { mutableStateOf("") }
    var expiryYear by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val view = LocalView.current

    val detectedBrand = remember(number) { MockPaymentProcessor.detectBrand(number) }
    brand = detectedBrand

     VoltflowModalSheet(title = "Add Payment Method", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Live Card Preview
            CardPreview(
                brand = brand,
                number = number,
                expiryMonth = expiryMonth,
                expiryYear = expiryYear
            )

            Spacer(Modifier.height(8.dp))

            VoltflowInput(
                value = number,
                onValueChange = { newValue ->
                    val digits = newValue.filter { it.isDigit() }.take(16)
                    number = digits
                },
                placeholder = "Card Number",
                leadingIcon = Icons.Default.CreditCard
            )

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

            // Trust Section
            SupportedCardsRow(activeBrand = brand)

            error?.let { Text(it, color = VoltflowDesign.DestructiveRed, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.ManropeFont) }
            
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val month = expiryMonth.toIntOrNull() ?: 0
                    val year = expiryYear.toIntOrNull() ?: 0
                    val luhnValid = MockPaymentProcessor.validateLuhn(number)
                    val detailsValid = number.length >= 12 && month in 1..12 && year >= 2025
                    
                    if (!luhnValid || !detailsValid) {
                        performScreenHaptic(view, android.view.HapticFeedbackConstants.REJECT)
                        error = if (!luhnValid) "Invalid card number checksum." else "Enter valid card details."
                        return@Button
                    }
                    error = null
                    onConfirm(brand, number.trim(), month, year)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.BlueAccent)
            ) {
                Text("Save Securely", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            }
        }
    }
}

@Composable
private fun CardPreview(brand: String, number: String, expiryMonth: String, expiryYear: String) {
    val textColor = Color.White
    val cardBg = VoltflowDesign.PrimaryGradient
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.background(cardBg)) {
            // Chip and Brand
            Row(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.size(44.dp, 32.dp).background(Color(0xFFE0E0E0), RoundedCornerShape(4.dp)))
                Text(
                    brand.uppercase(),
                    color = textColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    fontFamily = VoltflowDesign.SoraFont,
                    letterSpacing = 2.sp
                )
            }

            // Number
            val displayNum = remember(number) {
                val digits = number.padEnd(16, '•')
                digits.chunked(4).joinToString("  ")
            }
            Text(
                displayNum,
                modifier = Modifier.align(Alignment.Center).padding(top = 20.dp),
                color = textColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = VoltflowDesign.SoraFont,
                letterSpacing = 2.sp
            )

            // Expiry
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                Text("VALID THRU", color = textColor.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${expiryMonth.padStart(2, '•')}/${expiryYear.takeLast(2).padStart(2, '•')}",
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = VoltflowDesign.SoraFont
                )
            }
        }
    }
}

@Composable
private fun SupportedCardsRow(activeBrand: String) {
    val brands = listOf("Visa", "Mastercard", "American Express", "Discover")
    Column {
        Text("SUPPORTED CARDS", color = VoltflowDesign.GrayText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            brands.forEach { brand ->
                val isActive = brand == activeBrand
                val icon = when(brand) {
                    "Visa" -> Icons.Default.CreditCard
                    "Mastercard" -> Icons.Default.CreditCard
                    "American Express" -> Icons.Default.CreditCard
                    else -> Icons.Default.CreditCard
                }
                Icon(
                    icon,
                    contentDescription = brand,
                    tint = if (isActive) VoltflowDesign.BlueAccent else VoltflowDesign.GrayText.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedDevicesScreen(
    state: UiState,
    innerPadding: PaddingValues,
    onBack: () -> Unit,
    onRevoke: (String) -> Unit,
    onRefreshLocation: () -> Unit,
) {
    val devices = state.dashboard.devices
    val textColor = MaterialTheme.colorScheme.onSurface
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasLocationPermission = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) {
            onRefreshLocation()
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            onRefreshLocation()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).statusBarsPadding()) {
        VoltflowHeader(title = "Connected Devices", subtitle = "Manage active sessions", onBack = onBack)
        Spacer(Modifier.height(24.dp))
        VoltflowCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = VoltflowDesign.BlueAccent)
                Spacer(Modifier.width(12.dp))
                Text(
                    if (hasLocationPermission) "Location access enabled for last seen location"
                    else "Enable location to show last seen device location",
                    color = VoltflowDesign.GrayText,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                    fontFamily = VoltflowDesign.ManropeFont
                )
                Text(
                    if (hasLocationPermission) "Refresh" else "Enable",
                    color = VoltflowDesign.BlueAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        if (hasLocationPermission) {
                            onRefreshLocation()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    fontFamily = VoltflowDesign.ManropeFont
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (devices.isEmpty()) {
            VoltflowCard {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("No devices yet", color = textColor, fontWeight = FontWeight.Bold, fontSize = 17.sp, fontFamily = VoltflowDesign.SoraFont)
                    Spacer(Modifier.height(8.dp))
                    Text("Sign in to register this device.", color = VoltflowDesign.GrayText, fontSize = 14.sp, fontFamily = VoltflowDesign.ManropeFont)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(devices) { device ->
                    VoltflowCard {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Smartphone, contentDescription = null, tint = textColor)
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(device.deviceName, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VoltflowDesign.SoraFont)
                                Text(device.platform, color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = VoltflowDesign.GrayText, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(device.location ?: "Unknown location", color = VoltflowDesign.GrayText, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
                                }
                                Text("Last active: ${formatTimestamp(device.lastActive)}", color = VoltflowDesign.GrayText, fontSize = 12.sp, fontFamily = VoltflowDesign.ManropeFont)
                            }
                            if (device.deviceId != state.dashboard.currentDeviceId) {
                                Button(
                                    onClick = { onRevoke(device.deviceId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = VoltflowDesign.DestructiveBg, contentColor = VoltflowDesign.DestructiveRed),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Logout", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Surface(
                                    color = VoltflowDesign.BlueAccent.copy(alpha = 0.1f),
                                    contentColor = VoltflowDesign.BlueAccent,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "Current",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
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
}

@Composable
fun HelpCenterScreen(innerPadding: PaddingValues, onBack: () -> Unit) {
    val helpTopics = remember {
        listOf(
            HelpTopic("How to make a payment", "Navigate to the Pay tab, enter your amount and meter number, then choose a payment method."),
            HelpTopic("Enable Auto-Pay", "Go to Profile → Auto-Pay and toggle on, then choose your payment day and method."),
            HelpTopic("Add funds to VoltFlow Wallet", "Open Wallet and tap Add Funds to top up your balance."),
            HelpTopic("Secure your account", "Use Security Settings to create a transaction PIN, choose your lock scope, and enable biometric approval."),
            HelpTopic("How to add a payment method", "Open Payment Methods and tap Add Payment Method."),
            HelpTopic("Payment failed, what do I do?", "Check your wallet balance or card details, then retry the payment."),
            HelpTopic("Contact support", "Use the Contact Us screen to reach VoltFlow support.")
        )
    }
    var query by remember { mutableStateOf("") }
    val textColor = MaterialTheme.colorScheme.onSurface
    val results by remember(query) {
        mutableStateOf(
            helpTopics.filter {
                it.title.contains(query, ignoreCase = true) || it.body.contains(query, ignoreCase = true)
            }
        )
    }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var expandedTopic by remember { mutableStateOf<String?>(null) }

    // Point 15: Scrollable and only one can be expanded at a time
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).statusBarsPadding()) {
        VoltflowHeader(title = "Help Center", subtitle = "Find answers fast", onBack = onBack)
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp)
        ) {
            item {
                VoltflowInput(value = query, onValueChange = { query = it }, placeholder = "Search help topics", leadingIcon = Icons.Outlined.Search)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.weight(1f)) { VoltflowSupportButton(icon = Icons.Outlined.Chat, text = "Chat Support") { uriHandler.openUri("mailto:support@voltflow.app") } }
                    Box(modifier = Modifier.weight(1f)) { VoltflowSupportButton(icon = Icons.Outlined.Call, text = "Call Support") { uriHandler.openUri("tel:+14155550111") } }
                }
            }
            item {
                Text("Common Topics", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            }
            
            if (results.isEmpty()) {
                item {
                    VoltflowCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("No results for \"$query\"", color = textColor, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                            Spacer(Modifier.height(6.dp))
                            Text("Try a different search term.", color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont)
                        }
                    }
                }
            } else {
                items(results) { topic ->
                    val isExpanded = expandedTopic == topic.title
                    VoltflowCard(modifier = Modifier.fillMaxWidth().clickable { 
                        expandedTopic = if (isExpanded) null else topic.title 
                    }) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(topic.title, color = textColor, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontFamily = VoltflowDesign.SoraFont)
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = VoltflowDesign.GrayText
                                )
                            }
                            AnimatedVisibility(visible = isExpanded) {
                                Column {
                                    Spacer(Modifier.height(12.dp))
                                    Text(topic.body, color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont, lineHeight = 22.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class HelpTopic(val title: String, val body: String)

@Composable
private fun ExpandableHelpTopic(topic: HelpTopic) {
    var expanded by remember { mutableStateOf(false) }
    val textColor = MaterialTheme.colorScheme.onSurface
    VoltflowCard(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(topic.title, color = textColor, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
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
    val textColor = MaterialTheme.colorScheme.onSurface
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
                    Icon(Icons.Outlined.Place, contentDescription = null, tint = textColor, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("VoltFlow HQ", color = textColor, fontWeight = FontWeight.Bold, fontSize = 17.sp, fontFamily = VoltflowDesign.SoraFont)
                    Text("901 Market Street, San Francisco, CA", color = VoltflowDesign.GrayText, fontSize = 14.sp, fontFamily = VoltflowDesign.ManropeFont)
                }
            }
        }
    }
}

@Composable
fun VoltflowSupportButton(icon: ImageVector, text: String, onClick: () -> Unit) {
    val textColor = MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier.fillMaxWidth().height(60.dp).clickable { onClick() },
        color = VoltflowDesign.CardBg,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = VoltflowDesign.BlueAccent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = VoltflowDesign.SoraFont)
        }
    }
}


@Composable
fun TermsSection(title: String, text: String) {
    val textColor = MaterialTheme.colorScheme.onSurface
    Column {
        Text(title, color = textColor, fontWeight = FontWeight.Black, fontSize = 17.sp, fontFamily = VoltflowDesign.SoraFont)
        Spacer(Modifier.height(8.dp))
        Text(text, color = VoltflowDesign.GrayText, fontSize = 15.sp, lineHeight = 22.sp, fontFamily = VoltflowDesign.ManropeFont)
    }
}

@Composable
private fun VoltflowLogoMark(tint: Color) {
    Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_icon_voltflow),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun parseLocalDate(value: String?): java.time.LocalDate? {
    if (value.isNullOrBlank()) return null
    return runCatching { java.time.LocalDate.parse(value) }.getOrNull()
}

private fun performScreenHaptic(view: android.view.View, constant: Int) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        view.performHapticFeedback(constant)
    } else {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = VoltflowDesign.GrayText, fontSize = 14.sp, fontFamily = VoltflowDesign.ManropeFont)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = VoltflowDesign.ManropeFont)
    }
}

@Composable
fun TransactionReceiptScreen(
    state: UiState,
    innerPadding: PaddingValues,
    onDone: () -> Unit
) {
    val transaction = state.dashboard.transactions.firstOrNull() ?: return
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val token = remember(transaction.id) { QrCodeGenerator.generatePrepaidToken(transaction.id) }
    val textColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
        ) {
            item {
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(42.dp))
                }
                Spacer(Modifier.height(24.dp))
                Text("Payment Successful", color = textColor, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont, textAlign = TextAlign.Center)
                Text("Your receipt is ready", color = VoltflowDesign.GrayText, fontSize = 16.sp, fontFamily = VoltflowDesign.ManropeFont, textAlign = TextAlign.Center)
                Spacer(Modifier.height(40.dp))
            }
            
            item {
                VoltflowCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ReceiptRow("Amount", amountFormatter(transaction.amount))
                        ReceiptRow("Status", transaction.status.replaceFirstChar { it.uppercase() })
                        ReceiptRow("Method", transaction.paymentMethod)
                        ReceiptRow("Date", formatTimestamp(transaction.occurredAt))
                        transaction.meterNumber?.let { ReceiptRow("Meter", it) }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            
            item {
                Text("Prepaid Meter Token", color = VoltflowDesign.GrayText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = VoltflowDesign.IconCircleBg
                ) {
                    Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(token, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont, modifier = Modifier.weight(1f))
                        IconButton(onClick = { clipboard.setText(AnnotatedString(token)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = VoltflowDesign.BlueAccent, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
            
            item {
                Surface(
                    modifier = Modifier.size(180.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    border = BorderStroke(4.dp, VoltflowDesign.BlueAccent)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        QrCodeView(token = token, modifier = Modifier.fillMaxSize(), tintColor = Color.Black)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        
        Box(modifier = Modifier.padding(bottom = 32.dp)) {
            VoltflowButton(text = "Done") { onDone() }
        }
    }
}

@Composable
fun ResetPasswordScreen(
    onReset: (String) -> Unit,
    onBack: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val textColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .statusBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        VoltflowLogo(size = 64.dp)
        Spacer(Modifier.height(24.dp))
        Text("Reset Password", color = textColor, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont)
        Text("Create a new secure password", color = VoltflowDesign.GrayText, fontSize = 16.sp, textAlign = TextAlign.Center, fontFamily = VoltflowDesign.ManropeFont)
        
        Spacer(Modifier.height(40.dp))
        
        VoltflowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                VoltflowInput(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "New Password",
                    isPassword = true,
                    showPassword = showPassword,
                    onTogglePassword = { showPassword = !showPassword }
                )
                VoltflowInput(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = "Confirm Password",
                    isPassword = true,
                    showPassword = showPassword,
                    onTogglePassword = { showPassword = !showPassword }
                )
                
                error?.let { Text(it, color = VoltflowDesign.WarningAmber, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont) }
                
                Spacer(Modifier.height(8.dp))
                
                VoltflowButton(text = "Update Password") {
                    if (password.length < 8) {
                        error = "Password must be at least 8 characters."
                        return@VoltflowButton
                    }
                    if (password != confirmPassword) {
                        error = "Passwords do not match."
                        return@VoltflowButton
                    }
                    error = null
                    onReset(password)
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        Text(
            "Back to Login",
            color = VoltflowDesign.BlueAccent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onBack() },
            fontFamily = VoltflowDesign.ManropeFont
        )
    }
}

@Composable
fun PredictedBillWidget(predictedAmount: Double) {
    val textColor = MaterialTheme.colorScheme.onSurface
    VoltflowCard(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.background(Brush.linearGradient(listOf(VoltflowDesign.BlueAccent.copy(alpha = 0.08f), Color.Transparent)))) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(VoltflowDesign.BlueAccent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Insights, contentDescription = null, tint = VoltflowDesign.BlueAccent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Billing Forecast", color = textColor, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                        Text("Based on recent usage trends", color = VoltflowDesign.GrayText, fontSize = 13.sp, fontFamily = VoltflowDesign.ManropeFont)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Column {
                        Text("Estimated Next Bill", color = VoltflowDesign.GrayText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                        Spacer(Modifier.height(4.dp))
                        Text(amountFormatter(predictedAmount), color = textColor, fontSize = 32.sp, fontWeight = FontWeight.Black, fontFamily = VoltflowDesign.SoraFont)
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        color = VoltflowDesign.BlueAccent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = VoltflowDesign.BlueAccent, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Predictive", color = VoltflowDesign.BlueAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
                        }
                    }
                }
            }
        }
    }
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

@Composable
fun ProfileHubScreen(
    state: UiState,
    innerPadding: PaddingValues,
    darkModeEnabled: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onOpenProfileOptions: () -> Unit,
    onOpenPaymentMethods: () -> Unit,
    onOpenAutopay: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenConnectedDevices: () -> Unit,
    onOpenHelpCenter: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenContact: () -> Unit,
    onSignOut: () -> Unit,
) {
    if (state.isLoading) {
        ProfileSkeletonLayout(innerPadding)
        return
    }

    var showLogoutDialog by remember { mutableStateOf(false) }
    val profile = state.dashboard.profile
    val displayName = profile?.firstName?.let { "$it ${profile.lastName}" }?.trim()?.ifBlank { "Alex Johnson" } ?: "Alex Johnson"
    val textColor = MaterialTheme.colorScheme.onSurface

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        // Point 5: Top margin 56dp+ for Profile header
        contentPadding = PaddingValues(top = innerPadding.calculateTopPadding() + 80.dp, bottom = 120.dp),
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

        item { SectionLabel("BILLING") }
        item {
            VoltflowCard {
                Column {
                    VoltflowRow(icon = Icons.Outlined.Autorenew, title = "Auto-Pay", hasChevron = true, onClick = onOpenAutopay)
                    VoltflowDivider()
                    VoltflowRow(icon = Icons.Outlined.CreditCard, title = "Payment Methods", hasChevron = true, onClick = onOpenPaymentMethods)
                }
            }
        }

        item { SectionLabel("ACCOUNT") }
        item {
            VoltflowCard {
                Column {
                    VoltflowRow(icon = Icons.Outlined.Devices, title = "Connected Devices", hasChevron = true, onClick = onOpenConnectedDevices)
                    VoltflowDivider()
                    VoltflowRow(icon = Icons.Outlined.Notifications, title = "Notifications", hasChevron = true, onClick = onOpenNotifications)
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

