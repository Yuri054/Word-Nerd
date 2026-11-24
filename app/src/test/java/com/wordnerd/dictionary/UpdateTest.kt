package com.wordnerd.dictionary

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.wordnerd.dictionary.update.UpdateChecker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class UpdateTest {
    @get:Rule(order = 0) val github = FakeGitHubRule { releaseJson("v9.9", "Adds 120 new words and a dark-mode fix.") }
    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    @Test fun versionComparison() {
        assertTrue(UpdateChecker.isNewer("1.5", "1.4"))
        assertTrue(UpdateChecker.isNewer("v1.10", "1.9"))
        assertTrue(UpdateChecker.isNewer("2.0", "1.99.9"))
        assertFalse(UpdateChecker.isNewer("1.4", "1.4"))
        assertFalse(UpdateChecker.isNewer("1.4.0", "1.4"))
        assertFalse(UpdateChecker.isNewer("1.3", "1.4"))
        assertFalse(UpdateChecker.isNewer("1.4-beta", "1.4"))
    }

    @Test fun parsesGitHubRelease() {
        val r = UpdateChecker.parse(releaseJson("v1.5"))
        assertEquals("1.5", r.version)
        assertEquals("https://example.invalid/app.apk", r.apkUrl)
        assertEquals(1600000L, r.apkSize)
        assertNull(UpdateChecker.parse(releaseJson("v1.5", apk = false)).apkUrl)
    }

    @Test fun sameVersionIsNotAnUpdate() = runBlocking {
        UpdateChecker.fetchLatestJson = { releaseJson("v" + BuildConfig.VERSION_NAME) }
        assertNull(UpdateChecker.newerRelease(BuildConfig.VERSION_NAME))
        UpdateChecker.fetchLatestJson = { null } // repo with no releases yet
        assertNull(UpdateChecker.newerRelease(BuildConfig.VERSION_NAME))
    }

    @Test fun bannerAndSettingsOfferTheUpdate() {
        rule.waitUntil(5_000) {
            rule.onAllNodes(androidx.compose.ui.test.hasText("Update available · version 9.9"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onRoot().captureRoboImage("build/screenshots/9_update_banner.png")

        rule.onNodeWithContentDescription("Settings").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Update to version 9.9").fetchSemanticsNode()
        rule.onRoot().captureRoboImage("build/screenshots/10_settings_updates.png")
        rule.onNodeWithContentDescription("Back").performClick()

        rule.onNodeWithText("Later").performClick()
        rule.waitForIdle()
        assertTrue(rule.onAllNodes(androidx.compose.ui.test.hasText("Update available · version 9.9"))
            .fetchSemanticsNodes().isEmpty())
    }
}
