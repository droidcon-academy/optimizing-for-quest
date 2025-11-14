package com.droidcon.soundbox.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.droidcon.soundbox.ui.GradientTopAppBar

private data class SettingsBlurb(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val body: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: (() -> Unit)? = null
) {
    val blurbs = listOf(
        SettingsBlurb(
            icon = Icons.Default.Celebration,
            title = "Discover Sounds",
            body = "Explore our curated chaos\u2014tap anything that makes your eyebrows raise."
        ),
        SettingsBlurb(
            icon = Icons.Default.Mic,
            title = "Record Your Own",
            body = "Capture elevator hums, hallway whistles, or your dog\u2019s dramatic sighs. Everything deserves a remix."
        ),
        SettingsBlurb(
            icon = Icons.Default.PhoneAndroid,
            title = "Promote To Ringtone",
            body = "When a favorite hit lands, crown it as your ringtone and let every call feel like a headline."
        ),
        SettingsBlurb(
            icon = Icons.Default.NotificationsActive,
            title = "Need More Noise?",
            body = "Check back soon for fresh packs. Until then, enjoy the quiet knowing chaos is only one tap away."
        )
    )

    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(text = "SoundBox Settings") },
                navigationIcon = {
                    onBack?.let {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 24.dp,
                vertical = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(blurbs) { blurb ->
                SettingsCard(blurb)
            }
            item {
                Text(
                    text = "Still hunting for sliders? Same. For now, ride the vibes and get back to making glorious noise.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(blurb: SettingsBlurb) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = blurb.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = blurb.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = blurb.body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
