package com.wordnerd.dictionary.update

import java.io.File

sealed interface UpdateState {
    /** Nothing checked yet this session. */
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: Release) : UpdateState
    data class Downloading(val release: Release, val progress: Float) : UpdateState
    data class ReadyToInstall(val release: Release, val apk: File) : UpdateState
    data class Failed(val message: String, val release: Release? = null) : UpdateState
}
