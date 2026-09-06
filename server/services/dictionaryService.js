/**
 * Unified Dictionary Service
 * 
 * Orchestrates queries between:
 * 1. Merriam-Webster Collegiate Dictionary API
 * 2. Merriam-Webster Collegiate Thesaurus API
 * 
 * Normalizes both responses, applies in-memory caching, and provides
 * resilient fallback behavior if one or both services experience errors.
 */

const mwDict = require('./merriamWebsterDictionary');
const mwThes = require('./merriamWebsterThesaurus');

// In-Memory Cache with TTL (24 hours) and max size to optimize performance
const CACHE_TTL_MS = 24 * 60 * 60 * 1000;
const MAX_CACHE_ENTRIES = 1000;
const dictionaryCache = new Map();

/**
 * Clean up expired cache items or cap max size
 */
function pruneCache() {
  const now = Date.now();
  for (const [key, entry] of dictionaryCache.entries()) {
    if (now - entry.timestamp > CACHE_TTL_MS) {
      dictionaryCache.delete(key);
    }
  }
  if (dictionaryCache.size > MAX_CACHE_ENTRIES) {
    // Evict oldest entries
    const keys = Array.from(dictionaryCache.keys());
    for (let i = 0; i < 200 && i < keys.length; i++) {
      dictionaryCache.delete(keys[i]);
    }
  }
}

/**
 * Lookup a word across Merriam-Webster Dictionary and Thesaurus
 * 
 * @param {string} word - Search term
 * @param {object} [options]
 * @param {boolean} [options.skipCache=false] - Force fresh API lookup
 * @returns {Promise<object>} Combined normalized dictionary result
 */
async function lookupWordUnified(word, options = {}) {
  if (!word || typeof word !== 'string' || !word.trim()) {
    return {
      success: false,
      message: 'Please provide a valid word to search.'
    };
  }

  const normalizedQuery = word.trim().toLowerCase();

  // 1. Check cache first unless explicitly skipped
  if (!options.skipCache && dictionaryCache.has(normalizedQuery)) {
    const cachedEntry = dictionaryCache.get(normalizedQuery);
    if (Date.now() - cachedEntry.timestamp < CACHE_TTL_MS) {
      return {
        ...cachedEntry.data,
        source: {
          ...cachedEntry.data.source,
          cached: true
        }
      };
    } else {
      dictionaryCache.delete(normalizedQuery);
    }
  }

  // 2. Query Merriam-Webster Collegiate Dictionary and Thesaurus concurrently
  const [dictResultSettled, thesResultSettled] = await Promise.allSettled([
    mwDict.lookupDictionary(word),
    mwThes.lookupThesaurus(word)
  ]);

  const dictResult = dictResultSettled.status === 'fulfilled'
    ? dictResultSettled.value
    : { success: false, error: 'PROMISE_REJECTED', message: dictResultSettled.reason?.message };

  const thesResult = thesResultSettled.status === 'fulfilled'
    ? thesResultSettled.value
    : { success: false, error: 'PROMISE_REJECTED', message: thesResultSettled.reason?.message };

  const dictSuccess = dictResult && dictResult.success && dictResult.data;
  const thesSuccess = thesResult && thesResult.success && thesResult.data;

  // Handle case where both completely fail or are not found
  if (!dictSuccess && !thesSuccess) {
    // Gather any suggestions from either API
    const combinedSuggestions = [];
    if (dictResult?.suggestions) combinedSuggestions.push(...dictResult.suggestions);
    if (thesResult?.suggestions) {
      for (const s of thesResult.suggestions) {
        if (!combinedSuggestions.includes(s)) combinedSuggestions.push(s);
      }
    }

    return {
      success: false,
      word: word.trim(),
      message: dictResult?.message || thesResult?.message || `No results found for "${word.trim()}".`,
      error: dictResult?.error || thesResult?.error || 'NOT_FOUND',
      suggestions: combinedSuggestions.slice(0, 8),
      source: {
        attribution: 'Merriam-Webster Collegiate® Dictionary & Thesaurus',
        dictionarySuccess: false,
        thesaurusSuccess: false,
        dictionaryError: dictResult?.message || null,
        thesaurusError: thesResult?.message || null,
        cached: false
      }
    };
  }

  // Extract dictionary sections
  const dictData = dictSuccess ? dictResult.data : null;
  const thesData = thesSuccess ? thesResult.data : null;

  const wordDisplay = dictData?.word || word.trim();
  const syllables = dictData?.syllables || wordDisplay;
  const pronunciation = dictData?.pronunciation || { written: '', ipa: '' };
  const definitions = dictData?.definitions || [];
  const partsOfSpeech = dictData?.partsOfSpeech || [];
  const etymology = dictData?.etymology || null;
  const audio = dictData?.audio || [];
  const idioms = dictData?.idioms || [];

  // Combine examples from dictionary and thesaurus
  const examplesSet = new Set();
  if (dictData?.examples) dictData.examples.forEach(e => examplesSet.add(e));
  if (thesData?.examples) thesData.examples.forEach(e => examplesSet.add(e));
  const combinedExamples = Array.from(examplesSet);

  // Extract thesaurus sections
  const synonyms = thesData?.synonyms || [];
  const antonyms = thesData?.antonyms || [];
  const relatedWords = thesData?.relatedWords || [];
  const similarWords = thesData?.similarWords || [];

  // Determine top concise short & full definitions for top-level convenience
  const primaryDefinition = definitions.length > 0 ? definitions[0].text : '';
  const secondaryDefinition = definitions.length > 1 ? definitions[1].text : '';
  const primaryPartOfSpeech = partsOfSpeech.length > 0 ? partsOfSpeech[0] : '';

  // 3. Assemble normalized contract response
  const combinedResponse = {
    success: true,
    word: wordDisplay,
    syllables: syllables,
    dictionary: {
      pronunciation: pronunciation,
      definitions: definitions,
      examples: combinedExamples,
      partsOfSpeech: partsOfSpeech,
      etymology: etymology,
      audio: audio,
      idioms: idioms
    },
    thesaurus: {
      synonyms: synonyms,
      antonyms: antonyms,
      relatedWords: relatedWords,
      similarWords: similarWords
    },
    // Top-level convenience properties for backwards compatibility with existing UI
    phonetic: pronunciation.ipa || pronunciation.written ? (pronunciation.ipa || `/${pronunciation.written}/`) : '',
    partOfSpeech: primaryPartOfSpeech,
    shortDefinition: primaryDefinition,
    fullDefinition: secondaryDefinition ? `${primaryDefinition} | ${secondaryDefinition}` : primaryDefinition,
    synonyms: synonyms.slice(0, 10),
    antonyms: antonyms.slice(0, 8),
    examples: combinedExamples.slice(0, 4),
    source: {
      attribution: 'Merriam-Webster Collegiate® Dictionary & Thesaurus',
      dictionarySuccess: !!dictSuccess,
      thesaurusSuccess: !!thesSuccess,
      dictionaryError: dictSuccess ? null : (dictResult?.message || 'Dictionary lookup failed'),
      thesaurusError: thesSuccess ? null : (thesResult?.message || 'Thesaurus lookup failed'),
      cached: false
    }
  };

  // 4. Save to in-memory cache if at least one service succeeded
  pruneCache();
  dictionaryCache.set(normalizedQuery, {
    timestamp: Date.now(),
    data: combinedResponse
  });

  return combinedResponse;
}

/**
 * Clear cache utility (useful for testing)
 */
function clearCache() {
  dictionaryCache.clear();
}

/**
 * Get cache statistics
 */
function getCacheStats() {
  return {
    size: dictionaryCache.size,
    maxSize: MAX_CACHE_ENTRIES,
    ttlMinutes: CACHE_TTL_MS / (60 * 1000)
  };
}

module.exports = {
  lookupWordUnified,
  clearCache,
  getCacheStats
};
