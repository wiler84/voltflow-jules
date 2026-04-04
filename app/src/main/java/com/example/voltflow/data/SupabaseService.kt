package com.example.voltflow.data

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.voltflow.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.util.Locale
import java.util.UUID

private const val PREFS_NAME = "voltflow_supabase"
private const val SESSION_KEY = "session_snapshot"
private const val DEVICE_ID_KEY = "device_id"

class SupabaseService(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences by lazy { createSecurePrefs(appContext) }
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val client = HttpClient(io.ktor.client.engine.okhttp.OkHttp) {
        install(ContentNegotiation) { json(json) }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("VoltflowHttp", message)
                }
            }
            level = LogLevel.INFO
        }
    }

    private val supabaseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY.trim()

    fun isConfigured(): Boolean = supabaseUrl.isNotBlank() && anonKey.isNotBlank()
    fun supabaseUrl(): String = supabaseUrl
    fun anonKey(): String = anonKey

    fun currentSession(): SessionSnapshot? {
        val raw = prefs.getString(SESSION_KEY, null) ?: return null
        return runCatching { json.decodeFromString<SessionSnapshot>(raw) }.getOrNull()
    }

    fun clearSession() {
        prefs.edit().remove(SESSION_KEY).apply()
    }

    fun currentDeviceId(): String {
        val existing = prefs.getString(DEVICE_ID_KEY, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
            ?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()
        prefs.edit().putString(DEVICE_ID_KEY, generated).apply()
        return generated
    }

    suspend fun signIn(email: String, password: String): SessionSnapshot {
        checkConfigured()
        val response = client.post("$supabaseUrl/auth/v1/token") {
            parameter("grant_type", "password")
            header("apikey", anonKey)
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(email = email, password = password))
        }
        val tokens = decodeResponse<AuthTokens>(response.bodyAsText())
        return saveSession(tokens)
    }

    suspend fun signUp(email: String, password: String, firstName: String, lastName: String) {
        checkConfigured()
        val response = client.post("$supabaseUrl/auth/v1/signup") {
            header("apikey", anonKey)
            contentType(ContentType.Application.Json)
            setBody(
                SignUpRequest(
                    email = email,
                    password = password,
                    data = mapOf("first_name" to firstName, "last_name" to lastName),
                )
            )
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IllegalStateException(parseError(body, null))
        }
        // Signup successful. Note: We don't parse tokens here as Supabase might return User only.
        // The repository should follow up with a signIn if immediate session is expected.
    }

    suspend fun signOut() {
        val session = currentSession() ?: return
        runCatching {
            client.post("$supabaseUrl/auth/v1/logout") {
                authorized(session.accessToken)
            }
        }
        clearSession()
    }

    suspend fun requireValidSession(): SessionSnapshot {
        val session = currentSession() ?: throw IllegalStateException("Authentication required")
        val now = Instant.now().epochSecond
        if (session.expiresAtEpochSeconds > now + 60) return session
        return refreshSession(session.refreshToken)
    }

    suspend fun refreshSession(refreshToken: String): SessionSnapshot {
        checkConfigured()
        val response = client.post("$supabaseUrl/auth/v1/token") {
            parameter("grant_type", "refresh_token")
            header("apikey", anonKey)
            contentType(ContentType.Application.Json)
            setBody(RefreshTokenRequest(refreshToken))
        }
        val tokens = decodeResponse<AuthTokens>(response.bodyAsText())
        return saveSession(tokens)
    }

    suspend fun fetchProfile(userId: String): UserProfile? = fetchListSafe<UserProfile>("profiles", userId).firstOrNull()
    suspend fun fetchWallet(userId: String): Wallet? = fetchListSafe<Wallet>("wallets", userId).firstOrNull()
    suspend fun fetchUsage(userId: String): UsageMetrics? = fetchListSafe<UsageMetrics>("usage", userId).firstOrNull()
    suspend fun fetchAutopay(userId: String): AutopaySettings? = fetchListSafe<AutopaySettings>("autopay_settings", userId).firstOrNull()
    suspend fun fetchSecuritySettings(userId: String): SecuritySettings? = fetchListSafe<SecuritySettings>("security_settings", userId).firstOrNull()
    suspend fun fetchBillingAccounts(userId: String): List<BillingAccount> =
        fetchListSafe("billing_accounts", userId, order = "is_default.desc,created_at.desc")

    suspend fun fetchBills(userId: String, limit: Int? = null): List<Bill> =
        fetchListSafe("bills", userId, order = "created_at.desc", limit = limit)

    suspend fun fetchWalletTransactions(userId: String, limit: Int? = null): List<WalletTransaction> =
        fetchListSafe("wallet_transactions", userId, order = "occurred_at.desc", limit = limit)

    suspend fun fetchUsageMetrics(userId: String): List<UsageMetricPeriod> =
        fetchListSafe("usage_metrics", userId, order = "period_start.desc")

    suspend fun fetchPaymentMethods(userId: String): List<PaymentMethod> =
        fetchListSafe("payment_methods", userId, order = "is_default.desc,created_at.desc")

    suspend fun fetchTransactions(userId: String, limit: Int? = null): List<TransactionRecord> =
        fetchListSafe("transactions", userId, order = "created_at.desc", limit = limit)

    suspend fun fetchNotifications(userId: String): List<AppNotification> =
        fetchListSafe("notifications", userId, order = "created_at.desc")

    suspend fun fetchDevices(userId: String): List<ConnectedDevice> =
        fetchListSafe("connected_devices", userId, order = "last_active.desc")

    suspend fun upsertProfile(profile: UserProfile) {
        upsert("profiles", profile, conflictColumn = "user_id")
    }

    suspend fun upsertWallet(wallet: Wallet) {
        upsert("wallets", wallet, conflictColumn = "user_id")
    }

    suspend fun upsertUsage(usageMetrics: UsageMetrics) {
        upsert("usage", usageMetrics, conflictColumn = "user_id")
    }

    suspend fun upsertAutopay(settings: AutopaySettings) {
        upsert("autopay_settings", settings, conflictColumn = "user_id")
    }

    suspend fun upsertSecuritySettings(settings: SecuritySettings) {
        upsert("security_settings", settings, conflictColumn = "user_id")
    }

    suspend fun requestPinResetToken(): PinResetRequestResult {
        val session = requireValidSession()
        val response = client.post("$supabaseUrl/functions/v1/request-pin-reset") {
            authorized(session.accessToken)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { })
        }
        val raw = response.bodyAsText()
        ensureSuccess(raw, response.status.isSuccess())
        val root = runCatching { json.parseToJsonElement(raw) as? JsonObject }.getOrNull()
        return PinResetRequestResult(
            cooldownSeconds = root?.get("cooldown_seconds")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 59,
            resendCount = root?.get("resend_count")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
            maxResends = root?.get("max_resends")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 4,
            expiresAt = root?.get("expires_at")?.jsonPrimitive?.contentOrNull
        )
    }

    suspend fun verifyPinResetToken(token: String): Boolean {
        val session = requireValidSession()
        val response = client.post("$supabaseUrl/functions/v1/verify-pin-reset-token") {
            authorized(session.accessToken)
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("token", token)
                }
            )
        }
        val raw = response.bodyAsText()
        ensureSuccess(raw, response.status.isSuccess())
        val root = runCatching { json.parseToJsonElement(raw) as? JsonObject }.getOrNull()
        return root?.get("valid")?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: true
    }

    suspend fun completePinReset(token: String, newPin: String) {
        val session = requireValidSession()
        val response = client.post("$supabaseUrl/functions/v1/complete-pin-reset") {
            authorized(session.accessToken)
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("token", token)
                    put("new_pin", newPin)
                }
            )
        }
        ensureSuccess(response.bodyAsText(), response.status.isSuccess())
    }

    suspend fun addPaymentMethod(paymentMethod: PaymentMethod) {
        insert("payment_methods", paymentMethod)
    }

    suspend fun addPayment(payment: PaymentRecord) {
        insert("payments", payment)
    }

    suspend fun addTransaction(transaction: TransactionRecord) {
        insert("transactions", transaction)
    }

    suspend fun addNotification(notification: AppNotification) {
        insert("notifications", notification)
    }

    suspend fun addBillingAccount(account: BillingAccount) {
        insert("billing_accounts", account)
    }

    suspend fun addBill(bill: Bill) {
        insert("bills", bill)
    }

    suspend fun addWalletTransaction(transaction: WalletTransaction) {
        insert("wallet_transactions", transaction)
    }

    suspend fun addUsageMetric(metric: UsageMetricPeriod) {
        insert("usage_metrics", metric)
    }

    suspend fun upsertBillingAccount(account: BillingAccount) {
        upsert("billing_accounts", account, conflictColumn = "id")
    }

    suspend fun upsertBill(bill: Bill) {
        upsert("bills", bill, conflictColumn = "id")
    }

    suspend fun addAnalyticsEvent(event: AnalyticsEvent) {
        insert("analytics_events", event)
    }

    suspend fun addEmailOutbox(email: EmailOutbox) {
        insert("email_outbox", email)
    }

    suspend fun upsertDevice(device: ConnectedDevice) {
        upsert("connected_devices", device, conflictColumn = "user_id,device_id")
    }

    suspend fun markNotificationRead(userId: String, notificationId: String) {
        val session = requireValidSession()
        val response = client.post("$supabaseUrl/rest/v1/notifications") {
            restHeaders(session.accessToken)
            parameter("id", "eq.$notificationId")
            parameter("user_id", "eq.$userId")
            header("Prefer", "resolution=merge-duplicates,return=representation")
            contentType(ContentType.Application.Json)
            // Use explicit JSON string to avoid Ktor serialization issues with mixed-type maps
            setBody("""{"is_read":true,"read_at":"${Instant.now()}"}""")
        }
        ensureSuccess(response.bodyAsText(), response.status.isSuccess())
    }

    suspend fun deleteDevice(userId: String, deviceId: String) {
        val session = requireValidSession()
        val response = client.delete("$supabaseUrl/rest/v1/connected_devices") {
            restHeaders(session.accessToken)
            parameter("user_id", "eq.$userId")
            parameter("device_id", "eq.$deviceId")
        }
        ensureSuccess(response.bodyAsText(), response.status.isSuccess())
    }

    fun buildCurrentDevice(sessionToken: String, userId: String, pushToken: String? = null): ConnectedDevice {
        val now = Instant.now().toString()
        return ConnectedDevice(
            userId = userId,
            deviceId = currentDeviceId(),
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            platform = "Android ${Build.VERSION.RELEASE}",
            lastActive = now,
            location = bestEffortLocationLabel(),
            sessionToken = sessionToken,
            pushToken = pushToken,
        )
    }

    private fun bestEffortLocationLabel(): String? {
        val fineGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) return null

        return runCatching {
            val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val providers = manager.getProviders(true)
            val lastKnown = providers
                .mapNotNull { provider ->
                    runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
                }
                .maxByOrNull { it.time }
                ?: return "Location unavailable"
            val lat = String.format(Locale.US, "%.4f", lastKnown.latitude)
            val lon = String.format(Locale.US, "%.4f", lastKnown.longitude)
            "Lat $lat, Lon $lon"
        }.getOrNull()
    }

    private suspend inline fun <reified T : Any> fetchListSafe(
        table: String,
        userId: String,
        order: String? = null,
        limit: Int? = null,
    ): List<T> = runCatching {
        val session = requireValidSession()
        val response = client.get("$supabaseUrl/rest/v1/$table") {
            restHeaders(session.accessToken)
            parameter("user_id", "eq.$userId")
            parameter("select", "*")
            if (!order.isNullOrBlank()) {
                parameter("order", order)
            }
            if (limit != null) {
                parameter("limit", limit)
            }
        }
        if (response.status.value == 404 || response.status.value == 400) {
            Log.w("SupabaseService", "Table or column missing: $table. Returning empty list.")
            return emptyList()
        }
        ensureSuccess(response.bodyAsText(), response.status.isSuccess())
        decodeListResponse<T>(response.bodyAsText())
    }.getOrElse { error ->
        Log.e("SupabaseService", "Failed to fetch from $table", error)
        emptyList()
    }

    private suspend inline fun <reified T : Any> insert(table: String, body: T) {
        runCatching {
            val session = requireValidSession()
            val response = client.post("$supabaseUrl/rest/v1/$table") {
                restHeaders(session.accessToken)
                header("Prefer", "return=representation")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            ensureSuccess(response.bodyAsText(), response.status.isSuccess())
        }.onFailure { Log.e("SupabaseService", "Insert failed for $table", it) }
    }

    private suspend inline fun <reified T : Any> upsert(table: String, body: T, conflictColumn: String) {
        runCatching {
            val session = requireValidSession()
            val response = client.post("$supabaseUrl/rest/v1/$table") {
                restHeaders(session.accessToken)
                parameter("on_conflict", conflictColumn)
                header("Prefer", "resolution=merge-duplicates,return=representation")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            ensureSuccess(response.bodyAsText(), response.status.isSuccess())
        }.onFailure { Log.e("SupabaseService", "Upsert failed for $table", it) }
    }

    private fun saveSession(tokens: AuthTokens): SessionSnapshot {
        val snapshot = SessionSnapshot(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            userId = tokens.user.id,
            email = tokens.user.email.orEmpty(),
            expiresAtEpochSeconds = tokens.expiresAt ?: (Instant.now().epochSecond + tokens.expiresIn),
        )
        prefs.edit().putString(SESSION_KEY, json.encodeToString(snapshot)).apply()
        return snapshot
    }

    private fun checkConfigured() {
        if (!isConfigured()) {
            throw IllegalStateException("Set SUPABASE_URL and SUPABASE_ANON_KEY in gradle.properties")
        }
    }

    private inline fun <reified T> decodeResponse(raw: String): T = runCatching {
        json.decodeFromString<T>(raw)
    }.getOrElse { throwable ->
        throw IllegalStateException(parseError(raw, throwable), throwable)
    }

    private inline fun <reified T> decodeListResponse(raw: String): List<T> = runCatching {
        json.decodeFromString<List<T>>(raw)
    }.getOrElse { throwable ->
        throw IllegalStateException(parseError(raw, throwable), throwable)
    }

    private fun ensureSuccess(raw: String, success: Boolean) {
        if (success) return
        throw IllegalStateException(parseError(raw, null))
    }

    private fun parseError(raw: String, throwable: Throwable?): String {
        return try {
            val root = json.parseToJsonElement(raw)
            when (root) {
                is JsonObject -> {
                    root["msg"]?.jsonPrimitive?.contentOrNull
                        ?: root["message"]?.jsonPrimitive?.contentOrNull
                        ?: raw.ifBlank { throwable?.message ?: "Unknown Supabase error" }
                }
                else -> raw.ifBlank { throwable?.message ?: "Unknown Supabase error" }
            }
        } catch (_: SerializationException) {
            raw.ifBlank { throwable?.message ?: "Unknown Supabase error" }
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.authorized(accessToken: String) {
        header("apikey", anonKey)
        header(HttpHeaders.Authorization, "Bearer $accessToken")
    }

    private fun io.ktor.client.request.HttpRequestBuilder.restHeaders(accessToken: String) {
        authorized(accessToken)
        header(HttpHeaders.Accept, "application/json")
        header("X-Client-Info", "voltflow-android")
    }

    private fun createSecurePrefs(context: Context): SharedPreferences {
        return runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrElse { error ->
            Log.w("VoltflowPrefs", "Encrypted prefs unavailable, falling back to unencrypted storage.", error)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
}

private val kotlinx.serialization.json.JsonPrimitive.contentOrNull: String?
    get() = runCatching { content }.getOrNull()
