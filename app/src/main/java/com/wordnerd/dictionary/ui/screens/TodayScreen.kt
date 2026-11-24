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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wordnerd.dictionary.WordViewModel
import com.wordnerd.dictionary.data.ALL_WORDS
import com.wordnerd.dictionary.data.Category
import com.wordnerd.dictionary.data.Word
import com.wordnerd.dictionary.ui.components.UpdateBanner
import com.wordnerd.dictionary.ui.components.shareWord
import com.wordnerd.dictionary.ui.theme.HeadwordFont
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    vm: WordViewModel,
    onOpenWord: (Word) -> Unit,
    onOpenCategory: (Category) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val favorites by vm.favorites.collectAsState()
    val discover by vm.discover.collectAsState()
    val update by vm.update.collectAsState()
    val bannerDismissed by vm.updateBannerDismissed.collectAsState()
    val context = LocalContext.current
    val scroll = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Word Nerd", fontFamily = HeadwordFont, fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp, bottom = 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            if (!bannerDismissed) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    UpdateBanner(
                        state = update,
                        onUpdate = vm::downloadUpdate,
                        onInstall = vm::installUpdate,
                        onLater = vm::dismissUpdateBanner,
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                val w = vm.wordOfTheDay
                HeroCard(
                    word = w,
                    isFavorite = w.id in favorites,
                    onClick = { onOpenWord(w) },
                    onSpeak = { vm.speak(w) },
                    onFavorite = { vm.toggleFavorite(w.id) },
                    onShare = { shareWord(context, w) },
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                DiscoverCard(discover, onClick = { onOpenWord(discover) }, onShuffle = vm::shuffle)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Explore by theme",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                )
            }
            items(Category.entries) { cat ->
                CategoryCard(cat, ALL_WORDS.count { it.category == cat }) { onOpenCategory(cat) }
            }
        }
    }
}

@Composable
private fun HeroCard(
    word: Word,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onSpeak: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(24.dp)) {
            Text("WORD OF THE DAY", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(12.dp))
            Text(
                word.word,
                fontFamily = HeadwordFont,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.displaySmall,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${word.partOfSpeech} · /${word.pronunciation}/",
                    fontStyle = FontStyle.Italic,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onSpeak) {
                    Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = "Pronounce")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(word.definition, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onFavorite) {
                    Icon(
                        if (isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = null, modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isFavorite) "Saved" else "Save")
                }
                OutlinedButton(onClick = onShare) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share")
                }
            }
        }
    }
}

@Composable
private fun DiscoverCard(word: Word, onClick: () -> Unit, onShuffle: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Discover",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    word.word, fontFamily = HeadwordFont, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    word.definition,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            FilledTonalIconButton(onClick = onShuffle, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Outlined.Casino, contentDescription = "Random word")
            }
        }
    }
}

@Composable
private fun CategoryCard(category: Category, count: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth().height(112.dp),
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Icon(category.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            Text(category.label, style = MaterialTheme.typography.titleSmall)
            Text(
                "$count words",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
