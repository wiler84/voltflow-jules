package com.example.voltflow

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PaymentViewModel : ViewModel() {
    var isProcessing: getValue by mutableStateOf(false)
        private set

    var paymentSuccess: getValue by mutableStateOf(false)
        private set

    fun startPayment() {
        if (isProcessing) return
        isProcessing = true
        viewModelScope.launch {
            // simulate network/payment processing
            delay(1500)
            isProcessing = false
            paymentSuccess = true
        }
    }

    fun clearSuccess() {
        paymentSuccess = false
    }
}
