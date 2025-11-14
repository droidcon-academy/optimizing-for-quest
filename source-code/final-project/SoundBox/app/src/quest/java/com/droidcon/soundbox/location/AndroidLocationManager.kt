package com.droidcon.soundbox.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class AndroidLocationManager(
    context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : SoundboxLocationManager {

    private val appContext = context.applicationContext
    private val locationManager: LocationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override suspend fun getLastLocation(): Result<Location> = withContext(dispatcher) {
        ensurePermissions()
        val provider = locationManager.getBestProvider(Criteria(), true)
        val location = provider?.let { locationManager.getLastKnownLocation(it) }

        return@withContext if (location != null) {
            Log.d(TAG, "Platform cached fix: lat=${location.latitude}, lon=${location.longitude}")
            Result.success(location)
        } else {
            Log.w(TAG, "No platform cached fix available")
            Result.failure(LocationException.LocationUnavailable("No cached location"))
        }
    }

    override suspend fun getCurrentLocation(
        params: LocationRequestParams
    ): Result<Location> = withContext(dispatcher) {
        ensurePermissions()
        val provider = locationManager.getBestProvider(Criteria(), true)
        if (provider == null) {
            return@withContext Result.failure(LocationException.LocationUnavailable("No available provider"))
        }

        return@withContext suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    continuation.resume(Result.success(location))
                }

                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }

            locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())

            continuation.invokeOnCancellation {
                locationManager.removeUpdates(listener)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun observeLocationUpdates(
        params: LocationRequestParams
    ): Flow<Result<Location>> = callbackFlow {
        if (!hasLocationPermission()) {
            trySend(Result.failure(LocationException.PermissionDenied))
            close(LocationException.PermissionDenied)
            return@callbackFlow
        }

        val provider = locationManager.getBestProvider(Criteria(), true)
        if (provider == null) {
            trySend(Result.failure(LocationException.LocationUnavailable("No available provider")))
            close()
            return@callbackFlow
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(Result.success(location))
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        locationManager.requestLocationUpdates(
            provider,
            params.minUpdateTimeMillis,
            params.minUpdateDistanceMeters,
            listener,
            Looper.getMainLooper()
        )

        awaitClose {
            locationManager.removeUpdates(listener)
        }
    }

    private fun ensurePermissions() {
        if (!hasLocationPermission()) throw LocationException.PermissionDenied
    }

    private fun hasLocationPermission(): Boolean {
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

    companion object {
        private const val TAG = "AndroidLocationManager"
    }
}