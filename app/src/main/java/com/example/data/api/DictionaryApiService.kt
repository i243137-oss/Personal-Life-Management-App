package com.example.data.api

import com.example.data.model.AiAssistantRequest
import com.example.data.model.AiAssistantResponse
import com.example.data.model.LookupWordRequest
import com.example.data.model.SaveWordRequest
import com.example.data.model.UpdateMasteryRequest
import com.example.data.model.VocabularyStatsResponse
import com.example.data.model.WordListResponse
import com.example.data.model.WordLookupResponse
import com.example.data.model.WordMutationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DictionaryApiService {

    @GET("api/dictionary/{word}")
    suspend fun getWordByParam(
        @Path("word") word: String
    ): Response<WordLookupResponse>

    @POST("api/dictionary/lookup")
    suspend fun lookupWord(
        @Body request: LookupWordRequest
    ): Response<WordLookupResponse>

    @POST("api/dictionary/ai-assistant")
    suspend fun getAiAssistant(
        @Body request: AiAssistantRequest
    ): Response<AiAssistantResponse>

    @POST("api/dictionary/save")
    suspend fun saveWord(
        @Body request: SaveWordRequest
    ): Response<WordMutationResponse>

    @GET("api/dictionary/words")
    suspend fun getLearnedWords(
        @Query("status") status: String? = null,
        @Query("search") search: String? = null
    ): Response<WordListResponse>

    @PATCH("api/dictionary/words/{id}/mastery")
    suspend fun updateMastery(
        @Path("id") id: String,
        @Body request: UpdateMasteryRequest
    ): Response<WordMutationResponse>

    @DELETE("api/dictionary/words/{id}")
    suspend fun deleteLearnedWord(
        @Path("id") id: String
    ): Response<WordMutationResponse>

    @GET("api/dictionary/stats")
    suspend fun getVocabularyStats(): Response<VocabularyStatsResponse>
}
