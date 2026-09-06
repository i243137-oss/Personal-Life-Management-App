/**
 * Merriam-Webster Collegiate Thesaurus API Service
 * 
 * Fetches synonyms, antonyms, related words, and near antonyms from the
 * Merriam-Webster Collegiate Thesaurus API v3.
 * Securely uses process.env.MW_THESAURUS_API_KEY.
 * Never exposes the API key to clients or responses.
 */

const { cleanMwMarkup } = require('./merriamWebsterDictionary');

const MW_THESAURUS_BASE_URL = 'https://www.dictionaryapi.com/api/v3/references/thesaurus/json';
const REQUEST_TIMEOUT_MS = 6000;

/**
 * Defensive parser for sense lists in Collegiate Thesaurus
 */
function extractThesaurusSenses(defArray) {
  const synonyms = new Set();
  const antonyms = new Set();
  const relatedWords = new Set();
  const similarWords = new Set();
  const examples = [];

  if (!Array.isArray(defArray)) {
    return { synonyms, antonyms, relatedWords, similarWords, examples };
  }

  for (const defBlock of defArray) {
    if (!defBlock || !Array.isArray(defBlock.sseq)) continue;

    for (const senseSeq of defBlock.sseq) {
      if (!Array.isArray(senseSeq)) continue;

      for (const senseItem of senseSeq) {
        if (!Array.isArray(senseItem)) continue;
        const [type, data] = senseItem;

        if (type === 'sense' && data) {
          // 1. Synonym lists inside sense
          if (Array.isArray(data.syn_list)) {
            for (const group of data.syn_list) {
              if (Array.isArray(group)) {
                for (const item of group) {
                  if (item && item.wd) synonyms.add(item.wd.trim());
                }
              }
            }
          }

          // 2. Related words lists inside sense
          if (Array.isArray(data.rel_list)) {
            for (const group of data.rel_list) {
              if (Array.isArray(group)) {
                for (const item of group) {
                  if (item && item.wd) relatedWords.add(item.wd.trim());
                }
              }
            }
          }

          // 3. Near antonyms / similar contrast lists
          if (Array.isArray(data.near_list)) {
            for (const group of data.near_list) {
              if (Array.isArray(group)) {
                for (const item of group) {
                  if (item && item.wd) similarWords.add(item.wd.trim());
                }
              }
            }
          }

          // 4. Antonym lists inside sense
          if (Array.isArray(data.ant_list)) {
            for (const group of data.ant_list) {
              if (Array.isArray(group)) {
                for (const item of group) {
                  if (item && item.wd) antonyms.add(item.wd.trim());
                }
              }
            }
          }

          // 5. Examples inside sense dt
          if (Array.isArray(data.dt)) {
            for (const dtItem of data.dt) {
              if (Array.isArray(dtItem) && dtItem[0] === 'vis' && Array.isArray(dtItem[1])) {
                for (const vis of dtItem[1]) {
                  if (vis && vis.t) {
                    const cleanedEx = cleanMwMarkup(vis.t);
                    if (cleanedEx && !examples.includes(cleanedEx)) {
                      examples.push(cleanedEx);
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  return { synonyms, antonyms, relatedWords, similarWords, examples };
}

/**
 * Lookup a word in Merriam-Webster Collegiate Thesaurus
 * 
 * @param {string} word - Word to query
 * @param {object} [options]
 * @returns {Promise<object>} Result containing parsed thesaurus data or error status
 */
async function lookupThesaurus(word, options = {}) {
  const apiKey = process.env.MW_THESAURUS_API_KEY;

  if (!apiKey || !apiKey.trim() || apiKey === 'YOUR_MW_THESAURUS_API_KEY') {
    return {
      success: false,
      error: 'MISSING_API_KEY',
      message: 'Merriam-Webster Thesaurus API key (MW_THESAURUS_API_KEY) is not configured in server environment.'
    };
  }

  if (!word || typeof word !== 'string' || !word.trim()) {
    return {
      success: false,
      error: 'INVALID_WORD',
      message: 'Please provide a valid word to search.'
    };
  }

  const queryWord = word.trim();
  const url = `${MW_THESAURUS_BASE_URL}/${encodeURIComponent(queryWord)}?key=${encodeURIComponent(apiKey.trim())}`;

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

    const response = await fetch(url, {
      method: 'GET',
      headers: { 'Accept': 'application/json' },
      signal: controller.signal
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      if (response.status === 401 || response.status === 403) {
        return {
          success: false,
          error: 'AUTH_FAILED',
          message: 'Invalid Merriam-Webster Thesaurus API Key.'
        };
      }
      if (response.status === 429) {
        return {
          success: false,
          error: 'RATE_LIMITED',
          message: 'Merriam-Webster Thesaurus API rate limit reached. Please try again shortly.'
        };
      }
      return {
        success: false,
        error: `HTTP_${response.status}`,
        message: `Merriam-Webster Thesaurus API responded with status ${response.status}.`
      };
    }

    const rawData = await response.json();

    if (!Array.isArray(rawData) || rawData.length === 0) {
      return {
        success: false,
        error: 'NOT_FOUND',
        message: `No thesaurus entries found for "${queryWord}".`,
        suggestions: []
      };
    }

    // MW returns suggestions array of strings when not found
    if (typeof rawData[0] === 'string') {
      return {
        success: false,
        error: 'NOT_FOUND_WITH_SUGGESTIONS',
        message: 'Word not found in thesaurus.',
        suggestions: rawData.slice(0, 8)
      };
    }

    const allSynonyms = new Set();
    const allAntonyms = new Set();
    const allRelatedWords = new Set();
    const allSimilarWords = new Set();
    const allExamples = [];

    for (const entry of rawData) {
      if (!entry || typeof entry !== 'object') continue;

      // Meta synonyms and antonyms
      if (entry.meta) {
        if (Array.isArray(entry.meta.syns)) {
          for (const synGroup of entry.meta.syns) {
            if (Array.isArray(synGroup)) {
              for (const s of synGroup) {
                if (typeof s === 'string' && s.trim()) allSynonyms.add(s.trim());
              }
            }
          }
        }

        if (Array.isArray(entry.meta.ants)) {
          for (const antGroup of entry.meta.ants) {
            if (Array.isArray(antGroup)) {
              for (const a of antGroup) {
                if (typeof a === 'string' && a.trim()) allAntonyms.add(a.trim());
              }
            }
          }
        }
      }

      // Detailed sense breakdown from 'def'
      if (Array.isArray(entry.def)) {
        const senseData = extractThesaurusSenses(entry.def);
        senseData.synonyms.forEach(w => allSynonyms.add(w));
        senseData.antonyms.forEach(w => allAntonyms.add(w));
        senseData.relatedWords.forEach(w => allRelatedWords.add(w));
        senseData.similarWords.forEach(w => allSimilarWords.add(w));
        for (const ex of senseData.examples) {
          if (!allExamples.includes(ex)) allExamples.push(ex);
        }
      }
    }

    // Remove duplicates across lists where appropriate
    // E.g., if a word is in synonyms, don't keep in relatedWords
    const finalSynonyms = Array.from(allSynonyms);
    const finalAntonyms = Array.from(allAntonyms);
    const finalRelatedWords = Array.from(allRelatedWords).filter(w => !allSynonyms.has(w));
    const finalSimilarWords = Array.from(allSimilarWords).filter(w => !allSynonyms.has(w) && !allAntonyms.has(w));

    return {
      success: true,
      data: {
        synonyms: finalSynonyms,
        antonyms: finalAntonyms,
        relatedWords: finalRelatedWords,
        similarWords: finalSimilarWords,
        examples: allExamples
      }
    };
  } catch (err) {
    if (err.name === 'AbortError') {
      return {
        success: false,
        error: 'TIMEOUT',
        message: 'Merriam-Webster Thesaurus API request timed out.'
      };
    }
    return {
      success: false,
      error: 'NETWORK_ERROR',
      message: err.message || 'Failed to connect to Merriam-Webster Thesaurus API.'
    };
  }
}

module.exports = {
  lookupThesaurus
};
