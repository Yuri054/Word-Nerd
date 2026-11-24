package com.wordnerd.dictionary

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithContentDescription
import com.github.takahirom.roborazzi.captureRoboImage
import com.wordnerd.dictionary.data.ALL_WORDS
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule(order = 0) val offline = FakeGitHubRule()
    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    @Test fun wordIdsAreUnique() {
        val dupes = ALL_WORDS.groupBy { it.id }.filterValues { it.size > 1 }.keys
        assertEquals("duplicate words: " + dupes, emptySet<String>(), dupes)
    }

    @Test fun everyThemeHasPlentyOfWords() {
        com.wordnerd.dictionary.data.Category.entries.forEach { c ->
            val n = ALL_WORDS.count { it.category == c }
            org.junit.Assert.assertTrue("${c.label} has only " + n, n >= 15)
        }
        println("Lexicon size: " + ALL_WORDS.size + " — " +
            com.wordnerd.dictionary.data.Category.entries.joinToString { it.label + "=" + ALL_WORDS.count { w -> w.category == it } })
    }

    @Test fun walkthrough() {
        rule.onRoot().captureRoboImage("build/screenshots/1_today.png")

        rule.onNodeWithText("Browse").performClick()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/screenshots/2_browse.png")

        rule.onNodeWithText("Search words & meanings").performTextInput("Yiddish")
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/screenshots/3_search.png")

        rule.onNodeWithText("Kvell").performClick()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/screenshots/4_detail.png")
        rule.onNodeWithContentDescription("Save word").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Saved").performClick()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/screenshots/5_saved.png")
    }
}
