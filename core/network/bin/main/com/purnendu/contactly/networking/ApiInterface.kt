package com.purnendu.contactly.networking

import com.purnendu.contactly.networking.model.TimeApiResponse
import io.ktor.client.call.body
import io.ktor.client.request.get

private const val TIME_API_BASE_URL = "https://timeapi.io/api/time/current/zone"

object ApiInterface {
    suspend fun fetchCurrentTime(timeZone: String): TimeApiResponse {
        return KtorClient.client.get("$TIME_API_BASE_URL?timeZone=$timeZone").body()
    }
}
