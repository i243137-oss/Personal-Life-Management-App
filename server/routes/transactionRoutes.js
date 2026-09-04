const express = require('express');
const router = express.Router();
const {
  getTransactions,
  createTransaction,
  deleteTransaction,
  getCategories,
  getTransactionStats,
} = require('../controllers/transactionController');
const { protect } = require('../middleware/auth');

// Public route for category presets
router.get('/categories', getCategories);

// Protected routes
router.use(protect);
router.get('/', getTransactions);
router.post('/', createTransaction);
router.delete('/:id', deleteTransaction);
router.get('/stats', getTransactionStats);

module.exports = router;
