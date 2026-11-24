package com.wordnerd.dictionary.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.wordnerd.dictionary.WordViewModel
import com.wordnerd.dictionary.data.HistoryEntry
import com.wordnerd.dictionary.data.Word
import com.wordnerd.dictionary.ui.components.WordListItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** "Yesterday", "Mon, Nov 23", or "Mon, Nov 23, 2025" for an earlier year. */
fun historyDateLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String = when {
    ChronoUnit.DAYS.between(date, today) == 1L -> "Yesterday"
    date.year == today.year -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(vm: WordViewModel, onBack: () -> Unit, onOpenWord: (Word) -> Unit) {
    val history by vm.history.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Past words of the day") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        if (history.isEmpty()) {
            Column(Modifier.padding(padding)) {
                EmptyState(
                    icon = Icons.Outlined.History,
                    title = "No past words yet",
                    body = "Each day's word of the day will be collected here, starting tomorrow.",
                )
            }
            return@Scaffold
        }
        LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 16.dp)) {
            history.groupBy { it.date.withDayOfMonth(1) }.forEach { (month, entries) ->
                stickyHeader(key = "m$month") {
                    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }
                }
                items(entries, key = { it.date.toString() }) { e ->
                    HistoryItem(e, e.word.id in favorites, onOpenWord) { vm.toggleFavorite(e.word.id) }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    entry: HistoryEntry,
    isFavorite: Boolean,
    onOpenWord: (Word) -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Column {
        Text(
            historyDateLabel(entry.date),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 72.dp, top = 8.dp),
        )
        WordListItem(entry.word, isFavorite, { onOpenWord(entry.word) }, onToggleFavorite)
    }
}
