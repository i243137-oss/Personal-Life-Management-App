const express = require('express');
const router = express.Router();
const { protect, optionalProtect } = require('../middleware/auth');
const {
  getWordByParam,
  lookupWord,
  aiLearningAssistant,
  saveWord,
  getLearnedWords,
  updateMastery,
  deleteLearnedWord,
  getVocabularyStats,
  testGemini,
  getCacheStatus
} = require('../controllers/dictionaryController');

// Public/Optional-Auth endpoints for dictionary searches and learning assistance
router.get('/test-gemini', testGemini);
router.get('/cache/stats', getCacheStatus);
router.post('/lookup', optionalProtect, lookupWord);
router.post('/ai-assistant', optionalProtect, aiLearningAssistant);

// Protected endpoints for user's vocabulary notebook & mastery management
router.post('/save', protect, saveWord);
router.get('/words', protect, getLearnedWords);
router.patch('/words/:id/mastery', protect, updateMastery);
router.delete('/words/:id', protect, deleteLearnedWord);
router.get('/stats', protect, getVocabularyStats);

// GET /api/dictionary/:word (Direct word lookup required by specification)
router.get('/:word', optionalProtect, getWordByParam);

module.exports = router;
