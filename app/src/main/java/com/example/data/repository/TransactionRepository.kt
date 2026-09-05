package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.local.LocalDataManager
import com.example.data.model.CategoriesDataDto
import com.example.data.model.CreateTransactionRequest
import com.example.data.model.TransactionDto
import com.example.data.model.TransactionListResponse
import com.example.data.model.TransactionMutationResponse
import com.example.data.model.TransactionStatsData
import org.json.JSONObject

class TransactionRepository(
    private val apiClient: ApiClient,
    private val localDataManager: LocalDataManager
) {

    suspend fun getTransactions(
        type: String? = null,
        category: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        search: String? = null,
        limit: Int? = 100
    ): Result<TransactionListResponse> {
        return try {
            val api = apiClient.getTransactionApiService()
            val response = api.getTransactions(
                type = type,
                category = category,
                startDate = startDate,
                endDate = endDate,
                search = search,
                limit = limit
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                fallbackGetTransactions(type, category, search)
            }
        } catch (e: Exception) {
            fallbackGetTransactions(type, category, search)
        }
    }

    private fun fallbackGetTransactions(
        type: String?,
        category: String?,
        search: String?
    ): Result<TransactionListResponse> {
        var list = localDataManager.getTransactions()
        if (!type.isNullOrBlank()) {
            list = list.filter { it.type.equals(type, ignoreCase = true) }
        }
        if (!category.isNullOrBlank()) {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }
        if (!search.isNullOrBlank()) {
            val q = search.lowercase()
            list = list.filter {
                it.category.lowercase().contains(q) ||
                (it.description?.lowercase()?.contains(q) == true)
            }
        }
        return Result.success(
            TransactionListResponse(
                success = true,
                count = list.size,
                data = list
            )
        )
    }

    suspend fun createTransaction(
        type: String,
        amount: Double,
        category: String,
        description: String? = null,
        date: String? = null,
        allowOverdraft: Boolean = false
    ): Result<TransactionMutationResponse> {
        return try {
            val api = apiClient.getTransactionApiService()
            val response = api.createTransaction(
                CreateTransactionRequest(
                    type = type,
                    amount = amount,
                    category = category,
                    description = description,
                    date = date,
                    allowOverdraft = allowOverdraft
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                fallbackCreateTransaction(type, amount, category, description, date)
            }
        } catch (e: Exception) {
            fallbackCreateTransaction(type, amount, category, description, date)
        }
    }

    private fun fallbackCreateTransaction(
        type: String,
        amount: Double,
        category: String,
        description: String?,
        date: String?
    ): Result<TransactionMutationResponse> {
        val tx = localDataManager.addTransaction(
            type = type,
            amount = amount,
            category = category,
            description = description,
            date = date
        )
        val stats = localDataManager.getTransactionStats()
        return Result.success(
            TransactionMutationResponse(
                success = true,
                message = "Transaction saved successfully",
                newBalance = stats.totalIncome - stats.totalExpense,
                data = tx
            )
        )
    }

    suspend fun deleteTransaction(id: String): Result<TransactionMutationResponse> {
        return try {
            val api = apiClient.getTransactionApiService()
            val response = api.deleteTransaction(id)
            if (response.isSuccessful && response.body() != null) {
                localDataManager.deleteTransaction(id)
                Result.success(response.body()!!)
            } else {
                localDataManager.deleteTransaction(id)
                val stats = localDataManager.getTransactionStats()
                Result.success(
                    TransactionMutationResponse(
                        success = true,
                        message = "Transaction deleted",
                        newBalance = stats.totalIncome - stats.totalExpense
                    )
                )
            }
        } catch (e: Exception) {
            localDataManager.deleteTransaction(id)
            val stats = localDataManager.getTransactionStats()
            Result.success(
                TransactionMutationResponse(
                    success = true,
                    message = "Transaction deleted",
                    newBalance = stats.totalIncome - stats.totalExpense
                )
            )
        }
    }

    suspend fun getCategories(): Result<CategoriesDataDto> {
        return try {
            val api = apiClient.getTransactionApiService()
            val response = api.getCategories()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.success(localDataManager.getCategories())
            }
        } catch (e: Exception) {
            Result.success(localDataManager.getCategories())
        }
    }

    suspend fun getStats(): Result<TransactionStatsData> {
        return try {
            val api = apiClient.getTransactionApiService()
            val response = api.getStats()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.success(localDataManager.getTransactionStats())
            }
        } catch (e: Exception) {
            Result.success(localDataManager.getTransactionStats())
        }
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

class OverdraftException(message: String) : Exception(message)
