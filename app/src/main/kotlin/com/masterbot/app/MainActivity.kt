package com.masterbot.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.masterbot.app.ui.home.HomeScreen
import com.masterbot.app.ui.profile.ProfileScreen
import com.masterbot.app.ui.review.ReviewScreen
import com.masterbot.app.ui.review.ReviewViewModel
import com.masterbot.app.ui.theme.MasterBotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MasterBotTheme {
                MasterBotNavHost(application)
            }
        }
    }
}

@Composable
private fun MasterBotNavHost(application: Application) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 5 } },
        exitTransition = { fadeOut(tween(220)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(220)) + slideOutHorizontally(tween(220)) { it / 5 } },
    ) {
        composable("home") {
            HomeScreen(
                onStartTodayReview = { navController.navigate("review") },
                onStartTopic = { topicId ->
                    navController.navigate("review/${encodeTopicId(topicId)}")
                },
                onOpenProfile = { navController.navigate("profile") },
            )
        }
        composable("review") {
            val viewModel: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory(application, topicId = null))
            ReviewScreen(onBack = { navController.popBackStack() }, viewModel = viewModel)
        }
        composable(
            "review/{topicId}",
            arguments = listOf(navArgument("topicId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val encodedTopicId = backStackEntry.arguments?.getString("topicId")
            val topicId = encodedTopicId?.let { decodeTopicId(it) }
            val viewModel: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory(application, topicId = topicId))
            ReviewScreen(onBack = { navController.popBackStack() }, viewModel = viewModel)
        }
        composable("profile") {
            ProfileScreen(onBack = { navController.popBackStack() })
        }
    }
}

// Topic ids look like "it/programming/ros2-nodes-topics". Navigation-Compose's route
// matcher splits/decodes path segments on '/' internally, so URL-encoding a slash and
// packing it into a single {topicId} segment is unreliable -- it can extract the wrong
// argument. '~' never appears in a topic id slug, so this substitution is unambiguous
// and never touches Navigation's own URI parsing at all.
private fun encodeTopicId(topicId: String) = topicId.replace("/", "~")
private fun decodeTopicId(encoded: String) = encoded.replace("~", "/")
