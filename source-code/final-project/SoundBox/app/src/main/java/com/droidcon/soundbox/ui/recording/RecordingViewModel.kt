package com.droidcon.soundbox.ui.recording

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.droidcon.soundbox.data.LocationRepository
import com.droidcon.soundbox.data.Sound
import com.droidcon.soundbox.data.SoundRepository
import com.droidcon.soundbox.location.LocationPriority
import com.droidcon.soundbox.location.LocationRequestParams
import com.droidcon.soundbox.location.SoundboxLocationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

enum class RecordingScreenState { READY_TO_RECORD, RECORDING, RECORDED, SAVED }

data class RecordingUiState(
    val recordingState: RecordingScreenState = RecordingScreenState.READY_TO_RECORD,
    val soundName: String = "",
    val hasAudioPermission: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val isPlayingPreview: Boolean = false,
    val isSaving: Boolean = false
)

sealed interface RecordingUiEvent {
    data object CheckAudioPermission : RecordingUiEvent
    data object CheckLocationPermission : RecordingUiEvent
    data class SoundNameChanged(val name: String) : RecordingUiEvent
    data class AudioPermissionResult(val isGranted: Boolean) : RecordingUiEvent
    data class LocationPermissionResult(val isGranted: Boolean) : RecordingUiEvent
    data object StartRecording : RecordingUiEvent
    data object StopRecording : RecordingUiEvent
    data object TogglePreview : RecordingUiEvent
    data object RecordAgain : RecordingUiEvent
    data object SaveSound : RecordingUiEvent
}

class RecordingViewModel(
    application: Application,
    private val soundRepository: SoundRepository,
    private val locationRepository: LocationRepository,
    private val locationManager: SoundboxLocationManager
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var outputFilePath: String? = null

    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: RecordingUiEvent) {
        when (event) {
            RecordingUiEvent.CheckAudioPermission -> checkAudioPermission()
            RecordingUiEvent.CheckLocationPermission -> checkLocationPermission()
            RecordingUiEvent.RecordAgain -> recordAgain()
            RecordingUiEvent.SaveSound -> saveSound()
            RecordingUiEvent.StartRecording -> startRecording()
            RecordingUiEvent.StopRecording -> stopRecording()
            RecordingUiEvent.TogglePreview -> togglePreview()
            is RecordingUiEvent.AudioPermissionResult -> updateAudioPermission(event.isGranted)
            is RecordingUiEvent.LocationPermissionResult -> updateLocationPermission(event.isGranted)
            is RecordingUiEvent.SoundNameChanged -> updateSoundName(event.name)
        }
    }

    private fun updateSoundName(name: String) {
        _uiState.update { it.copy(soundName = name) }
    }

    private fun checkAudioPermission() {
        val granted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        updateAudioPermission(granted)
    }

    private fun updateAudioPermission(isGranted: Boolean) {
        _uiState.update { it.copy(hasAudioPermission = isGranted) }
    }

    private fun checkLocationPermission() {
        updateLocationPermission(hasLocationPermissionGranted())
    }

    private fun updateLocationPermission(isGranted: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = isGranted) }
    }

    private fun startRecording() {
        val currentState = _uiState.value
        if (!currentState.hasAudioPermission || currentState.soundName.isBlank()) return

        val file = File(appContext.cacheDir, "${currentState.soundName}.3gp")
        outputFilePath = file.absolutePath

        mediaRecorder = createMediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        _uiState.update { it.copy(recordingState = RecordingScreenState.RECORDING) }
    }

    private fun stopRecording() {
        mediaRecorder?.runCatching {
            stop()
            reset()
        }
        _uiState.update { it.copy(recordingState = RecordingScreenState.RECORDED) }
    }

    private fun togglePreview() {
        if (mediaPlayer?.isPlaying == true) {
            stopPreview()
        } else {
            playPreview()
        }
    }

    private fun playPreview() {
        mediaPlayer = MediaPlayer().apply {
            setDataSource(outputFilePath)
            prepare()
            start()
            setOnCompletionListener {
                _uiState.update { state -> state.copy(isPlayingPreview = false) }
            }
        }
        _uiState.update { it.copy(isPlayingPreview = true) }
    }

    private fun stopPreview() {
        mediaPlayer?.stop()
        mediaPlayer?.reset()
        _uiState.update { it.copy(isPlayingPreview = false) }
    }

    private fun saveSound() {
        val filePath = outputFilePath ?: return
        if (_uiState.value.soundName.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val city = resolveCityName()
                soundRepository.addSound(
                    Sound(
                        name = _uiState.value.soundName,
                        icon = Icons.Default.Mic,
                        filePath = filePath,
                        city = city
                    )
                )
                _uiState.update { state ->
                    state.copy(
                        recordingState = RecordingScreenState.SAVED,
                        isSaving = false
                    )
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to save sound", t)
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private suspend fun resolveCityName(): String {
        if (!hasLocationPermissionGranted()) return DEFAULT_CITY
        Log.d(TAG, "Attempting to obtain location for city resolution")
        val params = LocationRequestParams(
            priority = LocationPriority.BALANCED,
            minUpdateDistanceMeters = 10f,
            minUpdateTimeMillis = 1_000L,
            timeoutMillis = 15_000L,
            maxCacheAgeMillis = 30_000L,
            acceptableAccuracyMeters = 200f,
            maxRetries = 2
        )
        val immediateLocation = locationManager.getCurrentLocation(params).getOrElse {
            Log.w(TAG, "Immediate fused location unavailable", it)
            null
        }
        val location = immediateLocation ?: withTimeoutOrNull(params.timeoutMillis) {
            locationManager.observeLocationUpdates(params)
                .mapNotNull { result ->
                    result.getOrElse {
                        Log.w(TAG, "Location update failed", it)
                        null
                    }
                }
                .firstOrNull()
        }
        if (location == null) {
            Log.w(TAG, "Timed out waiting for a location update")
            return DEFAULT_CITY
        }
        Log.d(TAG, "Location fix received: lat=${location.latitude}, lon=${location.longitude}, accuracy=${location.accuracy}")
        Log.d(TAG, "Requesting city name from repository")
        return try {
            locationRepository.fetchCityName(location.latitude, location.longitude)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to fetch city name", t)
            DEFAULT_CITY
        }
    }


    private fun hasLocationPermissionGranted(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    private fun recordAgain() {
        outputFilePath = null
        stopPreview()
        _uiState.update {
            it.copy(
                recordingState = RecordingScreenState.READY_TO_RECORD,
                isPlayingPreview = false,
                isSaving = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaRecorder?.release()
        mediaPlayer?.release()
    }

    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(appContext)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    companion object {
        private const val DEFAULT_CITY = "Unknown City"
        private const val TAG = "RecordingViewModel"
    }
}
