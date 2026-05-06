package com.freshtrack.data.ocr

import com.freshtrack.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrEdgeFunction @Inject constructor(
    private val supabase: SupabaseClient,
    private val httpClient: HttpClient
) {
    // imageDataUrl: "data:image/jpeg;base64,…"
    // Edge Function returns {"date": "YYYY-MM-DD", "raw_text": "…"}
    suspend fun parseDate(imageDataUrl: String): String? = runCatching {
        val token = supabase.auth.currentSessionOrNull()?.accessToken
            ?: return@runCatching null
        val body = buildJsonObject { put("image", imageDataUrl) }.toString()

        val response = httpClient.post(
            "${BuildConfig.SUPABASE_URL}/functions/v1/ocr-date"
        ) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        json["date"]?.jsonPrimitive?.content?.takeIf { it != "null" }
    }.getOrNull()
}
