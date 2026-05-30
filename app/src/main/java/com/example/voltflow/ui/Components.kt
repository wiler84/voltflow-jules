package com.example.voltflow.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.SignalWifiOff
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voltflow.bouncyClick
import com.example.voltflow.Screen
import com.example.voltflow.R
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image

data class NavItem(val label: String, val screen: Screen, val icon: ImageVector)

fun performPlatformHaptic(view: android.view.View, constant: Int) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        view.performHapticFeedback(constant)
    } else {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }
}

@Composable
fun VoltflowButton(text: String, icon: ImageVector? = null, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(60.dp).bouncyClick { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent
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
        modifier = Modifier.fillMaxWidth().height(60.dp).bouncyClick { onClick() },
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
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isError) VoltflowDesign.DestructiveRed.copy(alpha = 0.1f) else VoltflowDesign.InputBg
    val borderColor = if (isError) VoltflowDesign.DestructiveRed else Color.Transparent
    
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        placeholder = { Text(placeholder, color = VoltflowDesign.GrayText, fontFamily = VoltflowDesign.ManropeFont) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = if (isError) VoltflowDesign.DestructiveRed else VoltflowDesign.BlueAccent
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
fun VoltflowHeader(title: String, subtitle: String = "", onBack: () -> Unit) {
    val textColor = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(44.dp).clip(CircleShape).background(VoltflowDesign.HeaderCircle)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = textColor)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = VoltflowDesign.SoraFont)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = VoltflowDesign.GrayText, fontSize = 14.sp, fontFamily = VoltflowDesign.ManropeFont)
            }
        }
    }
}

@Composable
fun VoltflowCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = VoltflowDesign.CardBg,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        shadowElevation = 2.dp
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
    isDropdown: Boolean = false,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier.fillMaxWidth().height(72.dp).bouncyClick(enabled = onClick != null) { onClick?.invoke() }.padding(horizontal = 20.dp),
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
        modifier = modifier.height(56.dp).bouncyClick { onClick() },
        color = VoltflowDesign.CardBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(VoltflowDesign.IconCircleBg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = VoltflowDesign.BlueAccent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = VoltflowDesign.ManropeFont)
        }
    }
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

@Composable
fun DegradedModeBanner(modifier: Modifier = Modifier) {
    val textColor = MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(16.dp)),
        color = VoltflowDesign.CardBgSolid,
        border = BorderStroke(1.dp, VoltflowDesign.WarningAmber.copy(alpha = 0.4f)),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = VoltflowDesign.WarningAmber, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Degraded Mode (Cached Data)", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = VoltflowDesign.ManropeFont)
        }
    }
}

@Composable
fun VoltflowLogo(modifier: Modifier = Modifier, size: Dp = 72.dp) {
    Image(
        painter = painterResource(id = R.drawable.ic_launcher_foreground_logo),
        contentDescription = "Voltflow Logo",
        modifier = modifier.size(size)
    )
}
