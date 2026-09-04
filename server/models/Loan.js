const mongoose = require('mongoose');
const { isDBConnected } = require('../config/db');

const repaymentSchema = new mongoose.Schema({
  amount: {
    type: Number,
    required: true,
    min: 1,
  },
  date: {
    type: Date,
    default: Date.now,
  },
  notes: {
    type: String,
    trim: true,
    default: '',
  },
});

const loanSchema = new mongoose.Schema({
  userId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
    index: true,
  },
  personName: {
    type: String,
    required: [true, 'Please provide person name'],
    trim: true,
  },
  phoneNumber: {
    type: String,
    trim: true,
    default: '',
  },
  type: {
    type: String,
    enum: ['lent', 'borrowed'], // lent = I gave money (they owe me), borrowed = I took money (I owe them)
    required: true,
  },
  amount: {
    type: Number,
    required: [true, 'Please provide loan amount'],
    min: 1,
  },
  remainingAmount: {
    type: Number,
    required: true,
    min: 0,
  },
  dueDate: {
    type: Date,
    default: null,
  },
  status: {
    type: String,
    enum: ['pending', 'partially_paid', 'paid'],
    default: 'pending',
  },
  notes: {
    type: String,
    trim: true,
    default: '',
  },
  repayments: [repaymentSchema],
  affectBalance: {
    type: Boolean,
    default: true,
  },
  createdAt: {
    type: Date,
    default: Date.now,
  },
  updatedAt: {
    type: Date,
    default: Date.now,
  },
});

let MongooseLoanModel;
try {
  MongooseLoanModel = mongoose.model('Loan', loanSchema);
} catch (e) {
  MongooseLoanModel = mongoose.model('Loan');
}

// In-memory fallback
const memoryLoans = [];

const Loan = {
  schema: loanSchema,

  find(query = {}) {
    if (isDBConnected()) {
      return MongooseLoanModel.find(query);
    }

    let results = memoryLoans.filter((item) => {
      if (query.userId && item.userId.toString() !== query.userId.toString()) {
        return false;
      }
      if (query.type && item.type !== query.type) {
        return false;
      }
      if (query.status && item.status !== query.status) {
        return false;
      }
      return true;
    });

    const chainable = {
      _results: results,
      sort(sortObj) {
        if (sortObj) {
          const key = Object.keys(sortObj)[0];
          const dir = sortObj[key];
          this._results.sort((a, b) => {
            const valA = a[key] ? new Date(a[key]).getTime() || a[key] : 0;
            const valB = b[key] ? new Date(b[key]).getTime() || b[key] : 0;
            return dir === -1 ? (valB > valA ? 1 : -1) : (valA > valB ? 1 : -1);
          });
        }
        return this;
      },
      limit(n) {
        if (n && typeof n === 'number') {
          this._results = this._results.slice(0, n);
        }
        return this;
      },
      then(resolve, reject) {
        return Promise.resolve(this._results).then(resolve, reject);
      },
    };

    return chainable;
  },

  async findById(id) {
    if (isDBConnected()) {
      return await MongooseLoanModel.findById(id);
    }
    const found = memoryLoans.find((item) => item._id.toString() === id.toString());
    return found ? { ...found } : null;
  },

  async create(loanData) {
    if (isDBConnected()) {
      return await MongooseLoanModel.create(loanData);
    }

    const newLoan = {
      _id: 'loan_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9),
      userId: loanData.userId,
      personName: loanData.personName.trim(),
      phoneNumber: loanData.phoneNumber ? loanData.phoneNumber.trim() : '',
      type: loanData.type,
      amount: Number(loanData.amount),
      remainingAmount: Number(loanData.remainingAmount !== undefined ? loanData.remainingAmount : loanData.amount),
      dueDate: loanData.dueDate ? new Date(loanData.dueDate) : null,
      status: loanData.status || 'pending',
      notes: loanData.notes ? loanData.notes.trim() : '',
      repayments: loanData.repayments || [],
      affectBalance: loanData.affectBalance !== undefined ? Boolean(loanData.affectBalance) : true,
      createdAt: new Date(),
      updatedAt: new Date(),
    };

    memoryLoans.unshift(newLoan);
    return { ...newLoan };
  },

  async findByIdAndUpdate(id, updateData, options = { new: true }) {
    if (isDBConnected()) {
      return await MongooseLoanModel.findByIdAndUpdate(id, updateData, options);
    }

    const index = memoryLoans.findIndex((item) => item._id.toString() === id.toString());
    if (index === -1) return null;

    const existing = memoryLoans[index];
    const updated = {
      ...existing,
      ...updateData,
      updatedAt: new Date(),
    };

    if (updateData.$push && updateData.$push.repayments) {
      updated.repayments = [...(existing.repayments || []), updateData.$push.repayments];
      delete updated.$push;
    }

    memoryLoans[index] = updated;
    return { ...updated };
  },

  async findByIdAndDelete(id) {
    if (isDBConnected()) {
      return await MongooseLoanModel.findByIdAndDelete(id);
    }

    const index = memoryLoans.findIndex((item) => item._id.toString() === id.toString());
    if (index === -1) return null;

    const deleted = memoryLoans.splice(index, 1)[0];
    return deleted;
  },
};

module.exports = Loan;
