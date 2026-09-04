const mongoose = require('mongoose');

const checklistItemSchema = new mongoose.Schema(
  {
    text: {
      type: String,
      required: true,
      trim: true,
    },
    isDone: {
      type: Boolean,
      default: false,
    },
  },
  { _id: true }
);

const noteSchema = new mongoose.Schema(
  {
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
      index: true,
    },
    title: {
      type: String,
      required: [true, 'Note title is required'],
      trim: true,
      maxlength: [200, 'Title cannot exceed 200 characters'],
    },
    content: {
      type: String,
      default: '',
      trim: true,
    },
    category: {
      type: String,
      default: 'Personal',
      enum: ['Personal', 'Work', 'Ideas', 'Travel', 'Finances', 'Checklist', 'Archive', 'Other'],
    },
    tags: {
      type: [String],
      default: [],
    },
    isPinned: {
      type: Boolean,
      default: false,
    },
    isArchived: {
      type: Boolean,
      default: false,
    },
    colorHex: {
      type: String,
      default: '#FEF3C7', // Default warm yellow note
    },
    checklist: {
      type: [checklistItemSchema],
      default: [],
    },
  },
  {
    timestamps: true,
  }
);

// Indexes for fast querying & search
noteSchema.index({ userId: 1, isArchived: 1, isPinned: -1, updatedAt: -1 });

module.exports = mongoose.model('Note', noteSchema);
