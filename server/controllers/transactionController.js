const Transaction = require('../models/Transaction');

// Predefined categories with metadata
const PREDEFINED_CATEGORIES = {
  expense: [
    { name: 'Fee', icon: 'school', color: '#1E88E5', description: 'Tuition, exam fees, academy' },
    { name: 'Transport', icon: 'directions_bus', color: '#FB8C00', description: 'Bus, fuel, rickshaw, taxi' },
    { name: 'Meal & Food', icon: 'restaurant', color: '#E53935', description: 'Breakfast, lunch, dinner, cafe' },
    { name: 'Shopping', icon: 'shopping_bag', color: '#8E24AA', description: 'Clothes, personal items' },
    { name: 'Bills & Utilities', icon: 'receipt_long', color: '#00897B', description: 'Electricity, mobile package, wifi' },
    { name: 'Entertainment', icon: 'movie', color: '#D81B60', description: 'Outings, movies, snacks' },
    { name: 'Health & Medical', icon: 'medical_services', color: '#43A047', description: 'Doctor, pharmacy, medicine' },
    { name: 'Other Expense', icon: 'more_horiz', color: '#757575', description: 'Miscellaneous expenses' }
  ],
  income: [
    { name: 'Pocket Money', icon: 'savings', color: '#2E7D32', description: 'Family allowance, pocket money' },
    { name: 'Salary', icon: 'payments', color: '#1B5E20', description: 'Monthly salary, stipend' },
    { name: 'Freelance & Gig', icon: 'laptop_mac', color: '#00838F', description: 'Online projects, client gigs' },
    { name: 'Gift / Cash Inflow', icon: 'card_giftcard', color: '#00ACC1', description: 'Eidi, gifts, rewards' },
    { name: 'Investment / Profit', icon: 'trending_up', color: '#558B2F', description: 'Trading, profit, dividend' },
    { name: 'Other Income', icon: 'add_circle', color: '#689F38', description: 'Other cash inflows' }
  ],
  quickPresets: [
    { label: 'Lunch', category: 'Meal & Food', amount: 200, type: 'expense' },
    { label: 'Transport', category: 'Transport', amount: 100, type: 'expense' },
    { label: 'Tea / Snack', category: 'Meal & Food', amount: 60, type: 'expense' },
    { label: 'Mobile Package', category: 'Bills & Utilities', amount: 500, type: 'expense' },
    { label: 'Pocket Money', category: 'Pocket Money', amount: 2000, type: 'income' }
  ]
};

// Helper to compute user balance
const calculateUserBalance = async (userId) => {
  const transactions = await Transaction.find({ userId });
  let balance = 0;
  for (const t of transactions) {
    if (t.type === 'income' || t.type === 'loan_received' || t.type === 'loan_repayment_received') {
      balance += t.amount;
    } else if (t.type === 'expense' || t.type === 'loan_given' || t.type === 'loan_repayment_sent') {
      balance -= t.amount;
    }
  }
  return balance;
};

// @desc    Get all transactions with filtering
// @route   GET /api/transactions
// @access  Private
const getTransactions = async (req, res) => {
  try {
    const userId = req.user.id || req.user._id;
    const { type, category, startDate, endDate, search, limit = 50 } = req.query;

    const query = { userId };
    if (type && type !== 'all') {
      query.type = type;
    }
    if (category && category !== 'all') {
      query.category = category;
    }

    let transactions = await Transaction.find(query).sort({ date: -1 }).limit(parseInt(limit, 10));

    // Date filtering if passed
    if (startDate || endDate) {
      transactions = transactions.filter((t) => {
        const d = new Date(t.date).getTime();
        if (startDate && d < new Date(startDate).getTime()) return false;
        if (endDate && d > new Date(endDate).getTime()) return false;
        return true;
      });
    }

    // Text search filter
    if (search && search.trim()) {
      const q = search.trim().toLowerCase();
      transactions = transactions.filter(
        (t) =>
          (t.category && t.category.toLowerCase().includes(q)) ||
          (t.description && t.description.toLowerCase().includes(q))
      );
    }

    // Compute totals for summary
    const allUserTx = await Transaction.find({ userId });
    let totalIncome = 0;
    let totalExpense = 0;
    let currentBalance = 0;

    for (const t of allUserTx) {
      if (t.type === 'income' || t.type === 'loan_received' || t.type === 'loan_repayment_received') {
        currentBalance += t.amount;
        if (t.type === 'income') totalIncome += t.amount;
      } else if (t.type === 'expense' || t.type === 'loan_given' || t.type === 'loan_repayment_sent') {
        currentBalance -= t.amount;
        if (t.type === 'expense') totalExpense += t.amount;
      }
    }

    const formatted = transactions.map((t) => ({
      id: (t._id || t.id).toString(),
      type: t.type,
      amount: t.amount,
      category: t.category,
      description: t.description || '',
      date: t.date ? new Date(t.date).toISOString() : new Date().toISOString(),
      createdAt: t.createdAt ? new Date(t.createdAt).toISOString() : new Date().toISOString()
    }));

    res.status(200).json({
      success: true,
      count: formatted.length,
      currentBalance,
      totalIncome,
      totalExpense,
      data: formatted
    });
  } catch (err) {
    console.error('getTransactions error:', err);
    res.status(500).json({
      success: false,
      message: 'Failed to fetch transactions: ' + err.message
    });
  }
};

// @desc    Create a new transaction (Income or Expense)
// @route   POST /api/transactions
// @access  Private
const createTransaction = async (req, res) => {
  try {
    const userId = req.user.id || req.user._id;
    const { type, amount, category, description, date, allowOverdraft } = req.body;

    if (!type || !['income', 'expense'].includes(type)) {
      return res.status(400).json({
        success: false,
        message: 'Transaction type must be either "income" or "expense".'
      });
    }

    const parsedAmount = parseFloat(amount);
    if (isNaN(parsedAmount) || parsedAmount <= 0) {
      return res.status(400).json({
        success: false,
        message: 'Amount must be a valid positive number greater than 0.'
      });
    }

    if (!category || !category.trim()) {
      return res.status(400).json({
        success: false,
        message: 'Category is required.'
      });
    }

    // Overdraft validation for expenses
    const currentBalance = await calculateUserBalance(userId);
    if (type === 'expense' && parsedAmount > currentBalance && !allowOverdraft) {
      return res.status(400).json({
        success: false,
        overdraft: true,
        currentBalance,
        message: `Insufficient balance! Your current balance is Rs. ${currentBalance.toLocaleString()}, but this expense is Rs. ${parsedAmount.toLocaleString()}.`
      });
    }

    const transaction = await Transaction.create({
      userId,
      type,
      amount: parsedAmount,
      category: category.trim(),
      description: description ? description.trim() : '',
      date: date ? new Date(date) : new Date()
    });

    const newBalance = await calculateUserBalance(userId);

    res.status(201).json({
      success: true,
      message: `${type === 'income' ? 'Money added' : 'Expense recorded'} successfully.`,
      newBalance,
      data: {
        id: (transaction._id || transaction.id).toString(),
        type: transaction.type,
        amount: transaction.amount,
        category: transaction.category,
        description: transaction.description || '',
        date: transaction.date ? new Date(transaction.date).toISOString() : new Date().toISOString(),
        createdAt: transaction.createdAt ? new Date(transaction.createdAt).toISOString() : new Date().toISOString()
      }
    });
  } catch (err) {
    console.error('createTransaction error:', err);
    res.status(500).json({
      success: false,
      message: 'Failed to record transaction: ' + err.message
    });
  }
};

// @desc    Delete a transaction
// @route   DELETE /api/transactions/:id
// @access  Private
const deleteTransaction = async (req, res) => {
  try {
    const userId = req.user.id || req.user._id;
    const { id } = req.params;

    const transaction = await Transaction.findById(id);
    if (!transaction) {
      return res.status(404).json({
        success: false,
        message: 'Transaction not found.'
      });
    }

    // Verify ownership
    if (transaction.userId.toString() !== userId.toString()) {
      return res.status(403).json({
        success: false,
        message: 'Not authorized to delete this transaction.'
      });
    }

    await Transaction.findByIdAndDelete(id);
    const newBalance = await calculateUserBalance(userId);

    res.status(200).json({
      success: true,
      message: 'Transaction deleted successfully.',
      newBalance
    });
  } catch (err) {
    console.error('deleteTransaction error:', err);
    res.status(500).json({
      success: false,
      message: 'Failed to delete transaction: ' + err.message
    });
  }
};

// @desc    Get predefined categories and quick add presets
// @route   GET /api/transactions/categories
// @access  Public or Private
const getCategories = (req, res) => {
  res.status(200).json({
    success: true,
    data: PREDEFINED_CATEGORIES
  });
};

// @desc    Get category-wise breakdown & stats
// @route   GET /api/transactions/stats
// @access  Private
const getTransactionStats = async (req, res) => {
  try {
    const userId = req.user.id || req.user._id;
    const transactions = await Transaction.find({ userId });

    const categoryMap = {};
    let totalExpense = 0;
    let totalIncome = 0;

    for (const t of transactions) {
      if (t.type === 'expense') {
        totalExpense += t.amount;
        categoryMap[t.category] = (categoryMap[t.category] || 0) + t.amount;
      } else if (t.type === 'income') {
        totalIncome += t.amount;
      }
    }

    const expenseBreakdown = Object.entries(categoryMap).map(([category, amount]) => ({
      category,
      amount,
      percentage: totalExpense > 0 ? Math.round((amount / totalExpense) * 100) : 0
    })).sort((a, b) => b.amount - a.amount);

    res.status(200).json({
      success: true,
      data: {
        totalIncome,
        totalExpense,
        expenseBreakdown
      }
    });
  } catch (err) {
    console.error('getTransactionStats error:', err);
    res.status(500).json({
      success: false,
      message: 'Failed to generate stats: ' + err.message
    });
  }
};

module.exports = {
  getTransactions,
  createTransaction,
  deleteTransaction,
  getCategories,
  getTransactionStats
};
