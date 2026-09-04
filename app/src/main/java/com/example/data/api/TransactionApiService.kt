package com.example.data.api

import com.example.data.model.CategoriesResponse
import com.example.data.model.CreateTransactionRequest
import com.example.data.model.TransactionListResponse
import com.example.data.model.TransactionMutationResponse
import com.example.data.model.TransactionStatsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TransactionApiService {

    @GET("api/transactions")
    suspend fun getTransactions(
        @Query("type") type: String? = null,
        @Query("category") category: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("search") search: String? = null,
        @Query("limit") limit: Int? = 50
    ): Response<TransactionListResponse>

    @POST("api/transactions")
    suspend fun createTransaction(
        @Body request: CreateTransactionRequest
    ): Response<TransactionMutationResponse>

    @DELETE("api/transactions/{id}")
    suspend fun deleteTransaction(
        @Path("id") id: String
    ): Response<TransactionMutationResponse>

    @GET("api/transactions/categories")
    suspend fun getCategories(): Response<CategoriesResponse>

    @GET("api/transactions/stats")
    suspend fun getStats(): Response<TransactionStatsResponse>
}
