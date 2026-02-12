package com.example.voltflow

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        val viewModel = PaymentViewModel()
        assertFalse(viewModel.isProcessing)
        assertFalse(viewModel.paymentSuccess)
    }

    @Test
    fun `startPayment updates state correctly`() = runTest {
        val viewModel = PaymentViewModel()
        viewModel.startPayment()
        assertTrue(viewModel.isProcessing)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.isProcessing)
        assertTrue(viewModel.paymentSuccess)
    }
}