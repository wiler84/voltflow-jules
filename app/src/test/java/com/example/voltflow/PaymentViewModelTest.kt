package com.example.voltflow

import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentViewModelTest {
    @Test
    fun meterLengthPolicy_isExactly15Digits() {
        val validMeter = "123456789012345"
        val shortMeter = "12345678901234"
        val longMeter = "1234567890123456"

        assertEquals(true, validMeter.length == 15)
        assertEquals(false, shortMeter.length == 15)
        assertEquals(false, longMeter.length == 15)
    }
}
