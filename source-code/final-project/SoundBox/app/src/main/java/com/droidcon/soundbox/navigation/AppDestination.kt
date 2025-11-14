package com.droidcon.soundbox.navigation

import kotlinx.serialization.Serializable

sealed interface AppDestination {
    @Serializable
    data object Home : AppDestination

    @Serializable
    data object Recording : AppDestination

    @Serializable
    data class SoundPlayback(val soundName: String) : AppDestination
}
