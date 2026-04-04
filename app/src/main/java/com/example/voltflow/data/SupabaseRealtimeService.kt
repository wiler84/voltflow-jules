package com.example.voltflow.data

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresListDataFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import android.util.Log
import java.time.Instant

data class RealtimeStreams(
    val wallet: Flow<Wallet>,
    val usage: Flow<UsageMetrics>,
    val autopay: Flow<AutopaySettings>,
    val paymentMethods: Flow<List<PaymentMethod>>,
    val transactions: Flow<List<TransactionRecord>>,
    val notifications: Flow<List<AppNotification>>,
    val billingAccounts: Flow<List<BillingAccount>>,
    val bills: Flow<List<Bill>>,
    val devices: Flow<List<ConnectedDevice>>,
)

class SupabaseRealtimeService(
    supabaseUrl: String,
    supabaseKey: String,
) {
    private val supabase = createSupabaseClient(
        supabaseUrl = supabaseUrl,
        supabaseKey = supabaseKey,
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
    }

    private var channel: RealtimeChannel? = null

    suspend fun connect(session: SessionSnapshot): RealtimeStreams {
        val expiresIn = (session.expiresAtEpochSeconds - Instant.now().epochSecond).coerceAtLeast(0)
        supabase.auth.importSession(
            UserSession(
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                expiresIn = expiresIn,
                tokenType = "Bearer",
                user = null,
            )
        )

        channel?.unsubscribe()
        val activeChannel = supabase.channel("voltflow-${session.userId}")
        channel = activeChannel

        // Safely subscribe to each table with error isolation
        // One broken subscription must not poison the rest
        
        // Single-item tables: wallets, usage, autopay_settings
        val walletFlow = try {
            activeChannel.postgresListDataFlow<Wallet, String>(schema = "public", table = "wallets", primaryKey = Wallet::userId)
                .map { list: List<Wallet> -> list.firstOrNull { it.userId == session.userId } ?: Wallet(userId = session.userId, balance = 0.0) }
        } catch (e: Exception) {
            Log.w("SupabaseRealtimeService", "Failed to subscribe to wallets: ${e.message}")
            flowOf(Wallet(userId = session.userId, balance = 0.0))
        }
        
        val usageFlow = try {
            activeChannel.postgresListDataFlow<UsageMetrics, String>(schema = "public", table = "usage", primaryKey = UsageMetrics::userId)
                .map { list: List<UsageMetrics> -> list.firstOrNull { it.userId == session.userId } ?: UsageMetrics(userId = session.userId) }
        } catch (e: Exception) {
            Log.w("SupabaseRealtimeService", "Failed to subscribe to usage: ${e.message}")
            flowOf(UsageMetrics(userId = session.userId))
        }
        
        val autopayFlow = try {
            activeChannel.postgresListDataFlow<AutopaySettings, String>(schema = "public", table = "autopay_settings", primaryKey = AutopaySettings::userId)
                .map { list: List<AutopaySettings> -> list.firstOrNull { it.userId == session.userId } ?: AutopaySettings(userId = session.userId) }
        } catch (e: Exception) {
            Log.w("SupabaseRealtimeService", "Failed to subscribe to autopay_settings: ${e.message}")
            flowOf(AutopaySettings(userId = session.userId))
        }
        
        // Multi-item tables: payment_methods, transactions, notifications, billing_accounts, bills, devices
        val methodsFlow = try {
            activeChannel.postgresListDataFlow<PaymentMethod, String>(schema = "public", table = "payment_methods", primaryKey = PaymentMethod::id)
                .map { list: List<PaymentMethod> -> list.filter { it.userId == session.userId } }
        } catch (e: Exception) {
            Log.w("SupabaseRealtimeService", "Failed to subscribe to payment_methods: ${e.message}")
            flowOf(emptyList<PaymentMethod>())
        }
        
        val transactionsFlow = try {
            activeChannel.postgresListDataFlow<TransactionRecord, String>(schema = "public", table = "transactions", primaryKey = TransactionRecord::id)
                .map { list: List<TransactionRecord> -> list.filter { it.userId == session.userId } }
        } catch (e: Exception) {
            Log.w("SupabaseRealtimeService", "Failed to subscribe to transactions: ${e.message}")
            flowOf(emptyList<TransactionRecord>())
        }
        
        val notificationsFlow = try {
            activeChannel.postgresListDataFlow<AppNotification, String>(schema = "public", table = "notifications", primaryKey = AppNotification::id)
                .map { list: List<AppNotification> -> list.filter { it.userId == session.userId } }
        } catch (e: Exception) {
            Log.w("SupabaseRealtimeService", "Failed to subscribe to notifications: ${e.message}")
            flowOf(emptyList<AppNotification>())
        }
        
        val billingAccountsFlow = try {
            activeChannel.postgresListDataFlow<BillingAccount, String>(schema = "public", table = "billing_accounts", primaryKey = BillingAccount::id)
                .map { list: List<BillingAccount> -> list.filter { it.userId == session.userId } }
        } catch (e: Exception) {
            Log.w("SupabaseRealtimeService", "Failed to subscribe to billing_accounts: ${e.message}")
            flowOf(emptyList<BillingAccount>())
        }

        // Bills subscription: if fails, fallback to empty but don't crash
        val billsFlow = try {
            activeChannel.postgresListDataFlow<Bill, String>(schema = "public", table = "bills", primaryKey = Bill::id)
                .map { list: List<Bill> -> list.filter { it.userId == session.userId } }
        } catch (e: Exception) {
            Log.w("SupabaseRealtimeService", "Failed to subscribe to bills (expected during schema repair): ${e.message}")
            flowOf(emptyList<Bill>())
        }

        val devicesFlow = try {
            activeChannel.postgresListDataFlow<ConnectedDevice, String>(schema = "public", table = "connected_devices", primaryKey = ConnectedDevice::id)
                .map { list: List<ConnectedDevice> -> list.filter { it.userId == session.userId } }
        } catch (e: Exception) {
            Log.w("SupabaseRealtimeService", "Failed to subscribe to connected_devices: ${e.message}")
            flowOf(emptyList<ConnectedDevice>())
        }

        activeChannel.subscribe()

        return RealtimeStreams(
            wallet = walletFlow,
            usage = usageFlow,
            autopay = autopayFlow,
            paymentMethods = methodsFlow,
            transactions = transactionsFlow,
            notifications = notificationsFlow,
            billingAccounts = billingAccountsFlow,
            bills = billsFlow,
            devices = devicesFlow,
        )
    }

    suspend fun disconnect() {
        channel?.unsubscribe()
        channel = null
    }
}
