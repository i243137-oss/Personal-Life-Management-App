const express = require('express');
const router = express.Router();
const { protect } = require('../middleware/auth');
const {
  getLoans,
  getLoansSummary,
  createLoan,
  recordRepayment,
  updateLoanStatus,
  deleteLoan,
} = require('../controllers/loanController');

// All loan endpoints are protected
router.use(protect);

router.route('/')
  .get(getLoans)
  .post(createLoan);

router.get('/summary', getLoansSummary);

router.post('/:id/repay', recordRepayment);
router.patch('/:id/status', updateLoanStatus);
router.delete('/:id', deleteLoan);

module.exports = router;
