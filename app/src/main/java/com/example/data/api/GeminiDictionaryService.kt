package com.example.data.api

import com.example.BuildConfig
import com.example.data.model.WordLookupResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Direct Gemini REST client for Dictionary & Concept Explanations.
 * Compliant with Google AI Studio guidelines (gemini-3.5-flash model).
 */
class GeminiDictionaryService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun lookupWithGemini(word: String, mode: String): Result<WordLookupResult> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Throwable) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "undefined") {
            return@withContext Result.failure(IllegalStateException("No valid Gemini API key configured"))
        }

        val prompt = if (mode == "explain") {
            """
            Explain the concept or word "$word" thoroughly and clearly.
            Return ONLY a valid, raw JSON object (without markdown code fences, backticks, or other text) with this exact schema:
            {
              "word": "$word",
              "phonetic": "phonetic pronunciation",
              "partOfSpeech": "part of speech",
              "shortDefinition": "concise 1-sentence definition",
              "fullDefinition": "detailed explanation of nuances and depth",
              "synonyms": ["3-5 synonyms"],
              "antonyms": ["2-4 antonyms"],
              "examples": ["2-3 practical example sentences"],
              "keyPoints": ["3-4 bullet point conceptual takeaways"],
              "eli5Analogy": "creative, vivid explain-like-I'm-5 analogy",
              "keyTakeaway": "a powerful concluding insight"
            }
            """.trimIndent()
        } else {
            """
            Provide comprehensive dictionary information for the word "$word".
            Return ONLY a valid, raw JSON object (without markdown code fences, backticks, or other text) with this exact schema:
            {
              "word": "$word",
              "phonetic": "phonetic pronunciation",
              "partOfSpeech": "part of speech",
              "shortDefinition": "concise 1-sentence definition",
              "fullDefinition": "detailed definition and practical usage rules",
              "synonyms": ["4-6 synonyms"],
              "antonyms": ["2-4 antonyms"],
              "examples": ["3 diverse example sentences"],
              "keyPoints": ["2-3 practical usage contexts"],
              "eli5Analogy": "simple relatable analogy",
              "keyTakeaway": "core insight"
            }
            """.trimIndent()
        }

        try {
            val requestBodyJson = JSONObject().apply {
                val partsArray = JSONArray().put(JSONObject().put("text", prompt))
                val contentsArray = JSONArray().put(JSONObject().put("parts", partsArray))
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("maxOutputTokens", 1024)
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gemini API Error ${response.code}: $responseBody"))
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""

            if (text.isBlank()) {
                return@withContext Result.failure(Exception("Empty response from Gemini"))
            }

            // Clean up any markdown wrapping if the model returned backticks
            val cleanedJson = text
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val resultJson = JSONObject(cleanedJson)

            val parsed = WordLookupResult(
                word = resultJson.optString("word", word),
                mode = mode,
                phonetic = resultJson.optString("phonetic", null),
                partOfSpeech = resultJson.optString("partOfSpeech", null),
                shortDefinition = resultJson.optString("shortDefinition", null),
                fullDefinition = resultJson.optString("fullDefinition", null),
                synonyms = jsonArrayToList(resultJson.optJSONArray("synonyms")),
                antonyms = jsonArrayToList(resultJson.optJSONArray("antonyms")),
                examples = jsonArrayToList(resultJson.optJSONArray("examples")),
                keyPoints = jsonArrayToList(resultJson.optJSONArray("keyPoints")),
                eli5Analogy = resultJson.optString("eli5Analogy", null),
                keyTakeaway = resultJson.optString("keyTakeaway", null),
                isSaved = false,
                savedWordId = null,
                masteryStatus = null
            )

            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun jsonArrayToList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val item = array.optString(i)
            if (!item.isNullOrBlank()) list.add(item)
        }
        return list
    }
}
