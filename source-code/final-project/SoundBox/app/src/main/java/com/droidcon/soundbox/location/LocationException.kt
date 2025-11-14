package com.droidcon.soundbox.location

sealed class LocationException(message: String) : Exception(message) {
    object PermissionDenied : LocationException("Location permission not granted")
    object ProvidersDisabled : LocationException("No location providers are enabled")
    class Timeout : LocationException("Timed out while waiting for a location fix")
    class LocationUnavailable(reason: String) : LocationException(reason)
}
