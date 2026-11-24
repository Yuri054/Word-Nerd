package com.wordnerd.dictionary.update

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** A published GitHub release. */
data class Release(
    val version: String,
    val title: String,
    val notes: String,
    val pageUrl: String,
    val apkUrl: String?,
    val apkSize: Long,
)

object UpdateChecker {
    const val REPO = "Yuri054/Word-Nerd"
    const val RELEASES_PAGE = "https://github.com/$REPO/releases"
    private const val LATEST_API = "https://api.github.com/repos/$REPO/releases/latest"

    /** Fetches the latest-release JSON, or null if the repo has no releases yet. Swappable in tests. */
    @VisibleForTesting
    var fetchLatestJson: suspend () -> String? = { httpGet(LATEST_API) }

    /** Returns the latest release if it is newer than [currentVersion], otherwise null. */
    suspend fun newerRelease(currentVersion: String): Release? {
        val json = fetchLatestJson() ?: return null
        val release = parse(json)
        return release.takeIf { isNewer(it.version, currentVersion) }
    }

    fun parse(json: String): Release {
        val o = JSONObject(json)
        val tag = o.getString("tag_name")
        val assets = o.optJSONArray("assets")
        var apkUrl: String? = null
        var apkSize = 0L
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                    apkUrl = a.optString("browser_download_url").ifBlank { null }
                    apkSize = a.optLong("size")
                    break
                }
            }
        }
        return Release(
            version = tag.trimStart('v', 'V'),
            title = o.optString("name").ifBlank { tag },
            notes = o.optString("body").trim(),
            pageUrl = o.optString("html_url").ifBlank { RELEASES_PAGE },
            apkUrl = apkUrl,
            apkSize = apkSize,
        )
    }

    /** Compares dotted versions numerically: 1.10 > 1.9, 1.4 == 1.4.0. */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = numbers(candidate)
        val b = numbers(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun numbers(v: String): List<Int> =
        v.trimStart('v', 'V').substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }

    private suspend fun httpGet(url: String): String? = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "WordNerd-Android")
        }
        try {
            when (val code = conn.responseCode) {
                200 -> conn.inputStream.bufferedReader().use { it.readText() }
                404 -> null // no releases published yet
                else -> throw IOException("GitHub responded with HTTP $code")
            }
        } finally {
            conn.disconnect()
        }
    }
}
