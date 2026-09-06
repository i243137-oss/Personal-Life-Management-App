const Word = require('../models/Word');
const https = require('https');
const { lookupWordUnified, getCacheStats, clearCache } = require('../services/dictionaryService');

/**
 * Curated educational aids for offline or when Gemini API key is not yet set
 */
const CURATED_LEARNING_AIDS = {
  mitigate: {
    simple: "To mitigate means to make a bad situation, problem, or pain less severe, less painful, or easier to deal with.",
    urdu: "کسی تکلیف، شدت یا نقصان کو کم کرنا، ہلکا کرنا یا رفع کرنا۔ (Mitigate = شدت کم کرنا)",
    academic: "Frequently used in environmental science (climate change mitigation), law (mitigating circumstances), and economics (mitigating financial risk) to denote systematic actions that reduce adverse impacts.",
    collocations: ["mitigate risk", "mitigate the effects of", "mitigate impact", "mitigate damages", "mitigate circumstances"],
    mistakes: "Do not confuse 'mitigate' with 'militate'. 'Militate against' means to work against or oppose something, whereas 'mitigate' means to make less severe.",
    mnemonic: "Think of a 'mitt' (baseball glove) catching a fast hard ball—it softens and 'mitigates' the blow so your hand doesn't hurt.",
    quiz: {
      question: "Which of the following actions best illustrates 'mitigating' a risk?",
      options: [
        "Ignoring a leak until the pipe bursts",
        "Installing backup batteries before a storm to avoid power loss",
        "Selling all assets at a total loss",
        "Increasing the speed limit during heavy rain"
      ],
      correctIndex: 1,
      explanation: "Installing backup batteries lessens the adverse impact of a power outage, perfectly embodying mitigation."
    }
  },
  ephemeral: {
    simple: "Ephemeral describes something that lasts for only a very brief moment before disappearing.",
    urdu: "عارضی، چند روزہ، ناپائیدار، یا جلد ختم ہو جانے والی چیز۔ (Ephemeral = عارضی)",
    academic: "Derived from Greek 'ephemeros' (lasting only a day). Widely employed in literature, botanical taxonomy (ephemeral desert blooms), and digital communication (ephemeral messaging).",
    collocations: ["ephemeral nature", "ephemeral beauty", "ephemeral pleasures", "ephemeral fame"],
    mistakes: "Do not use 'ephemeral' for long gradual decline; it implies an inherently transient, short-lived span.",
    mnemonic: "Sounds like 'a feather will' float away in the wind in just seconds—light and ephemeral.",
    quiz: {
      question: "Which phenomenon is most accurately characterized as 'ephemeral'?",
      options: [
        "A 200-year-old oak tree",
        "Morning dew evaporating as soon as the sun rises",
        "The permanent stone walls of a fortress",
        "The laws of gravity"
      ],
      correctIndex: 1,
      explanation: "Morning dew vanishes within hours, making it a classic example of ephemerality."
    }
  },
  ubiquitous: {
    simple: "Ubiquitous means present, appearing, or found everywhere at the same time.",
    urdu: "ہر جگہ موجود، ہمہ گیر، ہر طرف پایا جانے والا۔ (Ubiquitous = ہمہ گیر / ہرجائی)",
    academic: "Commonly used in technology sociology ('ubiquitous computing' / pervasive tech) and ecology to indicate comprehensive presence across all sectors or domains.",
    collocations: ["ubiquitous presence", "become ubiquitous", "ubiquitous technology", "virtually ubiquitous"],
    mistakes: "Remember that 'ubiquitous' does not mean infinite in quantity; it means widespread in presence and availability.",
    mnemonic: "UBIQUITOUS = 'U-BI-QUIT-US'? No, it won't quit appearing everywhere you look!",
    quiz: {
      question: "What best demonstrates a 'ubiquitous' item in modern society?",
      options: [
        "A rare 17th-century postage stamp",
        "Smartphones in urban areas",
        "A customized handmade gold coin",
        "A submarine docked at the South Pole"
      ],
      correctIndex: 1,
      explanation: "Smartphones are present virtually everywhere in modern daily life."
    }
  },
  exacerbate: {
    simple: "To exacerbate means to make a problem, disease, or bad situation much worse or more severe.",
    urdu: "معاملے کو بگاڑنا، شدت پیدا کرنا، یا زخم پر نمک چھڑکنا۔ (Exacerbate = بگاڑنا / بڑھا دینا)",
    academic: "Standard in medical diagnostics (exacerbation of asthma symptoms) and macroeconomic policy (tariffs exacerbating inflation).",
    collocations: ["exacerbate the problem", "exacerbate tensions", "exacerbate poverty", "exacerbate symptoms"],
    mistakes: "Do not confuse 'exacerbate' (make worse) with 'exasperate' (greatly irritate or annoy someone).",
    mnemonic: "EXACERBATE looks like 'EX + ACERB' (acerbic = sharp/acidic). Pouring acid on a wound makes it worse!",
    quiz: {
      question: "Which action would 'exacerbate' a debt problem?",
      options: [
        "Refinancing at a 0% interest rate",
        "Taking out high-interest payday loans to pay past loans",
        "Negotiating a debt forgiveness settlement",
        "Increasing monthly repayments from savings"
      ],
      correctIndex: 1,
      explanation: "Adding high-interest debt compounds and worsens the financial crisis."
    }
  },
  paradigm: {
    simple: "A paradigm is a typical example, pattern, or accepted model of how something works or should be done.",
    urdu: "نمونہ، مثال، فکری سانچہ، یا طریقہ کار۔ (Paradigm = فکری نمونہ / اصول)",
    academic: "Popularized in epistemology by Thomas Kuhn's 'The Structure of Scientific Revolutions' (paradigm shifts: fundamental changes in underlying assumptions).",
    collocations: ["paradigm shift", "dominant paradigm", "new paradigm", "programming paradigm"],
    mistakes: "The 'g' in 'paradigm' is silent (pronounced 'PAIR-uh-dyme').",
    mnemonic: "PARA-DIGM: Think of a pair of dimes that set the exact mold and standard for all currency coins.",
    quiz: {
      question: "What is an example of a 'paradigm shift'?",
      options: [
        "Replacing one brand of printer paper with another",
        "The transition from classical Newtonian mechanics to Einstein's relativity",
        "Repainting an office wall blue instead of gray",
        "Sending an email 5 minutes earlier than usual"
      ],
      correctIndex: 1,
      explanation: "Relativity transformed the fundamental conceptual framework of physics, a classic paradigm shift."
    }
  }
};

/**
 * Call Gemini API specifically for learning assistance (never replaces dictionary data)
 */
async function callGeminiLearningAssistant(word, feature, definitionText, apiKey) {
  let prompt = '';

  switch (feature) {
    case 'simple':
      prompt = `For the word "${word}" (definition: "${definitionText || ''}"), provide an extremely clear, accessible plain-English explanation for general readers and English learners. Keep it to 2-3 sentences with one vivid real-life example. Do NOT output markdown code blocks.`;
      break;
    case 'urdu':
      prompt = `For the English word "${word}", provide:
1. The precise Urdu meaning and script (Nastaliq/Arabic script).
2. The Roman Urdu transliteration.
3. A natural Urdu explanation sentence showing its real-world nuance.
Keep it concise and culturally authentic.`;
      break;
    case 'academic':
      prompt = `For the word "${word}" (definition: "${definitionText || ''}"), explain how this term is applied in scholarly publications, academic research papers, and university textbooks. Provide 2 sophisticated context sentences illustrating academic usage.`;
      break;
    case 'collocations':
      prompt = `For the word "${word}", list the 5 most frequent academic and professional collocations (word pairings), each with a 1-sentence demonstration. Return clean formatted text with bullet points.`;
      break;
    case 'mistakes':
      prompt = `For the word "${word}", identify 1 or 2 common usage mistakes, false friends, or confusingly similar words that non-native and academic writers frequently confuse with it, and clearly distinguish between them.`;
      break;
    case 'mnemonic':
      prompt = `For the word "${word}", provide a clever, memorable mnemonic device or visual memory trick to help a university student permanently remember its meaning and spelling.`;
      break;
    case 'quiz':
      prompt = `Generate a high-yield vocabulary test question for the word "${word}".
Return ONLY a valid JSON object with:
{
  "question": "Question text testing the nuance of ${word}",
  "options": ["Option A", "Option B", "Option C", "Option D"],
  "correctIndex": 0,
  "explanation": "Brief explanation of why the correct option fits best."
}`;
      break;
    default:
      prompt = `Provide a concise learning insight for the word "${word}" to help a student master its academic nuance.`;
  }

  const models = ['gemini-3.6-flash', 'gemini-flash-latest', 'gemini-3.5-flash'];
  let lastError = null;

  for (const model of models) {
    try {
      return await new Promise((resolve, reject) => {
        const postData = JSON.stringify({
          contents: [{ parts: [{ text: prompt }] }],
          generationConfig: {
            temperature: 0.3,
            maxOutputTokens: 600
          }
        });

        const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;
        const parsedUrl = new URL(url);

        const req = https.request({
          hostname: parsedUrl.hostname,
          path: parsedUrl.pathname + parsedUrl.search,
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Content-Length': Buffer.byteLength(postData)
          },
          timeout: 8000
        }, (res) => {
          let body = '';
          res.on('data', chunk => body += chunk);
          res.on('end', () => {
            try {
              if (res.statusCode >= 200 && res.statusCode < 300) {
                const data = JSON.parse(body);
                const text = data.candidates?.[0]?.content?.parts?.[0]?.text;
                if (text) {
                  if (feature === 'quiz') {
                    const cleaned = text.replace(/```json/g, '').replace(/```/g, '').trim();
                    try {
                      return resolve(JSON.parse(cleaned));
                    } catch (e) {
                      return resolve({ text });
                    }
                  }
                  return resolve({ text: text.trim(), model });
                }
              }
              reject(new Error(`Gemini API HTTP ${res.statusCode}: ${body}`));
            } catch (e) {
              reject(e);
            }
          });
        });

        req.on('error', reject);
        req.on('timeout', () => {
          req.destroy();
          reject(new Error(`Gemini API request timed out`));
        });

        req.write(postData);
        req.end();
      });
    } catch (err) {
      lastError = err;
      continue;
    }
  }

  throw lastError || new Error('All Gemini models failed');
}

/**
 * GET /api/dictionary/:word
 * 
 * Direct endpoint required by prompt:
 * 1. Receives word from URL param.
 * 2. Queries Merriam-Webster Collegiate Dictionary API.
 * 3. Queries Merriam-Webster Collegiate Thesaurus API.
 * 4. Normalizes responses into unified format.
 * 5. Combines results & returns clean response.
 */
exports.getWordByParam = async (req, res) => {
  try {
    const { word } = req.params;
    if (!word || !word.trim()) {
      return res.status(400).json({
        success: false,
        message: 'Please provide a word to look up.'
      });
    }

    const trimmed = word.trim();
    const result = await lookupWordUnified(trimmed);

    // If user is authenticated, check if this word is in their learned vocabulary
    let isSaved = false;
    let savedWordId = null;
    let masteryStatus = null;
    let personalNotes = null;

    if (req.user && req.user.id) {
      const existing = await Word.findOne({ userId: req.user.id, word: trimmed });
      if (existing) {
        isSaved = true;
        savedWordId = existing._id;
        masteryStatus = existing.masteryStatus;
        personalNotes = existing.personalNotes;
      }
    }

    if (!result.success && (!result.dictionary || !result.thesaurus)) {
      return res.status(404).json({
        ...result,
        isSaved,
        savedWordId,
        masteryStatus
      });
    }

    res.json({
      ...result,
      isSaved,
      savedWordId,
      masteryStatus,
      personalNotes
    });
  } catch (error) {
    console.error('Error in getWordByParam:', error);
    res.status(500).json({
      success: false,
      message: error.message || 'Internal error looking up word in dictionary.'
    });
  }
};

/**
 * POST /api/dictionary/lookup
 * 
 * Lookup a word via POST payload { word, skipCache }
 */
exports.lookupWord = async (req, res) => {
  try {
    const { word, skipCache = false } = req.body;
    if (!word || !word.trim()) {
      return res.status(400).json({
        success: false,
        message: 'Please provide a word to look up'
      });
    }

    const trimmed = word.trim();
    const result = await lookupWordUnified(trimmed, { skipCache });

    // Check if word is already saved in Learned Words notebook
    let isSaved = false;
    let savedWordId = null;
    let masteryStatus = null;
    let personalNotes = null;

    if (req.user && req.user.id) {
      const existing = await Word.findOne({ userId: req.user.id, word: trimmed });
      if (existing) {
        isSaved = true;
        savedWordId = existing._id;
        masteryStatus = existing.masteryStatus;
        personalNotes = existing.personalNotes;
      }
    }

    res.json({
      success: result.success,
      data: {
        ...result,
        isSaved,
        savedWordId,
        masteryStatus,
        personalNotes
      },
      suggestions: result.suggestions || [],
      message: result.message
    });
  } catch (error) {
    console.error('Error looking up word:', error);
    res.status(500).json({
      success: false,
      message: error.message || 'Error looking up word in dictionary'
    });
  }
};

/**
 * POST /api/dictionary/ai-assistant
 * 
 * Gemini-powered Learning Assistant
 * Separated from Merriam-Webster dictionary data.
 * Provides learning aids: simple, urdu, academic, collocations, mistakes, mnemonic, quiz.
 */
exports.aiLearningAssistant = async (req, res) => {
  try {
    const { word, feature = 'simple', definition } = req.body;
    if (!word || !word.trim()) {
      return res.status(400).json({ success: false, message: 'Word is required' });
    }

    const trimmed = word.trim().toLowerCase();
    const apiKey = process.env.GEMINI_API_KEY;

    // 1. If Gemini API key is configured, query Gemini
    if (apiKey && apiKey !== 'MY_GEMINI_API_KEY' && apiKey !== 'undefined' && apiKey !== 'your_api_key_here') {
      try {
        const geminiResult = await callGeminiLearningAssistant(trimmed, feature, definition, apiKey);
        return res.json({
          success: true,
          word: trimmed,
          feature,
          source: 'Gemini AI Learning Assistant',
          data: geminiResult
        });
      } catch (geminiErr) {
        console.warn('Gemini learning assistant call failed, using curated educational aid:', geminiErr.message);
      }
    }

    // 2. Curated or synthesized fallback learning aid
    const curated = CURATED_LEARNING_AIDS[trimmed];
    if (curated && curated[feature]) {
      return res.json({
        success: true,
        word: trimmed,
        feature,
        source: 'Curated Academic Lexicon',
        data: typeof curated[feature] === 'object' ? curated[feature] : { text: curated[feature] }
      });
    }

    // Dynamic educational synthesis when offline / no key
    let fallbackText = '';
    switch (feature) {
      case 'simple':
        fallbackText = `"${trimmed}" refers to ${definition || 'this core concept'} in clear, everyday terms. Use it when describing situations with precision.`;
        break;
      case 'urdu':
        fallbackText = `The term "${trimmed}" conveys the concept of (${definition || 'a key principle'}). Commonly translated according to context in formal and literary discussions.`;
        break;
      case 'academic':
        fallbackText = `In academic literature and university textbooks, "${trimmed}" is employed as an analytical term to describe systematic patterns and measurable phenomena.`;
        break;
      case 'collocations':
        fallbackText = `• primary ${trimmed}\n• systematic ${trimmed}\n• ${trimmed} analysis\n• practical ${trimmed}`;
        break;
      case 'mistakes':
        fallbackText = `Be careful not to use "${trimmed}" outside of its specific grammatical part of speech. Check the Merriam-Webster entry for exact transitivity and syntax rules.`;
        break;
      case 'mnemonic':
        fallbackText = `Visualize the first syllable of "${trimmed}" associated with a clear mental anchor to easily recall its meaning during exams and reading.`;
        break;
      case 'quiz':
        return res.json({
          success: true,
          word: trimmed,
          feature: 'quiz',
          source: 'Vocabulary Practice Generator',
          data: {
            question: `What is the most accurate definition of the word "${trimmed}"?`,
            options: [
              definition || `An essential concept representing ${trimmed}`,
              `The exact opposite of ${trimmed}`,
              `A random unrelated term`,
              `None of the above`
            ],
            correctIndex: 0,
            explanation: `Based on the Merriam-Webster Collegiate Dictionary, option A captures the true sense.`
          }
        });
      default:
        fallbackText = `Learning guidance for ${trimmed}.`;
    }

    res.json({
      success: true,
      word: trimmed,
      feature,
      source: 'Educational Reference',
      data: { text: fallbackText }
    });
  } catch (error) {
    console.error('Error in aiLearningAssistant:', error);
    res.status(500).json({ success: false, message: error.message || 'Error generating learning aid' });
  }
};

/**
 * POST /api/dictionary/save
 * 
 * Save word to Learned Words vocabulary notebook
 * Sources Merriam-Webster for definition/thesaurus information.
 */
exports.saveWord = async (req, res) => {
  try {
    const {
      word,
      mode = 'meaning',
      phonetic,
      partOfSpeech,
      shortDefinition,
      fullDefinition,
      synonyms = [],
      antonyms = [],
      relatedWords = [],
      examples = [],
      etymology,
      audioUrl,
      personalNotes,
      masteryStatus = 'learning'
    } = req.body;

    if (!word || !word.trim()) {
      return res.status(400).json({ success: false, message: 'Word is required' });
    }

    const trimmed = word.trim();
    let wordDoc = await Word.findOne({ userId: req.user.id, word: trimmed });

    const payload = {
      mode,
      phonetic,
      partOfSpeech,
      shortDefinition,
      fullDefinition,
      synonyms,
      antonyms,
      relatedWords,
      examples,
      etymology,
      audioUrl,
      masteryStatus,
      source: 'Merriam-Webster',
      personalNotes: personalNotes !== undefined ? personalNotes : (wordDoc ? wordDoc.personalNotes : '')
    };

    if (wordDoc) {
      wordDoc = await Word.findByIdAndUpdate(wordDoc._id, payload, { new: true });
    } else {
      wordDoc = await Word.create({
        userId: req.user.id,
        word: trimmed,
        ...payload
      });
    }

    res.status(201).json({
      success: true,
      message: 'Word saved to your Learned Vocabulary',
      data: wordDoc
    });
  } catch (error) {
    console.error('Error saving word:', error);
    res.status(500).json({ success: false, message: error.message || 'Error saving word' });
  }
};

/**
 * GET /api/dictionary/words
 * 
 * Get Learned Words with filter & search
 */
exports.getLearnedWords = async (req, res) => {
  try {
    const { status, search } = req.query;
    const query = { userId: req.user.id };

    if (status && ['learning', 'reviewing', 'mastered'].includes(status.toLowerCase())) {
      query.masteryStatus = status.toLowerCase();
    }

    let words = await Word.find(query);

    if (search && search.trim()) {
      const q = search.trim().toLowerCase();
      words = words.filter(w =>
        w.word.toLowerCase().includes(q) ||
        (w.shortDefinition && w.shortDefinition.toLowerCase().includes(q)) ||
        (w.partOfSpeech && w.partOfSpeech.toLowerCase().includes(q))
      );
    }

    res.json({
      success: true,
      count: words.length,
      data: words
    });
  } catch (error) {
    console.error('Error getting learned words:', error);
    res.status(500).json({ success: false, message: error.message || 'Error getting learned words' });
  }
};

/**
 * PATCH /api/dictionary/words/:id/mastery
 * 
 * Update mastery status & user personal notes
 */
exports.updateMastery = async (req, res) => {
  try {
    const { id } = req.params;
    const { status, personalNotes } = req.body;

    if (!['learning', 'reviewing', 'mastered'].includes(status)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid status. Must be learning, reviewing, or mastered'
      });
    }

    const wordDoc = await Word.findById(id);
    if (!wordDoc) {
      return res.status(404).json({ success: false, message: 'Word not found' });
    }

    if (wordDoc.userId.toString() !== req.user.id.toString()) {
      return res.status(403).json({ success: false, message: 'Not authorized' });
    }

    const updateData = { masteryStatus: status };
    if (personalNotes !== undefined) updateData.personalNotes = personalNotes;

    const updated = await Word.findByIdAndUpdate(id, updateData, { new: true });

    res.json({
      success: true,
      message: 'Mastery status updated',
      data: updated
    });
  } catch (error) {
    console.error('Error updating mastery:', error);
    res.status(500).json({ success: false, message: error.message || 'Error updating mastery' });
  }
};

/**
 * DELETE /api/dictionary/words/:id
 */
exports.deleteLearnedWord = async (req, res) => {
  try {
    const { id } = req.params;
    const wordDoc = await Word.findById(id);

    if (!wordDoc) {
      return res.status(404).json({ success: false, message: 'Word not found' });
    }

    if (wordDoc.userId.toString() !== req.user.id.toString()) {
      return res.status(403).json({ success: false, message: 'Not authorized' });
    }

    await Word.findByIdAndDelete(id);

    res.json({
      success: true,
      message: 'Word removed from Learned Words'
    });
  } catch (error) {
    console.error('Error deleting learned word:', error);
    res.status(500).json({ success: false, message: error.message || 'Error deleting word' });
  }
};

/**
 * GET /api/dictionary/stats
 */
exports.getVocabularyStats = async (req, res) => {
  try {
    const total = await Word.countDocuments({ userId: req.user.id });
    const mastered = await Word.countDocuments({ userId: req.user.id, masteryStatus: 'mastered' });
    const reviewing = await Word.countDocuments({ userId: req.user.id, masteryStatus: 'reviewing' });
    const learning = await Word.countDocuments({ userId: req.user.id, masteryStatus: 'learning' });

    res.json({
      success: true,
      data: {
        total,
        mastered,
        reviewing,
        learning,
        masteryPercentage: total > 0 ? Math.round((mastered / total) * 100) : 0
      }
    });
  } catch (error) {
    console.error('Error getting vocabulary stats:', error);
    res.status(500).json({ success: false, message: error.message || 'Error getting vocabulary stats' });
  }
};

/**
 * GET /api/dictionary/test-gemini
 */
exports.testGemini = async (req, res) => {
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey || apiKey === 'MY_GEMINI_API_KEY' || apiKey === 'undefined' || apiKey === 'your_api_key_here') {
    return res.status(400).json({
      success: false,
      configured: false,
      message: 'GEMINI_API_KEY is not configured in server environment (.env)'
    });
  }

  try {
    const result = await callGeminiLearningAssistant('mitigate', 'simple', 'to make less severe', apiKey);
    res.json({
      success: true,
      configured: true,
      model: result.model || 'gemini-3.6-flash',
      reply: result.text,
      timestamp: new Date().toISOString()
    });
  } catch (e) {
    let msg = e.message || 'Error communicating with Gemini API';
    if (msg.includes('403') || msg.includes('PERMISSION_DENIED')) {
      msg = `${msg}\n\nTroubleshooting: Your Google Cloud / AI Studio project has been denied access or restricted. Please create a new Gemini API key in Google AI Studio (aistudio.google.com) under an active personal project, or ensure the Generative Language API is enabled without restrictive API key constraints.`;
    }
    res.status(500).json({ success: false, message: msg });
  }
};

/**
 * GET /api/dictionary/cache/stats
 */
exports.getCacheStatus = async (req, res) => {
  res.json({
    success: true,
    cache: getCacheStats()
  });
};
