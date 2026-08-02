package com.masterbot.app.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.masterbot.app.MasterBotApplication
import com.masterbot.app.data.db.AppDatabase
import com.masterbot.app.data.db.UserProfileEntity
import com.masterbot.app.data.sync.SyncState
import com.masterbot.app.notifications.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(val name: String, val notificationsEnabled: Boolean)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.get(application).dao()
    private val syncCoordinator = (application as MasterBotApplication).syncCoordinator

    val syncState: StateFlow<SyncState> = syncCoordinator.state

    private val _uiState = MutableStateFlow(ProfileUiState(name = "", notificationsEnabled = false))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = dao.userProfile()
            if (profile != null) {
                _uiState.value = ProfileUiState(profile.name, profile.notificationsEnabled)
            }
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
        persist()
    }

    /** [granted] should already reflect the POST_NOTIFICATIONS result on Android 13+ (true on older versions). */
    fun setNotificationsEnabled(enabled: Boolean, granted: Boolean) {
        val actuallyEnabled = enabled && granted
        _uiState.value = _uiState.value.copy(notificationsEnabled = actuallyEnabled)
        persist()
        val app = getApplication<Application>()
        if (actuallyEnabled) NotificationScheduler.schedule(app) else NotificationScheduler.cancel(app)
    }

    fun checkForUpdates() = syncCoordinator.checkForUpdates()
    fun pullUpdate() = syncCoordinator.applyPendingUpdate()
    fun dismissUpdate() = syncCoordinator.dismissUpdate()

    private fun persist() {
        viewModelScope.launch {
            dao.upsertUserProfile(
                UserProfileEntity(name = _uiState.value.name, notificationsEnabled = _uiState.value.notificationsEnabled),
            )
        }
    }
}
