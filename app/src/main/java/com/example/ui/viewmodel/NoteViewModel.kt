package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChecklistItemDto
import com.example.data.model.CreateNoteRequest
import com.example.data.model.NoteDto
import com.example.data.model.UpdateNoteRequest
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteUiState(
    val isLoading: Boolean = false,
    val notes: List<NoteDto> = emptyList(),
    val pinnedNotes: List<NoteDto> = emptyList(),
    val otherNotes: List<NoteDto> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val isArchiveView: Boolean = false,
    val userMessage: String? = null,
    val errorMessage: String? = null
)

class NoteViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteUiState(isLoading = true))
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    init {
        // Collect reactive updates from repository
        viewModelScope.launch {
            repository.notesFlow.collect { allNotes ->
                applyFilters(
                    allNotes = allNotes,
                    search = _uiState.value.searchQuery,
                    category = _uiState.value.selectedCategory,
                    isArchive = _uiState.value.isArchiveView
                )
            }
        }
        loadNotes()
    }

    fun loadNotes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.getNotes(
                category = _uiState.value.selectedCategory,
                search = _uiState.value.searchQuery,
                isArchived = _uiState.value.isArchiveView
            )
            result.onSuccess { list ->
                applyFilters(
                    allNotes = list,
                    search = _uiState.value.searchQuery,
                    category = _uiState.value.selectedCategory,
                    isArchive = _uiState.value.isArchiveView
                )
                _uiState.update { it.copy(isLoading = false) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadNotes()
    }

    fun setSelectedCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        loadNotes()
    }

    fun toggleArchiveView() {
        val newArchiveView = !_uiState.value.isArchiveView
        _uiState.update { it.copy(isArchiveView = newArchiveView) }
        loadNotes()
    }

    fun createNote(
        title: String,
        content: String,
        category: String,
        tags: List<String>,
        isPinned: Boolean,
        colorHex: String,
        checklist: List<ChecklistItemDto>
    ) {
        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title cannot be blank") }
            return
        }

        viewModelScope.launch {
            val req = CreateNoteRequest(
                title = title.trim(),
                content = content.trim(),
                category = category,
                tags = tags,
                isPinned = isPinned,
                colorHex = colorHex,
                checklist = checklist
            )
            val res = repository.createNote(req)
            res.onSuccess {
                _uiState.update { it.copy(userMessage = "Note saved") }
                loadNotes()
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message ?: "Failed to save note") }
            }
        }
    }

    fun updateNote(
        id: String,
        title: String,
        content: String,
        category: String,
        tags: List<String>,
        isPinned: Boolean,
        colorHex: String,
        checklist: List<ChecklistItemDto>
    ) {
        viewModelScope.launch {
            val req = UpdateNoteRequest(
                title = title.trim(),
                content = content.trim(),
                category = category,
                tags = tags,
                isPinned = isPinned,
                colorHex = colorHex,
                checklist = checklist
            )
            val res = repository.updateNote(id, req)
            res.onSuccess {
                _uiState.update { it.copy(userMessage = "Note updated") }
                loadNotes()
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message ?: "Failed to update note") }
            }
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            val res = repository.deleteNote(id)
            res.onSuccess {
                _uiState.update { it.copy(userMessage = "Note deleted") }
                loadNotes()
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message ?: "Failed to delete note") }
            }
        }
    }

    fun togglePin(id: String) {
        viewModelScope.launch {
            val res = repository.togglePin(id)
            res.onSuccess { updated ->
                val msg = if (updated.isPinned) "Note pinned to top" else "Note unpinned"
                _uiState.update { it.copy(userMessage = msg) }
                loadNotes()
            }
        }
    }

    fun toggleArchive(id: String) {
        viewModelScope.launch {
            val res = repository.toggleArchive(id)
            res.onSuccess { updated ->
                val msg = if (updated.isArchived) "Note archived" else "Note unarchived"
                _uiState.update { it.copy(userMessage = msg) }
                loadNotes()
            }
        }
    }

    fun toggleChecklistItem(noteId: String, itemId: String) {
        viewModelScope.launch {
            repository.toggleChecklistItem(noteId, itemId)
            loadNotes()
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(userMessage = null, errorMessage = null) }
    }

    private fun applyFilters(
        allNotes: List<NoteDto>,
        search: String,
        category: String,
        isArchive: Boolean
    ) {
        val filtered = allNotes.filter { note ->
            val matchesArchive = note.isArchived == isArchive
            val matchesCategory = category == "All" || note.category.equals(category, ignoreCase = true)
            val matchesSearch = if (search.isBlank()) true else {
                note.title.contains(search, ignoreCase = true) ||
                note.content.contains(search, ignoreCase = true) ||
                note.tags.any { it.contains(search, ignoreCase = true) }
            }
            matchesArchive && matchesCategory && matchesSearch
        }

        val pinned = filtered.filter { it.isPinned }
        val other = filtered.filter { !it.isPinned }

        _uiState.update {
            it.copy(
                notes = filtered,
                pinnedNotes = pinned,
                otherNotes = other
            )
        }
    }
}

class NoteViewModelFactory(
    private val repository: NoteRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            return NoteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
