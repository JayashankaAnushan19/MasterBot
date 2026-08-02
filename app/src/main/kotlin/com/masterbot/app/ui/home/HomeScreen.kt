package com.masterbot.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.masterbot.app.R
import com.masterbot.engine.MasteryTier

@Composable
fun HomeScreen(
    onStartTodayReview: () -> Unit,
    onStartTopic: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val s = state) {
            is HomeUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is HomeUiState.SyncFailed -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sync failed", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(s.message, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = viewModel::refresh) { Text("Retry") }
                }
            }
            is HomeUiState.Ready -> HomeReadyContent(s, onStartTodayReview, onStartTopic)
        }
    }
}

@Composable
private fun HomeReadyContent(
    state: HomeUiState.Ready,
    onStartTodayReview: () -> Unit,
    onStartTopic: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            StatHeader(state)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onStartTodayReview,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            ) {
                Text("Start today's review")
            }
            Spacer(Modifier.height(24.dp))
        }

        state.pillars.forEach { section ->
            item {
                Text(
                    section.pillar.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
            itemsIndexed(section.topics) { index, topic ->
                TopicPathNode(index = index, node = topic, onClick = { onStartTopic(topic.topicId) })
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun StatHeader(state: HomeUiState.Ready) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val avatarRes = when (state.avatarTier) {
            1 -> R.drawable.avatar_tier_1
            2 -> R.drawable.avatar_tier_2
            3 -> R.drawable.avatar_tier_3
            4 -> R.drawable.avatar_tier_4
            else -> R.drawable.avatar_tier_5
        }
        androidx.compose.foundation.Image(
            painter = painterResource(avatarRes),
            contentDescription = "Robot avatar, tier ${state.avatarTier}",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(72.dp),
        )
        Column(horizontalAlignment = Alignment.End) {
            Text("🔥 ${state.currentStreak} day streak", style = MaterialTheme.typography.titleMedium)
            Text("🪙 ${state.totalCoins} coins", style = MaterialTheme.typography.titleMedium)
            if (state.longestStreak > state.currentStreak) {
                Text("Best: ${state.longestStreak}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private val tierColors = mapOf(
    MasteryTier.NONE to Color(0xFF3A4552),
    MasteryTier.BRONZE to Color(0xFFCD7F32),
    MasteryTier.SILVER to Color(0xFFC0C0C0),
    MasteryTier.GOLD to Color(0xFFFFD700),
)

@Composable
private fun TopicPathNode(index: Int, node: TopicNode, onClick: () -> Unit) {
    val alignment = when (index % 3) {
        0 -> Alignment.Start
        1 -> Alignment.CenterHorizontally
        else -> Alignment.End
    }
    val color = tierColors.getValue(node.tier)

    Box(modifier = Modifier.fillMaxWidth().height(84.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val x = size.width / 2f
            drawLine(
                color = Color(0xFF3A4552),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 4f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f)),
            )
        }
        Column(
            modifier = Modifier
                .align(
                    when (alignment) {
                        Alignment.Start -> Alignment.CenterStart
                        Alignment.End -> Alignment.CenterEnd
                        else -> Alignment.Center
                    },
                )
                .padding(horizontal = 32.dp)
                .clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Text(badgeGlyph(node.tier), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text(node.title, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun badgeGlyph(tier: MasteryTier): String = when (tier) {
    MasteryTier.NONE -> "•"
    MasteryTier.BRONZE -> "🥉"
    MasteryTier.SILVER -> "🥈"
    MasteryTier.GOLD -> "🥇"
}
