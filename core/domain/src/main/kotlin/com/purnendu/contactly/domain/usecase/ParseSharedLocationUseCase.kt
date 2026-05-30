package com.purnendu.contactly.domain.usecase

import com.purnendu.contactly.domain.model.LocationCoordinates
import com.purnendu.contactly.domain.repository.LocationParserRepository

class ParseSharedLocationUseCase(
    private val locationParserRepository: LocationParserRepository
) {
    suspend operator fun invoke(sharedText: String): LocationCoordinates? {
        return locationParserRepository.parseFromSharedText(sharedText)
    }
}
