package com.example.data.api

import com.example.data.local.UserSessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(private val sessionManager: UserSessionManager) {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val token = runBlocking {
            sessionManager.authTokenFlow.firstOrNull()
        }

        val requestBuilder = originalRequest.newBuilder()
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")

        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    fun getApiService(customBaseUrl: String? = null): AuthApiService {
        val baseUrl = customBaseUrl ?: runBlocking {
            sessionManager.backendUrlFlow.firstOrNull()
        } ?: UserSessionManager.DEFAULT_BACKEND_URL

        val sanitizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(sanitizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AuthApiService::class.java)
    }

    fun getTransactionApiService(customBaseUrl: String? = null): TransactionApiService {
        val baseUrl = customBaseUrl ?: runBlocking {
            sessionManager.backendUrlFlow.firstOrNull()
        } ?: UserSessionManager.DEFAULT_BACKEND_URL

        val sanitizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(sanitizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TransactionApiService::class.java)
    }

    fun getLoanApiService(customBaseUrl: String? = null): LoanApiService {
        val baseUrl = customBaseUrl ?: runBlocking {
            sessionManager.backendUrlFlow.firstOrNull()
        } ?: UserSessionManager.DEFAULT_BACKEND_URL

        val sanitizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(sanitizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(LoanApiService::class.java)
    }

    fun getDictionaryApiService(customBaseUrl: String? = null): DictionaryApiService {
        val baseUrl = customBaseUrl ?: runBlocking {
            sessionManager.backendUrlFlow.firstOrNull()
        } ?: UserSessionManager.DEFAULT_BACKEND_URL

        val sanitizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(sanitizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(DictionaryApiService::class.java)
    }

    fun getLuggageApiService(customBaseUrl: String? = null): LuggageApiService {
        val baseUrl = customBaseUrl ?: runBlocking {
            sessionManager.backendUrlFlow.firstOrNull()
        } ?: UserSessionManager.DEFAULT_BACKEND_URL

        val sanitizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(sanitizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(LuggageApiService::class.java)
    }
}
