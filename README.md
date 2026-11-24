# Word Nerd — a dictionary of obscure & enchanting words

An Android app for collecting delightful, rarely used words: *petrichor*, *apricity*,
*mundivagant*, *pogonophile*, *schadenfreude*, *kvell*, *Kafkaesque* and nearly 480 more. Built with Kotlin, Jetpack
Compose and Material 3 (Material You), in the style of Google's own apps.

| Today | Browse | Search | Word | Saved |
|---|---|---|---|---|
| ![](docs/screenshots/1_today.png) | ![](docs/screenshots/2_browse.png) | ![](docs/screenshots/3_search.png) | ![](docs/screenshots/4_detail.png) | ![](docs/screenshots/5_saved.png) |

| Settings | Notification time |
|---|---|
| ![](docs/screenshots/7_settings_on.png) | ![](docs/screenshots/8_time_picker.png) |

## Features
- **Word of the day**: a new word every day, with Save and Share buttons.
- **Discover**: shuffle through random words.
- **Browse**: the whole lexicon A–Z with sticky letter headers, a pill search bar that
  searches words, meanings and origins (try "Yiddish" or "Greek"), and filter chips
  for 11 themes, including Mind & Thought and Words from Names.
- **Word pages**: pronunciation (tap the speaker to hear it read aloud), part of speech,
  definition, an example sentence, origin/etymology, and related words.
- **Saved**: bookmark favourites; they are kept on the device with DataStore.
- **Daily word notification** (Settings, gear icon on Today): turn it on, pick a time,
  and get the word of the day every day; tapping it opens the word. Asks for the
  notification permission on Android 13+, and keeps its schedule through reboots.
- **Updates itself from GitHub Releases**: a banner on Today when a new version is out,
  with download progress and one-tap install. Can be turned off in Settings.
- Copy and share any word.
- Material You dynamic color on Android 12+ (with a teal fallback palette), dark theme,
  edge-to-edge, splash screen, and a themed adaptive icon.

All definitions, etymologies and example sentences are written fresh for this app.

### What makes the cut
Every word is either in standard English dictionaries (Merriam-Webster / OED) with some
living use, or a loanword English speakers genuinely use and write about (*hygge*,
*wabi-sabi*, *Weltschmerz*, *chutzpah*). Internet-only coinages and words that are only
recorded once in old texts are left out.

## Install
Download the latest `WordNerd-x.y.apk` from
[Releases](https://github.com/Yuri054/Word-Nerd/releases) (Android 8.0 or later).
After that, the app updates itself: it checks GitHub for new releases (Settings → App
updates), downloads the APK and hands it to Android's installer.

## Build
Requires JDK 17+ and the Android SDK (platform 35).
```
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease      # signed if keystore.properties exists, else unsigned
./gradlew testDebugUnitTest    # Robolectric screenshots → app/build/screenshots
```

## Adding words
Edit `app/src/main/java/com/wordnerd/dictionary/data/Words.kt`; each entry is one
`w(word, pronunciation, partOfSpeech, definition, origin, example, category)` call.

## Releasing
Releases are built and signed by GitHub Actions (`.github/workflows/release.yml`).
1. Bump `versionCode` and `versionName` in `app/build.gradle.kts` and push to `main`.
2. Optionally add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
3. On GitHub, draft a new release with the tag `v<versionName>` (e.g. `v1.5`) and
   publish it. The release notes you write appear in the app's update banner.
4. The workflow builds `WordNerd-<versionName>.apk`, signs it and attaches it to the
   release. Installed apps will see the update within 12 hours, or right away via
   Settings → Check for updates.

The workflow fails on purpose if the tag doesn't match `versionName`, so the app can
never offer an "update" that is really the same version.

Signing uses four repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`. Every release must be signed with the same key, or Android
will refuse to install it over the previous version. Keep a backup of the keystore.

## License
[GPL-3.0](LICENSE)
