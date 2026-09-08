package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.DashboardData
import com.example.data.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(val data: DashboardData) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

class DashboardViewModel(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _selectedMonth = MutableStateFlow<String?>(null)
    val selectedMonth: StateFlow<String?> = _selectedMonth.asStateFlow()

    private val numberFormatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }

    init {
        loadDashboard()
    }

    fun loadDashboard(month: String? = _selectedMonth.value) {
        _uiState.value = DashboardUiState.Loading
        viewModelScope.launch {
            dashboardRepository.getDashboardSummary(month)
                .onSuccess { summary ->
                    _uiState.value = DashboardUiState.Success(summary)
                }
                .onFailure { error ->
                    _uiState.value = DashboardUiState.Error(
                        error.localizedMessage ?: "Could not load dashboard data. Please check backend connection."
                    )
                }
        }
    }

    fun selectMonth(month: String?) {
        _selectedMonth.value = month
        loadDashboard(month)
    }

    fun resetToCurrentMonth() {
        selectMonth(null)
    }

    fun getTimeBasedGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Good Night"
        }
    }

    fun formatCurrency(amount: Double): String {
        return "Rs. ${numberFormatter.format(amount)}"
    }
}

class DashboardViewModelFactory(
    private val dashboardRepository: DashboardRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DashboardViewModel(dashboardRepository) as T
    }
}
