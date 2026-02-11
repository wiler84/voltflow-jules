package com.example.voltflow

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PaymentViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testPaymentFlowSetsSuccess(): runTest = runTest(testDispatcher) {
        val vm = PaymentViewModel()
        assertFalse(vm.isProcessing)
        assertFalse(vm.paymentSuccess)

        vm.startPayment()
        advanceUntilIdle() // Fast-forward time until all coroutines complete

        assertFalse(vm.isProcessing)
        assertTrue(vm.paymentSuccess)
    }
}
