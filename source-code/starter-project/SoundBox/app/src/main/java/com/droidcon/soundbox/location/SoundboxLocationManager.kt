package com.droidcon.soundbox.location

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface SoundboxLocationManager {
    suspend fun getLastLocation(): Result<Location>
    suspend fun getCurrentLocation(params: LocationRequestParams = LocationRequestParams()): Result<Location>
    fun observeLocationUpdates(params: LocationRequestParams = LocationRequestParams()): Flow<Result<Location>>
}
