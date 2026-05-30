package com.purnendu.contactly.domain.model

data class TimeValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)
