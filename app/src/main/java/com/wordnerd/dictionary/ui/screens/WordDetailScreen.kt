package com.wordnerd.dictionary.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wordnerd.dictionary.WordViewModel
import com.wordnerd.dictionary.data.ALL_WORDS
import com.wordnerd.dictionary.data.Word
import com.wordnerd.dictionary.ui.components.FavoriteButton
import com.wordnerd.dictionary.ui.components.shareWord
import com.wordnerd.dictionary.ui.theme.HeadwordFont
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailScreen(
    word: Word,
    vm: WordViewModel,
    onBack: () -> Unit,
    onOpenWord: (Word) -> Unit,
    onOpenCategory: () -> Unit,
) {
    val favorites by vm.favorites.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val related = remember(word) {
        ALL_WORDS.filter { it.category == word.category && it != word }.shuffled().take(8)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString("${word.word}: ${word.definition}"))
                        scope.launch { snackbar.showSnackbar("Copied to clipboard") }
                    }) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy") }
                    IconButton(onClick = { shareWord(context, word) }) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share")
                    }
                    FavoriteButton(word.id in favorites) { vm.toggleFavorite(word.id) }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding(), bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.padding(horizontal = 8.dp)) {
                    Text(
                        word.word,
                        fontFamily = HeadwordFont,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.displayMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledIconButton(onClick = { vm.speak(word) }) {
                            Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = "Pronounce")
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "/${word.pronunciation}/",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SuggestionChip(onClick = {}, label = { Text(word.partOfSpeech) })
                        AssistChip(
                            onClick = onOpenCategory,
                            label = { Text(word.category.label) },
                            leadingIcon = {
                                Icon(word.category.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                        )
                    }
                }
            }
            item {
                Section(Icons.Outlined.MenuBook, "Definition", highlight = true) {
                    Text(word.definition, style = MaterialTheme.typography.titleLarge)
                }
            }
            item {
                Section(Icons.Outlined.FormatQuote, "In a sentence") {
                    Text(
                        "“${word.example}”",
                        fontStyle = FontStyle.Italic,
                        fontFamily = HeadwordFont,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            item {
                Section(Icons.Outlined.History, "Origin") {
                    Text(word.origin, style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (related.isNotEmpty()) {
                item {
                    Text(
                        "More ${word.category.label.lowercase()}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 8.dp, top = 12.dp),
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(related, key = { it.id }) { r ->
                            Card(
                                onClick = { onOpenWord(r) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                ),
                                modifier = Modifier.width(180.dp),
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(r.word, fontFamily = HeadwordFont, fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                    Text(r.definition, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(
    icon: ImageVector,
    title: String,
    highlight: Boolean = false,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
