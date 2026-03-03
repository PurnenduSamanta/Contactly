package com.purnendu.contactly.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Utility to extract latitude/longitude from Google Maps shared text.
 *
 * Smart parsing strategy (in order):
 * 1. Direct text parsing — instant, no network needed
 * 2. Regex coordinate extraction — catches embedded coordinates
 * 3. Shortened URL resolution — follows redirects with strict timeout
 *
 * Handles formats:
 * - geo:28.6139,77.2090
 * - .../@28.6139,77.2090,17z
 * - ?q=28.6139,77.2090
 * - /place/Name/@lat,lng
 * - https://maps.app.goo.gl/xxxxx (resolved via redirect)
 * - !3d28.6139!4d77.2090 (Google's internal data params)
 */
object GoogleMapsUrlParser {

    private const val TAG = "MapsUrlParser"

    // Regex to find coordinate pairs like "28.6139,77.2090" anywhere in text
    // Matches: optional minus, 1-3 digits, dot, 1-15 digits, comma, same pattern
    private val COORD_PAIR_REGEX = Regex(
        """(-?\d{1,3}\.\d{1,15})\s*,\s*(-?\d{1,3}\.\d{1,15})"""
    )

    // Regex for Google's internal data format: !3d<lat>!4d<lng>
    private val DATA_PARAM_REGEX = Regex(
        """!3d(-?\d{1,3}\.\d{1,15})!4d(-?\d{1,3}\.\d{1,15})"""
    )

    data class LatLng(val latitude: Double, val longitude: Double)

    /**
     * Parse lat/lng from shared Google Maps text.
     * Returns null if parsing fails.
     */
    suspend fun parseFromSharedText(sharedText: String): LatLng? {
        val text = sharedText.trim()
        Log.d(TAG, "Parsing shared text: $text")

        // ─── PHASE 1: Direct text parsing (instant, no network) ───

        // 1a. Try geo: URI format (e.g., "geo:28.6139,77.2090")
        tryParseGeoUri(text)?.let { return it }

        // 1b. Try @ pattern (e.g., ".../@28.6139,77.2090,17z")
        tryParseAtPattern(text)?.let { return it }

        // 1c. Try query param ?q= (e.g., "...?q=28.6139,77.2090")
        tryParseQueryParam(text)?.let { return it }

        // 1d. Try Google's internal !3d...!4d... format
        tryParseDataParams(text)?.let { return it }

        // ─── PHASE 2: Regex extraction (catches coords anywhere in text) ───

        // Try to find any coordinate pair after '@' sign specifically
        tryParseRegexAfterAt(text)?.let { return it }

        // ─── PHASE 3: Network resolution (only for shortened URLs) ───

        val url = extractUrl(text)
        if (url != null && isGoogleShortenedUrl(url)) {
            Log.d(TAG, "Found shortened URL, resolving: $url")

            // Strict 8-second total timeout for the entire resolution
            val resolvedUrl = withTimeoutOrNull(8000L) {
                resolveShortUrl(url)
            }

            if (resolvedUrl != null) {
                Log.d(TAG, "Resolved to: $resolvedUrl")

                // Try all direct parsing methods on the resolved URL
                tryParseAtPattern(resolvedUrl)?.let { return it }
                tryParseQueryParam(resolvedUrl)?.let { return it }
                tryParseDataParams(resolvedUrl)?.let { return it }
                tryParseRegexAfterAt(resolvedUrl)?.let { return it }

                // Last resort: find any coordinate pair in the resolved URL
                tryParseAnyCoordPair(resolvedUrl)?.let { return it }
            } else {
                Log.w(TAG, "Failed to resolve shortened URL (timeout or error)")
            }
        }

        Log.w(TAG, "Could not parse location from text")
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

    // ──────────────────────────────────────────────────
    // Private parsing methods — each tries one strategy
    // ──────────────────────────────────────────────────

    private fun tryParseGeoUri(text: String): LatLng? {
        if (!text.contains("geo:")) return null
        val coordsPart = text.substringAfter("geo:").substringBefore("?").split(",")
        return extractFromParts(coordsPart, "geo: URI")
    }

    private fun tryParseAtPattern(text: String): LatLng? {
        if (!text.contains("@")) return null
        val afterAt = text.substringAfter("@")
        val coordsPart = afterAt.split(",")
        return extractFromParts(coordsPart, "@ pattern")
    }

    private fun tryParseQueryParam(text: String): LatLng? {
        if (!text.contains("q=")) return null
        val coordsPart = text.substringAfter("q=").substringBefore("&").substringBefore(" ").split(",")
        return extractFromParts(coordsPart, "q= param")
    }

    private fun tryParseDataParams(text: String): LatLng? {
        val match = DATA_PARAM_REGEX.find(text) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lng = match.groupValues[2].toDoubleOrNull() ?: return null
        if (!isValidLatLng(lat, lng)) return null
        Log.d(TAG, "Parsed from !3d!4d data params → lat=$lat, lng=$lng")
        return LatLng(lat, lng)
    }

    private fun tryParseRegexAfterAt(text: String): LatLng? {
        if (!text.contains("@")) return null
        val afterAt = text.substringAfter("@")
        val match = COORD_PAIR_REGEX.find(afterAt) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lng = match.groupValues[2].toDoubleOrNull() ?: return null
        if (!isValidLatLng(lat, lng)) return null
        Log.d(TAG, "Parsed from regex after @ → lat=$lat, lng=$lng")
        return LatLng(lat, lng)
    }

    private fun tryParseAnyCoordPair(text: String): LatLng? {
        // Find ALL coordinate pairs and pick the one that looks like a map coordinate
        for (match in COORD_PAIR_REGEX.findAll(text)) {
            val lat = match.groupValues[1].toDoubleOrNull() ?: continue
            val lng = match.groupValues[2].toDoubleOrNull() ?: continue
            if (isValidLatLng(lat, lng)) {
                Log.d(TAG, "Parsed from regex fallback → lat=$lat, lng=$lng")
                return LatLng(lat, lng)
            }
        }
        return null
    }

    // ──────────────────────────────────────
    // Utility methods
    // ──────────────────────────────────────

    private fun extractFromParts(parts: List<String>, source: String): LatLng? {
        if (parts.size < 2) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lng = parts[1].toDoubleOrNull() ?: return null
        if (!isValidLatLng(lat, lng)) return null
        Log.d(TAG, "Parsed from $source → lat=$lat, lng=$lng")
        return LatLng(lat, lng)
    }

    private fun extractUrl(text: String): String? {
        return text.split(" ", "\n", "\t")
            .find { it.startsWith("http://") || it.startsWith("https://") }
    }

    private fun isGoogleShortenedUrl(url: String): Boolean {
        return url.contains("maps.app.goo.gl/") ||
                url.contains("goo.gl/maps") ||
                url.contains("g.co/") ||
                url.contains("maps.google.com/") && url.length < 80 // Short enough to be a shortener
    }

    private fun isValidLatLng(lat: Double, lng: Double): Boolean {
        return lat in -90.0..90.0 && lng in -180.0..180.0
    }

    /**
     * Follow redirects from a shortened URL to get the final URL.
     *
     * Key improvements over naive approach:
     * - Uses HEAD request (no body download — much faster)
     * - Sets browser User-Agent (Google rejects bare requests)
     * - Handles relative redirect URLs
     * - Short per-hop timeout (2s) with max 7 hops
     * - Proper connection cleanup in finally blocks
     */
    private suspend fun resolveShortUrl(shortUrl: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                var currentUrl = shortUrl
                var redirectCount = 0
                val maxRedirects = 7

                while (redirectCount < maxRedirects) {
                    val url = java.net.URL(currentUrl)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    try {
                        connection.instanceFollowRedirects = false
                        connection.connectTimeout = 2000
                        connection.readTimeout = 2000
                        connection.requestMethod = "HEAD"
                        connection.setRequestProperty(
                            "User-Agent",
                            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        )
                        connection.connect()

                        val responseCode = connection.responseCode
                        val location = connection.getHeaderField("Location")

                        Log.d(TAG, "Hop #$redirectCount: $responseCode → ${location?.take(100)}")

                        if (responseCode in 300..399 && location != null) {
                            // Handle relative redirect URLs
                            currentUrl = if (location.startsWith("http")) {
                                location
                            } else {
                                java.net.URL(url, location).toString()
                            }
                            redirectCount++
                        } else {
                            // No more redirects — return what we have
                            return@withContext if (redirectCount > 0) currentUrl else null
                        }
                    } finally {
                        connection.disconnect()
                    }
                }

                // Reached max redirects, return whatever we have
                Log.w(TAG, "Reached max redirects ($maxRedirects), using: ${currentUrl.take(100)}")
                currentUrl
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resolve: $shortUrl — ${e.message}")
                null
            }
        }
    }
}
