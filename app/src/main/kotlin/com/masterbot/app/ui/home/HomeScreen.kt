package com.masterbot.app.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.masterbot.app.R
import com.masterbot.app.data.sync.SyncState
import com.masterbot.engine.MasteryTier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartTodayReview: () -> Unit,
    onStartTopic: (String) -> Unit,
    onStartQuizStage: (Int) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenRedeem: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    // Home is the nav start destination, so its ViewModel survives navigating away and
    // back (e.g. finishing a topic and hitting back) -- without this, it just re-shows
    // whatever it loaded the first time, missing newly-unlocked topics/updated coins.
    // This is now a cheap local-only reload (see HomeViewModel.refresh), not a network sync.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (syncState is SyncState.UpdateAvailable) {
        AlertDialog(
            onDismissRequest = viewModel::dismissUpdate,
            title = { Text("New content available") },
            text = { Text("There's new content in MasterBot_Repo. Pull it now?") },
            confirmButton = {
                TextButton(onClick = viewModel::pullUpdate) { Text("Pull now") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissUpdate) { Text("Later") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MasterBot") },
                actions = {
                    IconButton(onClick = onOpenRedeem) {
                        Text("🎁", style = MaterialTheme.typography.titleLarge)
                    }
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
                is HomeUiState.Loading -> PulsingAvatarLoading()
                is HomeUiState.SyncFailed -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Sync failed", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(s.message, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = viewModel::retrySync) { Text("Retry") }
                    }
                }
                is HomeUiState.Ready -> HomeReadyContent(
                    state = s,
                    onStartTodayReview = onStartTodayReview,
                    onStartTopic = onStartTopic,
                    onStartQuizStage = onStartQuizStage,
                    onResetTopic = viewModel::resetTopic,
                )
            }
        }
    }
}

@Composable
private fun PulsingAvatarLoading() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "scale",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.avatar_tier_1),
                contentDescription = null,
                modifier = Modifier.size(96.dp).scale(scale).alpha(alpha),
            )
            Spacer(Modifier.height(16.dp))
            Text("Pulling MasterBot_Repo…", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun HomeReadyContent(
    state: HomeUiState.Ready,
    onStartTodayReview: () -> Unit,
    onStartTopic: (String) -> Unit,
    onStartQuizStage: (Int) -> Unit,
    onResetTopic: (String) -> Unit,
) {
    var topicPendingReset by remember { mutableStateOf<TopicNode?>(null) }

    topicPendingReset?.let { topic ->
        AlertDialog(
            onDismissRequest = { topicPendingReset = null },
            title = { Text("Reset this topic?") },
            text = { Text("\"${topic.title}\" goes back to Not started. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onResetTopic(topic.topicId)
                    topicPendingReset = null
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { topicPendingReset = null }) { Text("Cancel") }
            },
        )
    }

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
            Text(
                "Tip: long-press a topic below to reset just that one",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }

        state.pillars.forEach { section ->
            item { SectionBanner(pillarStyles[section.pillar]?.label ?: section.pillar, pillarStyles[section.pillar]?.glyph ?: "•", pillarAccent(section.pillar)) }
            val currentIndex = section.topics.indexOfFirst { !it.locked && !it.completed }
            itemsIndexed(section.topics) { index, topic ->
                PathNode(
                    index = index,
                    title = topic.title,
                    locked = topic.locked,
                    isCurrent = index == currentIndex,
                    badgeGlyph = if (topic.locked) "🔒" else badgeGlyph(topic.tier),
                    badgeColor = if (topic.locked) LOCKED_COLOR else tierColors.getValue(topic.tier),
                    statusLabel = statusLabel(topic),
                    accent = pillarAccent(section.pillar),
                    onClick = { if (!topic.locked) onStartTopic(topic.topicId) },
                    onLongClick = { if (!topic.locked) topicPendingReset = topic },
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }

        if (state.quizChallengesUnlocked) {
            item { SectionBanner("Quiz Challenges", "🧠", QUIZ_ACCENT) }
            val currentQuizIndex = state.quizStages.indexOfFirst { !it.locked && it.timesCompleted == 0 }
            itemsIndexed(state.quizStages) { index, stage ->
                PathNode(
                    index = index,
                    title = "Quiz ${stage.stageIndex}",
                    locked = stage.locked,
                    isCurrent = index == currentQuizIndex,
                    badgeGlyph = if (stage.locked) "🔒" else if (stage.timesCompleted > 0) "⭐" else "🧠",
                    badgeColor = if (stage.locked) LOCKED_COLOR else QUIZ_ACCENT,
                    statusLabel = if (stage.locked) "Locked" else if (stage.timesCompleted > 0) "Completed x${stage.timesCompleted}" else "Not started",
                    accent = QUIZ_ACCENT,
                    onClick = { if (!stage.locked) onStartQuizStage(stage.stageIndex) },
                    onLongClick = null,
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
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
        Image(
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
private val QUIZ_ACCENT = Color(0xFFFFC107)
private val LOCKED_COLOR = Color(0xFF3A4552)

private fun pillarAccent(pillar: String): Color = pillarStyles[pillar]?.accent ?: Color(0xFF00E5A0)

/** Chunky, solid-colored unit banner -- the Duolingo "section header" look, replacing
 * the earlier small circle+text row. */
@Composable
private fun SectionBanner(label: String, glyph: String, accent: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(accent),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(glyph, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(10.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

private val tierColors = mapOf(
    MasteryTier.NONE to Color(0xFF3A4552),
    MasteryTier.BRONZE to Color(0xFFCD7F32),
    MasteryTier.SILVER to Color(0xFFC0C0C0),
    MasteryTier.GOLD to Color(0xFFFFD700),
)

/** Gentle side-to-side sway instead of a hard 3-position cycle -- reads as one
 * continuous winding path down the screen, closer to Duolingo's actual path shape. */
private val PATH_SWAY = listOf(0.5f, 0.78f, 0.5f, 0.22f)
private fun xFractionForIndex(index: Int): Float = PATH_SWAY[index % PATH_SWAY.size]

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PathNode(
    index: Int,
    title: String,
    locked: Boolean,
    isCurrent: Boolean,
    badgeGlyph: String,
    badgeColor: Color,
    statusLabel: String,
    accent: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    val xFraction = xFractionForIndex(index)
    val prevXFraction = if (index == 0) xFraction else xFractionForIndex(index - 1)
    val lineColor = Color(0xFF3A4552)

    Box(modifier = Modifier.fillMaxWidth().height(132.dp)) {
        if (index > 0) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val startX = prevXFraction * size.width
                val endX = xFraction * size.width
                val midY = size.height * 0.55f
                val path = Path().apply {
                    moveTo(startX, 0f)
                    quadraticTo(startX, midY, (startX + endX) / 2f, midY)
                    quadraticTo(endX, midY, endX, size.height - 44.dp.toPx())
                }
                drawPath(path, color = lineColor, style = Stroke(width = 8f, cap = StrokeCap.Round))
            }
        }

        Column(
            modifier = Modifier
                .align(BiasAlignment(horizontalBias = xFraction * 2f - 1f, verticalBias = 1f))
                .padding(bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCurrent) {
                    PulsingRing(accent)
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                        .combinedClickable(
                            enabled = !locked,
                            onClick = onClick,
                            onLongClick = onLongClick,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(badgeGlyph, style = MaterialTheme.typography.headlineSmall)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                color = if (locked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = accent.takeIf { !locked } ?: MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Expanding, fading ring around the "start here" node -- the one obvious next tap. */
@Composable
private fun PulsingRing(accent: Color) {
    val transition = rememberInfiniteTransition(label = "ring")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Restart),
        label = "ringScale",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Restart),
        label = "ringAlpha",
    )
    Box(
        modifier = Modifier
            .size(72.dp)
            .scale(scale)
            .clip(CircleShape)
            .border(BorderStroke(3.dp, accent.copy(alpha = alpha)), CircleShape),
    )
}

private fun statusLabel(node: TopicNode): String = when {
    node.locked -> "Locked"
    node.tier != MasteryTier.NONE -> node.tier.name.lowercase().replaceFirstChar(Char::uppercase)
    node.completed -> "Completed"
    node.answeredCount > 0 -> "Continue • ${node.answeredCount}/${node.totalCount}"
    else -> "Not started"
}

private fun badgeGlyph(tier: MasteryTier): String = when (tier) {
    MasteryTier.NONE -> "•"
    MasteryTier.BRONZE -> "🥉"
    MasteryTier.SILVER -> "🥈"
    MasteryTier.GOLD -> "🥇"
}
