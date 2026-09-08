package com.example.data.api

import com.example.data.model.AuthResponse
import com.example.data.model.DashboardResponse
import com.example.data.model.LoginRequest
import com.example.data.model.RegisterRequest
import com.example.data.model.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApiService {

    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @GET("api/auth/me")
    suspend fun getMe(): Response<UserResponse>

    @GET("api/dashboard/summary")
    suspend fun getDashboardSummary(
        @Query("month") month: String? = null
    ): Response<DashboardResponse>
}
