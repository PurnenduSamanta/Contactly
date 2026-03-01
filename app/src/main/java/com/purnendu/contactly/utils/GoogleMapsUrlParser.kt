package com.purnendu.contactly.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility to extract latitude/longitude from Google Maps shared text.
 *
 * Handles multiple URL formats using readable string manipulation:
 * - Full URL: https://www.google.com/maps/place/.../@28.6139,77.2090,17z
 * - Shortened URL: https://maps.app.goo.gl/xxxxx (follows HTTP redirect)
 * - Geo URI: geo:28.6139,77.2090
 * - Query param: ?q=28.6139,77.2090
 */
object GoogleMapsUrlParser {

    data class LatLng(val latitude: Double, val longitude: Double)

    /**
     * Parse lat/lng from shared Google Maps text.
     * Returns null if parsing fails.
     */
    suspend fun parseFromSharedText(sharedText: String): LatLng? {
        val text = sharedText.trim()

        // 1. Try geo: URI format (e.g., "geo:28.6139,77.2090")
        if (text.contains("geo:")) {
            val coordsPart = text.substringAfter("geo:").substringBefore("?").split(",")
            if (coordsPart.size >= 2) {
                val lat = coordsPart[0].toDoubleOrNull()
                val lng = coordsPart[1].toDoubleOrNull()
                if (lat != null && lng != null && isValidLatLng(lat, lng)) return LatLng(lat, lng)
            }
        }

        // 2. Try query parameter ?q= or &q= (e.g., "...?q=28.6139,77.2090")
        if (text.contains("q=")) {
            val coordsPart = text.substringAfter("q=").substringBefore("&").substringBefore(" ").split(",")
            if (coordsPart.size >= 2) {
                val lat = coordsPart[0].toDoubleOrNull()
                val lng = coordsPart[1].toDoubleOrNull()
                if (lat != null && lng != null && isValidLatLng(lat, lng)) return LatLng(lat, lng)
            }
        }

        // 3. Try direct coordinate pattern after '@' (e.g., ".../@28.6139,77.2090,17z")
        if (text.contains("@")) {
            val afterAt = text.substringAfter("@")
            val coordsPart = afterAt.split(",")
            if (coordsPart.size >= 2) {
                val lat = coordsPart[0].toDoubleOrNull()
                val lng = coordsPart[1].toDoubleOrNull()
                if (lat != null && lng != null && isValidLatLng(lat, lng)) return LatLng(lat, lng)
            }
        }

        // 4. Handle shortened URL (maps.app.goo.gl)
        if (text.contains("maps.app.goo.gl/")) {
            // Extract the URL from the text (it might be surrounded by words)
            val url = text.split(" ", "\n").find { it.contains("maps.app.goo.gl/") }
            if (url != null) {
                val resolvedUrl = resolveShortUrl(url)
                if (resolvedUrl != null) {
                    return parseFromSharedText(resolvedUrl) // Recursive call with resolved URL
                }
            }
        }

        return null
    }

    /**
     * Extract a human-readable location label from shared text.
     * Typically the first line (before the URL) in Google Maps shares.
     */
    fun extractLabel(sharedText: String): String? {
        val lines = sharedText.trim().lines()
        if (lines.size >= 2) {
            val firstLine = lines[0].trim()
            // If the first line is NOT a URL, it's likely a place name
            if (!firstLine.startsWith("http") && firstLine.isNotBlank() && firstLine != "Dropped pin") {
                return firstLine
            }
        }
        return null
    }

    private fun isValidLatLng(lat: Double, lng: Double): Boolean {
        return lat in -90.0..90.0 && lng in -180.0..180.0
    }

    private suspend fun resolveShortUrl(shortUrl: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val connection = java.net.URL(shortUrl).openConnection() as java.net.HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()
                val resolved = connection.getHeaderField("Location")
                connection.disconnect()
                resolved
            } catch (e: Exception) {
                null
            }
        }
    }
}
