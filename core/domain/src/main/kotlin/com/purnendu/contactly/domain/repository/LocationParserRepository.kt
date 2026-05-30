package com.purnendu.contactly.domain.repository

import com.purnendu.contactly.domain.model.LocationCoordinates

interface LocationParserRepository {
    suspend fun parseFromSharedText(sharedText: String): LocationCoordinates?
    fun extractLabel(sharedText: String): String?
}
