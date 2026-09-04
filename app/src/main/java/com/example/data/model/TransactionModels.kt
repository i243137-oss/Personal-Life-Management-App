package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TransactionListResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "count") val count: Int = 0,
    @Json(name = "currentBalance") val currentBalance: Double = 0.0,
    @Json(name = "totalIncome") val totalIncome: Double = 0.0,
    @Json(name = "totalExpense") val totalExpense: Double = 0.0,
    @Json(name = "data") val data: List<TransactionDto> = emptyList(),
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateTransactionRequest(
    @Json(name = "type") val type: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "category") val category: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "allowOverdraft") val allowOverdraft: Boolean = false
)

@JsonClass(generateAdapter = true)
data class TransactionMutationResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String? = null,
    @Json(name = "newBalance") val newBalance: Double? = null,
    @Json(name = "overdraft") val overdraft: Boolean? = null,
    @Json(name = "currentBalance") val currentBalance: Double? = null,
    @Json(name = "data") val data: TransactionDto? = null
)

@JsonClass(generateAdapter = true)
data class CategoryItemDto(
    @Json(name = "name") val name: String,
    @Json(name = "icon") val icon: String,
    @Json(name = "color") val color: String,
    @Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class QuickPresetDto(
    @Json(name = "label") val label: String,
    @Json(name = "category") val category: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "type") val type: String
)

@JsonClass(generateAdapter = true)
data class CategoriesDataDto(
    @Json(name = "expense") val expense: List<CategoryItemDto> = emptyList(),
    @Json(name = "income") val income: List<CategoryItemDto> = emptyList(),
    @Json(name = "quickPresets") val quickPresets: List<QuickPresetDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CategoriesResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: CategoriesDataDto? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class ExpenseBreakdownItem(
    @Json(name = "category") val category: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "percentage") val percentage: Int = 0
)

@JsonClass(generateAdapter = true)
data class TransactionStatsData(
    @Json(name = "totalIncome") val totalIncome: Double = 0.0,
    @Json(name = "totalExpense") val totalExpense: Double = 0.0,
    @Json(name = "expenseBreakdown") val expenseBreakdown: List<ExpenseBreakdownItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TransactionStatsResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: TransactionStatsData? = null,
    @Json(name = "message") val message: String? = null
)
