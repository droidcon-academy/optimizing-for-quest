package com.droidcon.soundbox.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import org.json.JSONObject

class LocationRepository(
    private val defaultCity: String = "Unknown City",
    private val userAgent: String = "SoundBox/1.0 (contact@soundbox.app)"
) {

    private val httpClient = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = CONNECTION_TIMEOUT_MS.toLong()
            connectTimeoutMillis = CONNECTION_TIMEOUT_MS.toLong()
            socketTimeoutMillis = READ_TIMEOUT_MS.toLong()
        }
    }

    suspend fun fetchCityName(latitude: Double, longitude: Double): String {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Calling OpenStreetMap reverse geocoding for lat=$latitude lon=$longitude")
            runCatching {
                val responseText = httpClient.get("https://nominatim.openstreetmap.org/reverse") {
                    parameter("format", "json")
                    parameter("lat", latitude)
                    parameter("lon", longitude)
                    parameter("zoom", 10)
                    parameter("addressdetails", 1)
                    parameter("accept-language", "en")
                    header("User-Agent", userAgent)
                }.body<String>()
                val city = parseCityFromResponse(responseText)
                Log.d(TAG, "Received response from OpenStreetMap: city=$city")
                city
            }.getOrElse {
                Log.e(TAG, "OpenStreetMap reverse geocoding failed", it)
                defaultCity
            }
        }
    }

    private fun parseCityFromResponse(response: String): String {
        val json = JSONObject(response)
        val address = json.optJSONObject("address") ?: return defaultCity
        return listOf("city", "state", "country", "municipality", "county")
            .firstNotNullOfOrNull { key ->
                address.optString(key).takeIf { it.isNotBlank() }
            } ?: defaultCity
    }

    companion object {
        private const val CONNECTION_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 10_000
        private const val TAG = "LocationRepository"
    }
}
