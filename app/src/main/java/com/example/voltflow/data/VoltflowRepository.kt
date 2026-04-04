package com.example.voltflow.data

import android.content.Context
import android.util.Log
import com.example.voltflow.data.local.BillEntity
import com.example.voltflow.data.local.NotificationEntity
import com.example.voltflow.data.local.ProfileEntity
import com.example.voltflow.data.local.TransactionEntity
import com.example.voltflow.data.local.VoltflowDatabase
import com.example.voltflow.data.local.WalletEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt
import java.time.Duration
import java.time.Instant
import java.util.UUID

class VoltflowRepository(
    context: Context,
    private val paymentProcessor: PaymentProcessor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val service = SupabaseService(context)
    private val preferences = UserPreferencesStore(context)
    private val database = VoltflowDatabase.create(context)
    private val dao = database.dao()
    private val networkMonitor = NetworkMonitor(context)
    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private var realtimeService: SupabaseRealtimeService? = null
    private var realtimeStreams: RealtimeStreams? = null
    @Volatile
    private var lastKnownPushToken: String? = null

    private val _uiState = MutableStateFlow(
        UiState(
            isLoading = true,
            activeError = if (service.isConfigured()) null else "Configure Supabase credentials in gradle.properties.",
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        repositoryScope.launch {
            restoreSession()
            startSyncLoop()
        }
        repositoryScope.launch {
            lastKnownPushToken = preferences.getPushToken()
        }
        repositoryScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOffline = !online) }
            }
        }
    }

    suspend fun restoreSession() {
        mutateLoading(true)
        withContext(ioDispatcher) {
            val session = service.currentSession()
            if (session == null) {
                _uiState.update { it.copy(isLoading = false, isAuthenticated = false) }
                return@withContext
            }
            runCatching {
                service.requireValidSession()
                startRealtimeSubscriptions(session)
                syncAllData(session.userId)
            }.onFailure { error ->
                Log.e("VoltflowRepository", "restoreSession", error)
                service.clearSession()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = false,
                        activeError = error.message,
                    )
                }
            }
        }
        mutateLoading(false)
    }

    suspend fun signIn(email: String, password: String) {
        mutateLoading(true)
        withContext(ioDispatcher) {
            runCatching {
                val session = service.signIn(email, password)
                ensureBootstrapRecords(session)
                startRealtimeSubscriptions(session)
                syncAllData(session.userId)
                service.upsertDevice(service.buildCurrentDevice(session.accessToken, session.userId, lastKnownPushToken))
                trackEvent(session.userId, "sign_in")
            }.onFailure { publishError(it) }
        }
        mutateLoading(false)
    }

    suspend fun signUp(email: String, password: String, firstName: String, lastName: String) {
        mutateLoading(true)
        withContext(ioDispatcher) {
            runCatching {
                // Fix for Error 1: Supabase signUp returns a User object, not always a session with tokens.
                service.signUp(email, password, firstName, lastName)
                
                // Follow up with signIn to ensure we have a session snapshot.
                val session = service.signIn(email, password)
                
                ensureBootstrapRecords(session)
                startRealtimeSubscriptions(session)
                service.upsertDevice(service.buildCurrentDevice(session.accessToken, session.userId, lastKnownPushToken))
                syncAllData(session.userId)
                service.addNotification(
                    AppNotification(
                        userId = session.userId,
                        title = "Welcome to Voltflow",
                        body = "Your account is ready for utility payments.",
                        type = "system",
                    )
                )
                trackEvent(session.userId, "sign_up")
            }.onFailure { error ->
                Log.e("VoltflowRepository", "Signup failed", error)
                publishError(error)
            }
        }
        mutateLoading(false)
    }

    private suspend fun startRealtimeSubscriptions(session: SessionSnapshot) {
        try {
            // Initialize realtime service with Supabase credentials
            realtimeService = SupabaseRealtimeService(service.supabaseUrl(), service.anonKey())
            realtimeStreams = realtimeService?.connect(session)
            
            // Launch collection jobs for realtime flows
            realtimeStreams?.let { streams ->
                repositoryScope.launch {
                    streams.wallet.collect { wallet ->
                        _uiState.update { it.copy(dashboard = it.dashboard.copy(wallet = wallet)) }
                        // Cache to Room
                        dao.upsertWallet(WalletEntity(userId = session.userId, balance = wallet.balance, updatedAt = wallet.updatedAt))
                    }
                }
                repositoryScope.launch {
                    streams.transactions.collect { transactions ->
                        _uiState.update { it.copy(dashboard = it.dashboard.copy(transactions = transactions)) }
                    }
                }
                repositoryScope.launch {
                    streams.notifications.collect { notifications ->
                        _uiState.update { it.copy(dashboard = it.dashboard.copy(notifications = notifications)) }
                        // Cache to Room
                        dao.upsertNotifications(
                            notifications.map { notification ->
                                NotificationEntity(
                                    id = notification.id,
                                    userId = session.userId,
                                    title = notification.title,
                                    body = notification.body,
                                    type = notification.type,
                                    isRead = notification.isRead,
                                    createdAt = notification.createdAt,
                                )
                            }
                        )
                    }
                }
                repositoryScope.launch {
                    streams.autopay.collect { autopay ->
                        _uiState.update { it.copy(dashboard = it.dashboard.copy(autopay = autopay)) }
                    }
                }
                repositoryScope.launch {
                    streams.billingAccounts.collect { accounts ->
                        _uiState.update { it.copy(dashboard = it.dashboard.copy(billingAccounts = accounts)) }
                    }
                }
                repositoryScope.launch {
                    streams.bills.collect { bills ->
                        _uiState.update { it.copy(dashboard = it.dashboard.copy(bills = bills)) }
                        // Cache to Room
                        dao.upsertBills(
                            bills.map { bill ->
                                BillEntity(
                                    id = bill.id,
                                    userId = session.userId,
                                    amountDue = bill.amountDue,
                                    dueDate = bill.dueDate,
                                    status = bill.status,
                                    createdAt = bill.createdAt,
                                )
                            }
                        )
                    }
                }
                repositoryScope.launch {
                    streams.paymentMethods.collect { methods ->
                        _uiState.update { it.copy(dashboard = it.dashboard.copy(paymentMethods = methods)) }
                    }
                }
                repositoryScope.launch {
                    streams.usage.collect { usage ->
                        _uiState.update { it.copy(dashboard = it.dashboard.copy(usage = usage)) }
                    }
                }
                repositoryScope.launch {
                    streams.devices.collect { devices ->
                        _uiState.update { it.copy(dashboard = it.dashboard.copy(devices = devices)) }
                    }
                }
            }
            Log.d("VoltflowRepository", "Realtime subscriptions started successfully")
        } catch (e: Exception) {
            Log.w("VoltflowRepository", "Realtime subscriptions failed, will use polling fallback", e)
        }
    }

    private suspend fun stopRealtimeSubscriptions() {
        try {
            realtimeService?.disconnect()
            realtimeService = null
            realtimeStreams = null
            Log.d("VoltflowRepository", "Realtime subscriptions stopped")
        } catch (e: Exception) {
            Log.w("VoltflowRepository", "Error stopping realtime subscriptions", e)
        }
    }

    suspend fun signOut() {
        withContext(ioDispatcher) {
            val currentUserId = service.currentSession()?.userId
            runCatching {
                if (currentUserId != null) {
                    service.deleteDevice(currentUserId, service.currentDeviceId())
                }
                service.signOut()
            }
            stopRealtimeSubscriptions()
            resetToSignedOutState()
        }
    }

    suspend fun revokeDevice(deviceId: String) {
        withCurrentUser { session ->
            val currentDeviceId = service.currentDeviceId()
            if (deviceId == currentDeviceId) {
                runCatching { service.deleteDevice(session.userId, deviceId) }
                service.signOut()
                resetToSignedOutState()
                return@withCurrentUser
            }
            service.deleteDevice(session.userId, deviceId)
            service.addNotification(
                AppNotification(
                    userId = session.userId,
                    title = "Device signed out",
                    body = "A device was removed from your active sessions.",
                    type = "security",
                )
            )
            trackEvent(session.userId, "device_revoked")
            syncAllData(session.userId)
        }
    }

    suspend fun refresh() {
        withContext(ioDispatcher) {
            val userId = service.currentSession()?.userId ?: return@withContext
            runCatching { syncAllData(userId) }.onFailure { publishError(it) }
        }
    }

    suspend fun saveProfile(firstName: String, lastName: String, phone: String) {
        withCurrentUser { session ->
            val current = uiState.value.dashboard.profile
            service.upsertProfile(
                UserProfile(
                    id = current?.id ?: UUID.randomUUID().toString(),
                    userId = session.userId,
                    firstName = firstName,
                    lastName = lastName,
                    email = session.email,
                    phone = phone.ifBlank { null },
                    location = current?.location,
                    avatarUrl = current?.avatarUrl,
                    darkMode = current?.darkMode ?: false,
                    updatedAt = Instant.now().toString(),
                )
            )
            syncAllData(session.userId)
            trackEvent(session.userId, "profile_updated")
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        withCurrentUser { session ->
            val current = uiState.value.dashboard.profile
            service.upsertProfile(
                UserProfile(
                    id = current?.id ?: UUID.randomUUID().toString(),
                    userId = session.userId,
                    firstName = current?.firstName ?: "",
                    lastName = current?.lastName ?: "",
                    email = session.email,
                    phone = current?.phone,
                    location = current?.location,
                    avatarUrl = current?.avatarUrl,
                    darkMode = enabled,
                    updatedAt = Instant.now().toString(),
                )
            )
            syncAllData(session.userId)
        }
    }

    suspend fun addPaymentMethod(cardBrand: String, cardNumber: String, expiryMonth: Int, expiryYear: Int) {
        withCurrentUser { session ->
            val methods = uiState.value.dashboard.paymentMethods
            val method = PaymentMethod(
                userId = session.userId,
                cardLast4 = cardNumber.takeLast(4),
                cardBrand = cardBrand,
                expiryMonth = expiryMonth,
                expiryYear = expiryYear,
                isDefault = methods.none { it.isDefault },
            )
            service.addPaymentMethod(method)
            trackEvent(session.userId, "payment_method_added")
            syncAllData(session.userId)
        }
    }

    suspend fun setAutopay(
        enabled: Boolean,
        paymentMethodId: String?,
        amountLimit: Double,
        billingCycle: String,
        paymentDay: Int = 15,
        meterNumber: String? = null,
    ) {
        withCurrentUser { session ->
            service.upsertAutopay(
                AutopaySettings(
                    userId = session.userId,
                    enabled = enabled,
                    paymentMethodId = paymentMethodId,
                    amountLimit = amountLimit,
                    billingCycle = billingCycle,
                    paymentDay = paymentDay,
                    meterNumber = meterNumber,
                    updatedAt = Instant.now().toString(),
                )
            )
            service.addNotification(
                AppNotification(
                    userId = session.userId,
                    title = if (enabled) "Autopay enabled" else "Autopay paused",
                    body = if (enabled) "Voltflow will handle eligible bills automatically." else "Automatic charges have been disabled.",
                    type = "autopay",
                )
            )
            queueEmail(
                userId = session.userId,
                toEmail = session.email,
                subject = if (enabled) "Autopay enabled" else "Autopay paused",
                body = if (enabled) "Autopay is now active for your Voltflow account." else "Autopay has been disabled for your Voltflow account.",
                metadata = mapOf(
                    "type" to "autopay",
                    "enabled" to enabled.toString(),
                    "payment_day" to paymentDay.toString(),
                    "meter_number" to (meterNumber ?: "")
                ),
            )
            trackEvent(session.userId, "autopay_updated")
            syncAllData(session.userId)
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        updateSecuritySettings("biometric_toggled") { it.copy(biometricEnabled = enabled) }
    }

    suspend fun setMfaEnabled(enabled: Boolean) {
        updateSecuritySettings("mfa_toggled") { it.copy(mfaEnabled = enabled) }
    }

    suspend fun setLockScope(scope: LockScope) {
        updateSecuritySettings("lock_scope_updated") { it.copy(lockScope = scope.value) }
    }

    suspend fun setPin(pin: String) {
        val validationError = validateNewPin(pin)
        if (validationError != null) {
            publishError(IllegalArgumentException(validationError))
            return
        }
        updateSecuritySettings("pin_set") {
            it.copy(
                pinHash = BCrypt.hashpw(pin, BCrypt.gensalt(12)),
                pinFailedAttempts = 0,
                pinLockedUntil = null,
            )
        }
    }

    suspend fun saveSecuritySetup(pin: String, enableBiometric: Boolean, scope: LockScope): Boolean {
        val validationError = validateNewPin(pin)
        if (validationError != null) {
            publishError(IllegalArgumentException(validationError))
            return false
        }

        return withContext(ioDispatcher) {
            runCatching {
                val session = service.requireValidSession()
                val current = uiState.value.dashboard.securitySettings ?: SecuritySettings(userId = session.userId)
                val updated = current.copy(
                    biometricEnabled = enableBiometric,
                    lockScope = scope.value,
                    pinHash = BCrypt.hashpw(pin, BCrypt.gensalt(12)),
                    pinFailedAttempts = 0,
                    pinLockedUntil = null,
                    updatedAt = Instant.now().toString(),
                )
                service.upsertSecuritySettings(updated)
                _uiState.update { it.copy(dashboard = it.dashboard.copy(securitySettings = updated)) }
                service.addNotification(
                    AppNotification(
                        userId = session.userId,
                        title = "Security settings updated",
                        body = "Your account security preferences were updated.",
                        type = "security",
                    )
                )
                trackEvent(session.userId, "security_setup_saved")
                syncAllData(session.userId)
                true
            }.getOrElse { error ->
                publishError(error)
                false
            }
        }
    }

    suspend fun requestPinResetToken(): PinResetRequestResult =
        withContext(ioDispatcher) {
            runCatching { service.requestPinResetToken() }
                .getOrElse { error ->
                    publishError(error)
                    PinResetRequestResult()
                }
        }

    suspend fun verifyPinResetToken(token: String): Boolean =
        withContext(ioDispatcher) {
            runCatching { service.verifyPinResetToken(token.trim()) }
                .getOrElse { error ->
                    publishError(error)
                    false
                }
        }

    suspend fun completePinReset(token: String, newPin: String): Boolean {
        val validationError = validateNewPin(newPin)
        if (validationError != null) {
            publishError(IllegalArgumentException(validationError))
            return false
        }
        return withContext(ioDispatcher) {
            runCatching {
                service.completePinReset(token.trim(), newPin)
                val session = service.requireValidSession()
                val refreshedSettings = service.fetchSecuritySettings(session.userId)
                _uiState.update {
                    it.copy(
                        dashboard = it.dashboard.copy(securitySettings = refreshedSettings ?: it.dashboard.securitySettings),
                        transientMessage = "PIN reset successfully"
                    )
                }
                true
            }.getOrElse { error ->
                publishError(error)
                false
            }
        }
    }

    suspend fun verifyPin(pin: String): PinVerificationResult {
        return withContext(ioDispatcher) {
            runCatching {
                val session = service.requireValidSession()
                val settings = uiState.value.dashboard.securitySettings ?: service.fetchSecuritySettings(session.userId)
                if (settings == null || settings.pinHash.isNullOrBlank()) {
                    return@runCatching PinVerificationResult.Error("PIN is not configured")
                }

                val now = Instant.now()
                val lockedUntil = settings.pinLockedUntil?.let { runCatching { Instant.parse(it) }.getOrNull() }
                if (lockedUntil != null && now.isBefore(lockedUntil)) {
                    val remaining = Duration.between(now, lockedUntil).toMillis().coerceAtLeast(0L)
                    return@runCatching PinVerificationResult.Locked(lockedUntil.toString(), remaining)
                }

                val verified = BCrypt.checkpw(pin, settings.pinHash)
                if (verified) {
                    val updated = settings.copy(
                        pinFailedAttempts = 0,
                        pinLockedUntil = null,
                        updatedAt = now.toString()
                    )
                    service.upsertSecuritySettings(updated)
                    _uiState.update { it.copy(dashboard = it.dashboard.copy(securitySettings = updated)) }
                    PinVerificationResult.Success
                } else {
                    val failedAttempts = (settings.pinFailedAttempts + 1).coerceAtMost(5)
                    if (failedAttempts >= 5) {
                        val lockUntil = now.plusSeconds(5 * 60L)
                        val updated = settings.copy(
                            pinFailedAttempts = failedAttempts,
                            pinLockedUntil = lockUntil.toString(),
                            updatedAt = now.toString()
                        )
                        service.upsertSecuritySettings(updated)
                        _uiState.update { it.copy(dashboard = it.dashboard.copy(securitySettings = updated)) }
                        PinVerificationResult.Locked(lockUntil.toString(), 5 * 60_000L)
                    } else {
                        val updated = settings.copy(
                            pinFailedAttempts = failedAttempts,
                            updatedAt = now.toString()
                        )
                        service.upsertSecuritySettings(updated)
                        _uiState.update { it.copy(dashboard = it.dashboard.copy(securitySettings = updated)) }
                        PinVerificationResult.Failed((5 - failedAttempts).coerceAtLeast(0))
                    }
                }
            }.getOrElse { error ->
                PinVerificationResult.Error(error.message ?: "PIN verification failed")
            }
        }
    }

    suspend fun fundWallet(amount: Double) {
        if (amount <= 0.0) {
            publishError(IllegalArgumentException("Enter an amount greater than zero"))
            return
        }
        withCurrentUser { session ->
            val wallet = uiState.value.dashboard.wallet ?: Wallet(session.userId, 0.0)
            val updatedWallet = wallet.copy(balance = wallet.balance + amount, updatedAt = Instant.now().toString())
            service.upsertWallet(updatedWallet)
            val clientReference = "wallet-${UUID.randomUUID()}"
            service.addPayment(
                PaymentRecord(
                    userId = session.userId,
                    amount = amount,
                    status = "succeeded",
                    processor = "wallet_topup",
                    processorReference = clientReference,
                    source = "external",
                    idempotencyKey = clientReference,
                    clientReference = clientReference,
                    metadata = mapOf("kind" to TransactionKind.WALLET_FUNDING.name),
                )
            )
            service.addTransaction(
                TransactionRecord(
                    userId = session.userId,
                    kind = TransactionKind.WALLET_FUNDING.name,
                    utilityType = UtilityType.WALLET.name,
                    amount = amount,
                    status = "succeeded",
                    paymentMethod = "Wallet top-up",
                    paymentMethodId = null,
                    processorReference = clientReference,
                    description = "Wallet funded successfully",
                    clientReference = clientReference,
                    metadata = mapOf("source" to "external"),
                    occurredAt = Instant.now().toString(),
                )
            )
            service.addWalletTransaction(
                WalletTransaction(
                    userId = session.userId,
                    kind = "deposit",
                    amount = amount,
                    methodLabel = "External funding",
                    occurredAt = Instant.now().toString(),
                )
            )
            service.addNotification(
                AppNotification(
                    userId = session.userId,
                    title = "Wallet funded",
                    body = "${amountFormatter(amount)} has been added to your wallet.",
                    type = "wallet",
                )
            )
            queueEmail(
                userId = session.userId,
                toEmail = session.email,
                subject = "Wallet funded",
                body = "Your Voltflow wallet was funded with ${amountFormatter(amount)}.",
                metadata = mapOf("type" to "wallet", "amount" to amount.toString()),
            )
            trackEvent(session.userId, "wallet_funded")
            syncAllData(session.userId)
        }
    }

    suspend fun withdrawWallet(amount: Double) {
        if (amount <= 0.0) {
            publishError(IllegalArgumentException("Enter an amount greater than zero"))
            return
        }
        withCurrentUser { session ->
            val wallet = uiState.value.dashboard.wallet ?: Wallet(session.userId, 0.0)
            if (wallet.balance < amount) {
                publishError(IllegalStateException("Insufficient wallet balance"))
                return@withCurrentUser
            }
            val updatedWallet = wallet.copy(balance = wallet.balance - amount, updatedAt = Instant.now().toString())
            service.upsertWallet(updatedWallet)
            service.addWalletTransaction(
                WalletTransaction(
                    userId = session.userId,
                    kind = "withdraw",
                    amount = amount,
                    methodLabel = "Withdrawal",
                    occurredAt = Instant.now().toString(),
                )
            )
            service.addNotification(
                AppNotification(
                    userId = session.userId,
                    title = "Withdrawal completed",
                    body = "${amountFormatter(amount)} has been withdrawn from your wallet.",
                    type = "wallet",
                )
            )
            trackEvent(session.userId, "wallet_withdraw")
            syncAllData(session.userId)
        }
    }

    suspend fun payUtility(draft: PaymentDraft) {
        if (draft.amount <= 0.0) {
            publishError(IllegalArgumentException("Enter an amount greater than zero"))
            return
        }
        withCurrentUser { session ->
            val processorResult = paymentProcessor.process(session.userId, draft)
            val snapshot = uiState.value.dashboard
            val wallet = snapshot.wallet ?: Wallet(session.userId, 0.0)
            val method = snapshot.paymentMethods.firstOrNull { it.id == draft.paymentMethodId }
                ?: snapshot.paymentMethods.firstOrNull { it.isDefault }
            if (!draft.useWallet && method == null) {
                publishError(IllegalStateException("Add a valid payment method first"))
                return@withCurrentUser
            }
            if (draft.useWallet && wallet.balance < draft.amount) {
                publishError(IllegalStateException("Insufficient wallet balance"))
                return@withCurrentUser
            }

            val clientReference = "payment-${UUID.randomUUID()}"
            val updatedWallet = if (draft.useWallet) {
                wallet.copy(balance = wallet.balance - draft.amount, updatedAt = Instant.now().toString())
            } else {
                wallet
            }
            val currentUsage = snapshot.usage ?: UsageMetrics(userId = session.userId)
            val updatedUsage = currentUsage.applyPayment(draft)

            if (draft.useWallet) {
                service.upsertWallet(updatedWallet)
            }
            service.upsertUsage(updatedUsage)
            service.addPayment(
                PaymentRecord(
                    userId = session.userId,
                    amount = draft.amount,
                    status = processorResult.status,
                    processor = "mock",
                    processorReference = processorResult.processorReference,
                    paymentMethodId = method?.id,
                    source = if (draft.useWallet) "wallet" else "card",
                    idempotencyKey = clientReference,
                    clientReference = clientReference,
                    metadata = mapOf("utility_type" to draft.utilityType.name),
                )
            )
            val methodLabel = if (draft.useWallet) {
                "Wallet"
            } else {
                method?.let { "${it.cardBrand} •••• ${it.cardLast4}" } ?: "Card"
            }
            service.addTransaction(
                TransactionRecord(
                    userId = session.userId,
                    kind = TransactionKind.UTILITY_PAYMENT.name,
                    utilityType = draft.utilityType.name,
                    amount = draft.amount,
                    status = processorResult.status,
                    meterNumber = draft.meterNumber, // Add meter number here
                    paymentMethod = methodLabel,
                    paymentMethodId = method?.id,
                    processorReference = processorResult.processorReference,
                    description = "${draft.utilityType.label} payment completed",
                    clientReference = clientReference,
                    metadata = mapOf("source" to if (draft.useWallet) "wallet" else "card"),
                    occurredAt = Instant.now().toString(),
                )
            )
            service.addWalletTransaction(
                WalletTransaction(
                    userId = session.userId,
                    kind = "payment",
                    amount = draft.amount,
                    methodLabel = methodLabel,
                    occurredAt = Instant.now().toString(),
                )
            )
            service.addNotification(
                AppNotification(
                    userId = session.userId,
                    title = "Payment successful",
                    body = "${amountFormatter(draft.amount)} paid for ${draft.utilityType.label.lowercase()}.",
                    type = "payment",
                )
            )
            queueEmail(
                userId = session.userId,
                toEmail = session.email,
                subject = "Payment successful",
                body = "We received your ${draft.utilityType.label.lowercase()} payment of ${amountFormatter(draft.amount)}.",
                metadata = mapOf("type" to "payment", "utility" to draft.utilityType.name),
            )

            if (draft.useWallet && updatedWallet.balance < 20.0) {
                service.addNotification(
                    AppNotification(
                        userId = session.userId,
                        title = "Low wallet balance",
                        body = "Your wallet balance is now ${amountFormatter(updatedWallet.balance)}.",
                        type = "wallet",
                    )
                )
                queueEmail(
                    userId = session.userId,
                    toEmail = session.email,
                    subject = "Low wallet balance",
                    body = "Your Voltflow wallet balance is now ${amountFormatter(updatedWallet.balance)}.",
                    metadata = mapOf("type" to "wallet", "balance" to updatedWallet.balance.toString()),
                )
            }

            trackEvent(session.userId, "utility_payment")
            syncAllData(session.userId)
            _uiState.update { it.copy(transientMessage = "Payment processed successfully") }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(transientMessage = null) }
    }

    fun consumeError() {
        _uiState.update { it.copy(activeError = null) }
    }

    suspend fun markNotificationRead(notificationId: String) {
        withCurrentUser { session ->
            service.markNotificationRead(session.userId, notificationId)
            syncAllData(session.userId)
        }
    }

    suspend fun updatePushToken(token: String) {
        if (token.isBlank()) return
        lastKnownPushToken = token
        preferences.setPushToken(token)
        val session = service.currentSession() ?: return
        runCatching {
            service.upsertDevice(service.buildCurrentDevice(session.accessToken, session.userId, token))
        }.onFailure { Log.w("VoltflowRepository", "Failed to sync push token", it) }
    }

    suspend fun syncCurrentDeviceLocation() {
        val session = service.currentSession() ?: return
        runCatching {
            service.upsertDevice(service.buildCurrentDevice(session.accessToken, session.userId, lastKnownPushToken))
            syncAllData(session.userId)
        }.onFailure { Log.w("VoltflowRepository", "Failed to sync device location", it) }
    }

    private suspend fun ensureBootstrapRecords(
        session: SessionSnapshot,
        firstName: String = session.email.substringBefore('@').replaceFirstChar { it.uppercase() },
        lastName: String = "",
    ) {
        val existingProfile = service.fetchProfile(session.userId)
        if (existingProfile == null) {
            service.upsertProfile(
                UserProfile(
                    userId = session.userId,
                    firstName = firstName,
                    lastName = lastName,
                    email = session.email,
                    updatedAt = Instant.now().toString(),
                )
            )
        }
        if (service.fetchWallet(session.userId) == null) {
            service.upsertWallet(Wallet(userId = session.userId, balance = 1000.0, updatedAt = Instant.now().toString()))
        }
        if (service.fetchUsage(session.userId) == null) {
            service.upsertUsage(UsageMetrics(userId = session.userId, updatedAt = Instant.now().toString()))
        }
        if (service.fetchAutopay(session.userId) == null) {
            service.upsertAutopay(AutopaySettings(userId = session.userId, updatedAt = Instant.now().toString()))
        }
        if (service.fetchSecuritySettings(session.userId) == null) {
            service.upsertSecuritySettings(SecuritySettings(userId = session.userId, updatedAt = Instant.now().toString()))
        }
        if (service.fetchBillingAccounts(session.userId).isEmpty()) {
            service.addBillingAccount(
                BillingAccount(
                    userId = session.userId,
                    providerName = "City Power & Light",
                    accountMasked = "****-****-4829",
                    meterNumber = "MTR-2847561",
                    isDefault = true,
                    updatedAt = Instant.now().toString(),
                )
            )
        }
        if (service.fetchBills(session.userId, limit = 1).isEmpty()) {
            val account = service.fetchBillingAccounts(session.userId).firstOrNull()
            service.addBill(
                Bill(
                    userId = session.userId,
                    billingAccountId = account?.id,
                    amountDue = 84.32,
                    dueDate = "2026-01-15",
                    status = "open",
                )
            )
        }
    }

    private fun UsageMetrics.applyPayment(draft: PaymentDraft): UsageMetrics {
        return copy(
            totalSpent = totalSpent + draft.amount,
            electricitySpent = electricitySpent + if (draft.utilityType == UtilityType.ELECTRICITY) draft.amount else 0.0,
            waterSpent = waterSpent + if (draft.utilityType == UtilityType.WATER) draft.amount else 0.0,
            gasSpent = gasSpent + if (draft.utilityType == UtilityType.GAS) draft.amount else 0.0,
            monthlyUsage = monthlyUsage + draft.amount,
            updatedAt = Instant.now().toString(),
        )
    }

    private suspend fun syncAllData(userId: String) {
        if (_uiState.value.isOffline) {
            loadCachedSnapshot(userId)
            return
        }
        val session = service.requireValidSession()
        
        // Tier 1 (Critical): Load immediately for first paint
        // Run these in parallel for speed
        val profileDeferred = repositoryScope.async { service.fetchProfile(userId) }
        val walletDeferred = repositoryScope.async { service.fetchWallet(userId) }
        val billingAccountsDeferred = repositoryScope.async { service.fetchBillingAccounts(userId) }
        val billsDeferred = repositoryScope.async { service.fetchBills(userId) }
        val recentTransactionsDeferred = repositoryScope.async { service.fetchTransactions(userId, limit = 5) }
        val notificationsDeferred = repositoryScope.async { service.fetchNotifications(userId) }
        
        // Await critical data
        val profile = profileDeferred.await()
        val wallet = walletDeferred.await()
        val billingAccounts = billingAccountsDeferred.await()
        val bills = billsDeferred.await()
        val recentTransactions = recentTransactionsDeferred.await()
        val notifications = notificationsDeferred.await()
        
        // Update UI with critical data (shows dashboard immediately)
        cacheSnapshot(userId, profile, wallet, recentTransactions, bills, notifications)
        _uiState.update {
            it.copy(
                isLoading = false,
                isAuthenticated = true,
                dashboard = DashboardState(
                    greeting = greetingForCurrentTime(profile?.firstName),
                    profile = profile,
                    wallet = wallet,
                    billingAccounts = billingAccounts,
                    bills = bills,
                    recentTransactions = recentTransactions,
                    notifications = notifications,
                    // Tier 2 data left from previous state until available
                    usage = it.dashboard.usage,
                    paymentMethods = it.dashboard.paymentMethods,
                    transactions = it.dashboard.transactions,
                    autopay = it.dashboard.autopay,
                    securitySettings = it.dashboard.securitySettings,
                    devices = it.dashboard.devices,
                    currentDeviceId = service.currentDeviceId(),
                    walletTransactions = it.dashboard.walletTransactions,
                    usagePeriods = it.dashboard.usagePeriods,
                ),
            )
        }
        
        // Tier 2 (Secondary): Load in background after first paint
        // These are less critical for immediate UX
        repositoryScope.launch {
            val usage = service.fetchUsage(userId)
            val autopay = service.fetchAutopay(userId)
            val securitySettings = service.fetchSecuritySettings(userId)
            val paymentMethods = service.fetchPaymentMethods(userId)
            val transactions = service.fetchTransactions(userId)
            val devices = service.fetchDevices(userId)
            val walletTransactions = service.fetchWalletTransactions(userId)
            val usagePeriods = service.fetchUsageMetrics(userId)
            
            // Update UI with secondary data
            _uiState.update { currentState ->
                currentState.copy(
                    dashboard = currentState.dashboard.copy(
                        usage = usage,
                        paymentMethods = paymentMethods,
                        transactions = transactions,
                        autopay = autopay,
                        securitySettings = securitySettings,
                        devices = devices,
                        walletTransactions = walletTransactions,
                        usagePeriods = usagePeriods,
                    ),
                )
            }
        }
        
        // Device upsert in background
        runCatching { service.upsertDevice(service.buildCurrentDevice(session.accessToken, userId, lastKnownPushToken)) }
    }

    private suspend fun cacheSnapshot(
        userId: String,
        profile: UserProfile?,
        wallet: Wallet?,
        transactions: List<TransactionRecord>,
        bills: List<Bill>,
        notifications: List<AppNotification>,
    ) {
        profile?.let {
            dao.upsertProfile(
                ProfileEntity(
                    userId = userId,
                    firstName = it.firstName,
                    lastName = it.lastName,
                    email = it.email,
                    phone = it.phone,
                    location = it.location,
                    avatarUrl = it.avatarUrl,
                    darkMode = it.darkMode,
                    accountStatus = it.accountStatus,
                    updatedAt = it.updatedAt,
                )
            )
        }
        wallet?.let {
            dao.upsertWallet(
                WalletEntity(
                    userId = userId,
                    balance = it.balance,
                    updatedAt = it.updatedAt,
                )
            )
        }
        dao.upsertTransactions(
            transactions.map { item ->
                TransactionEntity(
                    id = item.id,
                    userId = userId,
                    kind = item.kind,
                    utilityType = item.utilityType,
                    amount = item.amount,
                    status = item.status,
                    meterNumber = item.meterNumber,
                    paymentMethod = item.paymentMethod,
                    occurredAt = item.occurredAt,
                    createdAt = item.createdAt,
                )
            }
        )
        dao.upsertBills(
            bills.map { bill ->
                BillEntity(
                    id = bill.id,
                    userId = userId,
                    amountDue = bill.amountDue,
                    dueDate = bill.dueDate,
                    status = bill.status,
                    createdAt = bill.createdAt,
                )
            }
        )
        dao.upsertNotifications(
            notifications.map { notification ->
                NotificationEntity(
                    id = notification.id,
                    userId = userId,
                    title = notification.title,
                    body = notification.body,
                    type = notification.type,
                    isRead = notification.isRead,
                    createdAt = notification.createdAt,
                )
            }
        )
    }

    private suspend fun loadCachedSnapshot(userId: String) {
        val cachedProfile = dao.getProfile(userId)
        val cachedWallet = dao.getWallet(userId)
        val cachedTransactions = dao.getTransactions(userId)
        val cachedRecent = dao.getRecentTransactions(userId, 5)
        val cachedBills = dao.getBills(userId)
        val cachedNotifications = dao.getNotifications(userId)
        _uiState.update {
            it.copy(
                isLoading = false,
                isAuthenticated = true,
                dashboard = it.dashboard.copy(
                    greeting = greetingForCurrentTime(cachedProfile?.firstName),
                    profile = cachedProfile?.toModel(),
                    wallet = cachedWallet?.toModel(),
                    transactions = cachedTransactions.map { entity -> entity.toModel() },
                    recentTransactions = cachedRecent.map { entity -> entity.toModel() },
                    bills = cachedBills.map { entity -> entity.toModel() },
                    notifications = cachedNotifications.map { entity -> entity.toModel() },
                ),
            )
        }
    }

    private suspend fun trackEvent(userId: String, eventName: String) {
        runCatching {
            service.addAnalyticsEvent(
                AnalyticsEvent(
                    userId = userId,
                    eventName = eventName,
                    metadata = mapOf("source" to "android"),
                    createdAt = Instant.now().toString(),
                )
            )
        }
    }

    private suspend fun withCurrentUser(block: suspend (SessionSnapshot) -> Unit) {
        withContext(ioDispatcher) {
            runCatching {
                val session = service.requireValidSession()
                block(session)
            }.onFailure { error ->
                if (error.message?.contains("Authentication required", ignoreCase = true) == true) {
                    service.clearSession()
                    resetToSignedOutState()
                }
                publishError(error)
            }
        }
    }

    private suspend fun updateSecuritySettings(eventName: String, update: (SecuritySettings) -> SecuritySettings) {
        withCurrentUser { session ->
            val current = uiState.value.dashboard.securitySettings ?: SecuritySettings(userId = session.userId)
            val updated = update(current).copy(updatedAt = Instant.now().toString())
            service.upsertSecuritySettings(updated)
            _uiState.update { it.copy(dashboard = it.dashboard.copy(securitySettings = updated)) }
            service.addNotification(
                AppNotification(
                    userId = session.userId,
                    title = "Security settings updated",
                    body = "Your account security preferences were updated.",
                    type = "security",
                )
            )
            trackEvent(session.userId, eventName)
            syncAllData(session.userId)
        }
    }

    private fun resetToSignedOutState() {
        _uiState.value = UiState(
            isLoading = false,
            isAuthenticated = false,
            activeError = if (service.isConfigured()) null else "Configure Supabase credentials in gradle.properties.",
        )
    }

    private fun publishError(error: Throwable) {
        Log.e("VoltflowRepository", "operation failed", error)
        _uiState.update {
            it.copy(
                isLoading = false,
                activeError = error.message ?: "Something went wrong",
            )
        }
    }

    private fun mutateLoading(value: Boolean) {
        _uiState.update { it.copy(isLoading = value) }
    }

    private suspend fun startSyncLoop() {
        var lastRealtimeSyncTime = 0L
        val minimumRealtimeSyncInterval = 60_000 // 60 seconds
        while (true) {
            delay(60_000) // Primary polling interval: 60 seconds (reduced from 12s)
            val session = service.currentSession() ?: continue
            if (!_uiState.value.isOffline) {
                // Skip full sync if realtime is healthy and recent
                val now = System.currentTimeMillis()
                if (now - lastRealtimeSyncTime > minimumRealtimeSyncInterval) {
                    runCatching { syncAllData(session.userId) }
                    lastRealtimeSyncTime = now
                }
            }
        }
    }

    private suspend fun queueEmail(
        userId: String,
        toEmail: String?,
        subject: String,
        body: String,
        metadata: Map<String, String> = emptyMap(),
    ) {
        if (toEmail.isNullOrBlank()) return
        service.addEmailOutbox(
            EmailOutbox(
                userId = userId,
                toEmail = toEmail,
                subject = subject,
                body = body,
                metadata = metadata,
            )
        )
    }

    private fun validateNewPin(pin: String): String? {
        if (pin.length != 6 || pin.any { !it.isDigit() }) {
            return "PIN must be exactly 6 digits"
        }
        val weakPins = setOf("000000", "111111", "123456", "654321", "121212", "112233")
        return if (pin in weakPins) "Choose a stronger PIN" else null
    }
}

private fun ProfileEntity.toModel(): UserProfile = UserProfile(
    userId = userId,
    firstName = firstName,
    lastName = lastName,
    email = email,
    phone = phone,
    location = location,
    avatarUrl = avatarUrl,
    darkMode = darkMode,
    accountStatus = accountStatus,
    updatedAt = updatedAt,
)

private fun WalletEntity.toModel(): Wallet = Wallet(
    userId = userId,
    balance = balance,
    updatedAt = updatedAt,
)

private fun TransactionEntity.toModel(): TransactionRecord = TransactionRecord(
    id = id,
    userId = userId,
    kind = kind,
    utilityType = utilityType,
    amount = amount,
    status = status,
    meterNumber = meterNumber,
    paymentMethod = paymentMethod,
    description = "",
    clientReference = id,
    occurredAt = occurredAt,
    createdAt = createdAt,
)

private fun BillEntity.toModel(): Bill = Bill(
    id = id,
    userId = userId,
    amountDue = amountDue,
    dueDate = dueDate,
    status = status,
    createdAt = createdAt,
)

private fun NotificationEntity.toModel(): AppNotification = AppNotification(
    id = id,
    userId = userId,
    title = title,
    body = body,
    type = type,
    isRead = isRead,
    createdAt = createdAt,
)
