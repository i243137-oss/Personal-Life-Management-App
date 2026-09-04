package com.example.data.api

import com.example.data.model.CreateLoanRequest
import com.example.data.model.LoanListResponse
import com.example.data.model.LoanMutationResponse
import com.example.data.model.LoanSummaryResponse
import com.example.data.model.RecordRepaymentRequest
import com.example.data.model.UpdateLoanStatusRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface LoanApiService {

    @GET("api/loans")
    suspend fun getLoans(
        @Query("type") type: String? = null,
        @Query("status") status: String? = null,
        @Query("search") search: String? = null
    ): Response<LoanListResponse>

    @GET("api/loans/summary")
    suspend fun getLoanSummary(): Response<LoanSummaryResponse>

    @POST("api/loans")
    suspend fun createLoan(
        @Body request: CreateLoanRequest
    ): Response<LoanMutationResponse>

    @POST("api/loans/{id}/repay")
    suspend fun recordRepayment(
        @Path("id") id: String,
        @Body request: RecordRepaymentRequest
    ): Response<LoanMutationResponse>

    @PATCH("api/loans/{id}/status")
    suspend fun updateLoanStatus(
        @Path("id") id: String,
        @Body request: UpdateLoanStatusRequest
    ): Response<LoanMutationResponse>

    @DELETE("api/loans/{id}")
    suspend fun deleteLoan(
        @Path("id") id: String
    ): Response<LoanMutationResponse>
}
