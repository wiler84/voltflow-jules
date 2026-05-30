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

    companion object {
        fun validateLuhn(number: String): Boolean {
            val digits = number.filter { it.isDigit() }.reversed()
            if (digits.isEmpty()) return false
            var sum = 0
            for (i in digits.indices) {
                var digit = digits[i].toString().toInt()
                if (i % 2 == 1) {
                    digit *= 2
                    if (digit > 9) digit -= 9
                }
                sum += digit
            }
            return sum % 10 == 0
        }

        fun detectBrand(number: String): String {
            val clean = number.filter { it.isDigit() }
            return when {
                clean.startsWith("4") -> "Visa"
                clean.startsWith("5") || (clean.length >= 2 && clean.substring(0, 2).toIntOrNull() in 51..55) -> "Mastercard"
                clean.startsWith("34") || clean.startsWith("37") -> "American Express"
                clean.startsWith("6011") || clean.startsWith("65") -> "Discover"
                else -> "Unknown"
            }
        }
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
