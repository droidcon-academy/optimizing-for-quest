package com.droidcon.soundbox

import android.app.Application
import com.droidcon.soundbox.di.locationModule
import com.droidcon.soundbox.di.repositoryModule
import com.droidcon.soundbox.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SoundBoxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SoundBoxApp)
            modules(
                repositoryModule,
                locationModule,
                viewModelModule
            )
        }
    }
}
