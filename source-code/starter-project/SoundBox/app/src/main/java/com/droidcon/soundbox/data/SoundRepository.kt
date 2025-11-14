package com.droidcon.soundbox.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WavingHand
import com.droidcon.soundbox.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SoundRepository {

    private val _sounds = MutableStateFlow<List<Sound>>(emptyList())
    val sounds = _sounds.asStateFlow()

    init {
        _sounds.value = listOf(
            Sound("Drum Roll", Icons.Filled.MusicNote, R.raw.drum_roll, city = "Seattle"),
            Sound("Applause", Icons.Filled.WavingHand, R.raw.applause, city = "Austin"),
            Sound("Beep", Icons.Filled.VolumeUp, R.raw.beep, city = "Dublin"),
            Sound("Train Whistle", Icons.Filled.Train, R.raw.train_whistle, city = "Denver"),
            Sound("Air Horn", Icons.Filled.Campaign, R.raw.air_horn, city = "Toronto")
        )
    }

    fun addSound(sound: Sound) {
        val currentSounds = _sounds.value.toMutableList()
        currentSounds.add(sound)
        _sounds.value = currentSounds
    }
}
