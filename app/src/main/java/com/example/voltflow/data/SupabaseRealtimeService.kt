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
import java.time.Instant

data class RealtimeStreams(
    val wallet: Flow<Wallet>,
    val usage: Flow<UsageMetrics>,
    val autopay: Flow<AutopaySettings>,
    val paymentMethods: Flow<List<PaymentMethod>>,
    val transactions: Flow<List<TransactionRecord>>,
    val notifications: Flow<List<AppNotification>>,
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

        // We use client-side filtering to ensure compilation success across different library versions 
        // while maintaining the expected logic.
        val walletFlow = activeChannel.postgresListDataFlow<Wallet, String>(schema = "public", table = "wallets", primaryKey = Wallet::userId)
            .map { list: List<Wallet> -> list.first { it.userId == session.userId } }
            
        val usageFlow = activeChannel.postgresListDataFlow<UsageMetrics, String>(schema = "public", table = "usage", primaryKey = UsageMetrics::userId)
            .map { list: List<UsageMetrics> -> list.first { it.userId == session.userId } }
            
        val autopayFlow = activeChannel.postgresListDataFlow<AutopaySettings, String>(schema = "public", table = "autopay_settings", primaryKey = AutopaySettings::userId)
            .map { list: List<AutopaySettings> -> list.first { it.userId == session.userId } }
            
        val methodsFlow = activeChannel.postgresListDataFlow<PaymentMethod, String>(schema = "public", table = "payment_methods", primaryKey = PaymentMethod::id)
            .map { list: List<PaymentMethod> -> list.filter { it.userId == session.userId } }
            
        val transactionsFlow = activeChannel.postgresListDataFlow<TransactionRecord, String>(schema = "public", table = "transactions", primaryKey = TransactionRecord::id)
            .map { list: List<TransactionRecord> -> list.filter { it.userId == session.userId } }
            
        val notificationsFlow = activeChannel.postgresListDataFlow<AppNotification, String>(schema = "public", table = "notifications", primaryKey = AppNotification::id)
            .map { list: List<AppNotification> -> list.filter { it.userId == session.userId } }
            
        val devicesFlow = activeChannel.postgresListDataFlow<ConnectedDevice, String>(schema = "public", table = "connected_devices", primaryKey = ConnectedDevice::id)
            .map { list: List<ConnectedDevice> -> list.filter { it.userId == session.userId } }

        activeChannel.subscribe()

        return RealtimeStreams(
            wallet = walletFlow,
            usage = usageFlow,
            autopay = autopayFlow,
            paymentMethods = methodsFlow,
            transactions = transactionsFlow,
            notifications = notificationsFlow,
            devices = devicesFlow,
        )
    }

    suspend fun disconnect() {
        channel?.unsubscribe()
        channel = null
    }
}
