package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoriesDataDto
import com.example.data.model.CategoryItemDto
import com.example.data.model.QuickPresetDto
import com.example.data.model.TransactionDto
import com.example.data.model.TransactionStatsData
import com.example.data.repository.OverdraftException
import com.example.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MoneyUiState(
    val transactions: List<TransactionDto> = emptyList(),
    val filteredTransactions: List<TransactionDto> = emptyList(),
    val currentBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val categories: CategoriesDataDto = defaultCategories,
    val stats: TransactionStatsData? = null,
    val selectedTypeFilter: String = "all", // "all", "income", "expense"
    val selectedCategoryFilter: String = "all",
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val overdraftPrompt: String? = null,
    val actionSuccessMessage: String? = null
)

val defaultCategories = CategoriesDataDto(
    expense = listOf(
        CategoryItemDto("Fee", "school", "#1E88E5", "Tuition, college, exam"),
        CategoryItemDto("Transport", "directions_bus", "#FB8C00", "Bus, fuel, rickshaw, taxi"),
        CategoryItemDto("Meal & Food", "restaurant", "#E53935", "Breakfast, lunch, dinner, cafe"),
        CategoryItemDto("Shopping", "shopping_bag", "#8E24AA", "Clothes, personal items"),
        CategoryItemDto("Bills & Utilities", "receipt_long", "#00897B", "Electricity, mobile, wifi"),
        CategoryItemDto("Entertainment", "movie", "#D81B60", "Outings, games, movies"),
        CategoryItemDto("Health & Medical", "medical_services", "#43A047", "Doctor, pharmacy, medicine"),
        CategoryItemDto("Other Expense", "more_horiz", "#757575", "Miscellaneous")
    ),
    income = listOf(
        CategoryItemDto("Pocket Money", "savings", "#2E7D32", "Family allowance, pocket money"),
        CategoryItemDto("Salary", "payments", "#1B5E20", "Monthly salary, stipend"),
        CategoryItemDto("Freelance & Gig", "laptop_mac", "#00838F", "Online gigs, coding, design"),
        CategoryItemDto("Gift / Cash Inflow", "card_giftcard", "#00ACC1", "Eidi, gifts, rewards"),
        CategoryItemDto("Investment / Profit", "trending_up", "#558B2F", "Profit, dividend"),
        CategoryItemDto("Other Income", "add_circle", "#689F38", "Other cash inflows")
    ),
    quickPresets = listOf(
        QuickPresetDto("Lunch", "Meal & Food", 200.0, "expense"),
        QuickPresetDto("Transport", "Transport", 100.0, "expense"),
        QuickPresetDto("Tea/Snack", "Meal & Food", 60.0, "expense"),
        QuickPresetDto("Mobile", "Bills & Utilities", 500.0, "expense"),
        QuickPresetDto("Pocket Money", "Pocket Money", 2000.0, "income")
    )
)

class MoneyViewModel(
    private val transactionRepository: TransactionRepository,
    private val onDataChangedCallback: (() -> Unit)? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoneyUiState())
    val uiState: StateFlow<MoneyUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        loadTransactions()
        loadCategories()
        loadStats()
    }

    fun loadTransactions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = transactionRepository.getTransactions(
                type = if (_uiState.value.selectedTypeFilter == "all") null else _uiState.value.selectedTypeFilter,
                category = if (_uiState.value.selectedCategoryFilter == "all") null else _uiState.value.selectedCategoryFilter
            )

            result.fold(
                onSuccess = { response ->
                    _uiState.update { current ->
                        val filtered = filterList(
                            response.data,
                            current.selectedTypeFilter,
                            current.selectedCategoryFilter,
                            current.searchQuery
                        )
                        current.copy(
                            transactions = response.data,
                            filteredTransactions = filtered,
                            currentBalance = response.currentBalance,
                            totalIncome = response.totalIncome,
                            totalExpense = response.totalExpense,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load transactions"
                        )
                    }
                }
            )
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            val result = transactionRepository.getCategories()
            result.onSuccess { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            val result = transactionRepository.getStats()
            result.onSuccess { stats ->
                _uiState.update { it.copy(stats = stats) }
            }
        }
    }

    fun setTypeFilter(type: String) {
        _uiState.update { current ->
            val updated = current.copy(selectedTypeFilter = type)
            updated.copy(
                filteredTransactions = filterList(
                    current.transactions,
                    type,
                    current.selectedCategoryFilter,
                    current.searchQuery
                )
            )
        }
    }

    fun setCategoryFilter(category: String) {
        _uiState.update { current ->
            val updated = current.copy(selectedCategoryFilter = category)
            updated.copy(
                filteredTransactions = filterList(
                    current.transactions,
                    current.selectedTypeFilter,
                    category,
                    current.searchQuery
                )
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { current ->
            val updated = current.copy(searchQuery = query)
            updated.copy(
                filteredTransactions = filterList(
                    current.transactions,
                    current.selectedTypeFilter,
                    current.selectedCategoryFilter,
                    query
                )
            )
        }
    }

    fun createTransaction(
        type: String,
        amount: Double,
        category: String,
        description: String? = null,
        date: String? = null,
        allowOverdraft: Boolean = false,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, overdraftPrompt = null) }
            val result = transactionRepository.createTransaction(
                type = type,
                amount = amount,
                category = category,
                description = description,
                date = date,
                allowOverdraft = allowOverdraft
            )

            result.fold(
                onSuccess = { res ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            actionSuccessMessage = res.message ?: "Transaction recorded successfully"
                        )
                    }
                    loadData()
                    onDataChangedCallback?.invoke()
                    onSuccess()
                },
                onFailure = { err ->
                    if (err is OverdraftException) {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                overdraftPrompt = err.message
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = err.message ?: "Transaction failed"
                            )
                        }
                    }
                }
            )
        }
    }

    fun quickAddExpense(
        label: String,
        category: String,
        amount: Double,
        type: String = "expense"
    ) {
        createTransaction(
            type = type,
            amount = amount,
            category = category,
            description = "Quick Add: $label"
        )
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val result = transactionRepository.deleteTransaction(id)
            result.fold(
                onSuccess = { res ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            actionSuccessMessage = res.message ?: "Deleted transaction"
                        )
                    }
                    loadData()
                    onDataChangedCallback?.invoke()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = err.message ?: "Failed to delete"
                        )
                    }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                overdraftPrompt = null,
                actionSuccessMessage = null
            )
        }
    }

    private fun filterList(
        list: List<TransactionDto>,
        typeFilter: String,
        categoryFilter: String,
        searchQuery: String
    ): List<TransactionDto> {
        return list.filter { tx ->
            val matchType = when (typeFilter) {
                "income" -> tx.type == "income"
                "expense" -> tx.type == "expense"
                else -> true
            }
            val matchCategory = if (categoryFilter == "all") true else tx.category.equals(categoryFilter, ignoreCase = true)
            val matchSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                tx.category.lowercase().contains(q) || (tx.description?.lowercase()?.contains(q) == true)
            }
            matchType && matchCategory && matchSearch
        }
    }
}

class MoneyViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val onDataChangedCallback: (() -> Unit)? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MoneyViewModel(transactionRepository, onDataChangedCallback) as T
    }
}

