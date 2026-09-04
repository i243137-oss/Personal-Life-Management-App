const Transaction = require('../models/Transaction');
const Loan = require('../models/Loan');
const Luggage = require('../models/Luggage');
const Note = require('../models/Note');

// @desc    Get dashboard summary for authenticated user
// @route   GET /api/dashboard/summary
// @access  Private
const getDashboardSummary = async (req, res) => {
  try {
    const userId = req.user._id;

    // Fetch recent transactions for this user
    const transactions = await Transaction.find({ userId })
      .sort({ date: -1, createdAt: -1 })
      .limit(10);

    const startOfToday = new Date();
    startOfToday.setHours(0, 0, 0, 0);

    const allUserTransactions = await Transaction.find({ userId });

    let currentBalance = 0;
    let todayExpenses = 0;

    for (const t of allUserTransactions) {
      const amount = Number(t.amount) || 0;
      switch (t.type) {
        case 'income':
        case 'loan_received':
        case 'loan_repayment_received':
          currentBalance += amount;
          break;
        case 'expense':
        case 'loan_given':
        case 'loan_repayment_sent':
          currentBalance -= amount;
          break;
      }

      if (t.type === 'expense' && new Date(t.date) >= startOfToday) {
        todayExpenses += amount;
      }
    }

    // Compute active loan metrics (youOwe vs othersOwe)
    let youOwe = 0;
    let othersOwe = 0;

    try {
      const allLoans = await Loan.find({ userId });
      for (const l of allLoans) {
        if (l.status !== 'paid' && Number(l.remainingAmount) > 0) {
          if (l.type === 'borrowed') {
            youOwe += Number(l.remainingAmount);
          } else if (l.type === 'lent') {
            othersOwe += Number(l.remainingAmount);
          }
        }
      }
    } catch (loanErr) {
      console.warn('Could not query loans for dashboard:', loanErr.message);
    }

    // Compute luggage metrics
    let activeLuggageTrips = 0;
    let pendingPackingCount = 0;
    try {
      const allTrips = await Luggage.find({ userId });
      activeLuggageTrips = allTrips.length;
      for (const t of allTrips) {
        const items = t.items || [];
        pendingPackingCount += items.filter((i) => !i.isPacked).length;
      }
    } catch (luggageErr) {
      console.warn('Could not query luggage for dashboard:', luggageErr.message);
    }

    // Compute notes metrics
    let totalNotesCount = 0;
    let pinnedNotesCount = 0;
    try {
      totalNotesCount = await Note.countDocuments({ userId, isArchived: false });
      pinnedNotesCount = await Note.countDocuments({ userId, isArchived: false, isPinned: true });
    } catch (noteErr) {
      console.warn('Could not query notes for dashboard:', noteErr.message);
    }

    // Format recent activity
    const recentActivity = transactions.map((t) => ({
      id: t._id,
      type: t.type,
      amount: t.amount,
      category: t.category,
      description: t.description,
      date: t.date,
    }));

    return res.status(200).json({
      success: true,
      data: {
        currentBalance,
        todayExpenses,
        youOwe,
        othersOwe,
        activeLuggageTrips,
        pendingPackingCount,
        totalNotesCount,
        pinnedNotesCount,
        recentActivity,
      },
    });
  } catch (err) {
    console.error('Dashboard summary error:', err);
    return res.status(500).json({
      success: false,
      message: 'Failed to fetch dashboard summary',
      error: err.message,
    });
  }
};

module.exports = {
  getDashboardSummary,
};
