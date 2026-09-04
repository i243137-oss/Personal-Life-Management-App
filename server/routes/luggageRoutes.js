const express = require('express');
const router = express.Router();
const luggageController = require('../controllers/luggageController');
const { protect } = require('../middleware/auth');

// All luggage routes require user authentication
router.use(protect);

router.route('/')
  .get(luggageController.getTrips)
  .post(luggageController.createTrip);

router.route('/:id')
  .get(luggageController.getTripById)
  .put(luggageController.updateTrip)
  .delete(luggageController.deleteTrip);

router.post('/:id/items', luggageController.addItem);
router.patch('/:id/items/:itemId/toggle', luggageController.toggleItem);
router.put('/:id/items/:itemId', luggageController.updateItem);
router.delete('/:id/items/:itemId', luggageController.deleteItem);
router.post('/:id/template', luggageController.applyTemplate);

module.exports = router;
