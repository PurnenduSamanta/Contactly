package com.purnendu.contactly.utils

import android.content.Context
import com.purnendu.contactly.data.utils.isNetworkAvailable
import com.purnendu.contactly.networking.ApiInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone
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
        val deviceTimeZone = TimeZone.getDefault().id
        withContext(Dispatchers.IO){ ApiInterface.fetchCurrentTime(deviceTimeZone)}
    } catch (e: Exception) {
        return TimeValidationResult(
            isValid = false,
            errorMessage = e.localizedMessage
        )
    }

    val deviceMillis = System.currentTimeMillis()
    
    // Calculate network time in milliseconds from the response fields
    val networkCalendar = Calendar.getInstance(TimeZone.getTimeZone(response.timeZone)).apply {
        set(Calendar.YEAR, response.year)
        set(Calendar.MONTH, response.month - 1) // Calendar months are 0-indexed
        set(Calendar.DAY_OF_MONTH, response.day)
        set(Calendar.HOUR_OF_DAY, response.hour)
        set(Calendar.MINUTE, response.minute)
        set(Calendar.SECOND, response.seconds)
        set(Calendar.MILLISECOND, response.milliSeconds)
    }
    val networkMillis = networkCalendar.timeInMillis
    
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

