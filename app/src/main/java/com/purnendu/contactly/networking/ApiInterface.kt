package com.purnendu.contactly.networking

import com.purnendu.contactly.networking.model.WorldTimeApiResponse
import io.ktor.client.call.body
import io.ktor.client.request.get

private const val WORLD_TIME_API_URL = "https://worldtimeapi.org/api/ip"

object ApiInterface {
    suspend fun fetchCurrentTime(): WorldTimeApiResponse {
        return KtorClient.client.get(WORLD_TIME_API_URL).body()
    }
}
