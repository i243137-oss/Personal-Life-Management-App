package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.local.LocalDataManager
import com.example.data.model.CreateLoanRequest
import com.example.data.model.LoanDto
import com.example.data.model.LoanSummaryData
import com.example.data.model.RecordRepaymentRequest
import com.example.data.model.UpdateLoanStatusRequest
import org.json.JSONObject

class LoanRepository(
    private val apiClient: ApiClient,
    private val localDataManager: LocalDataManager
) {

    suspend fun getLoans(
        type: String? = null,
        status: String? = null,
        search: String? = null
    ): Result<List<LoanDto>> {
        return try {
            val api = apiClient.getLoanApiService()
            val response = api.getLoans(type = type, status = status, search = search)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                fallbackGetLoans(type, status, search)
            }
        } catch (e: Exception) {
            fallbackGetLoans(type, status, search)
        }
    }

    private fun fallbackGetLoans(
        type: String?,
        status: String?,
        search: String?
    ): Result<List<LoanDto>> {
        var list = localDataManager.getLoans()
        if (!type.isNullOrBlank()) {
            list = list.filter { it.type.equals(type, ignoreCase = true) }
        }
        if (!status.isNullOrBlank()) {
            list = list.filter { it.status.equals(status, ignoreCase = true) }
        }
        if (!search.isNullOrBlank()) {
            val q = search.lowercase()
            list = list.filter {
                it.personName.lowercase().contains(q) ||
                (it.notes?.lowercase()?.contains(q) == true) ||
                (it.phoneNumber?.contains(q) == true)
            }
        }
        return Result.success(list)
    }

    suspend fun getLoanSummary(): Result<LoanSummaryData> {
        return try {
            val api = apiClient.getLoanApiService()
            val response = api.getLoanSummary()
            if (response.isSuccessful && response.body() != null && response.body()!!.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.success(localDataManager.getLoanSummary())
            }
        } catch (e: Exception) {
            Result.success(localDataManager.getLoanSummary())
        }
    }

    suspend fun createLoan(
        personName: String,
        phoneNumber: String?,
        type: String,
        amount: Double,
        dueDate: String?,
        notes: String?,
        affectBalance: Boolean = true
    ): Result<LoanDto> {
        return try {
            val api = apiClient.getLoanApiService()
            val response = api.createLoan(
                CreateLoanRequest(
                    personName = personName,
                    phoneNumber = phoneNumber,
                    type = type,
                    amount = amount,
                    dueDate = dueDate,
                    notes = notes,
                    affectBalance = affectBalance
                )
            )
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                fallbackCreateLoan(personName, phoneNumber, type, amount, dueDate, notes, affectBalance)
            }
        } catch (e: Exception) {
            fallbackCreateLoan(personName, phoneNumber, type, amount, dueDate, notes, affectBalance)
        }
    }

    private fun fallbackCreateLoan(
        personName: String,
        phoneNumber: String?,
        type: String,
        amount: Double,
        dueDate: String?,
        notes: String?,
        affectBalance: Boolean
    ): Result<LoanDto> {
        val loan = localDataManager.addLoan(
            personName = personName,
            phoneNumber = phoneNumber,
            type = type,
            amount = amount,
            dueDate = dueDate,
            notes = notes,
            affectBalance = affectBalance
        )
        return Result.success(loan)
    }

    suspend fun recordRepayment(
        loanId: String,
        amount: Double,
        notes: String? = null
    ): Result<LoanDto> {
        return try {
            val api = apiClient.getLoanApiService()
            val response = api.recordRepayment(
                loanId,
                RecordRepaymentRequest(amount = amount, notes = notes)
            )
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                fallbackRecordRepayment(loanId, amount, notes)
            }
        } catch (e: Exception) {
            fallbackRecordRepayment(loanId, amount, notes)
        }
    }

    private fun fallbackRecordRepayment(
        loanId: String,
        amount: Double,
        notes: String?
    ): Result<LoanDto> {
        val updated = localDataManager.recordRepayment(loanId, amount, notes)
        return if (updated != null) {
            Result.success(updated)
        } else {
            Result.failure(Exception("Loan not found"))
        }
    }

    suspend fun updateLoanStatus(loanId: String, status: String): Result<LoanDto> {
        return try {
            val api = apiClient.getLoanApiService()
            val response = api.updateLoanStatus(loanId, UpdateLoanStatusRequest(status = status))
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val updated = localDataManager.updateLoanStatus(loanId, status)
                if (updated != null) Result.success(updated) else Result.failure(Exception("Loan not found"))
            }
        } catch (e: Exception) {
            val updated = localDataManager.updateLoanStatus(loanId, status)
            if (updated != null) Result.success(updated) else Result.failure(Exception("Loan not found"))
        }
    }

    suspend fun markAsPaid(loanId: String): Result<LoanDto> {
        return updateLoanStatus(loanId, "paid")
    }

    suspend fun deleteLoan(loanId: String): Result<Boolean> {
        return try {
            val api = apiClient.getLoanApiService()
            val response = api.deleteLoan(loanId)
            if (response.isSuccessful) {
                localDataManager.deleteLoan(loanId)
                Result.success(true)
            } else {
                val removed = localDataManager.deleteLoan(loanId)
                Result.success(removed)
            }
        } catch (e: Exception) {
            val removed = localDataManager.deleteLoan(loanId)
            Result.success(removed)
        }
    }

    private fun parseErrorMessage(json: String?): String? {
        if (json.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(json)
            obj.optString("message", null)
        } catch (_: Exception) {
            null
        }
    }
}
