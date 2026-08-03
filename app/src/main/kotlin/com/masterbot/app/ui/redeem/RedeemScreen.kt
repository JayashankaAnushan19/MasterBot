package com.masterbot.app.ui.redeem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.masterbot.app.data.db.GiftRedemptionEntity
import com.masterbot.engine.model.Gift
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedeemScreen(onBack: () -> Unit, viewModel: RedeemViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Redeem") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize().padding(padding), color = MaterialTheme.colorScheme.background) {
            when (val s = state) {
                is RedeemUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is RedeemUiState.SyncFailed -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Sync failed", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(s.message, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = viewModel::retrySync) { Text("Retry") }
                    }
                }
                is RedeemUiState.Ready -> RedeemReadyContent(
                    state = s,
                    onRedeem = viewModel::redeem,
                    onMarkUsed = viewModel::markUsed,
                )
            }
        }
    }
}

@Composable
private fun RedeemReadyContent(
    state: RedeemUiState.Ready,
    onRedeem: (Gift) -> Unit,
    onMarkUsed: (Long) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("🪙 ${state.currentCoins} coins", style = MaterialTheme.typography.headlineSmall)
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Shop") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("My Rewards") })
        }

        when (selectedTab) {
            0 -> ShopTab(state.gifts, state.currentCoins, state.currency, onRedeem)
            else -> RewardsTab(state.redemptions, onMarkUsed)
        }
    }
}

@Composable
private fun ShopTab(gifts: List<Gift>, currentCoins: Int, currency: String, onRedeem: (Gift) -> Unit) {
    var pendingRedeem by remember { mutableStateOf<Gift?>(null) }

    pendingRedeem?.let { gift ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingRedeem = null },
            title = { Text("Redeem ${gift.emoji} ${gift.name}?") },
            text = { Text("${gift.coinCost} coins will be deducted. Go buy it yourself once redeemed, then mark it used from My Rewards.") },
            confirmButton = {
                TextButton(onClick = { onRedeem(gift); pendingRedeem = null }) { Text("Redeem") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRedeem = null }) { Text("Cancel") }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
    ) {
        items(gifts) { gift ->
            val affordable = currentCoins >= gift.coinCost
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("${gift.emoji} ${gift.name}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${gift.priceInCurrency.toInt()} $currency  •  ${gift.coinCost} coins",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (affordable) {
                        Button(onClick = { pendingRedeem = gift }) { Text("Redeem") }
                    } else {
                        OutlinedButton(onClick = {}, enabled = false) {
                            Text("Need ${gift.coinCost - currentCoins} more")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RewardsTab(redemptions: List<GiftRedemptionEntity>, onMarkUsed: (Long) -> Unit) {
    if (redemptions.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                "No rewards redeemed yet -- earn coins and treat yourself in the Shop tab.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
    ) {
        items(redemptions) { redemption ->
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("${redemption.giftEmoji} ${redemption.giftName}", style = MaterialTheme.typography.titleMedium)
                    Text("${redemption.coinCost} coins spent", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Claimed: ${LocalDate.ofEpochDay(redemption.claimedEpochDay)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (redemption.usedEpochDay != null) {
                        Text(
                            "Used: ${LocalDate.ofEpochDay(redemption.usedEpochDay)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { onMarkUsed(redemption.id) }) { Text("Mark as used") }
                    }
                }
            }
        }
    }
}
