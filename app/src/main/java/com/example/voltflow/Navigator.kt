package com.example.voltflow

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable

class AppNavigator(initial: Screen = Screen.Home) {
    var currentScreen: getValue by mutableStateOf(initial)

    fun navigate(screen: Screen) {
        currentScreen = screen
    }
}

@Composable
fun rememberAppNavigator(initial: Screen = Screen.Home): AppNavigator {
    return remember { AppNavigator(initial) }
}
