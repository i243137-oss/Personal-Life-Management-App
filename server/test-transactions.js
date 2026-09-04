const http = require('http');

function request(options, data) {
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let body = '';
      res.on('data', (chunk) => (body += chunk));
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, data: JSON.parse(body) });
        } catch (e) {
          resolve({ status: res.statusCode, raw: body });
        }
      });
    });
    req.on('error', reject);
    if (data) req.write(JSON.stringify(data));
    req.end();
  });
}

async function runTests() {
  console.log('--- Running Phase 2 Transaction & Money API Tests ---');

  // 1. Categories endpoint (public)
  const catRes = await request({
    hostname: 'localhost',
    port: 5000,
    path: '/api/transactions/categories',
    method: 'GET'
  });
  console.log('1. Categories status:', catRes.status, 'Expense categories:', catRes.data.data.expense.length);
  if (catRes.status !== 200 || !catRes.data.data.expense.length) throw new Error('Categories failed');

  // 2. Register fresh user
  const email = `money_user_${Date.now()}@example.com`;
  const regRes = await request({
    hostname: 'localhost',
    port: 5000,
    path: '/api/auth/register',
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, {
    name: 'Money Tester',
    email,
    password: 'password123'
  });
  const token = regRes.data.token;
  console.log('2. Registered test user, token acquired');

  // 3. Try to spend before adding income -> Expect Overdraft Warning
  const overdraftExpense = await request({
    hostname: 'localhost',
    port: 5000,
    path: '/api/transactions',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  }, {
    type: 'expense',
    amount: 500,
    category: 'Meal & Food',
    description: 'Lunch at cafe'
  });
  console.log('3. Overdraft rejection status:', overdraftExpense.status, overdraftExpense.data.message);
  if (overdraftExpense.status !== 400 || !overdraftExpense.data.overdraft) {
    throw new Error('Overdraft protection should have rejected this expense');
  }

  // 4. Add Income (e.g. Salary Rs. 50,000)
  const incomeRes = await request({
    hostname: 'localhost',
    port: 5000,
    path: '/api/transactions',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  }, {
    type: 'income',
    amount: 50000,
    category: 'Salary',
    description: 'Monthly Salary'
  });
  console.log('4. Add income status:', incomeRes.status, 'New balance:', incomeRes.data.newBalance);
  if (incomeRes.status !== 201 || incomeRes.data.newBalance !== 50000) {
    throw new Error('Add income failed');
  }

  // 5. Add Expense (e.g. Transport Rs. 1500)
  const expenseRes = await request({
    hostname: 'localhost',
    port: 5000,
    path: '/api/transactions',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  }, {
    type: 'expense',
    amount: 1500,
    category: 'Transport',
    description: 'Fuel for car'
  });
  console.log('5. Add expense status:', expenseRes.status, 'New balance:', expenseRes.data.newBalance);
  if (expenseRes.status !== 201 || expenseRes.data.newBalance !== 48500) {
    throw new Error('Add expense failed');
  }

  // 6. Add Second Expense (Meal & Food Rs. 2000)
  const expense2Res = await request({
    hostname: 'localhost',
    port: 5000,
    path: '/api/transactions',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  }, {
    type: 'expense',
    amount: 2000,
    category: 'Meal & Food',
    description: 'Grocery shopping'
  });
  const txToDeleteId = expense2Res.data.data.id;
  console.log('6. Add expense 2 status:', expense2Res.status, 'New balance:', expense2Res.data.newBalance);
  if (expense2Res.data.newBalance !== 46500) throw new Error('Expense 2 calculation failed');

  // 7. Get All Transactions
  const listRes = await request({
    hostname: 'localhost',
    port: 5000,
    path: '/api/transactions',
    method: 'GET',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  console.log('7. Transactions list count:', listRes.data.count, 'Current balance:', listRes.data.currentBalance);
  if (listRes.data.count !== 3 || listRes.data.currentBalance !== 46500) throw new Error('Transaction list mismatch');

  // 8. Filter by type=expense
  const expenseFilterRes = await request({
    hostname: 'localhost',
    port: 5000,
    path: '/api/transactions?type=expense',
    method: 'GET',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  console.log('8. Expense filter count:', expenseFilterRes.data.count);
  if (expenseFilterRes.data.count !== 2) throw new Error('Type filter mismatch');

  // 9. Stats breakdown
  const statsRes = await request({
    hostname: 'localhost',
    port: 5000,
    path: '/api/transactions/stats',
    method: 'GET',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  console.log('9. Stats:', statsRes.data.data);
  if (statsRes.data.data.totalExpense !== 3500) throw new Error('Stats total expense mismatch');

  // 10. Delete transaction (Delete Expense 2: Rs. 2000)
  const delRes = await request({
    hostname: 'localhost',
    port: 5000,
    path: `/api/transactions/${txToDeleteId}`,
    method: 'DELETE',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  console.log('10. Delete status:', delRes.status, 'Restored balance:', delRes.data.newBalance);
  if (delRes.status !== 200 || delRes.data.newBalance !== 48500) throw new Error('Delete failed or balance not restored');

  // 11. Verify Dashboard Summary synchronicity
  const dashRes = await request({
    hostname: 'localhost',
    port: 5000,
    path: '/api/dashboard/summary',
    method: 'GET',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  console.log('11. Dashboard summary balance:', dashRes.data.data.currentBalance, 'Recent activity count:', dashRes.data.data.recentActivity.length);
  if (dashRes.data.data.currentBalance !== 48500) throw new Error('Dashboard summary balance out of sync');

  console.log('\n>>> SUCCESS! All Phase 2 Transaction & Money APIs PASSED 100%! <<<');
}

runTests().catch(err => {
  console.error('Test Failed:', err);
  process.exit(1);
});
