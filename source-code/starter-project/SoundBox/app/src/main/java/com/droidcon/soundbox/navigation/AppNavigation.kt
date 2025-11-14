package com.droidcon.soundbox.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.droidcon.soundbox.data.SoundRepository
import com.droidcon.soundbox.ui.home.HomeScreen
import com.droidcon.soundbox.ui.home.HomeViewModel
import com.droidcon.soundbox.ui.playback.SoundPlaybackScreen
import com.droidcon.soundbox.ui.recording.RecordingScreen
import com.droidcon.soundbox.ui.recording.RecordingViewModel
import org.koin.androidx.compose.get
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val soundRepository: SoundRepository = get()

    NavHost(navController = navController, startDestination = AppDestination.Home) {
        composable<AppDestination.Home> {
            val homeViewModel: HomeViewModel = koinViewModel()
            val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
            HomeScreen(
                state = homeState,
                onSoundClick = {
                    navController.navigate(AppDestination.SoundPlayback(it.name))
                },
                onRecordClick = {
                    navController.navigate(AppDestination.Recording)
                }
            )
        }
        composable<AppDestination.SoundPlayback> { backStackEntry ->
            val soundName = backStackEntry.toRoute<AppDestination.SoundPlayback>().soundName
            val sounds by soundRepository.sounds.collectAsStateWithLifecycle()
            val sound = sounds.find { it.name == soundName }
            if (sound != null) {
                SoundPlaybackScreen(
                    sound = sound,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable<AppDestination.Recording> {
            val recordingViewModel: RecordingViewModel = koinViewModel()
            val recordingState by recordingViewModel.uiState.collectAsStateWithLifecycle()
            RecordingScreen(
                state = recordingState,
                onEvent = recordingViewModel::onEvent,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
