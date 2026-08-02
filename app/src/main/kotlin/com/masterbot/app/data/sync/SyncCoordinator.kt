package com.masterbot.app.data.sync

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SyncState {
    data object Syncing : SyncState
    data object Ready : SyncState
    data object UpdateAvailable : SyncState
    data class Error(val message: String) : SyncState
}

/**
 * Single app-scoped owner of repo sync. Screens read local Room data directly and only
 * ever observe [state] to know whether that data is ready yet -- they never trigger a
 * network sync themselves. This is what keeps every screen visit instant instead of
 * doing a git pull every time, and keeps two screens from ever racing each other's sync.
 */
class SyncCoordinator(context: Context, private val scope: CoroutineScope) {

    private val repoSync = RepoSync(context)

    private val _state = MutableStateFlow<SyncState>(SyncState.Syncing)
    val state: StateFlow<SyncState> = _state.asStateFlow()

    init {
        scope.launch {
            if (repoSync.hasClonedRepo()) {
                // Already have content from a previous launch -- don't silently re-pull,
                // just check and let the user decide (same as a manual check).
                runCheck()
            } else {
                // First ever launch: nothing to conflict with, safe to sync unconditionally.
                runFullSync()
            }
        }
    }

    fun checkForUpdates() {
        scope.launch { runCheck() }
    }

    fun applyPendingUpdate() {
        scope.launch { runFullSync() }
    }

    fun dismissUpdate() {
        if (_state.value is SyncState.UpdateAvailable) _state.value = SyncState.Ready
    }

    private suspend fun runCheck() {
        _state.value = SyncState.Syncing
        _state.value = when (val result = repoSync.checkForUpdates()) {
            is RepoSync.CheckResult.UpToDate -> SyncState.Ready
            is RepoSync.CheckResult.UpdatesAvailable -> SyncState.UpdateAvailable
            is RepoSync.CheckResult.Failure -> SyncState.Error(result.message)
        }
    }

    private suspend fun runFullSync() {
        _state.value = SyncState.Syncing
        _state.value = when (val result = repoSync.syncAndLoad()) {
            is RepoSync.Result.Success -> SyncState.Ready
            is RepoSync.Result.Failure -> SyncState.Error(result.message)
        }
    }
}
