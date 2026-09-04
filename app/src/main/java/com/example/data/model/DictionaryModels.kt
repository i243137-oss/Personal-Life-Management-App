package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WordItemDto(
    @Json(name = "_id") val id: String = "",
    @Json(name = "word") val word: String = "",
    @Json(name = "mode") val mode: String = "meaning",
    @Json(name = "phonetic") val phonetic: String? = null,
    @Json(name = "partOfSpeech") val partOfSpeech: String? = null,
    @Json(name = "shortDefinition") val shortDefinition: String? = null,
    @Json(name = "fullDefinition") val fullDefinition: String? = null,
    @Json(name = "synonyms") val synonyms: List<String> = emptyList(),
    @Json(name = "antonyms") val antonyms: List<String> = emptyList(),
    @Json(name = "examples") val examples: List<String> = emptyList(),
    @Json(name = "keyPoints") val keyPoints: List<String> = emptyList(),
    @Json(name = "eli5Analogy") val eli5Analogy: String? = null,
    @Json(name = "keyTakeaway") val keyTakeaway: String? = null,
    @Json(name = "masteryStatus") val masteryStatus: String = "learning", // "learning", "reviewing", "mastered"
    @Json(name = "personalNotes") val personalNotes: String? = null,
    @Json(name = "createdAt") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class WordLookupResult(
    @Json(name = "word") val word: String = "",
    @Json(name = "mode") val mode: String = "meaning",
    @Json(name = "phonetic") val phonetic: String? = null,
    @Json(name = "partOfSpeech") val partOfSpeech: String? = null,
    @Json(name = "shortDefinition") val shortDefinition: String? = null,
    @Json(name = "fullDefinition") val fullDefinition: String? = null,
    @Json(name = "synonyms") val synonyms: List<String> = emptyList(),
    @Json(name = "antonyms") val antonyms: List<String> = emptyList(),
    @Json(name = "examples") val examples: List<String> = emptyList(),
    @Json(name = "keyPoints") val keyPoints: List<String> = emptyList(),
    @Json(name = "eli5Analogy") val eli5Analogy: String? = null,
    @Json(name = "keyTakeaway") val keyTakeaway: String? = null,
    @Json(name = "isSaved") val isSaved: Boolean = false,
    @Json(name = "savedWordId") val savedWordId: String? = null,
    @Json(name = "masteryStatus") val masteryStatus: String? = null
)

@JsonClass(generateAdapter = true)
data class LookupWordRequest(
    @Json(name = "word") val word: String,
    @Json(name = "mode") val mode: String = "meaning"
)

@JsonClass(generateAdapter = true)
data class SaveWordRequest(
    @Json(name = "word") val word: String,
    @Json(name = "mode") val mode: String = "meaning",
    @Json(name = "phonetic") val phonetic: String? = null,
    @Json(name = "partOfSpeech") val partOfSpeech: String? = null,
    @Json(name = "shortDefinition") val shortDefinition: String? = null,
    @Json(name = "fullDefinition") val fullDefinition: String? = null,
    @Json(name = "synonyms") val synonyms: List<String> = emptyList(),
    @Json(name = "antonyms") val antonyms: List<String> = emptyList(),
    @Json(name = "examples") val examples: List<String> = emptyList(),
    @Json(name = "keyPoints") val keyPoints: List<String> = emptyList(),
    @Json(name = "eli5Analogy") val eli5Analogy: String? = null,
    @Json(name = "keyTakeaway") val keyTakeaway: String? = null,
    @Json(name = "masteryStatus") val masteryStatus: String = "learning",
    @Json(name = "personalNotes") val personalNotes: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateMasteryRequest(
    @Json(name = "status") val status: String,
    @Json(name = "personalNotes") val personalNotes: String? = null
)

@JsonClass(generateAdapter = true)
data class WordLookupResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: WordLookupResult? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class WordListResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "count") val count: Int = 0,
    @Json(name = "data") val data: List<WordItemDto> = emptyList(),
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class WordMutationResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: WordItemDto? = null
)

@JsonClass(generateAdapter = true)
data class VocabularyStatsData(
    @Json(name = "total") val total: Int = 0,
    @Json(name = "mastered") val mastered: Int = 0,
    @Json(name = "reviewing") val reviewing: Int = 0,
    @Json(name = "learning") val learning: Int = 0,
    @Json(name = "masteryPercentage") val masteryPercentage: Int = 0
)

@JsonClass(generateAdapter = true)
data class VocabularyStatsResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: VocabularyStatsData? = null,
    @Json(name = "message") val message: String? = null
)
