const Loan = require('../models/Loan');
const Transaction = require('../models/Transaction');

// @desc    Get all loans for current user
// @route   GET /api/loans
// @access  Private
const getLoans = async (req, res) => {
  try {
    const userId = req.user._id;
    const { type, status, search } = req.query;

    const query = { userId };
    if (type && (type === 'lent' || type === 'borrowed')) {
      query.type = type;
    }
    if (status && ['pending', 'partially_paid', 'paid'].includes(status)) {
      query.status = status;
    }

    let loans = await Loan.find(query).sort({ createdAt: -1 });

    if (search && search.trim() !== '') {
      const q = search.toLowerCase().trim();
      loans = loans.filter(
        (item) =>
          item.personName.toLowerCase().includes(q) ||
          (item.notes && item.notes.toLowerCase().includes(q)) ||
          (item.phoneNumber && item.phoneNumber.includes(q))
      );
    }

    return res.status(200).json({
      success: true,
      count: loans.length,
      data: loans,
    });
  } catch (err) {
    console.error('getLoans error:', err);
    return res.status(500).json({
      success: false,
      message: 'Failed to retrieve loans',
      error: err.message,
    });
  }
};

// @desc    Get loan statistics & summary
// @route   GET /api/loans/summary
// @access  Private
const getLoansSummary = async (req, res) => {
  try {
    const userId = req.user._id;
    const allLoans = await Loan.find({ userId });

    let totalLent = 0;
    let totalBorrowed = 0;
    let activeLentCount = 0;
    let activeBorrowedCount = 0;
    let settledCount = 0;

    const now = new Date();
    let overdueCount = 0;

    allLoans.forEach((loan) => {
      const remaining = Number(loan.remainingAmount) || 0;
      if (loan.status === 'paid' || remaining <= 0) {
        settledCount++;
      } else {
        if (loan.type === 'lent') {
          totalLent += remaining;
          activeLentCount++;
        } else if (loan.type === 'borrowed') {
          totalBorrowed += remaining;
          activeBorrowedCount++;
        }

        if (loan.dueDate && new Date(loan.dueDate) < now) {
          overdueCount++;
        }
      }
    });

    const netPosition = totalLent - totalBorrowed;

    return res.status(200).json({
      success: true,
      data: {
        totalLent,
        totalBorrowed,
        netPosition,
        activeLentCount,
        activeBorrowedCount,
        settledCount,
        overdueCount,
        totalCount: allLoans.length,
      },
    });
  } catch (err) {
    console.error('getLoansSummary error:', err);
    return res.status(500).json({
      success: false,
      message: 'Failed to retrieve loan summary',
      error: err.message,
    });
  }
};

// @desc    Create a new loan
// @route   POST /api/loans
// @access  Private
const createLoan = async (req, res) => {
  try {
    const userId = req.user._id;
    const { personName, phoneNumber, type, amount, dueDate, notes, affectBalance = true } = req.body;

    if (!personName || !personName.trim()) {
      return res.status(400).json({
        success: false,
        message: 'Person name is required',
      });
    }

    if (!type || !['lent', 'borrowed'].includes(type)) {
      return res.status(400).json({
        success: false,
        message: 'Loan type must be either "lent" (You gave) or "borrowed" (You took)',
      });
    }

    const numAmount = parseFloat(amount);
    if (isNaN(numAmount) || numAmount <= 0) {
      return res.status(400).json({
        success: false,
        message: 'Amount must be a positive number',
      });
    }

    const loan = await Loan.create({
      userId,
      personName: personName.trim(),
      phoneNumber: phoneNumber ? phoneNumber.trim() : '',
      type,
      amount: numAmount,
      remainingAmount: numAmount,
      dueDate: dueDate ? new Date(dueDate) : null,
      notes: notes ? notes.trim() : '',
      affectBalance: Boolean(affectBalance),
      status: 'pending',
      repayments: [],
    });

    // If affectBalance is true, record a ledger transaction so wallet balance reflects this
    if (affectBalance) {
      const txType = type === 'lent' ? 'loan_given' : 'loan_received';
      const category = type === 'lent' ? 'Loan Given' : 'Loan Taken';
      const desc = type === 'lent' ? `Udhar to ${personName.trim()}` : `Udhar from ${personName.trim()}`;

      await Transaction.create({
        userId,
        type: txType,
        amount: numAmount,
        category,
        description: notes ? `${desc} (${notes.trim()})` : desc,
        date: new Date(),
      });
    }

    return res.status(201).json({
      success: true,
      message: 'Loan entry created successfully',
      data: loan,
    });
  } catch (err) {
    console.error('createLoan error:', err);
    return res.status(500).json({
      success: false,
      message: 'Failed to create loan',
      error: err.message,
    });
  }
};

// @desc    Record a repayment on a loan
// @route   POST /api/loans/:id/repay
// @access  Private
const recordRepayment = async (req, res) => {
  try {
    const userId = req.user._id;
    const { id } = req.params;
    const { amount, notes, date } = req.body;

    const loan = await Loan.findById(id);
    if (!loan) {
      return res.status(404).json({
        success: false,
        message: 'Loan not found',
      });
    }

    if (loan.userId.toString() !== userId.toString()) {
      return res.status(403).json({
        success: false,
        message: 'Unauthorized access to this loan',
      });
    }

    const repayAmount = parseFloat(amount);
    if (isNaN(repayAmount) || repayAmount <= 0) {
      return res.status(400).json({
        success: false,
        message: 'Repayment amount must be a positive number',
      });
    }

    const currentRemaining = Number(loan.remainingAmount);
    if (repayAmount > currentRemaining) {
      return res.status(400).json({
        success: false,
        message: `Repayment amount (${repayAmount}) cannot exceed remaining amount (${currentRemaining})`,
      });
    }

    const newRemaining = currentRemaining - repayAmount;
    const newStatus = newRemaining <= 0 ? 'paid' : 'partially_paid';

    const repaymentItem = {
      _id: 'repay_' + Date.now(),
      amount: repayAmount,
      date: date ? new Date(date) : new Date(),
      notes: notes ? notes.trim() : '',
    };

    const updatedLoan = await Loan.findByIdAndUpdate(
      id,
      {
        remainingAmount: newRemaining,
        status: newStatus,
        $push: { repayments: repaymentItem },
      },
      { new: true }
    );

    // Record wallet transaction if loan affects balance
    if (loan.affectBalance) {
      const isLent = loan.type === 'lent';
      // If we lent money and they paid us back: cash inflow (+ balance)
      // If we borrowed money and we paid them back: cash outflow (- balance)
      const txType = isLent ? 'loan_repayment_received' : 'loan_repayment_sent';
      const category = isLent ? 'Loan Repayment Received' : 'Loan Repayment Sent';
      const desc = isLent
        ? `Repayment from ${loan.personName}`
        : `Repayment to ${loan.personName}`;

      await Transaction.create({
        userId,
        type: txType,
        amount: repayAmount,
        category,
        description: notes ? `${desc} (${notes.trim()})` : desc,
        date: date ? new Date(date) : new Date(),
      });
    }

    return res.status(200).json({
      success: true,
      message: newStatus === 'paid' ? 'Loan marked as fully settled!' : 'Repayment recorded successfully',
      data: updatedLoan,
    });
  } catch (err) {
    console.error('recordRepayment error:', err);
    return res.status(500).json({
      success: false,
      message: 'Failed to record repayment',
      error: err.message,
    });
  }
};

// @desc    Update loan status or settle in full
// @route   PATCH /api/loans/:id/status
// @access  Private
const updateLoanStatus = async (req, res) => {
  try {
    const userId = req.user._id;
    const { id } = req.params;
    const { status } = req.body;

    const loan = await Loan.findById(id);
    if (!loan) {
      return res.status(404).json({
        success: false,
        message: 'Loan not found',
      });
    }

    if (loan.userId.toString() !== userId.toString()) {
      return res.status(403).json({
        success: false,
        message: 'Unauthorized access to this loan',
      });
    }

    if (status === 'paid' && loan.remainingAmount > 0) {
      // Settle entire remaining amount
      req.body.amount = loan.remainingAmount;
      req.body.notes = 'Full settlement';
      return recordRepayment(req, res);
    }

    const updated = await Loan.findByIdAndUpdate(id, { status }, { new: true });
    return res.status(200).json({
      success: true,
      data: updated,
    });
  } catch (err) {
    console.error('updateLoanStatus error:', err);
    return res.status(500).json({
      success: false,
      message: 'Failed to update loan status',
      error: err.message,
    });
  }
};

// @desc    Delete a loan
// @route   DELETE /api/loans/:id
// @access  Private
const deleteLoan = async (req, res) => {
  try {
    const userId = req.user._id;
    const { id } = req.params;

    const loan = await Loan.findById(id);
    if (!loan) {
      return res.status(404).json({
        success: false,
        message: 'Loan not found',
      });
    }

    if (loan.userId.toString() !== userId.toString()) {
      return res.status(403).json({
        success: false,
        message: 'Unauthorized access to this loan',
      });
    }

    await Loan.findByIdAndDelete(id);

    return res.status(200).json({
      success: true,
      message: 'Loan record deleted successfully',
      data: { id },
    });
  } catch (err) {
    console.error('deleteLoan error:', err);
    return res.status(500).json({
      success: false,
      message: 'Failed to delete loan',
      error: err.message,
    });
  }
};

module.exports = {
  getLoans,
  getLoansSummary,
  createLoan,
  recordRepayment,
  updateLoanStatus,
  deleteLoan,
};
