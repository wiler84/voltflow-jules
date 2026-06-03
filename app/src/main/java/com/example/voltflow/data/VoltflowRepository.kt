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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.LocalDate
import org.mindrot.jbcrypt.BCrypt
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID
import java.util.Locale

class VoltflowRepository(
    context: Context,
    private val paymentProcessor: PaymentProcessor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val service = SupabaseService(context)
    private val preferences = UserPreferencesStore(context)
    private val database by lazy { VoltflowDatabase.create(context) }
    private val dao by lazy { database.dao() }
    private val networkMonitor = NetworkMonitor(context)
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("SyncFlow", "Unhandled repository error", throwable)
        _uiState.update { it.copy(activeError = "Sync error: ${throwable.localizedMessage}") }
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    private val networkDispatcher = ioDispatcher.limitedParallelism(3)
    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher + exceptionHandler)
    private var realtimeService: SupabaseRealtimeService? = null
    private var realtimeStreams: RealtimeStreams? = null
    @Volatile
    private var lastKnownPushToken: String? = null
    private val circuitBreaker = CircuitBreaker()

    // GATING STARTUP (Point 2)
    private var _startupSyncCompleted = false

    private val _uiState = MutableStateFlow(
        UiState(
            isLoading = true,
            activeError = if (service.isConfigured()) null else "Configure Supabase credentials in gradle.properties.",
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ONE-TIME UI EVENTS (Point 2)
    private val _uiEvents = Channel<String>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    init {
        repositoryScope.launch {
            Log.d("AuthFlow", "Repository init: Starting session restore")
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

    // SAFE NOTIFICATION INSERTION (Point 1)
    private suspend fun safeAddNotification(
        title: String,
        body: String,
        type: String
    ) {
        val startTime = System.currentTimeMillis()
        val session = service.currentSession()
        
        if (!_startupSyncCompleted) {
            Log.w("NotificationFlow", "SafeInsert blocked: Startup sync not yet complete. Notification: $title")
            return
        }

        if (session == null || session.userId.isBlank()) {
            Log.w("NotificationFlow", "SafeInsert blocked: No authenticated session. Notification: $title")
            return
        }

        Log.d("NotificationFlow", "SafeInsert started: [Type: $type, User: ${session.userId}]")
        runCatching {
            service.addNotification(
                AppNotification(
                    userId = session.userId,
                    title = title,
                    body = body,
                    type = type,
                )
            )
        }.onSuccess {
            Log.d("NotificationFlow", "SafeInsert success in ${System.currentTimeMillis() - startTime}ms")
        }.onFailure { error ->
            Log.e("NotificationFlow", "SafeInsert failed: ${error.message}")
        }
    }

    suspend fun restoreSession() {
        val startTime = System.currentTimeMillis()
        Log.d("AuthFlow", "RestoreSession started")
        withContext(ioDispatcher) {
            val session = service.currentSession()
            if (session == null) {
                Log.d("AuthFlow", "RestoreSession: No local session found")
                _uiState.update { it.copy(isInitializing = false, isAuthReady = true, isLoading = false, isAuthenticated = false) }
                return@withContext
            }
            
            Log.d("AuthFlow", "RestoreSession: Found session for ${session.userId}")
            _uiState.update { it.copy(isInitializing = false, isAuthReady = true, isAuthenticated = true) }

            runCatching {
                service.requireValidSession()
                repositoryScope.launch { startRealtimeSubscriptions(session) }
                syncAllData(session.userId)
            }.onSuccess {
                _startupSyncCompleted = true
                Log.d("AuthFlow", "RestoreSession complete in ${System.currentTimeMillis() - startTime}ms")
            }.onFailure { error ->
                Log.e("AuthFlow", "RestoreSession failed", error)
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
        val startTime = System.currentTimeMillis()
        Log.d("AuthFlow", "SignIn started for $email")
        withContext(ioDispatcher) {
            runCatching {
                val session = service.signIn(email, password)
                mutateLoading(true)
                ensureBootstrapRecords(session)
                repositoryScope.launch { startRealtimeSubscriptions(session) }
                val device = service.buildCurrentDevice(session.accessToken, session.userId, lastKnownPushToken)
                service.upsertDevice(device)
                syncAllData(session.userId)
                trackEvent(session.userId, "sign_in")
            }.onSuccess {
                _startupSyncCompleted = true
                Log.d("AuthFlow", "SignIn success in ${System.currentTimeMillis() - startTime}ms")
            }.onFailure { 
                Log.e("AuthFlow", "SignIn failure", it)
                publishError(it) 
            }
        }
        mutateLoading(false)
    }

    suspend fun signUp(email: String, password: String, firstName: String, lastName: String) {
        val startTime = System.currentTimeMillis()
        Log.d("AuthFlow", "SignUp started for $email")
        withContext(ioDispatcher) {
            runCatching {
                service.signUp(email, password, firstName, lastName)
                val session = service.signIn(email, password)
                
                mutateLoading(true)
                ensureBootstrapRecords(session)
                repositoryScope.launch { startRealtimeSubscriptions(session) }
                val device = service.buildCurrentDevice(session.accessToken, session.userId, lastKnownPushToken)
                service.upsertDevice(device)
                syncAllData(session.userId)
                
                _startupSyncCompleted = true
                safeAddNotification(
                    title = "Welcome to Voltflow",
                    body = "Your account is ready for utility payments.",
                    type = "system",
                )
                trackEvent(session.userId, "sign_up")
            }.onSuccess {
                Log.d("AuthFlow", "SignUp success in ${System.currentTimeMillis() - startTime}ms")
            }.onFailure { error ->
                Log.e("AuthFlow", "SignUp failed", error)
                publishError(error)
            }
        }
        mutateLoading(false)
    }

    private suspend fun startRealtimeSubscriptions(session: SessionSnapshot) {
        Log.d("SyncFlow", "Realtime: Connecting for ${session.userId}")
        try {
            realtimeService = SupabaseRealtimeService(service.supabaseUrl(), service.anonKey())
            realtimeStreams = realtimeService?.connect(session)
            
            realtimeStreams?.let { streams ->
                repositoryScope.launch {
                    streams.wallet.collect { wallet ->
                        _uiState.update { it.copy(dashboard = it.dashboard.copy(wallet = wallet)) }
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
                        val currentId = service.currentDeviceId()
                        val isStillValid = devices.any { it.deviceId == currentId }
                        if (!isStillValid && _uiState.value.isAuthenticated && !devices.isEmpty()) {
                            Log.w("AuthFlow", "Realtime: Session revoked remotely")
                            signOut()
                        }
                        _uiState.update { it.copy(dashboard = it.dashboard.copy(devices = devices)) }
                    }
                }
            }
            Log.d("SyncFlow", "Realtime: Subscriptions active")
        } catch (e: Exception) {
            Log.w("SyncFlow", "Realtime: Connection failed", e)
        }
    }

    private suspend fun stopRealtimeSubscriptions() {
        Log.d("SyncFlow", "Realtime: Disconnecting")
        try {
            realtimeService?.disconnect()
            realtimeService = null
            realtimeStreams = null
        } catch (e: Exception) {
            Log.w("SyncFlow", "Realtime: Disconnect error", e)
        }
    }

    suspend fun signOut() {
        Log.d("AuthFlow", "SignOut started")
        mutateLoading(true)
        withContext(ioDispatcher) {
            val currentUserId = service.currentSession()?.userId
            runCatching {
                if (currentUserId != null) {
                    service.deleteDevice(currentUserId, service.currentDeviceId())
                }
                service.signOut()
            }
            stopRealtimeSubscriptions()
            _startupSyncCompleted = false
            resetToSignedOutState()
            Log.d("AuthFlow", "SignOut complete")
        }
    }

    suspend fun revokeDevice(deviceId: String) {
        Log.d("AuthFlow", "RevokeDevice: $deviceId")
        withCurrentUser { session ->
            val currentDeviceId = service.currentDeviceId()
            if (deviceId == currentDeviceId) {
                mutateLoading(true)
                runCatching { service.deleteDevice(session.userId, deviceId) }
                service.signOut()
                resetToSignedOutState()
                return@withCurrentUser
            }
            service.deleteDevice(session.userId, deviceId)
            syncAllData(session.userId)
        }
    }

    suspend fun refresh() {
        Log.d("SyncFlow", "Manual refresh triggered")
        withContext(ioDispatcher) {
            val userId = service.currentSession()?.userId ?: return@withContext
            runCatching { syncAllData(userId) }.onFailure { publishError(it) }
        }
    }

    suspend fun saveProfile(firstName: String, lastName: String, phone: String) {
        Log.d("AuthFlow", "SaveProfile: $firstName $lastName")
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
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        Log.d("AuthFlow", "SetDarkMode: $enabled")
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

    suspend fun updatePassword(newPassword: String) {
        Log.d("AuthFlow", "UpdatePassword")
        withCurrentUser {
            service.updatePassword(newPassword)
            _uiEvents.send("Password updated successfully")
        }
    }

    // SQL AGGREGATION (Point 3)
    fun getUsageChartData(range: UsageRange, isMoneyMode: Boolean): Flow<Result<UsageChartData>> = flow {
        val session = service.currentSession() ?: return@flow
        val startTime = System.currentTimeMillis()
        Log.d("AnalyticsFlow", "FetchAggregatedChart: [Range: ${range.label}, Mode: ${if (isMoneyMode) "Money" else "Usage"}]")
        try {
            val points = service.fetchAggregatedUsage(session.userId, range.days, isMoneyMode)
            Log.d("AnalyticsFlow", "FetchChart Points Received: ${points.size}")
            
            val totalValue = points.sumOf { it.value }
            
            // Percentage change still needs some historical context, let's keep it simple for now or extend RPC
            val percentageChange = 0.0 // Could be added to RPC return

            Log.d("AnalyticsFlow", "FetchChart success in ${System.currentTimeMillis() - startTime}ms")
            emit(Result.success(UsageChartData(
                points = points,
                periodLabel = range.label,
                totalUsage = totalValue,
                percentageChange = percentageChange
            )))
        } catch (e: Exception) {
            Log.e("AnalyticsFlow", "FetchChart failed", e)
            emit(Result.failure(e))
        }
    }

    suspend fun addPaymentMethod(cardBrand: String, cardNumber: String, expiryMonth: Int, expiryYear: Int) {
        Log.d("PaymentFlow", "AddPaymentMethod: $cardBrand")
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
        Log.d("PaymentFlow", "SetAutopay: Enabled=$enabled, Limit=$amountLimit")
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
            
            safeAddNotification(
                title = if (enabled) "Autopay enabled" else "Autopay paused",
                body = if (enabled) "Voltflow will handle eligible bills automatically." else "Automatic charges have been disabled.",
                type = "autopay",
            )
            syncAllData(session.userId)
        }
    }

    suspend fun fundWallet(amount: Double, paymentMethodId: String? = null) {
        val startTime = System.currentTimeMillis()
        Log.d("PaymentFlow", "FundWallet: $amount")
        if (amount <= 0.0) {
            publishError(IllegalArgumentException("Enter an amount greater than zero"))
            return
        }
        withCurrentUser { session ->
            val wallet = uiState.value.dashboard.wallet ?: Wallet(session.userId, 0.0)
            val updatedWallet = wallet.copy(balance = wallet.balance + amount, updatedAt = Instant.now().toString())
            service.upsertWallet(updatedWallet)
            val clientReference = "wallet-${UUID.randomUUID()}"
            
            val methods = uiState.value.dashboard.paymentMethods
            val selectedMethod = if (paymentMethodId != null) {
                methods.firstOrNull { it.id == paymentMethodId }
            } else {
                methods.firstOrNull { it.isDefault } ?: methods.firstOrNull()
            }
            val label = selectedMethod?.let { "${it.cardBrand} •••• ${it.cardLast4}" } ?: "External funding"
            
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
                )
            )
            service.addTransaction(
                TransactionRecord(
                    userId = session.userId,
                    kind = TransactionKind.WALLET_FUNDING.name,
                    utilityType = UtilityType.WALLET.name,
                    amount = amount,
                    status = "succeeded",
                    paymentMethod = label,
                    paymentMethodId = selectedMethod?.id,
                    processorReference = clientReference,
                    description = "Wallet funded successfully",
                    clientReference = clientReference,
                    occurredAt = Instant.now().toString(),
                )
            )
            service.addWalletTransaction(
                WalletTransaction(
                    userId = session.userId,
                    kind = "deposit",
                    amount = amount,
                    methodLabel = label,
                    occurredAt = Instant.now().toString(),
                )
            )
            
            safeAddNotification(
                title = "Wallet funded",
                body = "${amountFormatter(amount)} has been added to your wallet.",
                type = "wallet",
            )
            
            syncAllData(session.userId)
            Log.d("PaymentFlow", "FundWallet success in ${System.currentTimeMillis() - startTime}ms")
        }
    }

    suspend fun withdrawWallet(amount: Double, paymentMethodId: String? = null) {
        Log.d("PaymentFlow", "WithdrawWallet: $amount to $paymentMethodId")
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
            
            val methods = uiState.value.dashboard.paymentMethods
            val selectedMethod = if (paymentMethodId != null) {
                methods.firstOrNull { it.id == paymentMethodId }
            } else {
                methods.firstOrNull { it.isDefault } ?: methods.firstOrNull()
            }
            val label = selectedMethod?.let { "${it.cardBrand} •••• ${it.cardLast4}" } ?: "Withdrawal"
            
            service.addWalletTransaction(
                WalletTransaction(
                    userId = session.userId,
                    kind = "withdraw",
                    amount = amount,
                    methodLabel = label,
                    occurredAt = Instant.now().toString(),
                )
            )
            
            safeAddNotification(
                title = "Withdrawal completed",
                body = "${amountFormatter(amount)} has been withdrawn from your wallet.",
                type = "wallet",
            )
            
            syncAllData(session.userId)
        }
    }

    suspend fun payUtility(draft: PaymentDraft, onResult: (String?) -> Unit) {
        val startTime = System.currentTimeMillis()
        Log.d("PaymentFlow", "PayUtility started: Amount=${draft.amount}, Utility=${draft.utilityType.label}")
        if (draft.amount <= 0.0) {
            onResult("Enter an amount greater than zero")
            return
        }
        withCurrentUser { session ->
            val processorResult = paymentProcessor.process(session.userId, draft)
            val snapshot = uiState.value.dashboard
            val wallet = snapshot.wallet ?: Wallet(session.userId, 0.0)
            val method = snapshot.paymentMethods.firstOrNull { it.id == draft.paymentMethodId }
                ?: snapshot.paymentMethods.firstOrNull { it.isDefault }
            
            if (!draft.useWallet && method == null) {
                onResult("Add a valid payment method first")
                return@withCurrentUser
            }
            if (draft.useWallet && wallet.balance < draft.amount) {
                onResult("Insufficient wallet balance")
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

            val methodLabel = if (draft.useWallet) {
                "Wallet"
            } else {
                method?.let { "${it.cardBrand} •••• ${it.cardLast4}" } ?: "Card"
            }

            // OPTIMISTIC UI
            _uiState.update { it.copy(
                dashboard = it.dashboard.copy(
                    wallet = updatedWallet,
                    usage = updatedUsage
                )
            ) }

            runCatching {
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
                    )
                )
                service.addTransaction(
                    TransactionRecord(
                        userId = session.userId,
                        kind = TransactionKind.UTILITY_PAYMENT.name,
                        utilityType = draft.utilityType.name,
                        amount = draft.amount,
                        status = processorResult.status,
                        meterNumber = draft.meterNumber,
                        paymentMethod = methodLabel,
                        paymentMethodId = method?.id,
                        processorReference = processorResult.processorReference,
                        description = "${draft.utilityType.label} payment completed",
                        clientReference = clientReference,
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
                
                // ELECTRICITY USAGE CALCULATION & IDEMPOTENCY (Point 4 & 5)
                val electricityRate = 0.147
                val kwhUsed = draft.amount / electricityRate
                
                // Record in usage_history for the graph
                service.addUsageHistory(session.userId, kwhUsed, draft.amount)

                service.addUsageMetric(
                    UsageMetricPeriod(
                        userId = session.userId,
                        periodStart = Instant.now().toString(),
                        periodEnd = Instant.now().toString(),
                        kwhUsed = kwhUsed,
                        amountSpent = draft.amount,
                        unitRate = electricityRate,
                        idempotencyKey = "usage_$clientReference"
                    )
                )
                
                safeAddNotification(
                    title = "Payment successful",
                    body = "${amountFormatter(draft.amount)} paid for ${draft.utilityType.label.lowercase()}.",
                    type = "payment",
                )
                
                trackEvent(session.userId, "utility_payment")
                syncAllData(session.userId)
                
                // ONE-TIME UI EVENT (Point 2)
                _uiEvents.send("Payment processed successfully")
                onResult(null) // Success
            }.onSuccess {
                Log.d("PaymentFlow", "PayUtility success in ${System.currentTimeMillis() - startTime}ms")
            }.onFailure { error ->
                Log.e("PaymentFlow", "PayUtility failed", error)
                _uiState.update { it.copy(dashboard = snapshot) }
                onResult(error.message ?: "Transaction failed. Please try again.")
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(activeError = null) }
    }

    suspend fun markNotificationRead(notificationId: String) {
        Log.d("NotificationFlow", "MarkRead: $notificationId")
        withCurrentUser { session ->
            service.markNotificationRead(session.userId, notificationId)
            syncAllData(session.userId)
        }
    }

    suspend fun updatePushToken(token: String) {
        if (token.isBlank()) return
        Log.d("AuthFlow", "UpdatePushToken")
        lastKnownPushToken = token
        preferences.setPushToken(token)
        val session = service.currentSession() ?: return
        runCatching {
            val device = service.buildCurrentDevice(session.accessToken, session.userId, token)
            service.upsertDevice(device)
        }.onFailure { Log.w("AuthFlow", "Failed to sync push token", it) }
    }

    suspend fun syncCurrentDeviceLocation() {
        Log.d("AuthFlow", "SyncDeviceLocation")
        val session = service.currentSession() ?: return
        runCatching {
            val device = service.buildCurrentDevice(session.accessToken, session.userId, lastKnownPushToken)
            service.upsertDevice(device)
            syncAllData(session.userId)
        }.onFailure { Log.w("AuthFlow", "Failed to sync device location", it) }
    }

    private suspend fun ensureBootstrapRecords(session: SessionSnapshot) {
        Log.d("AuthFlow", "EnsureBootstrapRecords for ${session.userId}")
        if (service.fetchProfile(session.userId) == null) {
            service.upsertProfile(UserProfile(userId = session.userId, email = session.email, firstName = session.email.substringBefore('@')))
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
        val startTime = System.currentTimeMillis()
        Log.d("SyncFlow", "SyncAllData started for $userId")
        
        if (_uiState.value.isOffline) {
            loadCachedSnapshot(userId)
            return
        }

        if (!circuitBreaker.canAttempt()) {
            _uiState.update { it.copy(isDegraded = true) }
            loadCachedSnapshot(userId)
            return
        }

        _uiState.update { it.copy(isDegraded = false) }

        try {
            service.requireValidSession()
        } catch (e: Exception) {
            circuitBreaker.onFailure()
            throw e
        }
        
        // TIERED STARTUP (Point 6)
        // Tier 1 (critical): Profile, Wallet, Billing, Summary
        val profileDeferred = repositoryScope.async(networkDispatcher) { runCatching { service.fetchProfile(userId) }.getOrNull() }
        val walletDeferred = repositoryScope.async(networkDispatcher) { runCatching { service.fetchWallet(userId) }.getOrNull() }
        val billingAccountsDeferred = repositoryScope.async(networkDispatcher) { runCatching { service.fetchBillingAccounts(userId) }.getOrNull() ?: emptyList() }
        val recentTransactionsDeferred = repositoryScope.async(networkDispatcher) { runCatching { service.fetchTransactions(userId, limit = 5) }.getOrNull() ?: emptyList() }
        
        val profile = profileDeferred.await()
        val wallet = walletDeferred.await()
        val billingAccounts = billingAccountsDeferred.await()
        val recentTransactions = recentTransactionsDeferred.await()
        
        // EXIT SKELETON AFTER MINIMUM DURATION
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed < 2500) {
            delay(2500 - elapsed)
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                isAuthenticated = true,
                dashboard = it.dashboard.copy(
                    greeting = greetingForCurrentTime(profile?.firstName),
                    profile = profile,
                    wallet = wallet,
                    billingAccounts = billingAccounts,
                    recentTransactions = recentTransactions,
                ),
            )
        }
        Log.d("SyncFlow", "Tier 1 complete (UI Rendered) in ${System.currentTimeMillis() - startTime}ms")

        // Tier 2 (background): Notifications, Bills, Usage, Autopay, Methods
        repositoryScope.launch {
            val tier2Start = System.currentTimeMillis()
            val billsDeferred = async(networkDispatcher) { runCatching { service.fetchBills(userId) }.getOrNull() ?: emptyList() }
            val notificationsDeferred = async(networkDispatcher) { runCatching { service.fetchNotifications(userId) }.getOrNull() ?: emptyList() }
            val usageDeferred = async(networkDispatcher) { runCatching { service.fetchUsage(userId) }.getOrNull() }
            val autopayDeferred = async(networkDispatcher) { runCatching { service.fetchAutopay(userId) }.getOrNull() }
            val paymentMethodsDeferred = async(networkDispatcher) { runCatching { service.fetchPaymentMethods(userId) }.getOrNull() ?: emptyList() }

            val bills = billsDeferred.await()
            val notifications = notificationsDeferred.await()
            val usage = usageDeferred.await()
            val autopay = autopayDeferred.await()
            val paymentMethods = paymentMethodsDeferred.await()

            cacheSnapshot(userId, profile, wallet, recentTransactions, bills, notifications)
            
            _uiState.update { currentState ->
                currentState.copy(
                    dashboard = currentState.dashboard.copy(
                        bills = bills,
                        notifications = notifications,
                        usage = usage,
                        autopay = autopay,
                        paymentMethods = paymentMethods,
                        currentDeviceId = service.currentDeviceId()
                    ),
                )
            }
            Log.d("SyncFlow", "Tier 2 complete in ${System.currentTimeMillis() - tier2Start}ms")

            // Tier 3 (lazy): Full History, Devices, Usage Periods (Points)
            val tier3Start = System.currentTimeMillis()
            val transactionsDeferred = async(networkDispatcher) { runCatching { service.fetchTransactions(userId) }.getOrNull() ?: emptyList() }
            val devicesDeferred = async(networkDispatcher) { runCatching { service.fetchDevices(userId) }.getOrNull() ?: emptyList() }
            val walletTransactionsDeferred = async(networkDispatcher) { runCatching { service.fetchWalletTransactions(userId) }.getOrNull() ?: emptyList() }
            val usagePeriodsDeferred = async(networkDispatcher) { runCatching { service.fetchUsageMetrics(userId) }.getOrNull() ?: emptyList() }

            val transactions = transactionsDeferred.await()
            val devices = devicesDeferred.await()
            val walletTransactions = walletTransactionsDeferred.await()
            val usagePeriods = usagePeriodsDeferred.await()

            val predicted = predictNextBill(transactions)
            circuitBreaker.onSuccess()

            _uiState.update { currentState ->
                currentState.copy(
                    dashboard = currentState.dashboard.copy(
                        transactions = transactions,
                        devices = devices,
                        walletTransactions = walletTransactions,
                        usagePeriods = usagePeriods,
                        predictedBill = predicted,
                    ),
                )
            }
            Log.d("SyncFlow", "Tier 3 complete in ${System.currentTimeMillis() - tier3Start}ms")
            Log.d("SyncFlow", "Total sync cycle finished in ${System.currentTimeMillis() - startTime}ms")
        }
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
            dao.upsertProfile(ProfileEntity(userId = userId, firstName = it.firstName, lastName = it.lastName, email = it.email, phone = it.phone, location = it.location, avatarUrl = it.avatarUrl, darkMode = it.darkMode, accountStatus = it.accountStatus, updatedAt = it.updatedAt ?: ""))
        }
        wallet?.let {
            dao.upsertWallet(WalletEntity(userId = userId, balance = it.balance, updatedAt = it.updatedAt ?: ""))
        }
        dao.upsertTransactions(transactions.map { item -> TransactionEntity(id = item.id, userId = userId, kind = item.kind, utilityType = item.utilityType, amount = item.amount, status = item.status, meterNumber = item.meterNumber, paymentMethod = item.paymentMethod, occurredAt = item.occurredAt ?: "", createdAt = item.createdAt ?: "") })
        dao.upsertBills(bills.map { bill -> BillEntity(id = bill.id, userId = userId, amountDue = bill.amountDue, dueDate = bill.dueDate, status = bill.status, createdAt = bill.createdAt ?: "") })
        dao.upsertNotifications(notifications.map { notification -> NotificationEntity(id = notification.id, userId = userId, title = notification.title, body = notification.body, type = notification.type, isRead = notification.isRead, createdAt = notification.createdAt ?: "") })
    }

    private suspend fun loadCachedSnapshot(userId: String) {
        val cachedProfile = dao.getProfile(userId)
        val cachedWallet = dao.getWallet(userId)
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
                    idempotencyKey = "evt_${UUID.randomUUID()}", // Point 5
                    createdAt = Instant.now().toString()
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

    private fun resetToSignedOutState() {
        _uiState.value = UiState(
            isInitializing = false,
            isAuthReady = true,
            isLoading = false,
            isAuthenticated = false,
            activeError = if (service.isConfigured()) null else "Configure Supabase credentials in gradle.properties.",
        )
    }

    private fun publishError(error: Throwable) {
        Log.e("SyncFlow", "Operation failed", error)
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
        var lastSyncTime = 0L
        while (true) {
            delay(60_000)
            val session = service.currentSession() ?: continue
            if (!_uiState.value.isOffline) {
                val now = System.currentTimeMillis()
                if (now - lastSyncTime > 60_000) {
                    runCatching { syncAllData(session.userId) }
                    lastSyncTime = now
                }
            }
        }
    }

    private fun predictNextBill(transactions: List<TransactionRecord>): Double {
        val utilityPayments = transactions.filter { it.kind == TransactionKind.UTILITY_PAYMENT.name && it.status == "succeeded" }
        if (utilityPayments.isEmpty()) return 0.0
        
        // Calculate daily average based on the last 30 days
        val thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS)
        val recentPayments = utilityPayments.filter { 
            runCatching { OffsetDateTime.parse(it.occurredAt).toInstant() }.getOrNull()?.isAfter(thirtyDaysAgo) ?: false
        }
        
        if (recentPayments.isEmpty()) {
            // Fallback to overall average if no payments in last 30 days
            return utilityPayments.map { it.amount }.average()
        }
        
        val totalSpent = recentPayments.sumOf { it.amount }
        // Simple 30-day projection: if they spent X in the last 30 days, we predict they'll spend that again
        return totalSpent
    }

    private class CircuitBreaker(val threshold: Int = 3, val resetTimeoutMs: Long = 60_000L) {
        private var failures = 0
        private var lastFailureTime = 0L
        private var state = State.CLOSED
        enum class State { CLOSED, OPEN, HALF_OPEN }
        fun canAttempt(): Boolean {
            if (state == State.OPEN) {
                if (System.currentTimeMillis() - lastFailureTime > resetTimeoutMs) {
                    state = State.HALF_OPEN
                    return true
                }
                return false
            }
            return true
        }
        fun onSuccess() { failures = 0; state = State.CLOSED }
        fun onFailure() {
            failures++
            lastFailureTime = System.currentTimeMillis()
            if (failures >= threshold) state = State.OPEN
        }
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
