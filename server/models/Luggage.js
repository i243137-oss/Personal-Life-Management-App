const mongoose = require('mongoose');
const { isDBConnected } = require('../config/db');

const luggageItemSchema = new mongoose.Schema({
  name: {
    type: String,
    required: [true, 'Item name is required'],
    trim: true,
  },
  category: {
    type: String,
    enum: ['Clothing', 'Electronics', 'Toiletries', 'Documents', 'Medication', 'Valuables', 'Other'],
    default: 'Other',
  },
  quantity: {
    type: Number,
    default: 1,
    min: 1,
  },
  isPacked: {
    type: Boolean,
    default: false,
  },
  isEssential: {
    type: Boolean,
    default: false,
  },
  weightKg: {
    type: Number,
    default: 0,
    min: 0,
  },
  notes: {
    type: String,
    trim: true,
    default: '',
  },
});

const luggageTripSchema = new mongoose.Schema(
  {
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
      index: true,
    },
    title: {
      type: String,
      required: [true, 'Trip title is required'],
      trim: true,
    },
    destination: {
      type: String,
      trim: true,
      default: '',
    },
    bagType: {
      type: String,
      enum: ['Cabin Bag', 'Checked Suitcase', 'Backpack', 'Duffel Bag', 'Tech Pouch'],
      default: 'Cabin Bag',
    },
    departureDate: {
      type: String,
      trim: true,
      default: '',
    },
    returnDate: {
      type: String,
      trim: true,
      default: '',
    },
    maxWeightKg: {
      type: Number,
      default: 7.0,
      min: 0,
    },
    colorHex: {
      type: String,
      default: '#3B82F6',
    },
    items: [luggageItemSchema],
  },
  {
    timestamps: true,
  }
);

// In-memory mock fallback when MongoDB is not connected
const mockLuggageStore = [];

class LuggageMockModel {
  constructor(data) {
    this._id = data._id || `luggage_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
    this.userId = data.userId;
    this.title = data.title;
    this.destination = data.destination || '';
    this.bagType = data.bagType || 'Cabin Bag';
    this.departureDate = data.departureDate || '';
    this.returnDate = data.returnDate || '';
    this.maxWeightKg = data.maxWeightKg !== undefined ? Number(data.maxWeightKg) : 7.0;
    this.colorHex = data.colorHex || '#3B82F6';
    this.items = (data.items || []).map((item, idx) => ({
      _id: item._id || `item_${Date.now()}_${idx}`,
      name: item.name,
      category: item.category || 'Other',
      quantity: item.quantity || 1,
      isPacked: Boolean(item.isPacked),
      isEssential: Boolean(item.isEssential),
      weightKg: Number(item.weightKg || 0),
      notes: item.notes || '',
    }));
    this.createdAt = data.createdAt || new Date().toISOString();
    this.updatedAt = data.updatedAt || new Date().toISOString();
  }

  async save() {
    const existingIndex = mockLuggageStore.findIndex(
      (t) => t._id.toString() === this._id.toString()
    );
    this.updatedAt = new Date().toISOString();
    if (existingIndex >= 0) {
      mockLuggageStore[existingIndex] = this;
    } else {
      mockLuggageStore.unshift(this);
    }
    return this;
  }

  static async find(query = {}) {
    let result = mockLuggageStore.filter((t) => {
      if (query.userId && t.userId.toString() !== query.userId.toString()) return false;
      return true;
    });
    return result;
  }

  static async findById(id) {
    return mockLuggageStore.find((t) => t._id.toString() === id.toString()) || null;
  }

  static async findByIdAndDelete(id) {
    const index = mockLuggageStore.findIndex((t) => t._id.toString() === id.toString());
    if (index >= 0) {
      const removed = mockLuggageStore.splice(index, 1);
      return removed[0];
    }
    return null;
  }

  static async countDocuments(query = {}) {
    return (await this.find(query)).length;
  }
}

// Export Mongoose Model or Mock Proxy
const MongooseLuggage = mongoose.model('Luggage', luggageTripSchema);

const LuggageProxy = new Proxy(MongooseLuggage, {
  get(target, prop) {
    if (!isDBConnected()) {
      return LuggageMockModel[prop] || LuggageMockModel.prototype[prop];
    }
    return target[prop];
  },
  construct(target, args) {
    if (!isDBConnected()) {
      return new LuggageMockModel(...args);
    }
    return new target(...args);
  },
});

module.exports = LuggageProxy;
