const Luggage = require('../models/Luggage');

const PREMADE_TEMPLATES = {
  weekend: [
    { name: 'Casual T-Shirts (3)', category: 'Clothing', quantity: 3, isEssential: true, weightKg: 0.6 },
    { name: 'Jeans / Pants (2)', category: 'Clothing', quantity: 2, isEssential: true, weightKg: 1.0 },
    { name: 'Underwear & Socks', category: 'Clothing', quantity: 3, isEssential: true, weightKg: 0.3 },
    { name: 'Toothbrush & Paste', category: 'Toiletries', quantity: 1, isEssential: true, weightKg: 0.2 },
    { name: 'Deodorant / Fragrance', category: 'Toiletries', quantity: 1, isEssential: false, weightKg: 0.2 },
    { name: 'Phone Fast Charger', category: 'Electronics', quantity: 1, isEssential: true, weightKg: 0.15 },
    { name: 'Power Bank 10,000mAh', category: 'Electronics', quantity: 1, isEssential: true, weightKg: 0.3 },
    { name: 'CNIC / National ID Card', category: 'Documents', quantity: 1, isEssential: true, weightKg: 0.05 },
    { name: 'Emergency Cash / ATM Card', category: 'Valuables', quantity: 1, isEssential: true, weightKg: 0.05 },
  ],
  international: [
    { name: 'Passport & Visa Copies', category: 'Documents', quantity: 1, isEssential: true, weightKg: 0.1 },
    { name: 'Flight Boarding Pass / Tickets', category: 'Documents', quantity: 1, isEssential: true, weightKg: 0.05 },
    { name: 'Universal Travel Adapter', category: 'Electronics', quantity: 1, isEssential: true, weightKg: 0.2 },
    { name: 'Noise-Canceling Headphones', category: 'Electronics', quantity: 1, isEssential: false, weightKg: 0.35 },
    { name: 'Prescription Medicines & First Aid', category: 'Medication', quantity: 1, isEssential: true, weightKg: 0.3 },
    { name: 'Clear Toiletries Bag (<100ml)', category: 'Toiletries', quantity: 1, isEssential: true, weightKg: 0.5 },
    { name: 'Formal & Casual Outfits', category: 'Clothing', quantity: 5, isEssential: true, weightKg: 2.5 },
    { name: 'Light Jacket / Cardigan', category: 'Clothing', quantity: 1, isEssential: true, weightKg: 0.8 },
    { name: 'Travel Pillow & Eye Mask', category: 'Other', quantity: 1, isEssential: false, weightKg: 0.25 },
  ],
  tech: [
    { name: 'Laptop & Charger', category: 'Electronics', quantity: 1, isEssential: true, weightKg: 1.8 },
    { name: 'Smartphone & Backup Cables', category: 'Electronics', quantity: 2, isEssential: true, weightKg: 0.4 },
    { name: 'Wireless Mouse & Mousepad', category: 'Electronics', quantity: 1, isEssential: false, weightKg: 0.2 },
    { name: 'Notepad & Pen', category: 'Other', quantity: 1, isEssential: true, weightKg: 0.2 },
    { name: 'Office Access Badge / Keys', category: 'Valuables', quantity: 1, isEssential: true, weightKg: 0.05 },
    { name: 'Water Bottle (Tumbler)', category: 'Other', quantity: 1, isEssential: false, weightKg: 0.4 },
  ],
  hiking: [
    { name: 'Trekking Boots & Heavy Socks', category: 'Clothing', quantity: 2, isEssential: true, weightKg: 1.5 },
    { name: 'Waterproof Rain Jacket', category: 'Clothing', quantity: 1, isEssential: true, weightKg: 0.6 },
    { name: 'First Aid Kit & Bandages', category: 'Medication', quantity: 1, isEssential: true, weightKg: 0.4 },
    { name: 'Flashlight / Headlamp + Extra Batteries', category: 'Electronics', quantity: 1, isEssential: true, weightKg: 0.3 },
    { name: 'Electrolytes & Energy Bars', category: 'Other', quantity: 4, isEssential: true, weightKg: 0.4 },
    { name: 'Sunscreen & Insect Repellent', category: 'Toiletries', quantity: 1, isEssential: true, weightKg: 0.25 },
  ],
};

function enrichTrip(trip) {
  const rawItems = trip.items || [];
  const items = rawItems.map((i) => {
    const itemObj = i.toObject ? i.toObject() : { ...i };
    itemObj._id = (itemObj._id || '').toString();
    return itemObj;
  });
  const totalItems = items.length;
  const packedItems = items.filter((i) => i.isPacked).length;
  const totalWeightKg = items.reduce((acc, i) => acc + (i.weightKg || 0) * (i.quantity || 1), 0);
  const packedWeightKg = items
    .filter((i) => i.isPacked)
    .reduce((acc, i) => acc + (i.weightKg || 0) * (i.quantity || 1), 0);

  const obj = trip.toObject ? trip.toObject() : { ...trip };
  obj._id = (obj._id || '').toString();
  obj.items = items;
  obj.totalItems = totalItems;
  obj.packedItems = packedItems;
  obj.totalWeightKg = Math.round(totalWeightKg * 100) / 100;
  obj.packedWeightKg = Math.round(packedWeightKg * 100) / 100;
  obj.isWeightExceeded = obj.maxWeightKg > 0 && obj.totalWeightKg > obj.maxWeightKg;
  return obj;
}

// @desc    Get all luggage trips for logged in user
// @route   GET /api/luggage
exports.getTrips = async (req, res) => {
  try {
    const trips = await Luggage.find({ userId: req.user.id });
    const enriched = trips.map(enrichTrip);
    res.status(200).json({
      success: true,
      count: enriched.length,
      data: enriched,
    });
  } catch (error) {
    console.error('Error fetching luggage trips:', error);
    res.status(500).json({ success: false, message: error.message });
  }
};

// @desc    Get single trip with items
// @route   GET /api/luggage/:id
exports.getTripById = async (req, res) => {
  try {
    const trip = await Luggage.findById(req.params.id);
    if (!trip || trip.userId.toString() !== req.user.id.toString()) {
      return res.status(404).json({ success: false, message: 'Luggage trip not found' });
    }
    res.status(200).json({
      success: true,
      data: enrichTrip(trip),
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

// @desc    Create new luggage trip/bag
// @route   POST /api/luggage
exports.createTrip = async (req, res) => {
  try {
    const { title, destination, bagType, departureDate, returnDate, maxWeightKg, colorHex } = req.body;
    if (!title || !title.trim()) {
      return res.status(400).json({ success: false, message: 'Trip title is required' });
    }

    const trip = new Luggage({
      userId: req.user.id,
      title: title.trim(),
      destination: destination ? destination.trim() : '',
      bagType: bagType || 'Cabin Bag',
      departureDate: departureDate || '',
      returnDate: returnDate || '',
      maxWeightKg: maxWeightKg !== undefined ? Number(maxWeightKg) : 7.0,
      colorHex: colorHex || '#3B82F6',
      items: [],
    });

    await trip.save();
    res.status(201).json({
      success: true,
      message: 'Luggage trip created successfully',
      data: enrichTrip(trip),
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

// @desc    Update trip info
// @route   PUT /api/luggage/:id
exports.updateTrip = async (req, res) => {
  try {
    const trip = await Luggage.findById(req.params.id);
    if (!trip || trip.userId.toString() !== req.user.id.toString()) {
      return res.status(404).json({ success: false, message: 'Luggage trip not found' });
    }

    const fields = ['title', 'destination', 'bagType', 'departureDate', 'returnDate', 'maxWeightKg', 'colorHex'];
    fields.forEach((f) => {
      if (req.body[f] !== undefined) {
        trip[f] = req.body[f];
      }
    });

    await trip.save();
    res.status(200).json({
      success: true,
      message: 'Luggage trip updated',
      data: enrichTrip(trip),
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

// @desc    Delete trip
// @route   DELETE /api/luggage/:id
exports.deleteTrip = async (req, res) => {
  try {
    const trip = await Luggage.findById(req.params.id);
    if (!trip || trip.userId.toString() !== req.user.id.toString()) {
      return res.status(404).json({ success: false, message: 'Luggage trip not found' });
    }

    await Luggage.findByIdAndDelete(req.params.id);
    res.status(200).json({
      success: true,
      message: 'Luggage trip deleted successfully',
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

// @desc    Add item to luggage
// @route   POST /api/luggage/:id/items
exports.addItem = async (req, res) => {
  try {
    const trip = await Luggage.findById(req.params.id);
    if (!trip || trip.userId.toString() !== req.user.id.toString()) {
      return res.status(404).json({ success: false, message: 'Luggage trip not found' });
    }

    const { name, category, quantity, isPacked, isEssential, weightKg, notes } = req.body;
    if (!name || !name.trim()) {
      return res.status(400).json({ success: false, message: 'Item name is required' });
    }

    const newItem = {
      name: name.trim(),
      category: category || 'Other',
      quantity: quantity ? Number(quantity) : 1,
      isPacked: Boolean(isPacked),
      isEssential: Boolean(isEssential),
      weightKg: weightKg ? Number(weightKg) : 0,
      notes: notes ? notes.trim() : '',
    };

    trip.items.push(newItem);
    await trip.save();

    res.status(201).json({
      success: true,
      message: 'Item added to luggage',
      data: enrichTrip(trip),
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

// @desc    Toggle item packed status
// @route   PATCH /api/luggage/:id/items/:itemId/toggle
exports.toggleItem = async (req, res) => {
  try {
    const trip = await Luggage.findById(req.params.id);
    if (!trip || trip.userId.toString() !== req.user.id.toString()) {
      return res.status(404).json({ success: false, message: 'Luggage trip not found' });
    }

    const item = trip.items.find((i) => i._id.toString() === req.params.itemId.toString());
    if (!item) {
      return res.status(404).json({ success: false, message: 'Item not found in trip' });
    }

    item.isPacked = !item.isPacked;
    await trip.save();

    res.status(200).json({
      success: true,
      data: enrichTrip(trip),
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

// @desc    Update item
// @route   PUT /api/luggage/:id/items/:itemId
exports.updateItem = async (req, res) => {
  try {
    const trip = await Luggage.findById(req.params.id);
    if (!trip || trip.userId.toString() !== req.user.id.toString()) {
      return res.status(404).json({ success: false, message: 'Luggage trip not found' });
    }

    const item = trip.items.find((i) => i._id.toString() === req.params.itemId.toString());
    if (!item) {
      return res.status(404).json({ success: false, message: 'Item not found in trip' });
    }

    ['name', 'category', 'quantity', 'isPacked', 'isEssential', 'weightKg', 'notes'].forEach((k) => {
      if (req.body[k] !== undefined) {
        item[k] = req.body[k];
      }
    });

    await trip.save();
    res.status(200).json({
      success: true,
      data: enrichTrip(trip),
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

// @desc    Delete item
// @route   DELETE /api/luggage/:id/items/:itemId
exports.deleteItem = async (req, res) => {
  try {
    const trip = await Luggage.findById(req.params.id);
    if (!trip || trip.userId.toString() !== req.user.id.toString()) {
      return res.status(404).json({ success: false, message: 'Luggage trip not found' });
    }

    const itemIndex = trip.items.findIndex((i) => i._id.toString() === req.params.itemId.toString());
    if (itemIndex === -1) {
      return res.status(404).json({ success: false, message: 'Item not found in trip' });
    }

    trip.items.splice(itemIndex, 1);
    await trip.save();

    res.status(200).json({
      success: true,
      message: 'Item removed from luggage',
      data: enrichTrip(trip),
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};

// @desc    Apply template items
// @route   POST /api/luggage/:id/template
exports.applyTemplate = async (req, res) => {
  try {
    const { templateKey } = req.body;
    const templateItems = PREMADE_TEMPLATES[templateKey];
    if (!templateItems) {
      return res.status(400).json({
        success: false,
        message: `Unknown template: ${templateKey}. Available: ${Object.keys(PREMADE_TEMPLATES).join(', ')}`,
      });
    }

    const trip = await Luggage.findById(req.params.id);
    if (!trip || trip.userId.toString() !== req.user.id.toString()) {
      return res.status(404).json({ success: false, message: 'Luggage trip not found' });
    }

    templateItems.forEach((t) => {
      trip.items.push({
        name: t.name,
        category: t.category,
        quantity: t.quantity,
        isPacked: false,
        isEssential: t.isEssential,
        weightKg: t.weightKg,
        notes: '',
      });
    });

    await trip.save();
    res.status(200).json({
      success: true,
      message: `Applied ${templateItems.length} items from ${templateKey} template`,
      data: enrichTrip(trip),
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};
