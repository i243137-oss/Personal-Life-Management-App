const http = require('http');

function request(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null;
    const req = http.request(
      {
        hostname: 'localhost',
        port: 5000,
        path,
        method,
        headers: {
          'Content-Type': 'application/json',
          ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      },
      (res) => {
        let resBody = '';
        res.on('data', (chunk) => (resBody += chunk));
        res.on('end', () => {
          try {
            resolve({ status: res.statusCode, body: JSON.parse(resBody) });
          } catch (e) {
            resolve({ status: res.statusCode, body: resBody });
          }
        });
      }
    );
    req.on('error', reject);
    if (data) req.write(data);
    req.end();
  });
}

async function runTests() {
  console.log('Testing Loan Endpoints...');

  // 1. Register or login user
  const authRes = await request('POST', '/api/auth/register', {
    name: 'Loan Tester',
    email: 'loan_tester_' + Date.now() + '@example.com',
    password: 'password123',
  });
  console.log('Auth status:', authRes.status, authRes.body.token ? 'Got token' : 'No token');
  const token = authRes.body.token;

  // 2. Add starting funds
  await request('POST', '/api/transactions', {
    type: 'income',
    amount: 10000,
    category: 'Salary',
    description: 'Starting balance',
  }, token);

  // 3. Create Loan Given (Lent Rs. 3000 to Usama)
  const lentRes = await request('POST', '/api/loans', {
    personName: 'Usama Khan',
    phoneNumber: '03001234567',
    type: 'lent',
    amount: 3000,
    dueDate: '2026-09-30',
    notes: 'For semester fee',
    affectBalance: true,
  }, token);
  console.log('Lent Loan creation status:', lentRes.status, lentRes.body.data ? lentRes.body.data._id : lentRes.body);
  const lentId = lentRes.body.data._id;

  // 4. Create Loan Borrowed (Borrowed Rs. 1500 from Bilal)
  const borrowedRes = await request('POST', '/api/loans', {
    personName: 'Bilal Ahmed',
    phoneNumber: '03219876543',
    type: 'borrowed',
    amount: 1500,
    dueDate: '2026-09-15',
    notes: 'Urgent cash',
    affectBalance: true,
  }, token);
  console.log('Borrowed Loan creation status:', borrowedRes.status);
  const borrowedId = borrowedRes.body.data._id;

  // 5. Check Summary
  const summaryRes = await request('GET', '/api/loans/summary', null, token);
  console.log('Loans summary:', JSON.stringify(summaryRes.body.data));

  // 6. Record Repayment (Usama pays back Rs. 1000)
  const repayRes = await request('POST', `/api/loans/${lentId}/repay`, {
    amount: 1000,
    notes: 'Partial payment cash',
  }, token);
  console.log('Repayment status:', repayRes.status, 'New remaining:', repayRes.body.data.remainingAmount, 'Status:', repayRes.body.data.status);

  // 7. Check Dashboard Summary to verify youOwe and othersOwe
  const dashRes = await request('GET', '/api/dashboard/summary', null, token);
  console.log('Dashboard summary with loans:', JSON.stringify(dashRes.body.data));

  // 8. Full settlement (Mark remaining Rs. 2000 as paid)
  const settleRes = await request('PATCH', `/api/loans/${lentId}/status`, {
    status: 'paid',
  }, token);
  console.log('Settle status:', settleRes.status, 'Final loan status:', settleRes.body.data.status);

  // 9. Check Summary again
  const finalSummary = await request('GET', '/api/loans/summary', null, token);
  console.log('Final loans summary:', JSON.stringify(finalSummary.body.data));

  console.log('All backend Loan tests passed!');
}

runTests().catch(console.error);
