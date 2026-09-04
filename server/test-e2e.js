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

async function runE2E() {
  console.log('--- Running API End-to-End Verification ---');

  // 1. Health check
  const health = await request({
    hostname: 'localhost',
    port: 5000,
    path: '/api/health',
    method: 'GET',
  });
  console.log('1. Health check:', health.status, health.data);
  if (health.status !== 200 || health.data.status !== 'ok') throw new Error('Health check failed');

  // 2. Register new user
  const email = `e2e_${Date.now()}@example.com`;
  const registerRes = await request(
    {
      hostname: 'localhost',
      port: 5000,
      path: '/api/auth/register',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
    },
    {
      name: 'Test E2E User',
      email,
      password: 'password123',
    }
  );
  console.log('2. Register status:', registerRes.status, registerRes.data.success);
  if (registerRes.status !== 201 || !registerRes.data.token) throw new Error('Register failed');

  const token = registerRes.data.token;

  // 3. Login
  const loginRes = await request(
    {
      hostname: 'localhost',
      port: 5000,
      path: '/api/auth/login',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
    },
    {
      email,
      password: 'password123',
    }
  );
  console.log('3. Login status:', loginRes.status, loginRes.data.success);
  if (loginRes.status !== 200 || !loginRes.data.token) throw new Error('Login failed');

  // 4. Authenticated /me
  const meRes = await request({
    hostname: 'localhost',
    port: 5000,
    path: '/api/auth/me',
    method: 'GET',
    headers: { Authorization: `Bearer ${token}` },
  });
  console.log('4. Authenticated /me status:', meRes.status, meRes.data.user?.name);
  if (meRes.status !== 200 || meRes.data.user?.name !== 'Test E2E User') {
    throw new Error('Get /me failed');
  }

  // 5. Authenticated Dashboard summary
  const dashRes = await request({
    hostname: 'localhost',
    port: 5000,
    path: '/api/dashboard/summary',
    method: 'GET',
    headers: { Authorization: `Bearer ${token}` },
  });
  console.log('5. Dashboard summary status:', dashRes.status, dashRes.data.data);
  if (dashRes.status !== 200 || dashRes.data.data.currentBalance === undefined) {
    throw new Error('Get dashboard summary failed');
  }

  console.log('\n>>> SUCCESS! All API endpoints verified end-to-end! <<<');
}

runE2E().catch((err) => {
  console.error('E2E Test Failed:', err);
  process.exit(1);
});
