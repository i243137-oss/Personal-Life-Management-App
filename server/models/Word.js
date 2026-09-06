const mongoose = require('mongoose');
const { isDBConnected } = require('../config/db');

const wordSchema = new mongoose.Schema({
  userId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
    index: true,
  },
  word: {
    type: String,
    required: [true, 'Word is required'],
    trim: true,
  },
  mode: {
    type: String,
    enum: ['meaning', 'explain'],
    default: 'meaning',
  },
  phonetic: {
    type: String,
    trim: true,
    default: '',
  },
  partOfSpeech: {
    type: String,
    trim: true,
    default: '',
  },
  shortDefinition: {
    type: String,
    trim: true,
    default: '',
  },
  fullDefinition: {
    type: String,
    trim: true,
    default: '',
  },
  synonyms: {
    type: [String],
    default: [],
  },
  antonyms: {
    type: [String],
    default: [],
  },
  examples: {
    type: [String],
    default: [],
  },
  keyPoints: {
    type: [String],
    default: [],
  },
  eli5Analogy: {
    type: String,
    trim: true,
    default: '',
  },
  keyTakeaway: {
    type: String,
    trim: true,
    default: '',
  },
  masteryStatus: {
    type: String,
    enum: ['learning', 'reviewing', 'mastered'],
    default: 'learning',
  },
  personalNotes: {
    type: String,
    trim: true,
    default: '',
  },
  etymology: {
    type: String,
    trim: true,
    default: '',
  },
  audioUrl: {
    type: String,
    trim: true,
    default: '',
  },
  relatedWords: {
    type: [String],
    default: [],
  },
  source: {
    type: String,
    default: 'Merriam-Webster',
  },
  createdAt: {
    type: Date,
    default: Date.now,
  },
  updatedAt: {
    type: Date,
    default: Date.now,
  }
});

wordSchema.index({ userId: 1, word: 1 });

const MongooseWord = mongoose.model('Word', wordSchema);

// In-Memory Storage for standalone / offline resilience
const inMemoryWords = [];

class WordModelAdapter {
  static async create(data) {
    if (isDBConnected()) {
      try {
        return await MongooseWord.create(data);
      } catch (e) {
        console.warn('Mongoose Word.create failed, falling back to in-memory:', e.message);
      }
    }

    const doc = {
      _id: 'word_' + Math.random().toString(36).substring(2, 10),
      userId: data.userId,
      word: data.word,
      mode: data.mode || 'meaning',
      phonetic: data.phonetic || '',
      partOfSpeech: data.partOfSpeech || '',
      shortDefinition: data.shortDefinition || '',
      fullDefinition: data.fullDefinition || '',
      synonyms: data.synonyms || [],
      antonyms: data.antonyms || [],
      examples: data.examples || [],
      keyPoints: data.keyPoints || [],
      eli5Analogy: data.eli5Analogy || '',
      keyTakeaway: data.keyTakeaway || '',
      masteryStatus: data.masteryStatus || 'learning',
      personalNotes: data.personalNotes || '',
      etymology: data.etymology || '',
      audioUrl: data.audioUrl || '',
      relatedWords: data.relatedWords || [],
      source: data.source || 'Merriam-Webster',
      createdAt: new Date(),
      updatedAt: new Date(),
      save: async function() { return this; }
    };
    inMemoryWords.push(doc);
    return doc;
  }

  static async find(query = {}) {
    if (isDBConnected()) {
      try {
        return await MongooseWord.find(query).sort({ createdAt: -1 });
      } catch (e) {
        console.warn('Mongoose Word.find failed, falling back to in-memory:', e.message);
      }
    }

    let results = inMemoryWords.filter(w => {
      if (query.userId && w.userId.toString() !== query.userId.toString()) return false;
      if (query.masteryStatus && w.masteryStatus !== query.masteryStatus) return false;
      return true;
    });

    results.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    return results;
  }

  static async findById(id) {
    if (isDBConnected()) {
      try {
        return await MongooseWord.findById(id);
      } catch (e) {
        console.warn('Mongoose Word.findById failed, falling back to in-memory:', e.message);
      }
    }
    return inMemoryWords.find(w => w._id.toString() === id.toString()) || null;
  }

  static async findOne(query) {
    if (isDBConnected()) {
      try {
        return await MongooseWord.findOne(query);
      } catch (e) {
        console.warn('Mongoose Word.findOne failed, falling back to in-memory:', e.message);
      }
    }
    return inMemoryWords.find(w => {
      if (query.userId && w.userId.toString() !== query.userId.toString()) return false;
      if (query.word && w.word.toLowerCase() !== query.word.toLowerCase()) return false;
      return true;
    }) || null;
  }

  static async findByIdAndUpdate(id, update, options = {}) {
    if (isDBConnected()) {
      try {
        return await MongooseWord.findByIdAndUpdate(id, update, options);
      } catch (e) {
        console.warn('Mongoose Word.findByIdAndUpdate failed, falling back to in-memory:', e.message);
      }
    }

    const idx = inMemoryWords.findIndex(w => w._id.toString() === id.toString());
    if (idx === -1) return null;

    const fields = update.$set ? update.$set : update;
    inMemoryWords[idx] = {
      ...inMemoryWords[idx],
      ...fields,
      updatedAt: new Date()
    };
    return inMemoryWords[idx];
  }

  static async findByIdAndDelete(id) {
    if (isDBConnected()) {
      try {
        return await MongooseWord.findByIdAndDelete(id);
      } catch (e) {
        console.warn('Mongoose Word.findByIdAndDelete failed, falling back to in-memory:', e.message);
      }
    }

    const idx = inMemoryWords.findIndex(w => w._id.toString() === id.toString());
    if (idx === -1) return null;
    return inMemoryWords.splice(idx, 1)[0];
  }

  static async countDocuments(query = {}) {
    if (isDBConnected()) {
      try {
        return await MongooseWord.countDocuments(query);
      } catch (e) {
        console.warn('Mongoose countDocuments failed, falling back to in-memory:', e.message);
      }
    }
    return inMemoryWords.filter(w => {
      if (query.userId && w.userId.toString() !== query.userId.toString()) return false;
      if (query.masteryStatus && w.masteryStatus !== query.masteryStatus) return false;
      return true;
    }).length;
  }
}

module.exports = WordModelAdapter;
