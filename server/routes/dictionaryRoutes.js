const express = require('express');
const router = express.Router();
const { protect } = require('../middleware/auth');
const {
  lookupWord,
  saveWord,
  getLearnedWords,
  updateMastery,
  deleteLearnedWord,
  getVocabularyStats,
  testGemini
} = require('../controllers/dictionaryController');

// All dictionary endpoints are protected
router.use(protect);

router.get('/test-gemini', testGemini);
router.post('/lookup', lookupWord);
router.post('/save', saveWord);
router.get('/words', getLearnedWords);
router.patch('/words/:id/mastery', updateMastery);
router.delete('/words/:id', deleteLearnedWord);
router.get('/stats', getVocabularyStats);

module.exports = router;
