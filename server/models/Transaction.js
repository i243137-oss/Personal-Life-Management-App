const mongoose = require('mongoose');
const { isDBConnected } = require('../config/db');

const transactionSchema = new mongoose.Schema({
  userId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
    index: true,
  },
  type: {
    type: String,
    enum: [
      'income',
      'expense',
      'loan_given',
      'loan_received',
      'loan_repayment_received',
      'loan_repayment_sent',
    ],
    required: true,
  },
  amount: {
    type: Number,
    required: true,
    min: 0,
  },
  category: {
    type: String,
    required: true,
    trim: true,
  },
  description: {
    type: String,
    trim: true,
    default: '',
  },
  date: {
    type: Date,
    default: Date.now,
  },
  createdAt: {
    type: Date,
    default: Date.now,
  },
});

let MongooseTxModel;
try {
  MongooseTxModel = mongoose.model('Transaction', transactionSchema);
} catch (e) {
  MongooseTxModel = mongoose.model('Transaction');
}

const memoryTransactions = [];

const Transaction = {
  schema: transactionSchema,

  find(query) {
    if (isDBConnected()) {
      return MongooseTxModel.find(query);
    }
    let list = [...memoryTransactions];
    if (query && query.userId) {
      list = list.filter((t) => t.userId && t.userId.toString() === query.userId.toString());
    }
    if (query && query.type && query.type !== 'all') {
      list = list.filter((t) => t.type === query.type);
    }
    if (query && query.category) {
      list = list.filter((t) => t.category.toLowerCase() === query.category.toLowerCase());
    }
    const queryObj = {
      sort(sortObj) {
        if (sortObj && sortObj.date === -1) {
          list.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
        } else if (sortObj && sortObj.date === 1) {
          list.sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());
        }
        return queryObj;
      },
      limit(n) {
        list = list.slice(0, n);
        return queryObj;
      },
      then(resolve, reject) {
        return Promise.resolve(list).then(resolve, reject);
      },
      catch(reject) {
        return Promise.resolve(list).catch(reject);
      },
    };
    return queryObj;
  },

  async findById(id) {
    if (isDBConnected()) {
      return await MongooseTxModel.findById(id);
    }
    return memoryTransactions.find((t) => t._id.toString() === id.toString()) || null;
  },

  async findByIdAndDelete(id) {
    if (isDBConnected()) {
      return await MongooseTxModel.findByIdAndDelete(id);
    }
    const index = memoryTransactions.findIndex((t) => t._id.toString() === id.toString());
    if (index !== -1) {
      const removed = memoryTransactions.splice(index, 1)[0];
      return removed;
    }
    return null;
  },

  async create(txData) {
    if (isDBConnected()) {
      return await MongooseTxModel.create(txData);
    }
    const newTx = {
      _id: new mongoose.Types.ObjectId(),
      userId: txData.userId,
      type: txData.type,
      amount: Number(txData.amount),
      category: txData.category,
      description: txData.description || '',
      date: txData.date || new Date(),
      createdAt: new Date(),
    };
    memoryTransactions.push(newTx);
    return newTx;
  },

  async deleteMany(query) {
    if (isDBConnected()) {
      return await MongooseTxModel.deleteMany(query);
    }
    const count = memoryTransactions.length;
    memoryTransactions.length = 0;
    return { deletedCount: count };
  },
};

module.exports = Transaction;
