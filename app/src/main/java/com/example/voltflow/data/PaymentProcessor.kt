package com.example.voltflow.data

import kotlinx.coroutines.delay
import java.util.UUID

interface PaymentProcessor {
    suspend fun process(userId: String, draft: PaymentDraft): PaymentProcessorResult
}

data class PaymentProcessorResult(
    val processorReference: String,
    val status: String,
)

class MockPaymentProcessor : PaymentProcessor {
    override suspend fun process(userId: String, draft: PaymentDraft): PaymentProcessorResult {
        delay(450)
        return PaymentProcessorResult(
            processorReference = "mock-${UUID.randomUUID()}",
            status = "succeeded",
        )
    }
}

class BraintreePaymentProcessor : PaymentProcessor {
    override suspend fun process(userId: String, draft: PaymentDraft): PaymentProcessorResult {
        // Braintree integration reserved for future use.
        // For now, return a dummy/future-ready result that won't crash.
        // In production, this would initialize Braintree client from BuildConfig and process real payments.
        delay(300) // Simulate API call
        return PaymentProcessorResult(
            processorReference = "braintree-reserved-${UUID.randomUUID()}",
            status = "pending", // Reserved status for future implementation
        )
    }
}
