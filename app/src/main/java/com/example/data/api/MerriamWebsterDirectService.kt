package com.example.data.api

import com.example.BuildConfig
import com.example.data.model.MwAudioItem
import com.example.data.model.MwDefinitionItem
import com.example.data.model.MwDictionaryDetails
import com.example.data.model.MwIdiomItem
import com.example.data.model.MwPronunciation
import com.example.data.model.MwSourceDetails
import com.example.data.model.MwThesaurusDetails
import com.example.data.model.WordLookupResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Direct HTTPS client for Merriam-Webster Collegiate Dictionary & Thesaurus APIs.
 * Uses BuildConfig.MW_DICTIONARY_API_KEY and BuildConfig.MW_THESAURUS_API_KEY securely injected
 * via the Secrets Gradle Plugin from the AI Studio Secrets panel / .env.
 */
class MerriamWebsterDirectService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean {
        val dictKey = getDictionaryApiKey()
        return dictKey.isNotBlank() &&
                dictKey != "your_mw_dictionary_api_key_here" &&
                dictKey != "YOUR_MW_DICTIONARY_API_KEY" &&
                dictKey != "undefined"
    }

    private fun getDictionaryApiKey(): String {
        return try {
            BuildConfig.MW_DICTIONARY_API_KEY
        } catch (_: Throwable) {
            ""
        }.trim()
    }

    private fun getThesaurusApiKey(): String {
        return try {
            BuildConfig.MW_THESAURUS_API_KEY
        } catch (_: Throwable) {
            ""
        }.trim()
    }

    suspend fun lookupWord(word: String, mode: String = "meaning"): Result<WordLookupResult> = withContext(Dispatchers.IO) {
        val dictKey = getDictionaryApiKey()
        if (dictKey.isBlank() || dictKey == "your_mw_dictionary_api_key_here" || dictKey == "YOUR_MW_DICTIONARY_API_KEY") {
            return@withContext Result.failure(IllegalStateException("MW_DICTIONARY_API_KEY is not configured"))
        }

        val encodedWord = withContext(Dispatchers.IO) {
            URLEncoder.encode(word.trim(), "UTF-8")
        }

        val dictUrl = "https://www.dictionaryapi.com/api/v3/references/collegiate/json/$encodedWord?key=$dictKey"
        val thesKey = getThesaurusApiKey()
        val hasThesKey = thesKey.isNotBlank() &&
                thesKey != "your_mw_thesaurus_api_key_here" &&
                thesKey != "YOUR_MW_THESAURUS_API_KEY" &&
                thesKey != "undefined"
        val thesUrl = if (hasThesKey) {
            "https://www.dictionaryapi.com/api/v3/references/thesaurus/json/$encodedWord?key=$thesKey"
        } else null

        // Query Dictionary
        var dictResponseJson: String? = null
        var dictSuccess = false
        try {
            val req = Request.Builder().url(dictUrl).get().build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                dictResponseJson = resp.body?.string()
                dictSuccess = true
            }
        } catch (_: Exception) {
        }

        // Query Thesaurus
        var thesResponseJson: String? = null
        var thesSuccess = false
        if (thesUrl != null) {
            try {
                val req = Request.Builder().url(thesUrl).get().build()
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    thesResponseJson = resp.body?.string()
                    thesSuccess = true
                }
            } catch (_: Exception) {
            }
        }

        if (!dictSuccess && !thesSuccess) {
            return@withContext Result.failure(Exception("Unable to reach Merriam-Webster APIs. Check internet connection."))
        }

        // Parse Collegiate Dictionary
        val dictParsed = parseDictionaryResponse(dictResponseJson, word)
        if (dictParsed is DictParseResult.Suggestions && !thesSuccess) {
            return@withContext Result.failure(
                Exception("Word not found. Suggestions: ${dictParsed.list.take(5).joinToString(", ")}")
            )
        }

        val dictData = if (dictParsed is DictParseResult.Success) dictParsed.data else null

        // Parse Collegiate Thesaurus
        val thesData = parseThesaurusResponse(thesResponseJson)

        if (dictData == null && thesData == null) {
            return@withContext Result.failure(Exception("No definition found for \"$word\" in Merriam-Webster Collegiate."))
        }

        val primaryWord = dictData?.word?.takeIf { it.isNotBlank() } ?: word.trim()
        val shortDef = dictData?.definitions?.firstOrNull()?.text
            ?: dictData?.examples?.firstOrNull()
            ?: "Authoritative entry from Merriam-Webster Collegiate."
        val fullDef = dictData?.definitions?.getOrNull(1)?.text
            ?: dictData?.definitions?.firstOrNull()?.text
            ?: shortDef

        val combinedSynonyms = mutableListOf<String>()
        thesData?.synonyms?.let { combinedSynonyms.addAll(it) }
        val combinedAntonyms = mutableListOf<String>()
        thesData?.antonyms?.let { combinedAntonyms.addAll(it) }
        val combinedRelated = mutableListOf<String>()
        thesData?.relatedWords?.let { combinedRelated.addAll(it) }

        val combinedExamples = mutableListOf<String>()
        dictData?.examples?.let { combinedExamples.addAll(it) }
        thesData?.examples?.let { exList ->
            exList.forEach { if (!combinedExamples.contains(it)) combinedExamples.add(it) }
        }

        val primaryAudio = dictData?.audio?.firstOrNull()?.url
        val primaryPhonetic = dictData?.pronunciation?.written
            ?: dictData?.pronunciation?.ipa

        val keyPoints = dictData?.definitions?.take(3)?.mapIndexed { i, d ->
            "Sense ${i + 1} (${d.partOfSpeech ?: "general"}): ${d.text}"
        } ?: emptyList()

        val eli5Analogy = "In Merriam-Webster Collegiate: $primaryWord functions as a ${dictData?.partsOfSpeech?.firstOrNull() ?: "standard term"} defining $shortDef"
        val keyTakeaway = "Essential vocabulary defined by Merriam-Webster with verified pronunciation and collegiate usage nuances."

        val finalResult = WordLookupResult(
            word = primaryWord,
            mode = mode,
            phonetic = primaryPhonetic,
            partOfSpeech = dictData?.partsOfSpeech?.firstOrNull() ?: "word",
            shortDefinition = shortDef,
            fullDefinition = fullDef,
            synonyms = combinedSynonyms.distinct().take(12),
            antonyms = combinedAntonyms.distinct().take(10),
            relatedWords = combinedRelated.distinct().take(10),
            examples = combinedExamples.distinct().take(6),
            etymology = dictData?.etymology,
            audioUrl = primaryAudio,
            keyPoints = keyPoints,
            eli5Analogy = eli5Analogy,
            keyTakeaway = keyTakeaway,
            isSaved = false,
            savedWordId = null,
            masteryStatus = null,
            dictionary = dictData,
            thesaurus = thesData,
            source = MwSourceDetails(
                provider = "merriam_webster",
                attribution = "Merriam-Webster Collegiate® Dictionary & Thesaurus",
                dictionarySuccess = dictData != null,
                thesaurusSuccess = thesData != null,
                cached = false
            ),
            suggestions = if (dictParsed is DictParseResult.Suggestions) dictParsed.list else emptyList()
        )

        Result.success(finalResult)
    }

    private sealed class DictParseResult {
        data class Success(val data: MwDictionaryDetails) : DictParseResult()
        data class Suggestions(val list: List<String>) : DictParseResult()
        object Empty : DictParseResult()
    }

    private fun parseDictionaryResponse(jsonString: String?, queryWord: String): DictParseResult {
        if (jsonString.isNullOrBlank()) return DictParseResult.Empty
        try {
            val rootArray = JSONArray(jsonString)
            if (rootArray.length() == 0) return DictParseResult.Empty

            // Check if MW returned a list of spelling suggestions (strings instead of objects)
            if (rootArray.optJSONObject(0) == null) {
                val suggestions = mutableListOf<String>()
                for (i in 0 until rootArray.length()) {
                    val s = rootArray.optString(i)
                    if (!s.isNullOrBlank()) suggestions.add(s)
                }
                return DictParseResult.Suggestions(suggestions)
            }

            val definitions = mutableListOf<MwDefinitionItem>()
            val examples = mutableListOf<String>()
            val partsOfSpeech = mutableListOf<String>()
            val audioList = mutableListOf<MwAudioItem>()
            var wordHeadword = queryWord
            var syllables: String? = null
            var writtenPron: String? = null
            var etymology: String? = null

            for (i in 0 until rootArray.length()) {
                val entryObj = rootArray.optJSONObject(i) ?: continue

                val fl = entryObj.optString("fl")
                if (fl.isNotBlank() && !partsOfSpeech.contains(fl)) {
                    partsOfSpeech.add(fl)
                }

                // Headword & Pronunciations
                val hwi = entryObj.optJSONObject("hwi")
                if (hwi != null) {
                    val rawHw = hwi.optString("hw")
                    if (rawHw.isNotBlank() && wordHeadword == queryWord) {
                        wordHeadword = rawHw.replace("*", "")
                        syllables = rawHw.replace("*", "·")
                    }

                    val prs = hwi.optJSONArray("prs")
                    if (prs != null && prs.length() > 0) {
                        for (p in 0 until prs.length()) {
                            val pObj = prs.optJSONObject(p) ?: continue
                            val mw = pObj.optString("mw")
                            if (mw.isNotBlank() && writtenPron == null) {
                                writtenPron = "\\ $mw \\"
                            }
                            val sound = pObj.optJSONObject("sound")
                            if (sound != null) {
                                val audioFile = sound.optString("audio")
                                val audioUrl = constructAudioUrl(audioFile)
                                if (audioUrl != null) {
                                    audioList.add(MwAudioItem(audio = audioFile, url = audioUrl, written = mw))
                                }
                            }
                        }
                    }
                }

                // Etymology
                val etArray = entryObj.optJSONArray("et")
                if (etArray != null && etArray.length() > 0 && etymology == null) {
                    val etBuilder = StringBuilder()
                    for (e in 0 until etArray.length()) {
                        val item = etArray.optJSONArray(e) ?: continue
                        if (item.length() >= 2 && item.optString(0) == "text") {
                            val rawText = item.optString(1)
                            val cleaned = cleanMwMarkup(rawText)
                            if (cleaned.isNotBlank()) etBuilder.append(cleaned).append(" ")
                        }
                    }
                    if (etBuilder.isNotBlank()) etymology = etBuilder.toString().trim()
                }

                // Senses & Definitions
                val defArray = entryObj.optJSONArray("def")
                if (defArray != null) {
                    for (d in 0 until defArray.length()) {
                        val defObj = defArray.optJSONObject(d) ?: continue
                        val sseq = defObj.optJSONArray("sseq") ?: continue

                        for (s in 0 until sseq.length()) {
                            val senseGroup = sseq.optJSONArray(s) ?: continue
                            for (sg in 0 until senseGroup.length()) {
                                val senseTuple = senseGroup.optJSONArray(sg) ?: continue
                                if (senseTuple.length() < 2) continue
                                val tag = senseTuple.optString(0)
                                val data = senseTuple.optJSONObject(1) ?: continue

                                if (tag == "sense") {
                                    val dt = data.optJSONArray("dt") ?: continue
                                    var senseText = ""
                                    val senseExamples = mutableListOf<String>()

                                    for (dtIdx in 0 until dt.length()) {
                                        val dtItem = dt.optJSONArray(dtIdx) ?: continue
                                        if (dtItem.length() < 2) continue
                                        val dtType = dtItem.optString(0)

                                        if (dtType == "text") {
                                            val t = dtItem.optString(1)
                                            val cleaned = cleanMwMarkup(t)
                                            if (cleaned.isNotBlank()) {
                                                senseText = if (senseText.isBlank()) cleaned else "$senseText $cleaned"
                                            }
                                        } else if (dtType == "vis") {
                                            val visArray = dtItem.optJSONArray(1)
                                            if (visArray != null) {
                                                for (v in 0 until visArray.length()) {
                                                    val vObj = visArray.optJSONObject(v) ?: continue
                                                    val vt = cleanMwMarkup(vObj.optString("t"))
                                                    if (vt.isNotBlank()) {
                                                        senseExamples.add(vt)
                                                        if (!examples.contains(vt)) examples.add(vt)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (senseText.isNotBlank()) {
                                        definitions.add(
                                            MwDefinitionItem(
                                                partOfSpeech = fl.ifBlank { "definition" },
                                                text = senseText,
                                                examples = senseExamples
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (definitions.isEmpty() && rootArray.length() > 0) {
                // Fallback to shortdef if complex sseq parsing was empty
                val firstObj = rootArray.optJSONObject(0)
                val shortDefs = firstObj?.optJSONArray("shortdef")
                if (shortDefs != null) {
                    for (sd in 0 until shortDefs.length()) {
                        val text = shortDefs.optString(sd)
                        if (text.isNotBlank()) {
                            definitions.add(
                                MwDefinitionItem(
                                    partOfSpeech = partsOfSpeech.firstOrNull() ?: "definition",
                                    text = text,
                                    examples = emptyList()
                                )
                            )
                        }
                    }
                }
            }

            val details = MwDictionaryDetails(
                word = wordHeadword,
                syllables = syllables,
                pronunciation = MwPronunciation(written = writtenPron, ipa = null),
                definitions = definitions,
                examples = examples,
                partsOfSpeech = partsOfSpeech,
                etymology = etymology,
                audio = audioList,
                idioms = emptyList()
            )

            return DictParseResult.Success(details)
        } catch (_: Exception) {
            return DictParseResult.Empty
        }
    }

    private fun parseThesaurusResponse(jsonString: String?): MwThesaurusDetails? {
        if (jsonString.isNullOrBlank()) return null
        try {
            val rootArray = JSONArray(jsonString)
            if (rootArray.length() == 0 || rootArray.optJSONObject(0) == null) return null

            val synonyms = mutableListOf<String>()
            val antonyms = mutableListOf<String>()
            val relatedWords = mutableListOf<String>()
            val similarWords = mutableListOf<String>()
            val examples = mutableListOf<String>()

            for (i in 0 until rootArray.length()) {
                val entryObj = rootArray.optJSONObject(i) ?: continue

                // 1. Check meta syns and ants
                val meta = entryObj.optJSONObject("meta")
                if (meta != null) {
                    val metaSyns = meta.optJSONArray("syns")
                    if (metaSyns != null) {
                        for (s in 0 until metaSyns.length()) {
                            val group = metaSyns.optJSONArray(s) ?: continue
                            for (g in 0 until group.length()) {
                                val item = group.optString(g)
                                if (item.isNotBlank() && !synonyms.contains(item)) synonyms.add(item)
                            }
                        }
                    }

                    val metaAnts = meta.optJSONArray("ants")
                    if (metaAnts != null) {
                        for (a in 0 until metaAnts.length()) {
                            val group = metaAnts.optJSONArray(a) ?: continue
                            for (g in 0 until group.length()) {
                                val item = group.optString(g)
                                if (item.isNotBlank() && !antonyms.contains(item)) antonyms.add(item)
                            }
                        }
                    }
                }

                // 2. Deep senses in def[].sseq
                val defArray = entryObj.optJSONArray("def") ?: continue
                for (d in 0 until defArray.length()) {
                    val defObj = defArray.optJSONObject(d) ?: continue
                    val sseq = defObj.optJSONArray("sseq") ?: continue

                    for (s in 0 until sseq.length()) {
                        val senseGroup = sseq.optJSONArray(s) ?: continue
                        for (sg in 0 until senseGroup.length()) {
                            val senseTuple = senseGroup.optJSONArray(sg) ?: continue
                            if (senseTuple.length() < 2) continue
                            val data = senseTuple.optJSONObject(1) ?: continue

                            // syn_list
                            extractWordsFromList(data.optJSONArray("syn_list"), synonyms)
                            // rel_list
                            extractWordsFromList(data.optJSONArray("rel_list"), relatedWords)
                            // near_list
                            extractWordsFromList(data.optJSONArray("near_list"), similarWords)
                            // ant_list
                            extractWordsFromList(data.optJSONArray("ant_list"), antonyms)

                            // Examples in dt
                            val dt = data.optJSONArray("dt")
                            if (dt != null) {
                                for (dtIdx in 0 until dt.length()) {
                                    val dtItem = dt.optJSONArray(dtIdx) ?: continue
                                    if (dtItem.optString(0) == "vis") {
                                        val visArray = dtItem.optJSONArray(1) ?: continue
                                        for (v in 0 until visArray.length()) {
                                            val vObj = visArray.optJSONObject(v) ?: continue
                                            val t = cleanMwMarkup(vObj.optString("t"))
                                            if (t.isNotBlank() && !examples.contains(t)) examples.add(t)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return MwThesaurusDetails(
                synonyms = synonyms.distinct(),
                antonyms = antonyms.distinct(),
                relatedWords = relatedWords.distinct(),
                similarWords = similarWords.distinct(),
                examples = examples.distinct()
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun extractWordsFromList(listArray: JSONArray?, target: MutableList<String>) {
        if (listArray == null) return
        for (i in 0 until listArray.length()) {
            val group = listArray.optJSONArray(i) ?: continue
            for (j in 0 until group.length()) {
                val itemObj = group.optJSONObject(j) ?: continue
                val wd = itemObj.optString("wd")
                if (wd.isNotBlank() && !target.contains(wd)) {
                    target.add(wd)
                }
            }
        }
    }

    private fun cleanMwMarkup(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw
            .replace("{bc}", ": ")
            .replace(Regex("\\{(?:it|b|sc|wi|parahw|phrase|qword)\\}(.*?)\\{/(?:it|b|sc|wi|parahw|phrase|qword)\\}"), "$1")
            .replace(Regex("\\{sx\\|([^|}]+)(?:\\|[^}]*)?\\}"), "$1")
            .replace(Regex("\\{(?:a_link|d_link|dxt|mat)\\|([^|}]+)(?:\\|[^}]*)?\\}"), "$1")
            .replace(Regex("\\{dx(?:_def|_ety)?\\}.*?\\{/dx(?:_def|_ety)?\\}"), "")
            .replace(Regex("\\{gloss\\}(.*?)\\{/gloss\\}"), "[$1]")
            .replace(Regex("\\{[a-z0-9_]+(?::[a-z0-9_]+)?\\|?([^}]*)\\}", RegexOption.IGNORE_CASE), "$1")
            .replace(Regex(":\\s*:"), ":")
            .replace(Regex("\\s+"), " ")
            .trim()
            .removePrefix(":")
            .trim()
    }

    private fun constructAudioUrl(audioFilename: String?): String? {
        if (audioFilename.isNullOrBlank()) return null
        val base = audioFilename.trim()
        val subdir = when {
            base.startsWith("bix") -> "bix"
            base.startsWith("gg") -> "gg"
            base.matches(Regex("^[0-9_].*")) -> "number"
            else -> base.take(1).lowercase(Locale.US)
        }
        return "https://media.merriam-webster.com/audio/prons/en/us/mp3/$subdir/$base.mp3"
    }
}
