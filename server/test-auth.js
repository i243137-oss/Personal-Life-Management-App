const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '.env') });
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { connectDB, disconnectDB } = require('./config/db');
const User = require('./models/User');
const Transaction = require('./models/Transaction');

async function runTests() {
  console.log('--- Starting Phase 1 Backend Verification Tests ---');
  try {
    await connectDB();
    console.log('✓ Step 1: Database connected');

    // Clean up test users
    const testEmail = `test_${Date.now()}@example.com`;
    await User.deleteMany({ email: /test.*@example\.com/ });

    // Test password hashing
    const testPassword = 'SecurePassword123!';
    const salt = await bcrypt.genSalt(10);
    const passwordHash = await bcrypt.hash(testPassword, salt);
    const isMatch = await bcrypt.compare(testPassword, passwordHash);
    if (!isMatch) throw new Error('Bcrypt password hash verification failed');
    console.log('✓ Step 2: Password hashing verified');

    // Test user creation
    const user = await User.create({
      name: 'Ahmed Ali',
      email: testEmail,
      passwordHash,
    });
    if (!user._id) throw new Error('User creation in MongoDB failed');
    console.log(`✓ Step 3: User registered in MongoDB with ID: ${user._id}`);

    // Test duplicate registration rejection
    try {
      await User.create({
        name: 'Duplicate Ahmed',
        email: testEmail,
        passwordHash,
      });
      throw new Error('Duplicate user was allowed but should have failed!');
    } catch (dupErr) {
      if (dupErr.code === 11000) {
        console.log('✓ Step 4: Duplicate email correctly rejected with duplicate key error');
      } else {
        throw dupErr;
      }
    }

    // Test JWT generation and verification
    const secret = process.env.JWT_SECRET || 'super_secret_jwt_key_personal_life_manager_2026_dev';
    const token = jwt.sign({ id: user._id }, secret, { expiresIn: '30d' });
    const decoded = jwt.verify(token, secret);
    if (decoded.id !== user._id.toString()) throw new Error('JWT token payload mismatch');
    console.log('✓ Step 5: JWT token generation and verification passed');

    // Test dashboard summary computation
    const userSummary = await Transaction.find({ userId: user._id });
    console.log(`✓ Step 6: Transactions queried for user (found ${userSummary.length})`);

    console.log('\n>>> All Phase 1 Backend & MongoDB Tests PASSED Successfully! <<<');
    await disconnectDB();
    process.exit(0);
  } catch (err) {
    console.error('\n❌ Verification test failed:', err);
    try {
      await disconnectDB();
    } catch (_) {}
    process.exit(1);
  }
}

runTests();
