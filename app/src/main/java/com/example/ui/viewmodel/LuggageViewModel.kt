package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AddLuggageItemRequest
import com.example.data.model.CreateLuggageTripRequest
import com.example.data.model.LuggageItemDto
import com.example.data.model.LuggageTripDto
import com.example.data.model.UpdateLuggageItemRequest
import com.example.data.model.UpdateLuggageTripRequest
import com.example.data.repository.LuggageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LuggageUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isMutating: Boolean = false,
    val trips: List<LuggageTripDto> = emptyList(),
    val selectedTripId: String? = null,
    val selectedCategory: String? = null, // null = All
    val filterPacked: Boolean? = null, // null = All, false = Unpacked, true = Packed
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val selectedTrip: LuggageTripDto?
        get() = trips.find { it.id == selectedTripId } ?: trips.firstOrNull()

    val filteredItems: List<LuggageItemDto>
        get() {
            val trip = selectedTrip ?: return emptyList()
            var list = trip.items

            if (!selectedCategory.isNullOrBlank()) {
                list = list.filter { it.category.equals(selectedCategory, ignoreCase = true) }
            }

            if (filterPacked != null) {
                list = list.filter { it.isPacked == filterPacked }
            }

            if (searchQuery.isNotBlank()) {
                val q = searchQuery.lowercase().trim()
                list = list.filter {
                    it.name.lowercase().contains(q) ||
                    (it.notes?.lowercase()?.contains(q) == true) ||
                    it.category.lowercase().contains(q)
                }
            }

            return list
        }
}

class LuggageViewModel(
    private val luggageRepository: LuggageRepository,
    private val onDataChangedCallback: (() -> Unit)? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(LuggageUiState())
    val uiState: StateFlow<LuggageUiState> = _uiState.asStateFlow()

    init {
        loadTrips()
    }

    fun loadTrips(selectTripId: String? = null, isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
            } else {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            val result = luggageRepository.getTrips()
            result.onSuccess { tripsList ->
                val targetId = selectTripId
                    ?: _uiState.value.selectedTripId
                    ?: tripsList.firstOrNull()?.id

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        trips = tripsList,
                        selectedTripId = targetId,
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = err.message ?: "Failed to load luggage data"
                    )
                }
            }
        }
    }

    fun selectTrip(tripId: String) {
        _uiState.update { it.copy(selectedTripId = tripId, searchQuery = "") }
    }

    fun selectCategory(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun setFilterPacked(packed: Boolean?) {
        _uiState.update { it.copy(filterPacked = packed) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun createTrip(
        title: String,
        destination: String,
        bagType: String,
        departureDate: String,
        returnDate: String,
        maxWeightKg: Double,
        colorHex: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            val request = CreateLuggageTripRequest(
                title = title.trim(),
                destination = destination.trim(),
                bagType = bagType,
                departureDate = departureDate.trim(),
                returnDate = returnDate.trim(),
                maxWeightKg = maxWeightKg,
                colorHex = colorHex
            )
            val result = luggageRepository.createTrip(request)
            result.onSuccess { created ->
                val current = _uiState.value.trips.toMutableList().apply { add(0, created) }
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        trips = current,
                        selectedTripId = created.id,
                        successMessage = "Created trip '${created.title}'"
                    )
                }
                onDataChangedCallback?.invoke()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        errorMessage = err.message ?: "Failed to create trip"
                    )
                }
            }
        }
    }

    fun updateTrip(
        id: String,
        title: String?,
        destination: String?,
        bagType: String?,
        departureDate: String?,
        returnDate: String?,
        maxWeightKg: Double?,
        colorHex: String?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            val request = UpdateLuggageTripRequest(
                title = title,
                destination = destination,
                bagType = bagType,
                departureDate = departureDate,
                returnDate = returnDate,
                maxWeightKg = maxWeightKg,
                colorHex = colorHex
            )
            val result = luggageRepository.updateTrip(id, request)
            result.onSuccess { updated ->
                val current = _uiState.value.trips.map { if (it.id == id) updated else it }
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        trips = current,
                        successMessage = "Trip updated successfully"
                    )
                }
                onDataChangedCallback?.invoke()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        errorMessage = err.message ?: "Failed to update trip"
                    )
                }
            }
        }
    }

    fun deleteTrip(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            val result = luggageRepository.deleteTrip(id)
            result.onSuccess {
                val current = _uiState.value.trips.filter { it.id != id }
                val nextSelected = if (_uiState.value.selectedTripId == id) {
                    current.firstOrNull()?.id
                } else {
                    _uiState.value.selectedTripId
                }
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        trips = current,
                        selectedTripId = nextSelected,
                        successMessage = "Trip deleted"
                    )
                }
                onDataChangedCallback?.invoke()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        errorMessage = err.message ?: "Failed to delete trip"
                    )
                }
            }
        }
    }

    fun addItem(
        tripId: String,
        name: String,
        category: String,
        quantity: Int,
        isEssential: Boolean,
        weightKg: Double,
        notes: String?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            val request = AddLuggageItemRequest(
                name = name.trim(),
                category = category,
                quantity = quantity.coerceAtLeast(1),
                isPacked = false,
                isEssential = isEssential,
                weightKg = weightKg.coerceAtLeast(0.0),
                notes = notes?.trim()
            )
            val result = luggageRepository.addItem(tripId, request)
            result.onSuccess { updatedTrip ->
                val current = _uiState.value.trips.map { if (it.id == tripId) updatedTrip else it }
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        trips = current,
                        successMessage = "Added item '$name'"
                    )
                }
                onDataChangedCallback?.invoke()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        errorMessage = err.message ?: "Failed to add item"
                    )
                }
            }
        }
    }

    fun toggleItem(tripId: String, itemId: String) {
        viewModelScope.launch {
            val result = luggageRepository.toggleItem(tripId, itemId)
            result.onSuccess { updatedTrip ->
                val current = _uiState.value.trips.map { if (it.id == tripId) updatedTrip else it }
                _uiState.update { it.copy(trips = current) }
                onDataChangedCallback?.invoke()
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message ?: "Failed to toggle item") }
            }
        }
    }

    fun updateItem(
        tripId: String,
        itemId: String,
        name: String,
        category: String,
        quantity: Int,
        isEssential: Boolean,
        weightKg: Double,
        notes: String?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            val request = UpdateLuggageItemRequest(
                name = name.trim(),
                category = category,
                quantity = quantity.coerceAtLeast(1),
                isEssential = isEssential,
                weightKg = weightKg.coerceAtLeast(0.0),
                notes = notes?.trim()
            )
            val result = luggageRepository.updateItem(tripId, itemId, request)
            result.onSuccess { updatedTrip ->
                val current = _uiState.value.trips.map { if (it.id == tripId) updatedTrip else it }
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        trips = current,
                        successMessage = "Item updated"
                    )
                }
                onDataChangedCallback?.invoke()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        errorMessage = err.message ?: "Failed to update item"
                    )
                }
            }
        }
    }

    fun deleteItem(tripId: String, itemId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            val result = luggageRepository.deleteItem(tripId, itemId)
            result.onSuccess { updatedTrip ->
                val current = _uiState.value.trips.map { if (it.id == tripId) updatedTrip else it }
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        trips = current,
                        successMessage = "Item removed"
                    )
                }
                onDataChangedCallback?.invoke()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        errorMessage = err.message ?: "Failed to delete item"
                    )
                }
            }
        }
    }

    fun applyTemplate(tripId: String, templateKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            val result = luggageRepository.applyTemplate(tripId, templateKey)
            result.onSuccess { updatedTrip ->
                val current = _uiState.value.trips.map { if (it.id == tripId) updatedTrip else it }
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        trips = current,
                        successMessage = "Applied $templateKey packing template!"
                    )
                }
                onDataChangedCallback?.invoke()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        errorMessage = err.message ?: "Failed to apply template"
                    )
                }
            }
        }
    }

    fun packAll(tripId: String) {
        val trip = _uiState.value.trips.find { it.id == tripId } ?: return
        viewModelScope.launch {
            trip.items.filter { !it.isPacked }.forEach { item ->
                luggageRepository.toggleItem(tripId, item.id)
            }
            loadTrips(selectTripId = tripId)
            onDataChangedCallback?.invoke()
        }
    }

    fun resetPacking(tripId: String) {
        val trip = _uiState.value.trips.find { it.id == tripId } ?: return
        viewModelScope.launch {
            trip.items.filter { it.isPacked }.forEach { item ->
                luggageRepository.toggleItem(tripId, item.id)
            }
            loadTrips(selectTripId = tripId)
            onDataChangedCallback?.invoke()
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

class LuggageViewModelFactory(
    private val luggageRepository: LuggageRepository,
    private val onDataChangedCallback: (() -> Unit)? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LuggageViewModel::class.java)) {
            return LuggageViewModel(luggageRepository, onDataChangedCallback) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
