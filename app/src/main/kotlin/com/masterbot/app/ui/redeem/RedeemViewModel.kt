package com.masterbot.app.ui.redeem

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.masterbot.app.MasterBotApplication
import com.masterbot.app.data.db.AppDatabase
import com.masterbot.app.data.db.GiftRedemptionEntity
import com.masterbot.app.data.sync.RepoSync
import com.masterbot.app.data.sync.SyncState
import com.masterbot.engine.model.Gift
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface RedeemUiState {
    data object Loading : RedeemUiState
    data class SyncFailed(val message: String) : RedeemUiState
    data class Ready(
        val currentCoins: Int,
        val currency: String,
        val gifts: List<Gift>,
        val redemptions: List<GiftRedemptionEntity>,
    ) : RedeemUiState
}

class RedeemViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.get(application).dao()
    private val repoSync = RepoSync(application)
    private val syncCoordinator = (application as MasterBotApplication).syncCoordinator

    private val _uiState = MutableStateFlow<RedeemUiState>(RedeemUiState.Loading)
    val uiState: StateFlow<RedeemUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            syncCoordinator.state.collect { state ->
                when (state) {
                    is SyncState.Syncing -> _uiState.value = RedeemUiState.Loading
                    is SyncState.Error -> _uiState.value = RedeemUiState.SyncFailed(state.message)
                    else -> loadShop()
                }
            }
        }
    }

    fun retrySync() = syncCoordinator.applyPendingUpdate()

    private suspend fun loadShop() {
        val catalog = repoSync.currentGiftCatalog()
        val progress = dao.userProgress()
        val redemptions = dao.allRedemptions()
        _uiState.value = RedeemUiState.Ready(
            currentCoins = progress?.totalCoins ?: 0,
            currency = catalog.currency,
            gifts = catalog.gifts,
            redemptions = redemptions,
        )
    }

    /** No-op if coins are insufficient -- the UI already disables Redeem in that case, this is the backstop. */
    fun redeem(gift: Gift) {
        viewModelScope.launch {
            val progress = dao.userProgress() ?: return@launch
            if (progress.totalCoins < gift.coinCost) return@launch

            dao.upsertUserProgress(progress.copy(totalCoins = progress.totalCoins - gift.coinCost))
            dao.insertRedemption(
                GiftRedemptionEntity(
                    giftId = gift.id,
                    giftName = gift.name,
                    giftEmoji = gift.emoji,
                    coinCost = gift.coinCost,
                    claimedEpochDay = LocalDate.now().toEpochDay(),
                    usedEpochDay = null,
                ),
            )
            loadShop()
        }
    }

    fun markUsed(redemptionId: Long) {
        viewModelScope.launch {
            dao.markRedemptionUsed(redemptionId, LocalDate.now().toEpochDay())
            loadShop()
        }
    }
}
