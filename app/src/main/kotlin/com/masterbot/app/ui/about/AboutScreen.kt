package com.masterbot.app.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.masterbot.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(padding),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(16.dp))
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.avatar_tier_5),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(96.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text("MasterBot", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("v1.0", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(12.dp))
                Text(
                    "An offline-first, gamified mastery app for robotics knowledge across IT, Mechanical, and Electronic fundamentals.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(Modifier.height(24.dp))
                Text("Developer", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Text("Jayashankar Anushan", style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:jayasankaanushan199@gmail.com"))
                    context.startActivity(intent)
                }) {
                    Text("jayasankaanushan199@gmail.com")
                }
            }
        }
    }
}
