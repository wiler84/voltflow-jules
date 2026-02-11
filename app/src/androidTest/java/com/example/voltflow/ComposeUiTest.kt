package com.example.voltflow

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeUiTest {
    @get:Rule
    val composeTestRule: createAndroidComposeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeShowsGreeting() {
        // Home screen contains "Good morning" greeting
        composeTestRule.onNodeWithText("Good morning").assertIsDisplayed()
    }
}
