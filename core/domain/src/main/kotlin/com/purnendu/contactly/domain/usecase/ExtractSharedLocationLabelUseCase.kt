package com.purnendu.contactly.domain.usecase

import com.purnendu.contactly.domain.repository.LocationParserRepository

class ExtractSharedLocationLabelUseCase(
    private val locationParserRepository: LocationParserRepository
) {
    operator fun invoke(sharedText: String): String? = locationParserRepository.extractLabel(sharedText)
}
