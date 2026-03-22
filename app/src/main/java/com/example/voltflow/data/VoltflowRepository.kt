package com.example.voltflow.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

class VoltflowRepository(
    context: Context,
    private val paymentProcessor: PaymentProcessor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val service = SupabaseService(context)
    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)

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
    }

    suspend fun restoreSession() {
        withContext(ioDispatcher) {
            val session = service.currentSession()
            if (session == null) {
                _uiState.update { it.copy(isLoading = false, isAuthenticated = false) }
                return@withContext
            }
            runCatching {
                service.requireValidSession()
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
    }

    suspend fun signIn(email: String, password: String) {
        mutateLoading(true)
        withContext(ioDispatcher) {
            runCatching {
                val session = service.signIn(email, password)
                ensureBootstrapRecords(session)
                syncAllData(session.userId)
                service.upsertDevice(service.buildCurrentDevice(session.accessToken, session.userId))
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
                service.upsertDevice(service.buildCurrentDevice(session.accessToken, session.userId))
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

    suspend fun signOut() {
        withContext(ioDispatcher) {
            val currentUserId = service.currentSession()?.userId
            runCatching {
                if (currentUserId != null) {
                    service.deleteDevice(currentUserId, service.currentDeviceId())
                }
                service.signOut()
            }
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
                    updatedAt = Instant.now().toString(),
                )
            )
            syncAllData(session.userId)
            trackEvent(session.userId, "profile_updated")
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

    suspend fun setAutopay(enabled: Boolean, paymentMethodId: String?, amountLimit: Double, billingCycle: String) {
        withCurrentUser { session ->
            service.upsertAutopay(
                AutopaySettings(
                    userId = session.userId,
                    enabled = enabled,
                    paymentMethodId = paymentMethodId,
                    amountLimit = amountLimit,
                    billingCycle = billingCycle,
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
                metadata = mapOf("type" to "autopay", "enabled" to enabled.toString()),
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

    suspend fun setPinEnabled(enabled: Boolean) {
        updateSecuritySettings("pin_toggled") { it.copy(pinEnabled = enabled) }
    }

    suspend fun setAutoLockMinutes(minutes: Int) {
        updateSecuritySettings("auto_lock_updated") { it.copy(autoLockMinutes = minutes) }
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
                    paymentMethod = methodLabel,
                    paymentMethodId = method?.id,
                    processorReference = processorResult.processorReference,
                    description = "${draft.utilityType.label} payment completed",
                    clientReference = clientReference,
                    metadata = mapOf("source" to if (draft.useWallet) "wallet" else "card"),
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
            service.upsertWallet(Wallet(userId = session.userId, balance = 180.0, updatedAt = Instant.now().toString()))
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
        val session = service.requireValidSession()
        // Error 3 hardening: Each service call is now internally runCatching or fetchListSafe.
        // We'll wrap upsertDevice just in case.
        runCatching { service.upsertDevice(service.buildCurrentDevice(session.accessToken, userId)) }

        val profile = service.fetchProfile(userId)
        val wallet = service.fetchWallet(userId)
        val usage = service.fetchUsage(userId)
        val autopay = service.fetchAutopay(userId)
        val securitySettings = service.fetchSecuritySettings(userId)
        val paymentMethods = service.fetchPaymentMethods(userId)
        val transactions = service.fetchTransactions(userId)
        val recentTransactions = service.fetchTransactions(userId, limit = 5)
        val notifications = service.fetchNotifications(userId)
        val devices = service.fetchDevices(userId)

        _uiState.update {
            it.copy(
                isLoading = false,
                isAuthenticated = true,
                dashboard = DashboardState(
                    greeting = greetingForCurrentTime(profile?.firstName),
                    profile = profile,
                    wallet = wallet,
                    usage = usage,
                    paymentMethods = paymentMethods,
                    transactions = transactions,
                    recentTransactions = recentTransactions,
                    notifications = notifications,
                    autopay = autopay,
                    securitySettings = securitySettings,
                    devices = devices,
                    currentDeviceId = service.currentDeviceId(),
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
        while (true) {
            delay(12_000)
            val session = service.currentSession() ?: continue
            runCatching { syncAllData(session.userId) }
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
}
