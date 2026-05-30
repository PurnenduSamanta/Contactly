package com.purnendu.contactly.geofence

import android.content.Context
import com.purnendu.contactly.domain.model.LocationCoordinates
import com.purnendu.contactly.domain.repository.LocationParserRepository

class GoogleMapsLocationParserRepository(
    private val context: Context
) : LocationParserRepository {
    override suspend fun parseFromSharedText(sharedText: String): LocationCoordinates? {
        return GoogleMapsUrlParser.parseFromSharedText(sharedText, context)?.let {
            LocationCoordinates(
                latitude = it.latitude,
                longitude = it.longitude
            )
        }
    }

    override fun extractLabel(sharedText: String): String? {
        return GoogleMapsUrlParser.extractLabel(sharedText)
    }
}
