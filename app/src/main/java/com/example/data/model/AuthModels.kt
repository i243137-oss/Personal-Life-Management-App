package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "_id") val mongoId: String? = null,
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "createdAt") val createdAt: String? = null
) {
    val effectiveId: String
        get() = id ?: mongoId ?: ""
}

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "token") val token: String? = null,
    @Json(name = "user") val user: UserDto? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class UserResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "user") val user: UserDto? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class TransactionDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "_id") val mongoId: String? = null,
    @Json(name = "type") val type: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "category") val category: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "date") val date: String? = null
) {
    val effectiveId: String
        get() = id ?: mongoId ?: ""
}

@JsonClass(generateAdapter = true)
data class DashboardData(
    @Json(name = "currentBalance") val currentBalance: Double = 0.0,
    @Json(name = "todayExpenses") val todayExpenses: Double = 0.0,
    @Json(name = "youOwe") val youOwe: Double = 0.0,
    @Json(name = "othersOwe") val othersOwe: Double = 0.0,
    @Json(name = "recentActivity") val recentActivity: List<TransactionDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DashboardResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: DashboardData? = null,
    @Json(name = "message") val message: String? = null
)
