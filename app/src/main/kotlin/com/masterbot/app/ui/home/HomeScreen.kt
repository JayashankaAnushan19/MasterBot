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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartTodayReview: () -> Unit,
    onStartTopic: (String) -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MasterBot") },
                actions = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Profile")
                    }
                },
            )
        },
    ) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(padding),
            color = MaterialTheme.colorScheme.background,
        ) {
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
            item { PillarHeader(section.pillar) }
            itemsIndexed(section.topics) { index, topic ->
                TopicPathNode(
                    index = index,
                    node = topic,
                    accent = pillarAccent(section.pillar),
                    onClick = { if (!topic.locked) onStartTopic(topic.topicId) },
                )
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

private data class PillarStyle(val label: String, val glyph: String, val accent: Color)

private val pillarStyles = mapOf(
    "it" to PillarStyle("IT", "💻", Color(0xFF4FC3F7)),
    "mechanical" to PillarStyle("Mechanical", "⚙️", Color(0xFFFFB74D)),
    "electronic" to PillarStyle("Electronic", "🔌", Color(0xFFBA68C8)),
)

private fun pillarAccent(pillar: String): Color = pillarStyles[pillar]?.accent ?: Color(0xFF00E5A0)

@Composable
private fun PillarHeader(pillar: String) {
    val style = pillarStyles[pillar]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background((style?.accent ?: Color.Gray).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(style?.glyph ?: "•", style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            style?.label ?: pillar.replaceFirstChar(Char::uppercase),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = style?.accent ?: MaterialTheme.colorScheme.onBackground,
        )
    }
}

private val tierColors = mapOf(
    MasteryTier.NONE to Color(0xFF3A4552),
    MasteryTier.BRONZE to Color(0xFFCD7F32),
    MasteryTier.SILVER to Color(0xFFC0C0C0),
    MasteryTier.GOLD to Color(0xFFFFD700),
)

@Composable
private fun TopicPathNode(index: Int, node: TopicNode, accent: Color, onClick: () -> Unit) {
    val alignment = when (index % 3) {
        0 -> Alignment.Start
        1 -> Alignment.CenterHorizontally
        else -> Alignment.End
    }
    val badgeColor = if (node.locked) Color(0xFF2A323C) else tierColors.getValue(node.tier)

    Box(modifier = Modifier.fillMaxWidth().height(96.dp)) {
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
        Card(
            modifier = Modifier
                .align(
                    when (alignment) {
                        Alignment.Start -> Alignment.CenterStart
                        Alignment.End -> Alignment.CenterEnd
                        else -> Alignment.Center
                    },
                )
                .padding(horizontal = 24.dp)
                .clickable(enabled = !node.locked, onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (node.locked) {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
            ),
            border = if (!node.locked) {
                androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.5f))
            } else {
                null
            },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(badgeColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (node.locked) "🔒" else badgeGlyph(node.tier), style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        node.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (node.locked) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Text(statusLabel(node), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun statusLabel(node: TopicNode): String = when {
    node.locked -> "Locked"
    node.tier != MasteryTier.NONE -> node.tier.name.lowercase().replaceFirstChar(Char::uppercase)
    node.completed -> "In progress"
    else -> "Not started"
}

private fun badgeGlyph(tier: MasteryTier): String = when (tier) {
    MasteryTier.NONE -> "•"
    MasteryTier.BRONZE -> "🥉"
    MasteryTier.SILVER -> "🥈"
    MasteryTier.GOLD -> "🥇"
}
