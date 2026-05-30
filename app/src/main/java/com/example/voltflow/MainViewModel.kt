package com.example.voltflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.voltflow.data.LockScope
import com.example.voltflow.data.PaymentDraft
import com.example.voltflow.data.PinVerificationResult
import com.example.voltflow.data.PinResetRequestResult
import com.example.voltflow.data.UiState
import com.example.voltflow.data.UtilityType
import com.example.voltflow.data.VoltflowRepository
import com.example.voltflow.data.UsageRange
import com.example.voltflow.data.UsageChartData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UsageChartState(
    val range: UsageRange = UsageRange.SEVEN_DAYS,
    val isMoneyMode: Boolean = true,
    val data: UsageChartData? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class MainViewModel(
    private val repository: VoltflowRepository,
) : ViewModel() {
    val uiState: StateFlow<UiState> = repository.uiState
    val uiEvents = repository.uiEvents

    // Removed consumeMessage – UI events are now collected via the Channel.

    private val _usageChartState = MutableStateFlow(UsageChartState())
    val usageChartState: StateFlow<UsageChartState> = _usageChartState

    init {
        loadChartData(UsageRange.SEVEN_DAYS)
    }

    fun loadChartData(range: UsageRange, isMoneyMode: Boolean = usageChartState.value.isMoneyMode) {
        viewModelScope.launch {
            _usageChartState.value = _usageChartState.value.copy(isLoading = true, range = range, isMoneyMode = isMoneyMode)
            repository.getUsageChartData(range, isMoneyMode).collect { result ->
                result.onSuccess { chart ->
                    _usageChartState.value = UsageChartState(range = range, isMoneyMode = isMoneyMode, data = chart, isLoading = false)
                }.onFailure { err ->
                    _usageChartState.value = UsageChartState(range = range, isMoneyMode = isMoneyMode, isLoading = false, error = err.message)
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch { repository.signIn(email, password) }
    }

    fun signUp(email: String, password: String, firstName: String, lastName: String) {
        viewModelScope.launch { repository.signUp(email, password, firstName, lastName) }
    }

    fun signOut() {
        viewModelScope.launch { repository.signOut() }
    }

    fun refresh() {
        viewModelScope.launch { repository.refresh() }
    }

    fun restoreSession() {
        viewModelScope.launch { repository.restoreSession() }
    }

    fun saveProfile(firstName: String, lastName: String, phone: String) {
        viewModelScope.launch { repository.saveProfile(firstName, lastName, phone) }
    }

    fun addPaymentMethod(cardBrand: String, cardNumber: String, expiryMonth: Int, expiryYear: Int) {
        viewModelScope.launch { repository.addPaymentMethod(cardBrand, cardNumber, expiryMonth, expiryYear) }
    }

    fun setAutopay(enabled: Boolean, paymentMethodId: String?, amountLimit: Double, billingCycle: String, paymentDay: Int, meterNumber: String?) {
        viewModelScope.launch { repository.setAutopay(enabled, paymentMethodId, amountLimit, billingCycle, paymentDay, meterNumber) }
    }

    fun fundWallet(amount: Double, paymentMethodId: String? = null) {
        viewModelScope.launch { repository.fundWallet(amount, paymentMethodId) }
    }

    fun withdrawWallet(amount: Double, paymentMethodId: String? = null) {
        viewModelScope.launch { repository.withdrawWallet(amount, paymentMethodId) }
    }

    fun payUtility(utilityType: UtilityType, amount: Double, meterNumber: String, paymentMethodId: String?, useWallet: Boolean = true) {
        viewModelScope.launch {
            repository.payUtility(
                PaymentDraft(
                    utilityType = utilityType,
                    amount = amount,
                    meterNumber = meterNumber,
                    paymentMethodId = paymentMethodId,
                    useWallet = useWallet,
                )
            )
        }
    }

    fun revokeDevice(deviceId: String) {
        viewModelScope.launch { repository.revokeDevice(deviceId) }
    }



    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch { repository.markNotificationRead(notificationId) }
    }

    fun updatePushToken(token: String) {
        viewModelScope.launch { repository.updatePushToken(token) }
    }

    fun syncCurrentDeviceLocation() {
        viewModelScope.launch { repository.syncCurrentDeviceLocation() }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { repository.setDarkMode(enabled) }
    }

    fun updatePassword(newPassword: String) {
        viewModelScope.launch { repository.updatePassword(newPassword) }
    }


    fun consumeError() = repository.consumeError()

    companion object {
        fun factory(repository: VoltflowRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repository) as T
        }
    }
}
