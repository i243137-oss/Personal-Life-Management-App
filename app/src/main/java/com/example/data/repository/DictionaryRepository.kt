package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.api.GeminiDictionaryService
import com.example.data.api.MerriamWebsterDirectService
import com.example.data.local.LocalDataManager
import com.example.data.model.AiAssistantData
import com.example.data.model.AiAssistantRequest
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
    private val geminiService: GeminiDictionaryService = GeminiDictionaryService(),
    private val mwDirectService: MerriamWebsterDirectService = MerriamWebsterDirectService()
) {

    val learnedWordsFlow: Flow<List<WordItemDto>> = localDataManager.wordsFlow

    suspend fun lookupWord(word: String, mode: String = "meaning"): Result<WordLookupResult> {
        val wordTrimmed = word.trim()

        // 1. PRIMARY SOURCE: Direct Merriam-Webster Collegiate Dictionary & Thesaurus
        if (mwDirectService.isConfigured()) {
            val mwResult = mwDirectService.lookupWord(wordTrimmed, mode)
            if (mwResult.isSuccess) {
                val data = mwResult.getOrThrow()
                val localSaved = localDataManager.getLearnedWords().find { it.word.equals(wordTrimmed, ignoreCase = true) }
                return Result.success(
                    data.copy(
                        isSaved = localSaved != null || data.isSaved,
                        savedWordId = localSaved?.id ?: data.savedWordId,
                        masteryStatus = localSaved?.masteryStatus ?: data.masteryStatus
                    )
                )
            } else {
                val errMsg = mwResult.exceptionOrNull()?.message ?: ""
                if (errMsg.contains("Suggestions:") || errMsg.contains("No definition found")) {
                    return Result.failure(Exception(errMsg))
                }
            }
        }

        // 2. SECONDARY SOURCE: Backend Proxy (if running and reachable)
        try {
            val api = apiClient.getDictionaryApiService()
            val response = api.lookupWord(LookupWordRequest(word = wordTrimmed, mode = mode))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success && body.data != null) {
                    val data = body.data
                    val localSaved = localDataManager.getLearnedWords().find { it.word.equals(wordTrimmed, ignoreCase = true) }
                    return Result.success(
                        data.copy(
                            isSaved = localSaved != null || data.isSaved,
                            savedWordId = localSaved?.id ?: data.savedWordId,
                            masteryStatus = localSaved?.masteryStatus ?: data.masteryStatus
                        )
                    )
                } else if (!body.success && body.suggestions.isNotEmpty()) {
                    return Result.failure(Exception("Word not found. Suggestions: ${body.suggestions.take(5).joinToString(", ")}"))
                } else if (!body.message.isNullOrBlank()) {
                    return Result.failure(Exception(body.message))
                }
            }
        } catch (_: Exception) {
        }

        // 2. FALLBACK 1: Direct Gemini REST Client
        val directGeminiResult = geminiService.lookupWithGemini(wordTrimmed, mode)
        if (directGeminiResult.isSuccess) {
            val res = directGeminiResult.getOrThrow()
            val localSaved = localDataManager.getLearnedWords().find { it.word.equals(wordTrimmed, ignoreCase = true) }
            return Result.success(
                res.copy(
                    isSaved = localSaved != null,
                    savedWordId = localSaved?.id,
                    masteryStatus = localSaved?.masteryStatus
                )
            )
        }

        // 3. FALLBACK 2: Local curated offline lexicon
        try {
            val localResult = localDataManager.lookupWord(wordTrimmed, mode)
            return Result.success(localResult)
        } catch (_: Exception) {
        }

        return Result.failure(Exception("Please connect to internet or check server connection"))
    }

    suspend fun getAiAssistant(
        word: String,
        feature: String,
        definition: String? = null,
        context: String? = null
    ): Result<AiAssistantData> {
        try {
            val api = apiClient.getDictionaryApiService()
            val response = api.getAiAssistant(
                AiAssistantRequest(
                    word = word.trim(),
                    feature = feature,
                    definition = definition,
                    context = context
                )
            )
            if (response.isSuccessful && response.body()?.data != null) {
                return Result.success(response.body()!!.data!!)
            }
        } catch (_: Exception) {
        }

        // Fallback educational response
        val fallbackContent = when (feature) {
            "simple_explanation" -> "$word simply means: ${definition ?: "an important academic concept"}. In plain words, it describes an action or state commonly encountered in study and research."
            "urdu_explanation" -> "$word کا عام مفہوم: ${definition ?: "اہم تصور یا اصطلاح"} ہے۔ یہ علمی اور تدریسی سیاق و سباق میں کثرت سے مستعمل ہے۔"
            "academic_context" -> "$word appears frequently in literature reviews, research publications, and thesis argumentation to convey precision."
            "collocations" -> "Common pairings: 'great $word', 'demonstrate $word', 'seek to $word', '$word strategy'."
            "common_mistakes" -> "Avoid confusing $word with phonetically similar words. Check its specific part of speech before drafting."
            "memory_tricks" -> "Mnemonic: Break down '$word' into smaller recognizable syllables to anchor the concept in memory."
            "quiz" -> "Quick Check: How would you use '$word' in a sentence regarding academic problem-solving?"
            else -> definition ?: "No additional guide available."
        }

        return Result.success(
            AiAssistantData(
                word = word,
                feature = feature,
                content = fallbackContent,
                isFallback = true
            )
        )
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
        personalNotes: String? = null,
        relatedWords: List<String> = emptyList(),
        etymology: String? = null,
        audioUrl: String? = null
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
            personalNotes = personalNotes,
            relatedWords = relatedWords,
            etymology = etymology,
            audioUrl = audioUrl
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
                    relatedWords = relatedWords,
                    examples = examples,
                    etymology = etymology,
                    audioUrl = audioUrl,
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
