package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.LoanDto
import com.example.data.model.LoanSummaryData
import com.example.data.repository.LoanRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoanUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isMutating: Boolean = false,
    val loans: List<LoanDto> = emptyList(),
    val summary: LoanSummaryData = LoanSummaryData(),
    val selectedFilter: String? = null, // null = All, "lent" = They Owe, "borrowed" = I Owe, "settled" = Paid
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class LoanViewModel(
    private val loanRepository: LoanRepository,
    private val onDataChangedCallback: (() -> Unit)? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoanUiState())
    val uiState: StateFlow<LoanUiState> = _uiState.asStateFlow()

    private var searchDebounceJob: Job? = null

    init {
        loadData()
    }

    fun loadData(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
            } else {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            val filter = _uiState.value.selectedFilter
            val typeParam = when (filter) {
                "lent" -> "lent"
                "borrowed" -> "borrowed"
                else -> null
            }
            val statusParam = when (filter) {
                "settled" -> "paid"
                else -> null
            }
            val searchParam = _uiState.value.searchQuery.ifBlank { null }

            val loansResult = loanRepository.getLoans(
                type = typeParam,
                status = statusParam,
                search = searchParam
            )
            val summaryResult = loanRepository.getLoanSummary()

            loansResult.fold(
                onSuccess = { loanList ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isRefreshing = false,
                            loans = loanList,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = err.message ?: "Failed to load loans"
                        )
                    }
                }
            )

            summaryResult.onSuccess { summary ->
                _uiState.update { it.copy(summary = summary) }
            }
        }
    }

    fun setFilter(filter: String?) {
        _uiState.update { it.copy(selectedFilter = filter) }
        loadData()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(300)
            loadData()
        }
    }

    fun createLoan(
        personName: String,
        phoneNumber: String?,
        type: String,
        amount: Double,
        dueDate: String?,
        notes: String?,
        affectBalance: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            val result = loanRepository.createLoan(
                personName = personName,
                phoneNumber = phoneNumber,
                type = type,
                amount = amount,
                dueDate = dueDate,
                notes = notes,
                affectBalance = affectBalance
            )
            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            isMutating = false,
                            successMessage = "Loan record created successfully"
                        )
                    }
                    loadData()
                    onDataChangedCallback?.invoke()
                    onSuccess()
                },
                onFailure = { err ->
                    _uiState.update { state ->
                        state.copy(
                            isMutating = false,
                            errorMessage = err.message ?: "Failed to create loan"
                        )
                    }
                }
            )
        }
    }

    fun recordRepayment(
        loanId: String,
        amount: Double,
        notes: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            val result = loanRepository.recordRepayment(
                loanId = loanId,
                amount = amount,
                notes = notes
            )
            result.fold(
                onSuccess = { updatedLoan ->
                    val isSettled = updatedLoan.status == "paid"
                    _uiState.update { state ->
                        state.copy(
                            isMutating = false,
                            successMessage = if (isSettled) "Loan settled in full!" else "Repayment of Rs. ${amount.toInt()} recorded"
                        )
                    }
                    loadData()
                    onDataChangedCallback?.invoke()
                    onSuccess()
                },
                onFailure = { err ->
                    _uiState.update { state ->
                        state.copy(
                            isMutating = false,
                            errorMessage = err.message ?: "Failed to record repayment"
                        )
                    }
                }
            )
        }
    }

    fun markAsPaid(loanId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            val result = loanRepository.markAsPaid(loanId)
            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            isMutating = false,
                            successMessage = "Loan marked as fully settled"
                        )
                    }
                    loadData()
                    onDataChangedCallback?.invoke()
                    onSuccess()
                },
                onFailure = { err ->
                    _uiState.update { state ->
                        state.copy(
                            isMutating = false,
                            errorMessage = err.message ?: "Failed to settle loan"
                        )
                    }
                }
            )
        }
    }

    fun deleteLoan(loanId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            val result = loanRepository.deleteLoan(loanId)
            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            isMutating = false,
                            successMessage = "Loan deleted"
                        )
                    }
                    loadData()
                    onDataChangedCallback?.invoke()
                },
                onFailure = { err ->
                    _uiState.update { state ->
                        state.copy(
                            isMutating = false,
                            errorMessage = err.message ?: "Failed to delete loan"
                        )
                    }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

class LoanViewModelFactory(
    private val loanRepository: LoanRepository,
    private val onDataChangedCallback: (() -> Unit)? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LoanViewModel(loanRepository, onDataChangedCallback) as T
    }
}
