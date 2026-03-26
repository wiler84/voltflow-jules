package com.example.voltflow.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import java.util.UUID

enum class UtilityType(val label: String) {
    ELECTRICITY("Electricity"),
    WATER("Water"),
    GAS("Gas"),
    WALLET("Wallet")
}

enum class TransactionKind(val label: String) {
    UTILITY_PAYMENT("Utility payment"),
    WALLET_FUNDING("Wallet funding"),
    AUTOPAY_CHARGE("Autopay charge")
}

@Serializable
data class UserProfile(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String,
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
    @SerialName("email") val email: String = "",
    @SerialName("phone") val phone: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("dark_mode") val darkMode: Boolean = false,
    @SerialName("account_status") val accountStatus: String = "Pending",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class PaymentMethod(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String,
    @SerialName("card_last4") val cardLast4: String,
    @SerialName("card_brand") val cardBrand: String,
    @SerialName("expiry_month") val expiryMonth: Int,
    @SerialName("expiry_year") val expiryYear: Int,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class Wallet(
    @SerialName("user_id") val userId: String,
    @SerialName("balance") val balance: Double,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class UsageMetrics(
    @SerialName("user_id") val userId: String,
    @SerialName("total_spent") val totalSpent: Double = 0.0,
    @SerialName("electricity_spent") val electricitySpent: Double = 0.0,
    @SerialName("water_spent") val waterSpent: Double = 0.0,
    @SerialName("gas_spent") val gasSpent: Double = 0.0,
    @SerialName("monthly_usage") val monthlyUsage: Double = 0.0,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class AutopaySettings(
    @SerialName("user_id") val userId: String,
    @SerialName("enabled") val enabled: Boolean = false,
    @SerialName("payment_method_id") val paymentMethodId: String? = null,
    @SerialName("amount_limit") val amountLimit: Double = 0.0,
    @SerialName("billing_cycle") val billingCycle: String = "monthly",
    @SerialName("payment_day") val paymentDay: Int = 15,
    @SerialName("meter_number") val meterNumber: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class SecuritySettings(
    @SerialName("user_id") val userId: String,
    @SerialName("biometric_enabled") val biometricEnabled: Boolean = false,
    @SerialName("mfa_enabled") val mfaEnabled: Boolean = false,
    @SerialName("pin_enabled") val pinEnabled: Boolean = false,
    @SerialName("auto_lock_minutes") val autoLockMinutes: Int = 1,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class AppNotification(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String,
    @SerialName("title") val title: String,
    @SerialName("body") val body: String,
    @SerialName("type") val type: String,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class TransactionRecord(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String,
    @SerialName("kind") val kind: String,
    @SerialName("utility_type") val utilityType: String,
    @SerialName("amount") val amount: Double,
    @SerialName("status") val status: String,
    @SerialName("meter_number") val meterNumber: String? = null,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("payment_method_id") val paymentMethodId: String? = null,
    @SerialName("processor_reference") val processorReference: String? = null,
    @SerialName("description") val description: String,
    @SerialName("client_reference") val clientReference: String,
    @SerialName("metadata") val metadata: Map<String, String> = emptyMap(),
    @SerialName("occurred_at") val occurredAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class BillingAccount(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String,
    @SerialName("provider_name") val providerName: String,
    @SerialName("account_masked") val accountMasked: String,
    @SerialName("meter_number") val meterNumber: String,
    @SerialName("utility_type") val utilityType: String = "electricity",
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class Bill(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String,
    @SerialName("billing_account_id") val billingAccountId: String? = null,
    @SerialName("amount_due") val amountDue: Double,
    @SerialName("due_date") val dueDate: String,
    @SerialName("status") val status: String = "open",
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class WalletTransaction(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String,
    @SerialName("kind") val kind: String,
    @SerialName("amount") val amount: Double,
    @SerialName("method_label") val methodLabel: String,
    @SerialName("occurred_at") val occurredAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class UsageMetricPeriod(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String,
    @SerialName("period_start") val periodStart: String,
    @SerialName("period_end") val periodEnd: String,
    @SerialName("kwh_used") val kwhUsed: Double,
    @SerialName("amount_spent") val amountSpent: Double,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class PaymentRecord(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String,
    @SerialName("amount") val amount: Double,
    @SerialName("status") val status: String,
    @SerialName("processor") val processor: String,
    @SerialName("processor_reference") val processorReference: String,
    @SerialName("payment_method_id") val paymentMethodId: String? = null,
    @SerialName("source") val source: String,
    @SerialName("idempotency_key") val idempotencyKey: String,
    @SerialName("client_reference") val clientReference: String,
    @SerialName("metadata") val metadata: Map<String, String> = emptyMap(),
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class ConnectedDevice(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("platform") val platform: String,
    @SerialName("last_active") val lastActive: String,
    @SerialName("location") val location: String? = null,
    @SerialName("session_token") val sessionToken: String,
)

@Serializable
data class AnalyticsEvent(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String,
    @SerialName("event_name") val eventName: String,
    @SerialName("metadata") val metadata: Map<String, String> = emptyMap(),
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class EmailOutbox(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String,
    @SerialName("to_email") val toEmail: String,
    @SerialName("subject") val subject: String,
    @SerialName("body") val body: String,
    @SerialName("status") val status: String = "queued",
    @SerialName("metadata") val metadata: Map<String, String> = emptyMap(),
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class AuthTokens(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("expires_at") val expiresAt: Long? = null,
    @SerialName("user") val user: SupabaseUser,
)

@Serializable
data class SupabaseUser(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String? = null,
)

@Serializable
data class AuthRequest(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
)

@Serializable
data class SignUpRequest(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
    @SerialName("data") val data: Map<String, String>,
)

@Serializable
data class RefreshTokenRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class SessionSnapshot(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val expiresAtEpochSeconds: Long,
)

data class PaymentDraft(
    val utilityType: UtilityType,
    val amount: Double,
    val meterNumber: String,
    val paymentMethodId: String?,
    val useWallet: Boolean,
)

data class DashboardState(
    val greeting: String = "",
    val profile: UserProfile? = null,
    val wallet: Wallet? = null,
    val usage: UsageMetrics? = null,
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val transactions: List<TransactionRecord> = emptyList(),
    val recentTransactions: List<TransactionRecord> = emptyList(),
    val billingAccounts: List<BillingAccount> = emptyList(),
    val bills: List<Bill> = emptyList(),
    val walletTransactions: List<WalletTransaction> = emptyList(),
    val usagePeriods: List<UsageMetricPeriod> = emptyList(),
    val notifications: List<AppNotification> = emptyList(),
    val autopay: AutopaySettings? = null,
    val securitySettings: SecuritySettings? = null,
    val devices: List<ConnectedDevice> = emptyList(),
    val currentDeviceId: String? = null,
    val homeStatus: ScreenStatus = ScreenStatus(),
    val payStatus: ScreenStatus = ScreenStatus(),
    val historyStatus: ScreenStatus = ScreenStatus(),
    val walletStatus: ScreenStatus = ScreenStatus(),
    val autopayStatus: ScreenStatus = ScreenStatus(),
    val analyticsStatus: ScreenStatus = ScreenStatus(),
    val notificationsStatus: ScreenStatus = ScreenStatus(),
)

data class ScreenStatus(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEmpty: Boolean = false,
)

data class AuthFormState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val isCreatingAccount: Boolean = false,
)

data class UiState(
    val isLoading: Boolean = true,
    val isAuthenticated: Boolean = false,
    val dashboard: DashboardState = DashboardState(),
    val auth: AuthFormState = AuthFormState(),
    val restoredRoute: String? = null,
    val activeError: String? = null,
    val transientMessage: String? = null,
    val isOffline: Boolean = false,
)

data class PaymentResult(
    val transaction: TransactionRecord,
    val wallet: Wallet,
    val usage: UsageMetrics,
)

fun amountFormatter(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    formatter.currency = Currency.getInstance("USD")
    return formatter.format(amount)
}

fun greetingForCurrentTime(firstName: String?): String {
    val hour = LocalDateTime.now().hour
    val salutation = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..21 -> "Good Evening"
        else -> "Good Night"
    }
    return firstName?.takeIf { it.isNotBlank() }?.let { "$salutation, $it" } ?: salutation
}

fun formatTimestamp(iso: String?): String {
    if (iso.isNullOrBlank()) return "Just now"
    return runCatching {
        val time = LocalDateTime.ofInstant(Instant.parse(iso), ZoneId.systemDefault())
        time.format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
    }.getOrDefault("Just now")
}
