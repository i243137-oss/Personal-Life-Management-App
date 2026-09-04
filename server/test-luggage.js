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
  console.log('--- Running Phase 5 Luggage API Verification Tests ---');

  // 1. Auth register
  const testEmail = `test_luggage_${Date.now()}@example.com`;
  const registerRes = await request('POST', '/api/auth/register', {
    name: 'Traveler Tester',
    email: testEmail,
    password: 'password123',
  });
  console.log('1. Register:', registerRes.status, registerRes.body.success);
  const token = registerRes.body.token;
  if (!token) throw new Error('Failed to register user');

  // 2. Create a new luggage trip
  const tripRes = await request(
    'POST',
    '/api/luggage',
    {
      title: 'Dubai Business Trip',
      destination: 'Dubai, UAE',
      bagType: 'Cabin Bag',
      departureDate: '2026-10-15',
      returnDate: '2026-10-20',
      maxWeightKg: 7.0,
      colorHex: '#3B82F6',
    },
    token
  );
  console.log('2. Create trip:', tripRes.status, tripRes.body.data.title);
  if (tripRes.status !== 201) throw new Error('Create trip failed');
  const tripId = tripRes.body.data._id;

  // 3. Apply template
  const templateRes = await request(
    'POST',
    `/api/luggage/${tripId}/template`,
    { templateKey: 'weekend' },
    token
  );
  console.log('3. Apply template status:', templateRes.status, templateRes.body);
  if (templateRes.status !== 200 || !templateRes.body.data) {
    throw new Error('Template application failed: ' + JSON.stringify(templateRes.body));
  }

  // 4. Add custom item
  const addItemRes = await request(
    'POST',
    `/api/luggage/${tripId}/items`,
    {
      name: 'Noise Cancelling Headphones',
      category: 'Electronics',
      quantity: 1,
      isEssential: true,
      weightKg: 0.35,
      notes: 'Remember aux cable',
    },
    token
  );
  console.log('4. Add custom item:', addItemRes.status, addItemRes.body.data.totalItems);
  const itemId = addItemRes.body.data.items[addItemRes.body.data.items.length - 1]._id;

  // 5. Toggle packed status
  const toggleRes = await request('PATCH', `/api/luggage/${tripId}/items/${itemId}/toggle`, null, token);
  console.log('5. Toggle packed item:', toggleRes.status, toggleRes.body.data.packedItems);
  if (toggleRes.status !== 200 || toggleRes.body.data.packedItems === 0) {
    throw new Error('Toggle item failed');
  }

  // 6. Get all trips
  const getTripsRes = await request('GET', '/api/luggage', null, token);
  console.log('6. Get all trips:', getTripsRes.status, 'count:', getTripsRes.body.count);
  if (getTripsRes.status !== 200 || getTripsRes.body.count !== 1) {
    throw new Error('Get trips failed');
  }

  // 7. Check Dashboard Summary has activeLuggageTrips
  const dashboardRes = await request('GET', '/api/dashboard/summary', null, token);
  console.log(
    '7. Dashboard summary:',
    dashboardRes.status,
    'activeTrips:',
    dashboardRes.body.data.activeLuggageTrips,
    'pendingPacking:',
    dashboardRes.body.data.pendingPackingCount
  );
  if (dashboardRes.body.data.activeLuggageTrips !== 1) {
    throw new Error('Dashboard summary activeLuggageTrips mismatch');
  }

  console.log('\n>>> SUCCESS! All Phase 5 Luggage & Packing APIs PASSED 100%! <<<');
}

runTests().catch((err) => {
  console.error('Test error:', err);
  process.exit(1);
});
