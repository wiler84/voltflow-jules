package com.example.voltflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.voltflow.data.PaymentDraft
import com.example.voltflow.data.UiState
import com.example.voltflow.data.UtilityType
import com.example.voltflow.data.VoltflowRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: VoltflowRepository,
) : ViewModel() {
    val uiState: StateFlow<UiState> = repository.uiState

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

    fun fundWallet(amount: Double) {
        viewModelScope.launch { repository.fundWallet(amount) }
    }

    fun withdrawWallet(amount: Double) {
        viewModelScope.launch { repository.withdrawWallet(amount) }
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

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setBiometricEnabled(enabled) }
    }

    fun setMfaEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setMfaEnabled(enabled) }
    }

    fun setPinEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setPinEnabled(enabled) }
    }

    fun setAutoLockMinutes(minutes: Int) {
        viewModelScope.launch { repository.setAutoLockMinutes(minutes) }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch { repository.markNotificationRead(notificationId) }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { repository.setDarkMode(enabled) }
    }

    fun consumeMessage() = repository.consumeMessage()
    fun consumeError() = repository.consumeError()

    companion object {
        fun factory(repository: VoltflowRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repository) as T
        }
    }
}
