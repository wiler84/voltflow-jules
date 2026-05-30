package com.example.voltflow

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaymentFlowInstrumentedTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun payFlow_navigatesToSuccess() {
        // Find and click Pay Now button
        composeTestRule.onNodeWithText("Pay Now").performClick()

        // Wait and click Pay button on Pay screen (wait up to 5 seconds)
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Pay").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Pay").performClick()

        // Wait for Payment Successful to appear (up to 5 seconds)
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Payment Successful").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
