package com.droidcon.soundbox.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class AndroidLocationManager(
    context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : SoundboxLocationManager {

    private val appContext = context.applicationContext
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(appContext)

    override suspend fun getLastLocation(): Result<Location> = withContext(dispatcher) {
        ensurePermissions()
        val location = fetchLastLocationWithRetry()
        if (location != null) {
            Log.d(TAG, "Fused cached fix: lat=${location.latitude}, lon=${location.longitude}")
            Result.success(location)
        } else {
            Log.w(TAG, "No fused cached fix available")
            Result.failure(LocationException.LocationUnavailable("No cached location"))
        }
    }

    override suspend fun getCurrentLocation(
        params: LocationRequestParams
    ): Result<Location> = withContext(dispatcher) {
        ensurePermissions()
        val cached = fetchLastLocationWithRetry()
        if (cached != null) {
            Log.d(TAG, "Returning fused last known fix without delay")
            return@withContext Result.success(cached)
        }

        val current = fetchCurrentLocationWithRetry(params)
            ?: awaitStreamingLocation(params)

        if (current != null) {
            Result.success(current)
        } else {
            Log.w(TAG, "Unable to obtain fused current location")
            Result.failure(LocationException.LocationUnavailable("Unable to obtain current location"))
        }
    }

    override fun observeLocationUpdates(
        params: LocationRequestParams
    ): Flow<Result<Location>> = callbackFlow {
        if (!hasLocationPermission()) {
            trySend(Result.failure(LocationException.PermissionDenied))
            close(LocationException.PermissionDenied)
            return@callbackFlow
        }

        val request = params.toLocationRequest()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation
                if (location != null) {
                    trySend(Result.success(location))
                }
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }

    private suspend fun fetchLastLocationWithRetry(
        maxRetries: Int = DEFAULT_RETRY_COUNT
    ): Location? {
        var attempt = 0
        while (attempt <= maxRetries) {
            val location = runCatching { fusedClient.lastLocation.await() }.getOrNull()
            if (location != null) return location
            attempt++
            if (attempt <= maxRetries) delay(RETRY_BACKOFF_MS)
        }
        return null
    }

    private suspend fun fetchCurrentLocationWithRetry(
        params: LocationRequestParams,
        maxRetries: Int = DEFAULT_RETRY_COUNT
    ): Location? {
        var attempt = 0
        while (attempt <= maxRetries) {
            val location = requestSingleCurrentLocation(params)
            if (location != null) return location
            attempt++
            if (attempt <= maxRetries) delay(RETRY_BACKOFF_MS)
        }
        return null
    }

    private suspend fun requestSingleCurrentLocation(
        params: LocationRequestParams
    ): Location? {
        return withTimeoutOrNull(params.timeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                val tokenSource = CancellationTokenSource()
                val request = params.toCurrentLocationRequest()

                fusedClient.getCurrentLocation(request, tokenSource.token)
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) {
                            if (location != null && location.accuracy <= params.acceptableAccuracyMeters) {
                                continuation.resume(location)
                            } else {
                                continuation.resume(null)
                            }
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }

                continuation.invokeOnCancellation {
                    tokenSource.cancel()
                }
            }
        }
    }

    private suspend fun awaitStreamingLocation(
        params: LocationRequestParams
    ): Location? {
        return withTimeoutOrNull(params.timeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                val request = params.toLocationRequest()
                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        val location = result.lastLocation
                        if (
                            location != null &&
                            location.accuracy <= params.acceptableAccuracyMeters &&
                            continuation.isActive
                        ) {
                            fusedClient.removeLocationUpdates(this)
                            continuation.resume(location)
                        }
                    }
                }

                fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

                continuation.invokeOnCancellation {
                    fusedClient.removeLocationUpdates(callback)
                }
            }
        }
    }

    private fun LocationRequestParams.toLocationRequest(): LocationRequest {
        return LocationRequest.Builder(priority.toFusedPriority(), minUpdateTimeMillis)
            .setMinUpdateIntervalMillis(minUpdateTimeMillis)
            .setMinUpdateDistanceMeters(minUpdateDistanceMeters)
            .build()
    }

    private fun LocationRequestParams.toCurrentLocationRequest(): CurrentLocationRequest {
        return CurrentLocationRequest.Builder()
            .setPriority(priority.toFusedPriority())
            .setMaxUpdateAgeMillis(maxCacheAgeMillis)
            .build()
    }

    private fun LocationPriority.toFusedPriority(): Int {
        return when (this) {
            LocationPriority.HIGH_ACCURACY -> Priority.PRIORITY_HIGH_ACCURACY
            LocationPriority.BALANCED -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
            LocationPriority.LOW_POWER -> Priority.PRIORITY_LOW_POWER
            LocationPriority.PASSIVE -> Priority.PRIORITY_PASSIVE
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
        private const val DEFAULT_RETRY_COUNT = 2
        private const val RETRY_BACKOFF_MS = 1_000L
    }
}
