package com.droidcon.soundbox.di

import com.droidcon.soundbox.data.LocationRepository
import com.droidcon.soundbox.data.SoundRepository
import com.droidcon.soundbox.location.AndroidLocationManager
import com.droidcon.soundbox.location.SoundboxLocationManager
import com.droidcon.soundbox.ui.home.HomeViewModel
import com.droidcon.soundbox.ui.recording.RecordingViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koin.android.ext.koin.androidApplication

val repositoryModule = module {
    single { SoundRepository() }
    single { LocationRepository() }
}

val locationModule = module {
    single<SoundboxLocationManager> { AndroidLocationManager(androidContext()) }
}

val viewModelModule = module {
    viewModel { HomeViewModel(get()) }
    viewModel {
        RecordingViewModel(
            application = androidApplication(),
            soundRepository = get(),
            locationRepository = get(),
            locationManager = get()
        )
    }
}
