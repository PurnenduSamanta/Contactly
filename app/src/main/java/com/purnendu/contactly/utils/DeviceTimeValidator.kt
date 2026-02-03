package com.purnendu.contactly.utils

import android.content.Context
import com.purnendu.contactly.networking.ApiInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

private const val MAX_ALLOWED_DRIFT_MINUTES = 2L

data class TimeValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

suspend fun validateDeviceTime(
    context: Context,
    maxAllowedDriftMinutes: Long = MAX_ALLOWED_DRIFT_MINUTES
): TimeValidationResult {
    if (!isNetworkAvailable(context)) {
        return TimeValidationResult(
            isValid = false,
            errorMessage = "Check Network connection as it is necessary to validate device time"
        )
    }

    val response = try {
        withContext(Dispatchers.IO){ ApiInterface.fetchCurrentTime()}
    } catch (e: Exception) {
        return TimeValidationResult(
            isValid = false,
            errorMessage = e.localizedMessage
        )
    }

    val deviceMillis = System.currentTimeMillis()
    val networkMillis = response.unixtime * 1000L
    val driftMinutes = abs(deviceMillis - networkMillis) / 60_000L

    val isTimeValid = driftMinutes <= maxAllowedDriftMinutes
    if (!isTimeValid) {
        return TimeValidationResult(
            isValid = false,
            errorMessage = "Unable to validate device time.Please try again"
        )
    }

    return TimeValidationResult(isValid = true)
}
