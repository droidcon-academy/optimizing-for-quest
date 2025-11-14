package com.droidcon.soundbox.ui.recording

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.droidcon.soundbox.R
import com.droidcon.soundbox.ui.GradientTopAppBar
import com.droidcon.soundbox.ui.theme.SoundBoxTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    state: RecordingUiState,
    onEvent: (RecordingUiEvent) -> Unit,
    onBack: () -> Unit
) {
    val recordingState = state.recordingState
    val soundName = state.soundName
    val hasAudioPermission = state.hasAudioPermission
    val hasLocationPermission = state.hasLocationPermission
    val isPlayingPreview = state.isPlayingPreview
    val isSaving = state.isSaving

    var pendingSave by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            onEvent(RecordingUiEvent.AudioPermissionResult(isGranted))
        }
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            onEvent(RecordingUiEvent.LocationPermissionResult(isGranted))
            if (pendingSave && isGranted) {
                pendingSave = false
                onEvent(RecordingUiEvent.SaveSound)
            } else if (!isGranted) {
                pendingSave = false
            }
        }
    )

    LaunchedEffect(Unit) {
        onEvent(RecordingUiEvent.CheckAudioPermission)
        onEvent(RecordingUiEvent.CheckLocationPermission)
    }

    LaunchedEffect(recordingState) {
        if (recordingState == RecordingScreenState.SAVED) {
            onBack()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            GradientTopAppBar(
                title = { Text("Record New Sound") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(dimensionResource(id = R.dimen.padding_medium)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            val backgroundColor =
                if (isHovered) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else Color.Transparent

            OutlinedTextField(
                value = soundName,
                onValueChange = { onEvent(RecordingUiEvent.SoundNameChanged(it)) },
                label = { Text("Sound Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .hoverable(interactionSource = interactionSource)
                    .background(backgroundColor),
                enabled = recordingState == RecordingScreenState.READY_TO_RECORD,
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacing_xlarge)))

            when (recordingState) {
                RecordingScreenState.READY_TO_RECORD, RecordingScreenState.RECORDING -> {
                    val isRecording = recordingState == RecordingScreenState.RECORDING
                    IconButton(
                        onClick = {
                            if (hasAudioPermission) {
                                if (isRecording) {
                                    onEvent(RecordingUiEvent.StopRecording)
                                } else {
                                    onEvent(RecordingUiEvent.StartRecording)
                                }
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .size(dimensionResource(id = R.dimen.playback_play_button_size))
                            .clip(CircleShape)
                            .background(if (isRecording) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary),
                        enabled = soundName.isNotBlank() || isRecording
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Stop" else "Record",
                            modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_medium)),
                            tint = if (isRecording) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacing_medium)))
                    Text(
                        text = when {
                            isRecording -> "Recording..."
                            !hasAudioPermission -> "Tap the mic to grant permission"
                            soundName.isBlank() -> "Enter a sound name to begin"
                            else -> "Ready to record"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                RecordingScreenState.RECORDED -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_large)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onEvent(RecordingUiEvent.TogglePreview) }) {
                            Icon(
                                if (isPlayingPreview) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause"
                            )
                        }
                        IconButton(onClick = { onEvent(RecordingUiEvent.RecordAgain) }) {
                            Icon(Icons.Default.Replay, contentDescription = "Record Again")
                        }
                    }
                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacing_large)))
                    Button(
                        onClick = {
                            if (hasLocationPermission) {
                                onEvent(RecordingUiEvent.SaveSound)
                            } else {
                                pendingSave = true
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        },
                        enabled = !isSaving
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.padding(end = dimensionResource(id = R.dimen.padding_small))
                        )
                        Text(if (isSaving) "Saving..." else "Save Sound")
                    }
                }

                RecordingScreenState.SAVED -> {
                    // handled by navigation
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RecordingScreenPreview() {
    SoundBoxTheme {
        RecordingScreen(
            state = RecordingUiState(),
            onEvent = {},
            onBack = {}
        )
    }
}
