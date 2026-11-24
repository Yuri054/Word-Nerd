package com.wordnerd.dictionary

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.github.takahirom.roborazzi.captureRoboImage
import com.wordnerd.dictionary.data.HistoryRepository
import com.wordnerd.dictionary.data.HistoryRepository.Companion.MAX_BACKFILL_DAYS
import com.wordnerd.dictionary.data.wordOfTheDay
import com.wordnerd.dictionary.ui.screens.historyDateLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class HistoryTest {
    @get:Rule(order = 0) val offline = FakeGitHubRule()
    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    private val today = LocalDate.of(2026, 11, 24)

    @Test fun fillsDaysSinceInstall() {
        val days = HistoryRepository.missingDays(emptySet(), installed = today.minusDays(2), today = today)
        assertEquals(listOf(today.minusDays(2), today.minusDays(1), today), days)
    }

    @Test fun skipsDaysAlreadyRecorded() {
        val days = HistoryRepository.missingDays(setOf(today.minusDays(1)), today.minusDays(2), today)
        assertEquals(listOf(today.minusDays(2), today), days)
    }

    @Test fun backfillIsCapped() {
        val days = HistoryRepository.missingDays(emptySet(), installed = today.minusYears(3), today = today)
        assertEquals(MAX_BACKFILL_DAYS.toInt(), days.size)
        assertEquals(today, days.last())
    }

    @Test fun ignoresCorruptEntries() {
        val parsed = HistoryRepository.parse(setOf("2026-11-23|lustrum", "garbage", "2026-13-40|x", "2026-11-22|"))
        assertEquals(mapOf(LocalDate.of(2026, 11, 23) to "lustrum"), parsed)
    }

    @Test fun dateLabels() {
        assertEquals("Yesterday", historyDateLabel(today.minusDays(1), today))
        assertEquals("Sun, Nov 22", historyDateLabel(today.minusDays(2), today))
        assertEquals("Tue, Nov 25, 2025", historyDateLabel(LocalDate.of(2025, 11, 25), today))
    }

    @Test fun todayShowsPastWordsAndSeeAllOpensHistory() {
        // Robolectric reports an install date long ago, so history is backfilled.
        val yesterday = wordOfTheDay(LocalDate.now().minusDays(1))
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Past words of the day")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("See all"))
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/screenshots/11_today_history.png")
        rule.onNodeWithText(yesterday.word).fetchSemanticsNode()

        rule.onNodeWithText("See all").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Yesterday").fetchSemanticsNode()
        rule.onRoot().captureRoboImage("build/screenshots/12_history.png")

        rule.onNodeWithText(yesterday.word).performClick()
        rule.waitForIdle()
        assertTrue(rule.onAllNodes(hasText("Definition")).fetchSemanticsNodes().isNotEmpty())
    }
}
