package com.wordnerd.dictionary.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One past word of the day. */
data class HistoryEntry(val date: LocalDate, val word: Word)

/**
 * Remembers each day's word of the day, so it stays the same even if the word list
 * changes in a later update. Stored as "yyyy-mm-dd|wordId" strings.
 */
class HistoryRepository(private val context: Context) {
    private val key = stringSetPreferencesKey("wotd_history")

    /** Newest first. Entries whose word no longer exists are skipped. */
    val entries: Flow<List<HistoryEntry>> = context.dataStore.data.map { prefs ->
        parse(prefs[key] ?: emptySet()).mapNotNull { (date, id) ->
            wordById(id)?.let { HistoryEntry(date, it) }
        }.sortedByDescending { it.date }
    }

    /** Records today's word and fills in any days missed since the last visit or install. */
    suspend fun recordThrough(today: LocalDate = LocalDate.now()) {
        val installed = runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            Instant.ofEpochMilli(info.firstInstallTime).atZone(ZoneId.systemDefault()).toLocalDate()
        }.getOrDefault(today)
        context.dataStore.edit { prefs ->
            val existing = parse(prefs[key] ?: emptySet())
            val added = missingDays(existing.keys, installed, today)
            if (added.isNotEmpty()) {
                prefs[key] = (prefs[key] ?: emptySet()) + added.map { "$it|${wordOfTheDay(it).id}" }
            }
        }
    }

    companion object {
        /** How far back to fill in days you didn't open the app. */
        const val MAX_BACKFILL_DAYS = 60L

        fun parse(raw: Set<String>): Map<LocalDate, String> = raw.mapNotNull { entry ->
            val date = runCatching { LocalDate.parse(entry.substringBefore('|')) }.getOrNull()
            val id = entry.substringAfter('|', "")
            if (date != null && id.isNotEmpty()) date to id else null
        }.toMap()

        /** Days from [installed] (at most [MAX_BACKFILL_DAYS] back) through [today] not yet recorded. */
        fun missingDays(recorded: Set<LocalDate>, installed: LocalDate, today: LocalDate): List<LocalDate> {
            val earliest = maxOf(installed, today.minusDays(MAX_BACKFILL_DAYS - 1))
            return generateSequence(earliest) { it.plusDays(1) }
                .takeWhile { !it.isAfter(today) }
                .filter { it !in recorded }
                .toList()
        }
    }
}
