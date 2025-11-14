package com.droidcon.soundbox.ui.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.droidcon.soundbox.R
import com.droidcon.soundbox.data.Sound
import com.droidcon.soundbox.service.SoundPlaybackService
import com.droidcon.soundbox.ui.GradientTopAppBar
import com.droidcon.soundbox.ui.theme.SoundBoxTheme
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundPlaybackScreen(sound: Sound, onBack: () -> Unit) {
    val context = LocalContext.current
    var service by remember { mutableStateOf<SoundPlaybackService?>(null) }

    val isPlaying by service?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val currentPosition by service?.currentPosition?.collectAsState() ?: remember { mutableStateOf(0) }
    val duration by service?.duration?.collectAsState() ?: remember { mutableStateOf(0) }

    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                service = (binder as SoundPlaybackService.LocalBinder).getService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
            }
        }
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { }


    LaunchedEffect(sound) {
        Intent(context, SoundPlaybackService::class.java).also {
            it.putExtra(SoundPlaybackService.EXTRA_SOUND_NAME, sound.name)
            context.startService(it)
            context.bindService(it, connection, Context.BIND_AUTO_CREATE)
        }
    }

    LaunchedEffect(service) {
        if (sound.resourceId != null) {
            service?.playSound(sound.resourceId)
        } else if (sound.filePath != null) {
            service?.playSound(sound.filePath)
        }
    }

    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(sound.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {}
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    horizontal = dimensionResource(id = R.dimen.padding_large),
                    vertical = dimensionResource(id = R.dimen.spacing_xlarge)
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = sound.icon,
                    contentDescription = sound.name,
                    modifier = Modifier.size(dimensionResource(id = R.dimen.playback_main_icon_size)),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_12dp))
            ) {
                Text(
                    text = sound.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Sound Effect",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Recorded in ${sound.city}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacing_medium)))
                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                    onValueChange = { service?.seekTo(it) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(duration.toLong())
                    val durationSeconds = TimeUnit.MILLISECONDS.toSeconds(duration.toLong()) % 60
                    val currentMinutes = TimeUnit.MILLISECONDS.toMinutes(currentPosition.toLong())
                    val currentSeconds = TimeUnit.MILLISECONDS.toSeconds(currentPosition.toLong()) % 60

                    Text(
                        text = String.format("%02d:%02d", currentMinutes, currentSeconds),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%02d:%02d", durationMinutes, durationSeconds),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacing_medium)))

                IconButton(
                    onClick = { service?.togglePlayPause() },
                    modifier = Modifier
                        .size(dimensionResource(id = R.dimen.playback_play_button_size))
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_medium)),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacing_medium)))
                Button(onClick = {
                    checkAndSetRingtone(context, sound, settingsLauncher)
                }) {
                    Icon(Icons.Default.Phone, contentDescription = "Set as Ringtone")
                    Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.spacing_small)))
                    Text(text = "Set as Ringtone")
                }

            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            context.unbindService(connection)
        }
    }
}

private fun checkAndSetRingtone(
    context: Context,
    sound: Sound,
    settingsLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    if (!Settings.System.canWrite(context)) {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.data = Uri.parse("package:" + context.packageName)
        settingsLauncher.launch(intent)
    } else {
        setRingtone(context, sound)
    }
}

private fun setRingtone(context: Context, sound: Sound) {
    val soundUri = if (sound.resourceId != null) {
        ("android.resource://" + context.packageName + "/" + sound.resourceId).toUri()
    } else {
        Uri.parse(sound.filePath)
    }

    try {
        RingtoneManager.setActualDefaultRingtoneUri(
            context,
            RingtoneManager.TYPE_RINGTONE,
            soundUri
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}


@Preview(showBackground = true)
@Composable
fun SoundPlaybackScreenPreview() {
    SoundBoxTheme {
        // SoundPlaybackScreen(sound = Sound("Preview", Icons.Default.MusicNote, R.raw.drum_roll), onBack = {})
    }
}
