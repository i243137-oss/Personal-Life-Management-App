const mongoose = require('mongoose');
const { isDBConnected } = require('../config/db');

const userSchema = new mongoose.Schema({
  name: {
    type: String,
    required: [true, 'Please provide your name'],
    trim: true,
  },
  email: {
    type: String,
    required: [true, 'Please provide your email'],
    unique: true,
    lowercase: true,
    trim: true,
  },
  passwordHash: {
    type: String,
    required: [true, 'Please provide a password'],
    minlength: 6,
  },
  createdAt: {
    type: Date,
    default: Date.now,
  },
});

let MongooseUserModel;
try {
  MongooseUserModel = mongoose.model('User', userSchema);
} catch (e) {
  MongooseUserModel = mongoose.model('User');
}

// In-memory collection fallback when MongoDB server is not running
const memoryUsers = [];

const User = {
  schema: userSchema,

  async findOne(query) {
    if (isDBConnected()) {
      return await MongooseUserModel.findOne(query);
    }
    if (query.email) {
      const email = query.email.toString().toLowerCase().trim();
      return memoryUsers.find((u) => u.email === email) || null;
    }
    return null;
  },

  async findById(id) {
    if (isDBConnected()) {
      return await MongooseUserModel.findById(id);
    }
    const found = memoryUsers.find((u) => u._id.toString() === id.toString());
    if (!found) return null;
    return {
      ...found,
      select: function () {
        return this;
      },
    };
  },

  async create(userData) {
    if (isDBConnected()) {
      return await MongooseUserModel.create(userData);
    }
    const email = userData.email.toLowerCase().trim();
    if (memoryUsers.some((u) => u.email === email)) {
      const err = new Error('E11000 duplicate key error collection: user email already exists');
      err.code = 11000;
      throw err;
    }
    const newUser = {
      _id: new mongoose.Types.ObjectId(),
      name: userData.name,
      email: email,
      passwordHash: userData.passwordHash,
      createdAt: new Date(),
    };
    memoryUsers.push(newUser);
    return newUser;
  },

  async deleteMany(query) {
    if (isDBConnected()) {
      return await MongooseUserModel.deleteMany(query);
    }
    if (query && query.email) {
      const initialLen = memoryUsers.length;
      const filtered = memoryUsers.filter((u) => !query.email.test(u.email));
      memoryUsers.length = 0;
      memoryUsers.push(...filtered);
      return { deletedCount: initialLen - filtered.length };
    }
    const count = memoryUsers.length;
    memoryUsers.length = 0;
    return { deletedCount: count };
  },
};

module.exports = User;
