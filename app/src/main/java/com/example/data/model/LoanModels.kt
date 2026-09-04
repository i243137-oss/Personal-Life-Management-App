package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RepaymentDto(
    @Json(name = "_id") val id: String? = null,
    @Json(name = "amount") val amount: Double = 0.0,
    @Json(name = "date") val date: String? = null,
    @Json(name = "notes") val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class LoanDto(
    @Json(name = "_id") val id: String = "",
    @Json(name = "userId") val userId: String? = null,
    @Json(name = "personName") val personName: String = "",
    @Json(name = "phoneNumber") val phoneNumber: String? = null,
    @Json(name = "type") val type: String = "lent", // "lent" (they owe me) | "borrowed" (I owe them)
    @Json(name = "amount") val amount: Double = 0.0,
    @Json(name = "remainingAmount") val remainingAmount: Double = 0.0,
    @Json(name = "dueDate") val dueDate: String? = null,
    @Json(name = "status") val status: String = "pending", // "pending" | "partially_paid" | "paid"
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "repayments") val repayments: List<RepaymentDto> = emptyList(),
    @Json(name = "affectBalance") val affectBalance: Boolean = true,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class LoanListResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "count") val count: Int = 0,
    @Json(name = "data") val data: List<LoanDto> = emptyList(),
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class LoanSummaryData(
    @Json(name = "totalLent") val totalLent: Double = 0.0,
    @Json(name = "totalBorrowed") val totalBorrowed: Double = 0.0,
    @Json(name = "netPosition") val netPosition: Double = 0.0,
    @Json(name = "activeLentCount") val activeLentCount: Int = 0,
    @Json(name = "activeBorrowedCount") val activeBorrowedCount: Int = 0,
    @Json(name = "settledCount") val settledCount: Int = 0,
    @Json(name = "overdueCount") val overdueCount: Int = 0,
    @Json(name = "totalCount") val totalCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class LoanSummaryResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: LoanSummaryData? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateLoanRequest(
    @Json(name = "personName") val personName: String,
    @Json(name = "phoneNumber") val phoneNumber: String? = null,
    @Json(name = "type") val type: String, // "lent" or "borrowed"
    @Json(name = "amount") val amount: Double,
    @Json(name = "dueDate") val dueDate: String? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "affectBalance") val affectBalance: Boolean = true
)

@JsonClass(generateAdapter = true)
data class RecordRepaymentRequest(
    @Json(name = "amount") val amount: Double,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "date") val date: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateLoanStatusRequest(
    @Json(name = "status") val status: String
)

@JsonClass(generateAdapter = true)
data class LoanMutationResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: LoanDto? = null
)
