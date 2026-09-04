package com.example.data.api

import com.example.data.model.CreateNoteRequest
import com.example.data.model.NoteListResponse
import com.example.data.model.NoteMutationResponse
import com.example.data.model.NoteSingleResponse
import com.example.data.model.UpdateNoteRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface NoteApiService {

    @GET("api/notes")
    suspend fun getNotes(
        @Query("category") category: String? = null,
        @Query("search") search: String? = null,
        @Query("isPinned") isPinned: Boolean? = null,
        @Query("isArchived") isArchived: Boolean? = null,
        @Query("tag") tag: String? = null
    ): Response<NoteListResponse>

    @GET("api/notes/{id}")
    suspend fun getNoteById(@Path("id") id: String): Response<NoteSingleResponse>

    @POST("api/notes")
    suspend fun createNote(@Body request: CreateNoteRequest): Response<NoteSingleResponse>

    @PUT("api/notes/{id}")
    suspend fun updateNote(
        @Path("id") id: String,
        @Body request: UpdateNoteRequest
    ): Response<NoteSingleResponse>

    @DELETE("api/notes/{id}")
    suspend fun deleteNote(@Path("id") id: String): Response<NoteMutationResponse>

    @PATCH("api/notes/{id}/pin")
    suspend fun togglePin(@Path("id") id: String): Response<NoteSingleResponse>

    @PATCH("api/notes/{id}/archive")
    suspend fun toggleArchive(@Path("id") id: String): Response<NoteSingleResponse>
}
