package com.wordnerd.dictionary

import android.content.Context
import android.speech.tts.TextToSpeech
import com.wordnerd.dictionary.data.Word
import com.wordnerd.dictionary.data.spokenFor
import java.util.Locale

/**
 * Text-to-speech wrapper that only binds the (heavy, out-of-process) TTS engine the first
 * time something is actually spoken, and keeps a single instance for the whole app session.
 *
 * Words are not simply read from their spelling: see [com.wordnerd.dictionary.data.Spoken].
 */
class Speaker(context: Context) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: Word? = null

    fun speak(word: Word) {
        val engine = tts
        if (engine == null) {
            pending = word
            tts = TextToSpeech(appContext) { status ->
                ready = status == TextToSpeech.SUCCESS
                if (ready) {
                    tts?.setSpeechRate(0.9f) // a touch slower, for unfamiliar words
                    pending?.let { say(it) }
                }
                pending = null
            }
        } else if (ready) {
            say(word)
        } else {
            pending = word
        }
    }

    private fun say(word: Word) {
        val engine = tts ?: return
        val spoken = spokenFor(word)
        val native = spoken.locale?.let { Locale.forLanguageTag(it) }
        if (native != null && engine.trySetLanguage(native)) {
            engine.speak(spoken.native ?: word.word, TextToSpeech.QUEUE_FLUSH, null, word.id)
        } else {
            engine.trySetLanguage(Locale.US)
            engine.speak(spoken.english ?: word.word, TextToSpeech.QUEUE_FLUSH, null, word.id)
        }
    }

    /** True only if a voice for [locale] is installed and ready (not just downloadable). */
    private fun TextToSpeech.trySetLanguage(locale: Locale): Boolean =
        isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE &&
            setLanguage(locale) >= TextToSpeech.LANG_AVAILABLE

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
