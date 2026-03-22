package com.example.voltflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Home : Screen("home")
    data object Pay : Screen("pay")
    data object Wallet : Screen("wallet")
    data object History : Screen("history")
    data object Notifications : Screen("notifications")
    data object Usage : Screen("usage")
    data object AutoPay : Screen("autopay")
    data object PaymentMethods : Screen("payment_methods")
    data object ConnectedDevices : Screen("connected_devices")
    data object Profile : Screen("profile")
    data object ProfileOptions : Screen("profile_options")
    data object SecuritySettings : Screen("security_settings")
    data object HelpCenter : Screen("help_center")
    data object Terms : Screen("terms")
    data object Contact : Screen("contact")

    companion object {
        fun fromRoute(route: String): Screen? = when (route) {
            Auth.route -> Auth
            Home.route -> Home
            Pay.route -> Pay
            Wallet.route -> Wallet
            History.route -> History
            Notifications.route -> Notifications
            Usage.route -> Usage
            AutoPay.route -> AutoPay
            PaymentMethods.route -> PaymentMethods
            ConnectedDevices.route -> ConnectedDevices
            Profile.route -> Profile
            ProfileOptions.route -> ProfileOptions
            SecuritySettings.route -> SecuritySettings
            HelpCenter.route -> HelpCenter
            Terms.route -> Terms
            Contact.route -> Contact
            else -> null
        }
    }
}

@Stable
class AppNavigator(initial: Screen) {
    private val backStack = mutableStateListOf(initial)

    val currentScreen: Screen
        get() = backStack.last()

    val canPop: Boolean
        get() = backStack.size > 1

    fun replaceRoot(screen: Screen) {
        backStack.clear()
        backStack += screen
    }

    fun navigate(screen: Screen) {
        if (screen == currentScreen) return
        backStack += screen
    }

    fun switchTab(screen: Screen) {
        if (currentScreen == screen) return
        val root = backStack.firstOrNull() ?: screen
        backStack.clear()
        backStack += root
        if (screen != root) backStack += screen
    }

    fun pop(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }
}

@Composable
fun rememberAppNavigator(initial: Screen): AppNavigator = remember(initial) { AppNavigator(initial) }

fun screenFromRoute(route: String?): Screen? = when (route) {
    Screen.Auth.route -> Screen.Auth
    Screen.Home.route -> Screen.Home
    Screen.Pay.route -> Screen.Pay
    Screen.Wallet.route -> Screen.Wallet
    Screen.History.route -> Screen.History
    Screen.Notifications.route -> Screen.Notifications
    Screen.Usage.route -> Screen.Usage
    Screen.AutoPay.route -> Screen.AutoPay
    Screen.PaymentMethods.route -> Screen.PaymentMethods
    Screen.ConnectedDevices.route -> Screen.ConnectedDevices
    Screen.Profile.route -> Screen.Profile
    Screen.ProfileOptions.route -> Screen.ProfileOptions
    Screen.SecuritySettings.route -> Screen.SecuritySettings
    Screen.HelpCenter.route -> Screen.HelpCenter
    Screen.Terms.route -> Screen.Terms
    Screen.Contact.route -> Screen.Contact
    else -> null
}
