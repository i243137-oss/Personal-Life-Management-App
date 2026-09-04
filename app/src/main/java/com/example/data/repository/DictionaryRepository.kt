package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.api.GeminiDictionaryService
import com.example.data.local.LocalDataManager
import com.example.data.model.LookupWordRequest
import com.example.data.model.SaveWordRequest
import com.example.data.model.UpdateMasteryRequest
import com.example.data.model.VocabularyStatsData
import com.example.data.model.WordItemDto
import com.example.data.model.WordLookupResult
import kotlinx.coroutines.flow.Flow

class DictionaryRepository(
    private val apiClient: ApiClient,
    private val localDataManager: LocalDataManager,
    private val geminiService: GeminiDictionaryService = GeminiDictionaryService()
) {

    val learnedWordsFlow: Flow<List<WordItemDto>> = localDataManager.wordsFlow

    suspend fun lookupWord(word: String, mode: String = "meaning"): Result<WordLookupResult> {
        // 1. Try Backend Dictionary API
        try {
            val api = apiClient.getDictionaryApiService()
            val response = api.lookupWord(LookupWordRequest(word = word, mode = mode))
            if (response.isSuccessful && response.body()?.data != null) {
                val data = response.body()!!.data!!
                // Re-check with local manager for current saved status
                val localSaved = localDataManager.getLearnedWords().find { it.word.equals(word.trim(), ignoreCase = true) }
                return Result.success(
                    data.copy(
                        isSaved = localSaved != null || data.isSaved,
                        savedWordId = localSaved?.id ?: data.savedWordId,
                        masteryStatus = localSaved?.masteryStatus ?: data.masteryStatus
                    )
                )
            }
        } catch (_: Exception) {
            // Backend unreachable, continue to direct Gemini or local engine
        }

        // 2. Try Direct Gemini REST Client
        val directGeminiResult = geminiService.lookupWithGemini(word, mode)
        if (directGeminiResult.isSuccess) {
            val res = directGeminiResult.getOrThrow()
            val localSaved = localDataManager.getLearnedWords().find { it.word.equals(word.trim(), ignoreCase = true) }
            return Result.success(
                res.copy(
                    isSaved = localSaved != null,
                    savedWordId = localSaved?.id,
                    masteryStatus = localSaved?.masteryStatus
                )
            )
        }

        // 3. Resilient Local Smart Dictionary Engine
        val localResult = localDataManager.lookupWord(word, mode)
        return Result.success(localResult)
    }

    suspend fun saveWord(
        word: String,
        mode: String,
        phonetic: String?,
        partOfSpeech: String?,
        shortDefinition: String?,
        fullDefinition: String?,
        synonyms: List<String>,
        antonyms: List<String>,
        examples: List<String>,
        keyPoints: List<String>,
        eli5Analogy: String?,
        keyTakeaway: String?,
        masteryStatus: String = "learning",
        personalNotes: String? = null
    ): Result<WordItemDto> {
        // Always save locally first for immediate responsiveness
        val localItem = localDataManager.saveWord(
            word = word,
            mode = mode,
            phonetic = phonetic,
            partOfSpeech = partOfSpeech,
            shortDefinition = shortDefinition,
            fullDefinition = fullDefinition,
            synonyms = synonyms,
            antonyms = antonyms,
            examples = examples,
            keyPoints = keyPoints,
            eli5Analogy = eli5Analogy,
            keyTakeaway = keyTakeaway,
            masteryStatus = masteryStatus,
            personalNotes = personalNotes
        )

        // Best effort sync with backend
        try {
            val api = apiClient.getDictionaryApiService()
            api.saveWord(
                SaveWordRequest(
                    word = word,
                    mode = mode,
                    phonetic = phonetic,
                    partOfSpeech = partOfSpeech,
                    shortDefinition = shortDefinition,
                    fullDefinition = fullDefinition,
                    synonyms = synonyms,
                    antonyms = antonyms,
                    examples = examples,
                    keyPoints = keyPoints,
                    eli5Analogy = eli5Analogy,
                    keyTakeaway = keyTakeaway,
                    masteryStatus = masteryStatus,
                    personalNotes = personalNotes
                )
            )
        } catch (_: Exception) {
        }

        return Result.success(localItem)
    }

    suspend fun getLearnedWords(status: String? = null, search: String? = null): Result<List<WordItemDto>> {
        try {
            val api = apiClient.getDictionaryApiService()
            val response = api.getLearnedWords(status = status, search = search)
            if (response.isSuccessful && response.body()?.data != null) {
                return Result.success(response.body()!!.data)
            }
        } catch (_: Exception) {
        }
        return Result.success(localDataManager.getLearnedWords(status = status, search = search))
    }

    suspend fun updateMastery(id: String, status: String, personalNotes: String? = null): Result<WordItemDto> {
        val localUpdated = localDataManager.updateWordMastery(id, status, personalNotes)
        try {
            val api = apiClient.getDictionaryApiService()
            api.updateMastery(id, UpdateMasteryRequest(status = status, personalNotes = personalNotes))
        } catch (_: Exception) {
        }
        return if (localUpdated != null) {
            Result.success(localUpdated)
        } else {
            Result.failure(Exception("Word not found"))
        }
    }

    suspend fun deleteLearnedWord(id: String): Result<Boolean> {
        val removed = localDataManager.deleteLearnedWord(id)
        try {
            val api = apiClient.getDictionaryApiService()
            api.deleteLearnedWord(id)
        } catch (_: Exception) {
        }
        return Result.success(removed)
    }

    suspend fun getVocabularyStats(): Result<VocabularyStatsData> {
        try {
            val api = apiClient.getDictionaryApiService()
            val response = api.getVocabularyStats()
            if (response.isSuccessful && response.body()?.data != null) {
                return Result.success(response.body()!!.data!!)
            }
        } catch (_: Exception) {
        }
        return Result.success(localDataManager.getVocabularyStats())
    }
}
