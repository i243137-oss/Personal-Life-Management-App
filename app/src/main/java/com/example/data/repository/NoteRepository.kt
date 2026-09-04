package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.local.LocalDataManager
import com.example.data.model.CreateNoteRequest
import com.example.data.model.NoteDto
import com.example.data.model.UpdateNoteRequest
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val apiClient: ApiClient,
    private val localDataManager: LocalDataManager
) {
    val notesFlow: Flow<List<NoteDto>> = localDataManager.notesFlow

    suspend fun getNotes(
        category: String? = null,
        search: String? = null,
        isArchived: Boolean = false,
        isPinned: Boolean? = null
    ): Result<List<NoteDto>> {
        return try {
            val response = apiClient.getNoteApiService().getNotes(
                category = if (category == "All") null else category,
                search = search,
                isPinned = isPinned,
                isArchived = isArchived
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val remoteNotes = response.body()?.data ?: emptyList()
                if (remoteNotes.isNotEmpty() || (search.isNullOrBlank() && (category == null || category == "All"))) {
                    // Update local storage with remote notes if available
                    if (!isArchived && search.isNullOrBlank() && (category == null || category == "All")) {
                        localDataManager.saveNotes(remoteNotes)
                    }
                }
                Result.success(remoteNotes)
            } else {
                // Fallback to local
                val localNotes = localDataManager.getNotes(category, search, isArchived, isPinned)
                Result.success(localNotes)
            }
        } catch (e: Exception) {
            // Offline fallback
            val localNotes = localDataManager.getNotes(category, search, isArchived, isPinned)
            Result.success(localNotes)
        }
    }

    suspend fun createNote(request: CreateNoteRequest): Result<NoteDto> {
        // Save locally first for instant responsive UI
        val localCreated = localDataManager.createNote(request)

        return try {
            val response = apiClient.getNoteApiService().createNote(request)
            if (response.isSuccessful && response.body()?.data != null) {
                val remoteNote = response.body()!!.data!!
                // Replace local optimistic note with server note
                val current = localDataManager.getNotes(isArchived = false).toMutableList()
                val idx = current.indexOfFirst { it.id == localCreated.id }
                if (idx != -1) {
                    current[idx] = remoteNote
                    localDataManager.saveNotes(current)
                }
                Result.success(remoteNote)
            } else {
                Result.success(localCreated)
            }
        } catch (e: Exception) {
            Result.success(localCreated)
        }
    }

    suspend fun updateNote(id: String, request: UpdateNoteRequest): Result<NoteDto> {
        val localUpdated = localDataManager.updateNote(id, request)

        return try {
            val response = apiClient.getNoteApiService().updateNote(id, request)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                if (localUpdated != null) Result.success(localUpdated)
                else Result.failure(Exception("Note not found"))
            }
        } catch (e: Exception) {
            if (localUpdated != null) Result.success(localUpdated)
            else Result.failure(e)
        }
    }

    suspend fun deleteNote(id: String): Result<Unit> {
        localDataManager.deleteNote(id)

        return try {
            val response = apiClient.getNoteApiService().deleteNote(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.success(Unit) // Local deletion succeeded
            }
        } catch (e: Exception) {
            Result.success(Unit) // Offline deletion
        }
    }

    suspend fun togglePin(id: String): Result<NoteDto> {
        val localUpdated = localDataManager.togglePinNote(id)

        return try {
            val response = apiClient.getNoteApiService().togglePin(id)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                if (localUpdated != null) Result.success(localUpdated)
                else Result.failure(Exception("Note not found"))
            }
        } catch (e: Exception) {
            if (localUpdated != null) Result.success(localUpdated)
            else Result.failure(e)
        }
    }

    suspend fun toggleArchive(id: String): Result<NoteDto> {
        val localUpdated = localDataManager.toggleArchiveNote(id)

        return try {
            val response = apiClient.getNoteApiService().toggleArchive(id)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                if (localUpdated != null) Result.success(localUpdated)
                else Result.failure(Exception("Note not found"))
            }
        } catch (e: Exception) {
            if (localUpdated != null) Result.success(localUpdated)
            else Result.failure(e)
        }
    }

    suspend fun toggleChecklistItem(noteId: String, itemId: String): Result<NoteDto> {
        val localUpdated = localDataManager.toggleChecklistItem(noteId, itemId)
        if (localUpdated != null) {
            // Also push updated checklist to server in background
            try {
                apiClient.getNoteApiService().updateNote(
                    noteId,
                    UpdateNoteRequest(checklist = localUpdated.checklist)
                )
            } catch (_: Exception) {
            }
            return Result.success(localUpdated)
        }
        return Result.failure(Exception("Note or checklist item not found"))
    }
}
