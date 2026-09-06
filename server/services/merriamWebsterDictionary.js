/**
 * Merriam-Webster Collegiate Dictionary API Service
 * 
 * Fetches authoritative dictionary entries from the Merriam-Webster Collegiate Dictionary API v3.
 * Securely uses process.env.MW_DICTIONARY_API_KEY.
 * Never exposes the API key to clients or responses.
 */

const MW_DICT_BASE_URL = 'https://www.dictionaryapi.com/api/v3/references/collegiate/json';
const REQUEST_TIMEOUT_MS = 6000;

/**
 * Clean Merriam-Webster formatting tags from text.
 * Examples: {bc} -> ': ', {it}text{/it} -> 'text', {sx|word||} -> 'word'
 */
function cleanMwMarkup(text) {
  if (!text || typeof text !== 'string') return '';
  return text
    // Replace bold colon {bc} with standard colon and space
    .replace(/\{bc\}/g, ': ')
    // Replace italics, bold, small caps
    .replace(/\{(?:it|b|sc|wi|parahw|phrase|qword)\}(.*?)\{\/(?:it|b|sc|wi|parahw|phrase|qword)\}/g, '$1')
    // Replace synonym cross-references {sx|word|sense|...} -> word
    .replace(/\{sx\|([^|}]+)(?:\|[^}]*)?\}/g, '$1')
    // Replace link tags: {a_link|word}, {d_link|word|id}, {dxt|word|id} -> word
    .replace(/\{(?:a_link|d_link|dxt|mat)\|([^|}]+)(?:\|[^}]*)?\}/g, '$1')
    // Remove directional cross-references like {dx}...{/dx}
    .replace(/\{dx(?:_def|_ety)?\}.*?\{\/dx(?:_def|_ety)?\}/g, '')
    // Replace {gloss}...{/gloss} with brackets
    .replace(/\{gloss\}(.*?)\{\/gloss\}/g, '[$1]')
    // Clean any remaining MW tokens like {inf}, {sup}, {ma}, etc.
    .replace(/\{[a-z0-9_]+(?::[a-z0-9_]+)?\|?([^}]*)\}/gi, '$1')
    // Clean up excessive whitespace or double colons
    .replace(/:\s*:/g, ':')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/^:\s*/, ''); // Remove leading colon if definition starts with it
}

/**
 * Construct audio URL for Merriam-Webster audio file according to MW documentation rules:
 * - If filename starts with "bix", subdirectory is "bix"
 * - If filename starts with "gg", subdirectory is "gg"
 * - If filename starts with a number or punctuation, subdirectory is "number"
 * - Otherwise, subdirectory is the first letter of the base filename
 */
function constructAudioUrl(audioFilename) {
  if (!audioFilename || typeof audioFilename !== 'string') return null;
  const base = audioFilename.trim();
  if (!base) return null;

  let subdir = '';
  if (base.startsWith('bix')) {
    subdir = 'bix';
  } else if (base.startsWith('gg')) {
    subdir = 'gg';
  } else if (/^[0-9_]/.test(base)) {
    subdir = 'number';
  } else {
    subdir = base.charAt(0).toLowerCase();
  }

  return `https://media.merriam-webster.com/audio/prons/en/us/mp3/${subdir}/${base}.mp3`;
}

/**
 * Defensive parser for sense sequence definitions and examples
 */
function extractSensesAndExamples(defArray, partOfSpeech = '') {
  const definitions = [];
  const examples = [];

  if (!Array.isArray(defArray)) return { definitions, examples };

  for (const defBlock of defArray) {
    if (!defBlock || !Array.isArray(defBlock.sseq)) continue;

    for (const senseSeq of defBlock.sseq) {
      if (!Array.isArray(senseSeq)) continue;

      for (const senseItem of senseSeq) {
        if (!Array.isArray(senseItem)) continue;
        const [type, data] = senseItem;

        if (type === 'sense' && data && Array.isArray(data.dt)) {
          let senseText = '';
          const senseExamples = [];

          for (const dtItem of data.dt) {
            if (!Array.isArray(dtItem)) continue;
            const [dtType, dtContent] = dtItem;

            if (dtType === 'text' && typeof dtContent === 'string') {
              const cleaned = cleanMwMarkup(dtContent);
              if (cleaned) {
                senseText = senseText ? `${senseText} ${cleaned}` : cleaned;
              }
            } else if (dtType === 'vis' && Array.isArray(dtContent)) {
              for (const vis of dtContent) {
                if (vis && vis.t) {
                  const cleanedExample = cleanMwMarkup(vis.t);
                  if (cleanedExample) {
                    senseExamples.push(cleanedExample);
                    examples.push(cleanedExample);
                  }
                }
              }
            }
          }

          if (senseText) {
            definitions.push({
              partOfSpeech: partOfSpeech || 'definition',
              text: senseText,
              examples: senseExamples
            });
          }
        } else if (type === 'bs' && data && data.sense && Array.isArray(data.sense.dt)) {
          // Binding substitute sense
          let bsText = '';
          for (const dtItem of data.sense.dt) {
            if (Array.isArray(dtItem) && dtItem[0] === 'text') {
              const cleaned = cleanMwMarkup(dtItem[1]);
              if (cleaned) bsText = bsText ? `${bsText} ${cleaned}` : cleaned;
            }
          }
          if (bsText) {
            definitions.push({
              partOfSpeech: partOfSpeech || 'definition',
              text: bsText,
              examples: []
            });
          }
        }
      }
    }
  }

  return { definitions, examples };
}

/**
 * Lookup a word in Merriam-Webster Collegiate Dictionary
 * 
 * @param {string} word - Search term
 * @param {object} [options]
 * @returns {Promise<object>} Result containing parsed dictionary data or error status
 */
async function lookupDictionary(word, options = {}) {
  const apiKey = process.env.MW_DICTIONARY_API_KEY;

  if (!apiKey || !apiKey.trim() || apiKey === 'YOUR_MW_DICTIONARY_API_KEY') {
    return {
      success: false,
      error: 'MISSING_API_KEY',
      message: 'Merriam-Webster Dictionary API key (MW_DICTIONARY_API_KEY) is not configured in server environment.'
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
  const url = `${MW_DICT_BASE_URL}/${encodeURIComponent(queryWord)}?key=${encodeURIComponent(apiKey.trim())}`;

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
          message: 'Invalid Merriam-Webster Dictionary API Key.'
        };
      }
      if (response.status === 429) {
        return {
          success: false,
          error: 'RATE_LIMITED',
          message: 'Merriam-Webster Dictionary API rate limit reached. Please try again shortly.'
        };
      }
      return {
        success: false,
        error: `HTTP_${response.status}`,
        message: `Merriam-Webster Dictionary API responded with status ${response.status}.`
      };
    }

    const rawData = await response.json();

    // MW returns [] if word has no matches and no suggestions
    if (!Array.isArray(rawData) || rawData.length === 0) {
      return {
        success: false,
        error: 'NOT_FOUND',
        message: `No dictionary entries found for "${queryWord}".`,
        suggestions: []
      };
    }

    // When a word is misspelled or not found, MW returns an array of suggestion strings: ["word1", "word2"]
    if (typeof rawData[0] === 'string') {
      return {
        success: false,
        error: 'NOT_FOUND_WITH_SUGGESTIONS',
        message: `Word not found. Did you mean one of these?`,
        suggestions: rawData.slice(0, 8)
      };
    }

    // Parse collegiate dictionary entries
    const partsOfSpeech = new Set();
    const allDefinitions = [];
    const allExamples = [];
    const audioList = [];
    const idioms = [];
    let etymology = null;
    let writtenPronunciation = null;
    let headwordWithSyllables = null;

    for (const entry of rawData) {
      if (!entry || typeof entry !== 'object') continue;

      const pos = entry.fl || '';
      if (pos) partsOfSpeech.add(pos);

      // Headword & Pronunciation
      if (entry.hwi) {
        if (!headwordWithSyllables && entry.hwi.hw) {
          headwordWithSyllables = entry.hwi.hw.replace(/\*/g, '·');
        }

        if (Array.isArray(entry.hwi.prs)) {
          for (const pr of entry.hwi.prs) {
            if (!writtenPronunciation && pr.mw) {
              writtenPronunciation = pr.mw;
            }
            if (pr.sound && pr.sound.audio) {
              const audioUrl = constructAudioUrl(pr.sound.audio);
              if (audioUrl && !audioList.some(a => a.audio === pr.sound.audio)) {
                audioList.push({
                  audio: pr.sound.audio,
                  url: audioUrl,
                  written: pr.mw || writtenPronunciation || ''
                });
              }
            }
          }
        }
      }

      // Definitions & Examples from 'def'
      if (Array.isArray(entry.def)) {
        const { definitions, examples } = extractSensesAndExamples(entry.def, pos);
        for (const def of definitions) {
          if (!allDefinitions.some(d => d.text === def.text)) {
            allDefinitions.push(def);
          }
        }
        for (const ex of examples) {
          if (!allExamples.includes(ex)) {
            allExamples.push(ex);
          }
        }
      }

      // Fallback: if 'def' parsing found few definitions, check 'shortdef'
      if (Array.isArray(entry.shortdef) && entry.shortdef.length > 0) {
        for (const sdef of entry.shortdef) {
          const cleanedSdef = cleanMwMarkup(sdef);
          if (cleanedSdef && !allDefinitions.some(d => d.text === cleanedSdef)) {
            allDefinitions.push({
              partOfSpeech: pos || 'definition',
              text: cleanedSdef,
              examples: []
            });
          }
        }
      }

      // Etymology
      if (!etymology && Array.isArray(entry.et)) {
        for (const etItem of entry.et) {
          if (Array.isArray(etItem) && etItem[0] === 'text') {
            const cleanedEt = cleanMwMarkup(etItem[1]);
            if (cleanedEt) {
              etymology = cleanedEt;
              break;
            }
          }
        }
      }

      // Defined run-ons / Idioms / Phrases (dros)
      if (Array.isArray(entry.dros)) {
        for (const dro of entry.dros) {
          if (dro && dro.drp) {
            const phrase = cleanMwMarkup(dro.drp);
            let phraseDef = '';
            if (Array.isArray(dro.def)) {
              const res = extractSensesAndExamples(dro.def);
              if (res.definitions.length > 0) phraseDef = res.definitions[0].text;
            }
            if (phrase) {
              idioms.push({ phrase, definition: phraseDef });
            }
          }
        }
      }
    }

    return {
      success: true,
      data: {
        word: queryWord,
        syllables: headwordWithSyllables || queryWord,
        pronunciation: {
          written: writtenPronunciation || '',
          ipa: writtenPronunciation ? `/${writtenPronunciation}/` : ''
        },
        definitions: allDefinitions,
        examples: allExamples,
        partsOfSpeech: Array.from(partsOfSpeech),
        etymology: etymology || null,
        audio: audioList,
        idioms: idioms
      }
    };
  } catch (err) {
    if (err.name === 'AbortError') {
      return {
        success: false,
        error: 'TIMEOUT',
        message: 'Merriam-Webster Dictionary API request timed out.'
      };
    }
    return {
      success: false,
      error: 'NETWORK_ERROR',
      message: err.message || 'Failed to connect to Merriam-Webster Dictionary API.'
    };
  }
}

module.exports = {
  lookupDictionary,
  cleanMwMarkup,
  constructAudioUrl
};
