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
        error("BraintreePaymentProcessor is reserved for future integration.")
    }
}
