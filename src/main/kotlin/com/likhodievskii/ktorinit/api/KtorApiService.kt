package com.likhodievskii.ktorinit.api


import com.likhodievskii.ktorinit.model.KtorFeature
import com.likhodievskii.ktorinit.model.KtorProjectSettings
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.io.InputStream

class KtorApiService {

    companion object {
        private const val GENERATE_URL = "https://start.ktor.io/project/generate"
        private const val FEATURES_URL = "https://start.ktor.io/features/"
        private const val SETTINGS_URL = "https://start.ktor.io/project/settings"
    }

    private val client = OkHttpClient()

    private val snakeMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    private val camelMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    fun fetchSettings() : KtorProjectSettings {
        val request = Request.Builder().url(SETTINGS_URL).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Failed to fetch settings: ${response.code}")
            return snakeMapper.readValue(response.body.string(), KtorProjectSettings::class.java)
        }
    }

    fun fetchFeatures(ktorVersion: String) : List<KtorFeature> {
        val request = Request.Builder().url("$FEATURES_URL$ktorVersion").get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Failed to fetch features: ${response.code}")
            return camelMapper.readValue(
                response.body.string(),
                camelMapper.typeFactory.constructCollectionType(List::class.java, KtorFeature::class.java)
                )
        }
    }

    fun generateProject(payload: String) : InputStream {
        val body = payload.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(GENERATE_URL).post(body).build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) error("Failed to generate project: ${response.code}")
        return response.body.byteStream()
    }
}