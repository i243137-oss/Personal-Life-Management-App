const mongoose = require('mongoose');

let isConnected = false;

const connectDB = async () => {
  const uri = process.env.MONGODB_URI || 'mongodb://localhost:27017/personal_life_manager';

  try {
    console.log(`Connecting to MongoDB at: ${uri.replace(/\/\/[^:]+:[^@]+@/, '//***:***@')}`);
    await mongoose.connect(uri, {
      serverSelectionTimeoutMS: 2000,
    });
    isConnected = true;
    console.log('MongoDB connected successfully.');
  } catch (err) {
    console.warn('MongoDB connection failed:', err.message);
    console.log('Falling back to local in-memory document store for active development.');
    console.log('To connect to a live MongoDB instance, configure MONGODB_URI in server/.env');
    isConnected = false;
  }
};

const isDBConnected = () => isConnected;

const disconnectDB = async () => {
  if (isConnected) {
    await mongoose.disconnect();
    isConnected = false;
  }
};

module.exports = { connectDB, isDBConnected, disconnectDB };
