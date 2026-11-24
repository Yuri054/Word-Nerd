package com.wordnerd.dictionary.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.wordnerd.dictionary.WordViewModel
import com.wordnerd.dictionary.notifications.DailyWordNotifier
import com.wordnerd.dictionary.update.UpdateChecker
import com.wordnerd.dictionary.update.UpdateInstaller
import com.wordnerd.dictionary.update.UpdateState
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: WordViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val daily by vm.daily.collectAsState()
    val update by vm.update.collectAsState()
    val autoUpdate by vm.autoUpdateCheck.collectAsState()
    var canPost by remember { mutableStateOf(DailyWordNotifier.canPost(context)) }
    var showPicker by remember { mutableStateOf(false) }

    // Re-check when returning from the system notification settings.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { canPost = DailyWordNotifier.canPost(context) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        canPost = DailyWordNotifier.canPost(context)
        if (granted) vm.setDailyEnabled(true)
    }

    fun enable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !canPost) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.setDailyEnabled(true)
        }
    }

    val timeText = LocalTime.of(daily.hour, daily.minute)
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            Text(
                "Notifications",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
            )
            ListItem(
                modifier = Modifier.clickable { if (daily.enabled) vm.setDailyEnabled(false) else enable() },
                leadingContent = { Icon(Icons.Outlined.NotificationsActive, contentDescription = null) },
                headlineContent = { Text("Daily word") },
                supportingContent = { Text("Get the word of the day as a notification") },
                trailingContent = {
                    Switch(
                        checked = daily.enabled && canPost,
                        onCheckedChange = { on -> if (on) enable() else vm.setDailyEnabled(false) },
                    )
                },
            )
            ListItem(
                modifier = Modifier.clickable(enabled = daily.enabled) { showPicker = true },
                leadingContent = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                headlineContent = { Text("Time") },
                supportingContent = { Text(if (daily.enabled) "Every day at $timeText" else timeText) },
            )
            if (daily.enabled && canPost) {
                ListItem(
                    modifier = Modifier.clickable { vm.previewNotification() },
                    leadingContent = { Icon(Icons.Outlined.Send, contentDescription = null) },
                    headlineContent = { Text("Send a test notification") },
                    supportingContent = { Text("See what today's notification looks like") },
                )
            }
            if (daily.enabled && !canPost) {
                ListItem(
                    modifier = Modifier.clickable {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    },
                    leadingContent = {
                        Icon(Icons.Outlined.NotificationsOff, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error)
                    },
                    headlineContent = { Text("Notifications are turned off") },
                    supportingContent = { Text("Tap to allow them in system settings") },
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(
                "App updates",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
            )
            val busy = update is UpdateState.Checking || update is UpdateState.Downloading
            ListItem(
                modifier = Modifier.clickable(enabled = !busy) {
                    when (update) {
                        is UpdateState.Available -> vm.downloadUpdate()
                        is UpdateState.ReadyToInstall -> vm.installUpdate()
                        else -> vm.checkForUpdates()
                    }
                },
                leadingContent = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
                headlineContent = {
                    Text(
                        when (val u = update) {
                            is UpdateState.Available -> "Update to version ${u.release.version}"
                            is UpdateState.ReadyToInstall -> "Install version ${u.release.version}"
                            else -> "Check for updates"
                        },
                    )
                },
                supportingContent = {
                    Text(
                        when (val u = update) {
                            UpdateState.Idle -> "You have version ${vm.currentVersion}"
                            UpdateState.Checking -> "Checking GitHub…"
                            UpdateState.UpToDate -> "You're on the latest version (${vm.currentVersion})"
                            is UpdateState.Available -> "A new version is ready to download"
                            is UpdateState.Downloading -> "Downloading… ${(u.progress * 100).toInt()}%"
                            is UpdateState.ReadyToInstall ->
                                if (UpdateInstaller.canInstall(context)) "Downloaded — tap to install"
                                else "Allow Word Nerd to install updates, then tap here"
                            is UpdateState.Failed -> u.message
                        },
                    )
                },
                trailingContent = {
                    if (busy) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                },
            )
            ListItem(
                modifier = Modifier.clickable { vm.setAutoUpdateCheck(!autoUpdate) },
                leadingContent = { Icon(Icons.Outlined.Autorenew, contentDescription = null) },
                headlineContent = { Text("Check automatically") },
                supportingContent = { Text("Look for a new version when you open the app") },
                trailingContent = { Switch(checked = autoUpdate, onCheckedChange = vm::setAutoUpdateCheck) },
            )
            ListItem(
                modifier = Modifier.clickable { UpdateInstaller.openInBrowser(context, UpdateChecker.RELEASES_PAGE) },
                leadingContent = { Icon(Icons.Outlined.Code, contentDescription = null) },
                headlineContent = { Text("Release notes & source") },
                supportingContent = { Text("github.com/" + UpdateChecker.REPO) },
            )
        }
    }

    if (showPicker) {
        val state = rememberTimePickerState(
            initialHour = daily.hour,
            initialMinute = daily.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Notification time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    vm.setDailyTime(state.hour, state.minute)
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
        )
    }
}
