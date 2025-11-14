package com.droidcon.soundbox.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidcon.soundbox.data.Sound
import com.droidcon.soundbox.data.SoundRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val sounds: List<Sound> = emptyList()
)

class HomeViewModel(soundRepository: SoundRepository) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = soundRepository.sounds
        .map { HomeUiState(sounds = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )
}
