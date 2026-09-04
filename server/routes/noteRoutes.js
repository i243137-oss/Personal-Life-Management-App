const express = require('express');
const router = express.Router();
const { protect } = require('../middleware/auth');
const {
  getNotes,
  getNoteById,
  createNote,
  updateNote,
  deleteNote,
  togglePinNote,
  toggleArchiveNote,
} = require('../controllers/noteController');

// All note endpoints are protected
router.use(protect);

router.route('/')
  .get(getNotes)
  .post(createNote);

router.route('/:id')
  .get(getNoteById)
  .put(updateNote)
  .delete(deleteNote);

router.patch('/:id/pin', togglePinNote);
router.patch('/:id/archive', toggleArchiveNote);

module.exports = router;
