package com.wordnerd.dictionary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wordnerd.dictionary.data.ALL_WORDS
import com.wordnerd.dictionary.data.Category
import com.wordnerd.dictionary.data.FavoritesRepository
import com.wordnerd.dictionary.data.HistoryEntry
import com.wordnerd.dictionary.data.HistoryRepository
import com.wordnerd.dictionary.data.DailyNotificationSettings
import com.wordnerd.dictionary.data.SettingsRepository
import com.wordnerd.dictionary.data.Word
import com.wordnerd.dictionary.data.wordOfTheDay
import com.wordnerd.dictionary.notifications.DailyWordNotifier
import com.wordnerd.dictionary.update.UpdateChecker
import com.wordnerd.dictionary.update.UpdateInstaller
import com.wordnerd.dictionary.update.UpdateState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val AUTO_CHECK_INTERVAL_MS = 12 * 60 * 60 * 1000L

class WordViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = FavoritesRepository(app)

    val favorites: StateFlow<Set<String>> =
        repo.favorites.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val wordOfTheDay: Word = wordOfTheDay()

    // ── Word of the day history ──
    private val historyRepo = HistoryRepository(app)

    /** Past days only (today is shown in the hero card), newest first. */
    val history: StateFlow<List<HistoryEntry>> = historyRepo.entries
        .map { list -> list.filter { it.date.isBefore(LocalDate.now()) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch { historyRepo.recordThrough() }
    }

    // ── Pronunciation (engine is created lazily on first use) ──
    private val speaker = Speaker(app)
    fun speak(word: Word) = speaker.speak(word)
    override fun onCleared() = speaker.shutdown()

    // ── Daily notification ──
    private val settingsRepo = SettingsRepository(app)
    val daily: StateFlow<DailyNotificationSettings> =
        settingsRepo.daily.stateIn(viewModelScope, SharingStarted.Eagerly, DailyNotificationSettings())

    init {
        // Make sure the next reminder is queued (e.g. after an app update cleared jobs),
        // without pushing back one that is already pending.
        viewModelScope.launch {
            val s = settingsRepo.current()
            if (s.enabled) DailyWordNotifier.schedule(app, s.hour, s.minute, replace = false)
        }
    }

    fun setDailyEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepo.setEnabled(enabled)
        val s = settingsRepo.current()
        if (enabled) DailyWordNotifier.schedule(getApplication(), s.hour, s.minute)
        else DailyWordNotifier.cancel(getApplication())
    }

    fun setDailyTime(hour: Int, minute: Int) = viewModelScope.launch {
        settingsRepo.setTime(hour, minute)
        if (settingsRepo.current().enabled) DailyWordNotifier.schedule(getApplication(), hour, minute)
    }

    fun previewNotification() = DailyWordNotifier.show(getApplication(), wordOfTheDay)

    // ── App updates (GitHub Releases) ──
    val currentVersion: String = BuildConfig.VERSION_NAME
    private val _update = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val update: StateFlow<UpdateState> = _update.asStateFlow()
    val autoUpdateCheck: StateFlow<Boolean> =
        settingsRepo.autoUpdateCheck.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** Whether the Today banner was dismissed for this session. */
    private val _updateBannerDismissed = MutableStateFlow(false)
    val updateBannerDismissed: StateFlow<Boolean> = _updateBannerDismissed.asStateFlow()
    fun dismissUpdateBanner() { _updateBannerDismissed.value = true }

    init {
        UpdateInstaller.cleanUp(app)
        viewModelScope.launch {
            val due = System.currentTimeMillis() - settingsRepo.lastUpdateCheck() > AUTO_CHECK_INTERVAL_MS
            if (settingsRepo.autoUpdateCheck.first() && due) checkForUpdates(manual = false)
        }
    }

    fun setAutoUpdateCheck(enabled: Boolean) = viewModelScope.launch { settingsRepo.setAutoUpdateCheck(enabled) }

    /** A manual check reports errors; an automatic one fails silently. */
    fun checkForUpdates(manual: Boolean = true) {
        if (_update.value is UpdateState.Checking || _update.value is UpdateState.Downloading) return
        viewModelScope.launch {
            _update.value = UpdateState.Checking
            _update.value = try {
                val release = UpdateChecker.newerRelease(currentVersion)
                settingsRepo.setLastUpdateCheck(System.currentTimeMillis())
                if (release != null) UpdateState.Available(release) else UpdateState.UpToDate
            } catch (e: Exception) {
                if (manual) UpdateState.Failed("Couldn't reach GitHub. Check your connection and try again.")
                else UpdateState.Idle
            }
        }
    }

    fun downloadUpdate() {
        val release = when (val s = _update.value) {
            is UpdateState.Available -> s.release
            is UpdateState.Failed -> s.release
            else -> null
        } ?: return
        val url = release.apkUrl
        if (url == null) {
            UpdateInstaller.openInBrowser(getApplication(), release.pageUrl)
            return
        }
        viewModelScope.launch {
            _update.value = UpdateState.Downloading(release, 0f)
            _update.value = try {
                val apk = UpdateInstaller.download(getApplication(), url, release.apkSize) { p ->
                    _update.value = UpdateState.Downloading(release, p)
                }
                UpdateState.ReadyToInstall(release, apk).also { installUpdate() }
            } catch (e: Exception) {
                UpdateState.Failed("The download didn't finish. Try again.", release)
            }
        }
    }

    /** Opens the system installer, first asking for permission to install updates if needed. */
    fun installUpdate() {
        val state = _update.value as? UpdateState.ReadyToInstall ?: return
        val app = getApplication<Application>()
        if (UpdateInstaller.canInstall(app)) {
            UpdateInstaller.install(app, state.apk)
        } else {
            awaitingInstallPermission = true
            UpdateInstaller.openInstallPermissionSettings(app)
        }
    }

    private var awaitingInstallPermission = false

    /** Called when the app returns to the foreground: finish an install that was waiting on permission. */
    fun onAppResumed() {
        if (awaitingInstallPermission && UpdateInstaller.canInstall(getApplication())) {
            awaitingInstallPermission = false
            installUpdate()
        }
    }

    private val _discover = MutableStateFlow(pickRandom(exclude = wordOfTheDay))
    val discover: StateFlow<Word> = _discover.asStateFlow()

    fun shuffle() { _discover.value = pickRandom(exclude = _discover.value) }

    private fun pickRandom(exclude: Word): Word = ALL_WORDS.filter { it != exclude }.random()

    // ── Browse ──
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _category = MutableStateFlow<Category?>(null)
    val category: StateFlow<Category?> = _category.asStateFlow()

    fun setQuery(q: String) { _query.value = q }
    fun setCategory(c: Category?) { _category.value = c }

    val results: StateFlow<List<Word>> = combine(_query, _category) { q, c ->
        val needle = q.trim()
        val inCategory = ALL_WORDS.filter { c == null || it.category == c }
        if (needle.isEmpty()) return@combine inCategory
        // Rank: headword prefix, headword substring, definition, then origin
        // (so "Yiddish" or "Greek" finds words by where they come from).
        inCategory.mapNotNull { w ->
            when {
                w.word.startsWith(needle, ignoreCase = true) -> 0
                w.word.contains(needle, ignoreCase = true) -> 1
                w.definition.contains(needle, ignoreCase = true) -> 2
                w.origin.contains(needle, ignoreCase = true) -> 3
                else -> null
            }?.let { rank -> rank to w }
        }.sortedBy { it.first }.map { it.second }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ALL_WORDS)

    fun toggleFavorite(id: String) = viewModelScope.launch { repo.toggle(id) }
}
