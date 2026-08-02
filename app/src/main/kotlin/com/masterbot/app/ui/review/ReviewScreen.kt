package com.masterbot.app.ui.review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(onBack: () -> Unit, viewModel: ReviewViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to home")
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
                is ReviewUiState.Syncing -> SyncingContent()
                is ReviewUiState.SyncFailed -> SyncFailedContent(message = s.message, onRetry = viewModel::retry)
                is ReviewUiState.Ready -> ReadyContent(state = s, onReveal = viewModel::reveal, onAnswer = viewModel::answer)
                is ReviewUiState.GoalReached -> GoalReachedContent(reviewedToday = s.reviewedToday, onKeepPracticing = viewModel::keepPracticing)
                is ReviewUiState.AllCaughtUp -> AllCaughtUpContent(reviewedToday = s.reviewedToday)
            }
        }
    }
}

@Composable
private fun SyncingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Pulling MasterBot_Repo…", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SyncFailedContent(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Sync failed", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun GoalReachedContent(reviewedToday: Int, onKeepPracticing: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Daily goal reached 🔥", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Reviewed $reviewedToday cards today.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onKeepPracticing) { Text("Keep practicing") }
        }
    }
}

@Composable
private fun AllCaughtUpContent(reviewedToday: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("All caught up 🔥", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                if (reviewedToday > 0) "Reviewed $reviewedToday cards today. Nothing left to practice." else "Nothing due right now.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ReadyContent(
    state: ReviewUiState.Ready,
    onReveal: () -> Unit,
    onAnswer: (Boolean) -> Unit,
) {
    val card = state.queue[state.index]

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${state.index + 1} / ${state.queue.size}",
                style = MaterialTheme.typography.labelLarge,
            )
            AnimatedVisibility(
                visible = state.lastCoinsEarned != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut(),
            ) {
                Text(
                    "+${state.lastCoinsEarned ?: 0} 🪙",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        if (card.type == "mcq" && card.options.isNotEmpty()) {
            McqContent(question = card.question, options = card.options, correctAnswer = card.answer, cardKey = state.index, onAnswer = onAnswer)
        } else {
            SwipeReviewContent(state = state, onReveal = onReveal, onAnswer = onAnswer)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.SwipeReviewContent(
    state: ReviewUiState.Ready,
    onReveal: () -> Unit,
    onAnswer: (Boolean) -> Unit,
) {
    val card = state.queue[state.index]

    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        SwipeCard(
            onSwipedRight = { if (state.revealed) onAnswer(true) },
            onSwipedLeft = { if (state.revealed) onAnswer(false) },
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
        ) { offsetX ->
            Box(modifier = Modifier.fillMaxSize()) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            Text(card.type.uppercase(), style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(card.question, style = MaterialTheme.typography.headlineSmall)
                        }
                        if (state.revealed) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                Text("Answer", style = MaterialTheme.typography.labelMedium)
                                Text(card.answer, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                SwipeHintOverlay(offsetX, modifier = Modifier.fillMaxSize())
            }
        }
    }

    Spacer(Modifier.height(20.dp))

    if (!state.revealed) {
        Button(onClick = onReveal, modifier = Modifier.fillMaxWidth()) {
            Text("Reveal answer")
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedButton(onClick = { onAnswer(false) }, modifier = Modifier.weight(1f)) {
                Text("✗ Missed it")
            }
            Button(onClick = { onAnswer(true) }, modifier = Modifier.weight(1f)) {
                Text("✓ Got it")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "or swipe the card — right = got it, left = missed it",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** Multiple choice: tap an option to answer, no swipe/reveal — grading is immediate and objective. */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.McqContent(
    question: String,
    options: List<String>,
    correctAnswer: String,
    cardKey: Any,
    onAnswer: (Boolean) -> Unit,
) {
    var selected by remember(cardKey) { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("MCQ", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(4.dp))
                Text(question, style = MaterialTheme.typography.headlineSmall)
            }
        }
        Spacer(Modifier.height(20.dp))

        options.forEach { option ->
            val isSelected = option == selected
            val isCorrectOption = option == correctAnswer
            val colors = when {
                selected == null -> ButtonDefaults.outlinedButtonColors()
                isCorrectOption -> ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF00E5A0))
                isSelected -> ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFFF5470))
                else -> ButtonDefaults.outlinedButtonColors()
            }
            OutlinedButton(
                onClick = { if (selected == null) selected = option },
                enabled = selected == null,
                colors = colors,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Text(option)
            }
        }
    }

    Spacer(Modifier.height(20.dp))

    if (selected != null) {
        Button(
            onClick = { onAnswer(selected == correctAnswer) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continue")
        }
    }
}
