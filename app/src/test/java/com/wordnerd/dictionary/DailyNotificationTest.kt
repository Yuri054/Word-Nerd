package com.wordnerd.dictionary

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.github.takahirom.roborazzi.captureRoboImage
import com.wordnerd.dictionary.data.SettingsRepository
import com.wordnerd.dictionary.data.wordOfTheDay
import com.wordnerd.dictionary.notifications.DailyWordNotifier
import com.wordnerd.dictionary.notifications.DailyWordWorker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowTextToSpeech

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class DailyNotificationTest {
    @get:Rule(order = 0) val offline = FakeGitHubRule()
    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun initWork() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        shadowOf(rule.activity.application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    /** DataStore writes finish on a background thread, so pump the main looper while waiting. */
    private fun eventually(what: String, check: () -> Boolean) {
        repeat(100) {
            shadowOf(android.os.Looper.getMainLooper()).idle()
            rule.waitForIdle()
            if (check()) return
            Thread.sleep(50)
        }
        throw AssertionError("Timed out waiting for: $what (jobs=${dailyJobs().map { it.state }}, enabled=${runBlocking { SettingsRepository(context).current() }})")
    }

    private fun dailyJobs(): List<WorkInfo> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork("daily_word").get()

    @Test fun noTextToSpeechEngineUntilSpeakerTapped() {
        rule.waitForIdle()
        assertNull("TTS engine started at launch", ShadowTextToSpeech.getLastTextToSpeechInstance())

        rule.onNodeWithContentDescription("Pronounce").performClick()
        rule.waitForIdle()
        val tts: TextToSpeech? = ShadowTextToSpeech.getLastTextToSpeechInstance()
        assertTrue("TTS engine created on tap", tts != null)
    }

    @Test fun enablingDailyWordSchedulesIt() {
        initWork()
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/screenshots/6_settings_off.png")

        rule.onNodeWithText("Daily word").performClick()
        eventually("job enqueued") { dailyJobs().any { it.state == WorkInfo.State.ENQUEUED } }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/screenshots/7_settings_on.png")

        rule.onNodeWithText("Time").performClick()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/screenshots/8_time_picker.png")
        rule.onNodeWithText("Cancel").performClick()

        rule.onNodeWithText("Daily word").performClick()
        eventually("job cancelled") { dailyJobs().all { it.state == WorkInfo.State.CANCELLED } }
    }

    @Test fun workerPostsWordOfTheDayAndReschedules() {
        initWork()
        runBlocking {
            SettingsRepository(context).setEnabled(true)
            val result = TestListenableWorkerBuilder<DailyWordWorker>(context).build().doWork()
            assertTrue(result is androidx.work.ListenableWorker.Result.Success)
        }
        val nm = context.getSystemService(NotificationManager::class.java)
        val posted = shadowOf(nm).allNotifications
        assertEquals(1, posted.size)
        val title = posted[0].extras.getString("android.title")
        assertEquals("Word of the day: ${wordOfTheDay().word}", title)
        assertTrue(dailyJobs().any { it.state == WorkInfo.State.ENQUEUED })
        runBlocking { SettingsRepository(context).setEnabled(false) }
    }

    @Test fun tappingNotificationOpensTheWord() {
        val word = wordOfTheDay()
        rule.activity.runOnUiThread {
            rule.activity.onNewIntent(
                Intent(context, MainActivity::class.java).putExtra(DailyWordNotifier.EXTRA_WORD_ID, word.id),
            )
        }
        rule.waitForIdle()
        rule.onNodeWithText("Definition").fetchSemanticsNode()
        rule.onNodeWithText("Origin").fetchSemanticsNode()
    }
}
