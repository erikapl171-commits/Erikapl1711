package app.liveinterpreter.ui

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.liveinterpreter.MainActivity
import org.junit.Rule
import org.junit.Test

/** Runs on an Android emulator or device, not available in this execution environment. */
class InterpreterScreenTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun demoIsClearlyIdentifiedAndNotRepresentedAsMicrophoneListening() {
        rule.onNodeWithTag("demo_banner").assertExists()
        rule.onNodeWithText("MIC OFF").assertExists()
        rule.onNodeWithTag("demo_disclosure").assertExists()
        rule.onNodeWithTag("demo_toggle").performClick()
        rule.onNodeWithText("Stop sample").assertExists()
    }

    @Test fun settingsAndMicPermissionControlsAreAvailable() {
        rule.onNodeWithTag("open_settings").performClick()
        rule.onNodeWithText("Spoken English").assertExists()
        rule.onNodeWithTag("request_mic_permission").assertExists()
    }
}
