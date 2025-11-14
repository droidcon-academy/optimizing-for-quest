package com.droidcon.soundbox.location

data class LocationRequestParams(
    val priority: LocationPriority = LocationPriority.BALANCED,
    val minUpdateDistanceMeters: Float = 25f,
    val minUpdateTimeMillis: Long = 5_000L,
    val timeoutMillis: Long = 15_000L,
    val maxCacheAgeMillis: Long = 60_000L,
    val acceptableAccuracyMeters: Float = 150f,
    val maxRetries: Int = 2
)
