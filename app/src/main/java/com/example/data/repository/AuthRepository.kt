package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.local.LocalDataManager
import com.example.data.local.UserSessionManager
import com.example.data.model.DashboardData
import com.example.data.model.LoginRequest
import com.example.data.model.RegisterRequest
import com.example.data.model.UserDto
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

class AuthRepository(
    private val apiClient: ApiClient,
    private val sessionManager: UserSessionManager,
    private val localDataManager: LocalDataManager
) {
    val isLoggedIn: Flow<Boolean> = sessionManager.isLoggedInFlow
    val currentUser: Flow<UserDto?> = sessionManager.currentUserFlow
    val backendUrl: Flow<String> = sessionManager.backendUrlFlow

    suspend fun register(name: String, email: String, password: String): Result<UserDto> {
        return try {
            val api = apiClient.getApiService()
            val response = api.register(RegisterRequest(name = name, email = email, password = password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success && body.token != null && body.user != null) {
                    sessionManager.saveSession(body.token, body.user)
                    Result.success(body.user)
                } else {
                    Result.failure(Exception(body.message ?: "Registration failed"))
                }
            } else {
                val errBody = response.errorBody()?.string()
                val message = parseErrorMessage(errBody) ?: "Registration failed with code ${response.code()}"
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Unable to connect to server. Please check your internet connection."))
        }
    }

    suspend fun login(email: String, password: String): Result<UserDto> {
        return try {
            val api = apiClient.getApiService()
            val response = api.login(LoginRequest(email = email, password = password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success && body.token != null && body.user != null) {
                    sessionManager.saveSession(body.token, body.user)
                    Result.success(body.user)
                } else {
                    Result.failure(Exception(body.message ?: "Login failed"))
                }
            } else {
                val errBody = response.errorBody()?.string()
                val message = parseErrorMessage(errBody) ?: "Login failed with code ${response.code()}"
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Unable to connect to server. Please check your internet connection."))
        }
    }

    suspend fun fetchCurrentUser(): Result<UserDto> {
        return try {
            val api = apiClient.getApiService()
            val response = api.getMe()
            if (response.isSuccessful && response.body()?.user != null) {
                Result.success(response.body()!!.user!!)
            } else {
                Result.failure(Exception("Could not fetch user profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBackendUrl(url: String) {
        sessionManager.updateBackendUrl(url)
    }

    suspend fun logout() {
        sessionManager.clearSession()
    }

    private fun parseErrorMessage(json: String?): String? {
        if (json.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(json)
            if (obj.has("message")) obj.optString("message") else null
        } catch (_: Exception) {
            null
        }
    }
}

class DashboardRepository(
    private val apiClient: ApiClient,
    private val localDataManager: LocalDataManager
) {
    suspend fun getDashboardSummary(): Result<DashboardData> {
        return try {
            val api = apiClient.getApiService()
            val response = api.getDashboardSummary()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                // Return local dashboard calculation
                Result.success(localDataManager.getDashboardData())
            }
        } catch (e: Exception) {
            // Seamless offline fallback
            Result.success(localDataManager.getDashboardData())
        }
    }
}
