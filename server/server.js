const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '.env') });
require('dotenv').config({ path: path.join(__dirname, '..', '.env') });
const express = require('express');
const cors = require('cors');
const { connectDB } = require('./config/db');

const authRoutes = require('./routes/authRoutes');
const dashboardRoutes = require('./routes/dashboardRoutes');
const transactionRoutes = require('./routes/transactionRoutes');
const loanRoutes = require('./routes/loanRoutes');
const dictionaryRoutes = require('./routes/dictionaryRoutes');
const luggageRoutes = require('./routes/luggageRoutes');
const noteRoutes = require('./routes/noteRoutes');

const app = express();

// Middlewares
app.use(cors());
app.use(express.json());

// Request logger
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.originalUrl}`);
  next();
});

// Health check route
app.get('/api/health', (req, res) => {
  res.status(200).json({
    status: 'ok',
    message: 'Personal Life Manager Backend API is running',
    timestamp: new Date().toISOString(),
  });
});

// Mount modular API routes
app.use('/api/auth', authRoutes);
app.use('/api/dashboard', dashboardRoutes);
app.use('/api/transactions', transactionRoutes);
app.use('/api/loans', loanRoutes);
app.use('/api/dictionary', dictionaryRoutes);
app.use('/api/luggage', luggageRoutes);
app.use('/api/notes', noteRoutes);

// Serve static frontend assets
app.use(express.static(path.join(__dirname, 'public')));

// SPA fallback for frontend client (non-API GET requests)
app.use((req, res, next) => {
  if (req.method === 'GET' && !req.path.startsWith('/api')) {
    return res.sendFile(path.join(__dirname, 'public', 'index.html'));
  }
  next();
});

// 404 handler for unmatched API routes
app.use((req, res) => {
  res.status(404).json({
    success: false,
    message: `Route not found: ${req.method} ${req.originalUrl}`,
  });
});

// Global Error Handler
app.use((err, req, res, next) => {
  console.error('Unhandled error:', err);
  res.status(500).json({
    success: false,
    message: 'Internal server error',
    error: process.env.NODE_ENV === 'production' ? undefined : err.message,
  });
});

const PORT = process.env.API_PORT || (process.env.PORT && process.env.PORT !== '8080' ? process.env.PORT : 5000);

// Start server after DB connection
const startServer = async () => {
  try {
    await connectDB();
    const server = app.listen(PORT, () => {
      console.log(`=========================================`);
      console.log(` Personal Life Manager API Server Running`);
      console.log(` Port: ${PORT}`);
      console.log(` Health: http://localhost:${PORT}/api/health`);
      console.log(` Mode: ${process.env.NODE_ENV || 'development'}`);
      console.log(`=========================================`);
    });
    return server;
  } catch (err) {
    console.error('Failed to start server:', err);
    process.exit(1);
  }
};

if (require.main === module) {
  startServer();
}

module.exports = { app, startServer };
