package com.irlstreamer.reconstruction

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class SettingsFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun settingsNavigationAndBackFollowTheAuditedHierarchy() {
        composeRule.onNodeWithTag("settings").performClick()
        composeRule.onNodeWithTag("settings_app_bar").assertExists()
        composeRule.onNodeWithTag("setting_streamer").performClick()
        composeRule.onNodeWithTag("setting_show_platform_icons").assertExists()
        composeRule.onNodeWithTag("navigate_up").performClick()
        composeRule.onNodeWithTag("setting_connections").assertExists()
    }

    @Test
    fun connectionValidationIsVisibleAndSaveIsNotSilent() {
        composeRule.onNodeWithTag("settings").performClick()
        composeRule.onNodeWithTag("setting_connections").performClick()
        composeRule.onNodeWithTag("setting_new_connection").performClick()
        composeRule.onNodeWithTag("field_connection_name").assertExists()
        composeRule.onNodeWithTag("form_save").assertIsNotEnabled()
    }
}
