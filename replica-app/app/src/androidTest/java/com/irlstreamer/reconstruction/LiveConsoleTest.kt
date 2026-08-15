package com.irlstreamer.reconstruction

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class LiveConsoleTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchShowsFunctionalLiveConsoleAndBroadcastGuard() {
        composeRule.onNodeWithTag("live_console").assertExists()
        composeRule.onNodeWithTag("start_broadcast").performClick()
        composeRule.onNodeWithTag("dialog_no_connection").assertExists()
    }

    @Test
    fun quickSettingsCanOpenAndSwitchTabs() {
        composeRule.onNodeWithTag("quick_settings").performClick()
        composeRule.onNodeWithTag("quick_settings_panel").assertExists()
        composeRule.onNodeWithTag("quick_tab_network").performClick()
        composeRule.onNodeWithTag("quick_enable_cellular").assertExists()
    }
}
