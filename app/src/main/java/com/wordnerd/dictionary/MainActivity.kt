package com.wordnerd.dictionary

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wordnerd.dictionary.data.wordById
import com.wordnerd.dictionary.ui.screens.BrowseScreen
import com.wordnerd.dictionary.ui.screens.HistoryScreen
import com.wordnerd.dictionary.notifications.DailyWordNotifier
import com.wordnerd.dictionary.ui.screens.SavedScreen
import com.wordnerd.dictionary.ui.screens.SettingsScreen
import com.wordnerd.dictionary.ui.screens.TodayScreen
import com.wordnerd.dictionary.ui.screens.WordDetailScreen
import com.wordnerd.dictionary.ui.theme.WordNerdTheme

class MainActivity : ComponentActivity() {
    /** Word to open, set when the app is launched from the daily notification. */
    private val deepLinkWord = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) deepLinkWord.value = intent.wordExtra()
        setContent {
            WordNerdTheme {
                WordNerdApp(deepLinkWord = deepLinkWord.value, onDeepLinkHandled = { deepLinkWord.value = null })
            }
        }
    }

    private val vm: WordViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        vm.onAppResumed()
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.wordExtra()?.let { deepLinkWord.value = it }
    }

    private fun Intent.wordExtra(): String? = getStringExtra(DailyWordNotifier.EXTRA_WORD_ID)
}

private enum class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    TODAY("today", "Today", Icons.Outlined.WbSunny, Icons.Filled.WbSunny),
    BROWSE("browse", "Browse", Icons.Outlined.LibraryBooks, Icons.Filled.LibraryBooks),
    SAVED("saved", "Saved", Icons.Outlined.Bookmarks, Icons.Filled.Bookmarks),
}

@Suppress("DEPRECATION")
@Composable
private fun WordNerdApp(
    deepLinkWord: String?,
    onDeepLinkHandled: () -> Unit,
    vm: WordViewModel = viewModel(),
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    fun goTab(tab: Tab) = nav.navigate(tab.route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    fun openWord(id: String) = nav.navigate("word/$id")

    LaunchedEffect(deepLinkWord) {
        if (deepLinkWord != null && wordById(deepLinkWord) != null) openWord(deepLinkWord)
        if (deepLinkWord != null) onDeepLinkHandled()
    }

    Scaffold(
        bottomBar = {
            if (Tab.entries.any { it.route == route }) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        val selected = route == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { goTab(tab) },
                            icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Tab.TODAY.route, modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {
            composable(Tab.TODAY.route) {
                TodayScreen(
                    vm,
                    onOpenWord = { openWord(it.id) },
                    onOpenCategory = { vm.setQuery(""); vm.setCategory(it); goTab(Tab.BROWSE) },
                    onOpenSettings = { nav.navigate("settings") },
                    onOpenHistory = { nav.navigate("history") },
                )
            }
            composable("history") {
                HistoryScreen(vm, onBack = { nav.popBackStack() }, onOpenWord = { openWord(it.id) })
            }
            composable("settings") { SettingsScreen(vm, onBack = { nav.popBackStack() }) }
            composable(Tab.BROWSE.route) { BrowseScreen(vm) { openWord(it.id) } }
            composable(Tab.SAVED.route) { SavedScreen(vm) { openWord(it.id) } }
            composable("word/{id}") { back ->
                val word = back.arguments?.getString("id")?.let(::wordById) ?: return@composable
                WordDetailScreen(
                    word, vm,
                    onBack = { nav.popBackStack() },
                    onOpenWord = { openWord(it.id) },
                    onOpenCategory = { vm.setQuery(""); vm.setCategory(word.category); goTab(Tab.BROWSE) },
                )
            }
        }
    }
}
