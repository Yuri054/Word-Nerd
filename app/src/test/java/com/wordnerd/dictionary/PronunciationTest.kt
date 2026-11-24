package com.wordnerd.dictionary

import android.speech.tts.TextToSpeech
import androidx.test.core.app.ApplicationProvider
import com.wordnerd.dictionary.data.ALL_WORDS
import com.wordnerd.dictionary.data.SPOKEN
import com.wordnerd.dictionary.data.wordById
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowTextToSpeech
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PronunciationTest {
    private val speaker = Speaker(ApplicationProvider.getApplicationContext())

    @After fun tearDown() {
        speaker.shutdown()
        ShadowTextToSpeech.reset()
    }

    /** Speaks [id] and returns what the engine was asked to say, and in which language. */
    private fun say(id: String): Pair<String, Locale?> {
        speaker.speak(wordById(id)!!)
        val engine = shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance())
        engine.onInitListener.onInit(TextToSpeech.SUCCESS) // no-op if already initialized
        return engine.lastSpokenText to engine.currentLanguage
    }

    @Test fun everyEntryIsARealWord() {
        val ids = ALL_WORDS.map { it.id }.toSet()
        val unknown = SPOKEN.keys - ids
        assertEquals("pronunciation entries for missing words: $unknown", emptySet<String>(), unknown)
    }

    @Test fun nativeEntriesHaveValidLocales() {
        SPOKEN.forEach { (id, s) ->
            assertTrue("$id has a bad locale ${s.locale}", Locale.forLanguageTag(s.locale).language.length == 2)
        }
    }

    @Test fun regularWordsAreReadAsSpelled() {
        assertEquals("Kerfuffle", say("kerfuffle").first)
    }

    @Test fun englishWordsAreNeverRespelled() {
        // Respellings like "veh lee ih tee" were read chunk by chunk; the real spelling is better.
        assertEquals("Velleity", say("velleity").first)
        assertEquals("Victuals", say("victuals").first)
        assertEquals("Chutzpah", say("chutzpah").first)
    }

    @Test fun loanwordUsesNativeVoiceWhenInstalled() {
        ShadowTextToSpeech.addLanguageAvailability(Locale.GERMANY)
        val (text, lang) = say("weltschmerz")
        assertEquals("Weltschmerz", text)
        assertEquals("de", lang?.language)
    }

    @Test fun loanwordFallsBackToItsSpellingWithoutThatVoice() {
        assertEquals("Weltschmerz", say("weltschmerz").first)
    }

    @Test fun japaneseUsesNativeScript() {
        ShadowTextToSpeech.addLanguageAvailability(Locale.JAPAN)
        assertEquals("積ん読", say("tsundoku").first)
    }
}
