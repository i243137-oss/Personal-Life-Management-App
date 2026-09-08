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
    const requestedMonth = req.query.month; // e.g. "2026-09"

    const now = new Date();
    const currentMonthKey = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
    const activeMonth = requestedMonth && /^\d{4}-\d{2}$/.test(requestedMonth) ? requestedMonth : currentMonthKey;

    const [reqYear, reqMonth] = activeMonth.split('-').map(Number);
    const monthStartDate = new Date(reqYear, reqMonth - 1, 1, 0, 0, 0, 0);
    const monthEndDate = new Date(reqYear, reqMonth, 1, 0, 0, 0, 0);

    const monthNames = [
      'January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'
    ];
    const monthDisplayName = `${monthNames[reqMonth - 1]} ${reqYear}`;

    // Fetch recent transactions for this user
    const transactions = await Transaction.find({ userId })
      .sort({ date: -1, createdAt: -1 })
      .limit(10);

    const startOfToday = new Date();
    startOfToday.setHours(0, 0, 0, 0);
    const endOfToday = new Date();
    endOfToday.setHours(23, 59, 59, 999);

    const allUserTransactions = await Transaction.find({ userId });

    let currentBalance = 0;
    let todayExpenses = 0;
    let monthlyIncome = 0;
    let monthlyExpenses = 0;

    const availableMonthsSet = new Set([currentMonthKey]);

    for (const t of allUserTransactions) {
      const amount = Number(t.amount) || 0;
      const tDate = t.date ? new Date(t.date) : new Date(t.createdAt || Date.now());

      // Collect available financial months from actual transaction history
      if (!isNaN(tDate.getTime())) {
        const y = tDate.getFullYear();
        const m = String(tDate.getMonth() + 1).padStart(2, '0');
        availableMonthsSet.add(`${y}-${m}`);
      }

      // Net balance across all historical time
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

      // Today's daily spending
      if (t.type === 'expense' && tDate >= startOfToday && tDate <= endOfToday) {
        todayExpenses += amount;
      }

      // Monthly financial period statistics (Requirement 8 & 9)
      if (tDate >= monthStartDate && tDate < monthEndDate) {
        if (t.type === 'income') {
          monthlyIncome += amount;
        } else if (t.type === 'expense') {
          monthlyExpenses += amount;
        }
      }
    }

    // Sort available months descending (e.g. "2026-09", "2026-08")
    const availableMonths = Array.from(availableMonthsSet).sort((a, b) => b.localeCompare(a));

    // Requirement 10: Average Daily Income = Total Monthly Income / 30
    const averageDailyIncome = Math.round((monthlyIncome / 30) * 100) / 100;

    // Requirement 11: Benchmark comparison against average daily income
    const diff = Math.abs(todayExpenses - averageDailyIncome);
    const formattedDiff = Math.round(diff).toLocaleString();
    let spendingStatus = 'below';
    let spendingComparisonText = '';

    if (todayExpenses > averageDailyIncome) {
      spendingStatus = 'above';
      spendingComparisonText = `You are Rs. ${formattedDiff} above your average daily income.`;
    } else if (todayExpenses < averageDailyIncome) {
      spendingStatus = 'below';
      spendingComparisonText = `You are Rs. ${formattedDiff} below your average daily income.`;
    } else {
      spendingStatus = 'on_par';
      spendingComparisonText = `Your daily spending is right on par with your average daily income.`;
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
        selectedMonth: activeMonth,
        monthDisplayName,
        monthlyIncome,
        monthlyExpenses,
        averageDailyIncome,
        spendingStatus,
        spendingDifference: diff,
        spendingComparisonText,
        availableMonths,
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
