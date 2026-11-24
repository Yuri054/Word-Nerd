package com.wordnerd.dictionary

import com.wordnerd.dictionary.update.UpdateChecker
import org.junit.rules.ExternalResource
import java.io.IOException

/** Replaces the GitHub call so tests never touch the network. Must run before the activity starts. */
class FakeGitHubRule(private val response: () -> String? = { throw IOException("offline") }) : ExternalResource() {
    private lateinit var original: suspend () -> String?
    override fun before() {
        original = UpdateChecker.fetchLatestJson
        UpdateChecker.fetchLatestJson = { response() }
    }
    override fun after() { UpdateChecker.fetchLatestJson = original }
}

fun releaseJson(tag: String, notes: String = "New words and fixes.", apk: Boolean = true): String = """
{
  "tag_name": "$tag",
  "name": "Word Nerd ${tag.trimStart('v')}",
  "body": "$notes",
  "html_url": "https://github.com/Yuri054/Word-Nerd/releases/tag/$tag",
  "assets": [
    { "name": "checksums.txt", "browser_download_url": "https://example.invalid/sums", "size": 10 }${if (apk) """,
    { "name": "WordNerd-${tag.trimStart('v')}.apk", "browser_download_url": "https://example.invalid/app.apk", "size": 1600000 }""" else ""}
  ]
}
"""
