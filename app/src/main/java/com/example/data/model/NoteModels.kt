package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChecklistItemDto(
    @Json(name = "_id") val id: String = "",
    @Json(name = "text") val text: String = "",
    @Json(name = "isDone") val isDone: Boolean = false
)

@JsonClass(generateAdapter = true)
data class NoteDto(
    @Json(name = "_id") val id: String = "",
    @Json(name = "userId") val userId: String? = null,
    @Json(name = "title") val title: String = "",
    @Json(name = "content") val content: String = "",
    @Json(name = "category") val category: String = "Personal",
    @Json(name = "tags") val tags: List<String> = emptyList(),
    @Json(name = "isPinned") val isPinned: Boolean = false,
    @Json(name = "isArchived") val isArchived: Boolean = false,
    @Json(name = "colorHex") val colorHex: String = "#FEF3C7",
    @Json(name = "checklist") val checklist: List<ChecklistItemDto> = emptyList(),
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class NoteListResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "count") val count: Int = 0,
    @Json(name = "data") val data: List<NoteDto> = emptyList(),
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class NoteSingleResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: NoteDto? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class NoteMutationResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateNoteRequest(
    @Json(name = "title") val title: String,
    @Json(name = "content") val content: String = "",
    @Json(name = "category") val category: String = "Personal",
    @Json(name = "tags") val tags: List<String> = emptyList(),
    @Json(name = "isPinned") val isPinned: Boolean = false,
    @Json(name = "isArchived") val isArchived: Boolean = false,
    @Json(name = "colorHex") val colorHex: String = "#FEF3C7",
    @Json(name = "checklist") val checklist: List<ChecklistItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class UpdateNoteRequest(
    @Json(name = "title") val title: String? = null,
    @Json(name = "content") val content: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "tags") val tags: List<String>? = null,
    @Json(name = "isPinned") val isPinned: Boolean? = null,
    @Json(name = "isArchived") val isArchived: Boolean? = null,
    @Json(name = "colorHex") val colorHex: String? = null,
    @Json(name = "checklist") val checklist: List<ChecklistItemDto>? = null
)

enum class NoteCategory(val displayName: String) {
    ALL("All"),
    PERSONAL("Personal"),
    WORK("Work"),
    IDEAS("Ideas"),
    TRAVEL("Travel"),
    FINANCES("Finances"),
    CHECKLIST("Checklist"),
    OTHER("Other")
}

data class NoteColor(
    val name: String,
    val hex: String
)

val NOTE_COLORS = listOf(
    NoteColor("Sunny Yellow", "#FEF3C7"),
    NoteColor("Sky Blue", "#DBEAFE"),
    NoteColor("Mint Green", "#D1FAE5"),
    NoteColor("Lavender", "#EDE9FE"),
    NoteColor("Peach Rose", "#FFE4E6"),
    NoteColor("Slate White", "#F1F5F9")
)
