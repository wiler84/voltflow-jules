package com.example.voltflow

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

enum class ButtonState { Pressed, Idle }

/**
 * Professional Shake Animation for error feedback.
 */
fun Modifier.errorShake(trigger: Any?): Modifier = composed {
    val shakeOffset = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(trigger) {
        if (trigger != null && trigger != false && (trigger !is String || trigger.isNotEmpty())) {
            haptic.performHapticFeedback(HapticFeedbackType.Reject)
            repeat(4) {
                shakeOffset.animateTo(
                    targetValue = 10f,
                    animationSpec = spring(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioMediumBouncy)
                )
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                shakeOffset.animateTo(
                    targetValue = -10f,
                    animationSpec = spring(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioMediumBouncy)
                )
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            shakeOffset.animateTo(0f)
        }
    }

    this.graphicsLayer {
        translationX = shakeOffset.value
    }
}

/**
 * World-class UI interaction: Adds a physical "press" depth and 
 * stiffer spring-back to any clickable element for an "Electric" feel.
 */
fun Modifier.bouncyClick(
    enabled: Boolean = true,
    hapticFeedback: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    var buttonState by remember { mutableStateOf(ButtonState.Idle) }
    val haptic = LocalHapticFeedback.current
    
    val scale by animateFloatAsState(
        targetValue = if (buttonState == ButtonState.Pressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = 0.35f, // More "electric" snap
            stiffness = 800f      // Senior Staff level stiffness
        ),
        label = "scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(false)
                    buttonState = ButtonState.Pressed
                    waitForUpOrCancellation()
                    buttonState = ButtonState.Idle
                }
            }
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null, 
            enabled = enabled,
            onClick = {
                if (hapticFeedback) {
                    // PREMIUM FEEL: Use "Virtual Key" for a crisp mechanical click sensation
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onClick()
            }
        )
}
