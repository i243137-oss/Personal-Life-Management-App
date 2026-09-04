const Note = require('../models/Note');

// @desc    Get all notes for authenticated user
// @route   GET /api/notes
// @access  Private
const getNotes = async (req, res) => {
  try {
    const userId = req.user._id;
    const { category, search, isPinned, isArchived, tag } = req.query;

    const query = { userId };

    if (isArchived !== undefined) {
      query.isArchived = isArchived === 'true';
    } else {
      // Default: show non-archived notes unless explicitly requested
      query.isArchived = false;
    }

    if (isPinned !== undefined) {
      query.isPinned = isPinned === 'true';
    }

    if (category && category !== 'All') {
      query.category = category;
    }

    if (tag) {
      query.tags = tag;
    }

    if (search && search.trim() !== '') {
      const regex = new RegExp(search.trim(), 'i');
      query.$or = [{ title: regex }, { content: regex }, { tags: regex }];
    }

    // Sort: pinned first, then most recently updated
    const notes = await Note.find(query).sort({ isPinned: -1, updatedAt: -1 });

    return res.status(200).json({
      success: true,
      count: notes.length,
      data: notes,
    });
  } catch (err) {
    console.error('Error fetching notes:', err);
    return res.status(500).json({
      success: false,
      message: 'Failed to retrieve notes',
      error: err.message,
    });
  }
};

// @desc    Get a single note by ID
// @route   GET /api/notes/:id
// @access  Private
const getNoteById = async (req, res) => {
  try {
    const userId = req.user._id;
    const { id } = req.params;

    const note = await Note.findOne({ _id: id, userId });
    if (!note) {
      return res.status(404).json({
        success: false,
        message: 'Note not found',
      });
    }

    return res.status(200).json({
      success: true,
      data: note,
    });
  } catch (err) {
    console.error('Error fetching note by ID:', err);
    return res.status(500).json({
      success: false,
      message: 'Failed to retrieve note',
      error: err.message,
    });
  }
};

// @desc    Create a new note
// @route   POST /api/notes
// @access  Private
const createNote = async (req, res) => {
  try {
    const userId = req.user._id;
    const {
      title,
      content,
      category,
      tags,
      isPinned,
      isArchived,
      colorHex,
      checklist,
    } = req.body;

    if (!title || title.trim() === '') {
      return res.status(400).json({
        success: false,
        message: 'Note title is required',
      });
    }

    const note = await Note.create({
      userId,
      title: title.trim(),
      content: content ? content.trim() : '',
      category: category || 'Personal',
      tags: Array.isArray(tags) ? tags : [],
      isPinned: Boolean(isPinned),
      isArchived: Boolean(isArchived),
      colorHex: colorHex || '#FEF3C7',
      checklist: Array.isArray(checklist) ? checklist : [],
    });

    return res.status(201).json({
      success: true,
      message: 'Note created successfully',
      data: note,
    });
  } catch (err) {
    console.error('Error creating note:', err);
    return res.status(500).json({
      success: false,
      message: 'Failed to create note',
      error: err.message,
    });
  }
};

// @desc    Update an existing note
// @route   PUT /api/notes/:id
// @access  Private
const updateNote = async (req, res) => {
  try {
    const userId = req.user._id;
    const { id } = req.params;
    const {
      title,
      content,
      category,
      tags,
      isPinned,
      isArchived,
      colorHex,
      checklist,
    } = req.body;

    const note = await Note.findOne({ _id: id, userId });
    if (!note) {
      return res.status(404).json({
        success: false,
        message: 'Note not found',
      });
    }

    if (title !== undefined) note.title = title.trim();
    if (content !== undefined) note.content = content.trim();
    if (category !== undefined) note.category = category;
    if (tags !== undefined) note.tags = Array.isArray(tags) ? tags : note.tags;
    if (isPinned !== undefined) note.isPinned = Boolean(isPinned);
    if (isArchived !== undefined) note.isArchived = Boolean(isArchived);
    if (colorHex !== undefined) note.colorHex = colorHex;
    if (checklist !== undefined) note.checklist = Array.isArray(checklist) ? checklist : note.checklist;

    await note.save();

    return res.status(200).json({
      success: true,
      message: 'Note updated successfully',
      data: note,
    });
  } catch (err) {
    console.error('Error updating note:', err);
    return res.status(500).json({
      success: false,
      message: 'Failed to update note',
      error: err.message,
    });
  }
};

// @desc    Delete a note
// @route   DELETE /api/notes/:id
// @access  Private
const deleteNote = async (req, res) => {
  try {
    const userId = req.user._id;
    const { id } = req.params;

    const note = await Note.findOneAndDelete({ _id: id, userId });
    if (!note) {
      return res.status(404).json({
        success: false,
        message: 'Note not found',
      });
    }

    return res.status(200).json({
      success: true,
      message: 'Note deleted successfully',
    });
  } catch (err) {
    console.error('Error deleting note:', err);
    return res.status(500).json({
      success: false,
      message: 'Failed to delete note',
      error: err.message,
    });
  }
};

// @desc    Toggle pin status for a note
// @route   PATCH /api/notes/:id/pin
// @access  Private
const togglePinNote = async (req, res) => {
  try {
    const userId = req.user._id;
    const { id } = req.params;

    const note = await Note.findOne({ _id: id, userId });
    if (!note) {
      return res.status(404).json({
        success: false,
        message: 'Note not found',
      });
    }

    note.isPinned = !note.isPinned;
    await note.save();

    return res.status(200).json({
      success: true,
      message: note.isPinned ? 'Note pinned' : 'Note unpinned',
      data: note,
    });
  } catch (err) {
    console.error('Error toggling pin status:', err);
    return res.status(500).json({
      success: false,
      message: 'Failed to toggle pin status',
      error: err.message,
    });
  }
};

// @desc    Toggle archive status for a note
// @route   PATCH /api/notes/:id/archive
// @access  Private
const toggleArchiveNote = async (req, res) => {
  try {
    const userId = req.user._id;
    const { id } = req.params;

    const note = await Note.findOne({ _id: id, userId });
    if (!note) {
      return res.status(404).json({
        success: false,
        message: 'Note not found',
      });
    }

    note.isArchived = !note.isArchived;
    // If archived, unpin automatically
    if (note.isArchived) {
      note.isPinned = false;
    }
    await note.save();

    return res.status(200).json({
      success: true,
      message: note.isArchived ? 'Note archived' : 'Note restored from archive',
      data: note,
    });
  } catch (err) {
    console.error('Error toggling archive status:', err);
    return res.status(500).json({
      success: false,
      message: 'Failed to toggle archive status',
      error: err.message,
    });
  }
};

module.exports = {
  getNotes,
  getNoteById,
  createNote,
  updateNote,
  deleteNote,
  togglePinNote,
  toggleArchiveNote,
};
