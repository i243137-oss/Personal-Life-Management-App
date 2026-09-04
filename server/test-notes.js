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
  console.log('--- Running Phase 6 Notes & Documents API Tests ---');

  // 1. Auth register
  const testEmail = `test_notes_${Date.now()}@example.com`;
  const registerRes = await request('POST', '/api/auth/register', {
    name: 'Notes Tester',
    email: testEmail,
    password: 'password123',
  });
  console.log('1. Register:', registerRes.status, registerRes.body.success);
  const token = registerRes.body.token;
  if (!token) throw new Error('Failed to register user: ' + JSON.stringify(registerRes.body));

  // 2. Create Note 1: Project Ideas (Pinned)
  const note1Res = await request(
    'POST',
    '/api/notes',
    {
      title: 'Startup Architecture Ideas',
      content: '1. Modular microservices\n2. Real-time websocket sync\n3. Edge-caching for mobile',
      category: 'Ideas',
      tags: ['startup', 'tech', 'architecture'],
      isPinned: true,
      colorHex: '#FEF3C7',
      checklist: [
        { text: 'Draft system design diagram', isDone: true },
        { text: 'Benchmark database read speeds', isDone: false },
      ],
    },
    token
  );
  console.log('2. Create Pinned Note 1:', note1Res.status, note1Res.body.data?.title);
  const note1Id = note1Res.body.data?._id;

  // 3. Create Note 2: Grocery & House Supplies
  const note2Res = await request(
    'POST',
    '/api/notes',
    {
      title: 'Weekend Grocery Run',
      content: 'Buy essentials from supermarket',
      category: 'Checklist',
      tags: ['groceries', 'weekend'],
      isPinned: false,
      colorHex: '#D1FAE5',
      checklist: [
        { text: 'Oat milk and espresso beans', isDone: false },
        { text: 'Fresh greens and avocados', isDone: true },
        { text: 'Sparkling water (6-pack)', isDone: false },
      ],
    },
    token
  );
  console.log('3. Create Note 2:', note2Res.status, note2Res.body.data?.title);
  const note2Id = note2Res.body.data?._id;

  // 4. Create Note 3: Work Meeting Notes
  const note3Res = await request(
    'POST',
    '/api/notes',
    {
      title: 'Sprint Planning Q4',
      content: 'Sprint goals:\n- Finalize mobile client\n- Release phase 6 notes & docs',
      category: 'Work',
      tags: ['sprint', 'work'],
      isPinned: false,
      colorHex: '#DBEAFE',
    },
    token
  );
  console.log('4. Create Note 3:', note3Res.status, note3Res.body.data?.title);
  const note3Id = note3Res.body.data?._id;

  // 5. Query all active notes
  const allNotesRes = await request('GET', '/api/notes', null, token);
  console.log('5. List all active notes:', allNotesRes.status, 'Count:', allNotesRes.body.count);
  if (allNotesRes.body.count !== 3) {
    throw new Error(`Expected 3 notes, got ${allNotesRes.body.count}`);
  }

  // 6. Query with search filter
  const searchRes = await request('GET', '/api/notes?search=architecture', null, token);
  console.log('6. Search "architecture":', searchRes.status, 'Found:', searchRes.body.count);
  if (searchRes.body.count !== 1) {
    throw new Error(`Search failed, expected 1 note, got ${searchRes.body.count}`);
  }

  // 7. Query with category filter
  const catRes = await request('GET', '/api/notes?category=Checklist', null, token);
  console.log('7. Category filter "Checklist":', catRes.status, 'Found:', catRes.body.count);
  if (catRes.body.count !== 1) {
    throw new Error(`Category filter failed, expected 1 note, got ${catRes.body.count}`);
  }

  // 8. Toggle Pin on Note 2
  const pinRes = await request('PATCH', `/api/notes/${note2Id}/pin`, null, token);
  console.log('8. Toggle Pin on Note 2:', pinRes.status, 'isPinned:', pinRes.body.data?.isPinned);

  // 9. Update Note 3 content
  const updateRes = await request(
    'PUT',
    `/api/notes/${note3Id}`,
    {
      title: 'Sprint Planning Q4 - Updated',
      content: 'Updated sprint goals with new deadlines',
      colorHex: '#EDE9FE',
    },
    token
  );
  console.log('9. Update Note 3:', updateRes.status, updateRes.body.data?.title);

  // 10. Toggle Archive on Note 3
  const archiveRes = await request('PATCH', `/api/notes/${note3Id}/archive`, null, token);
  console.log('10. Archive Note 3:', archiveRes.status, 'isArchived:', archiveRes.body.data?.isArchived);

  // 11. Verify active notes count after archive (should be 2)
  const activeAfterArchive = await request('GET', '/api/notes', null, token);
  console.log('11. Active notes count after archive:', activeAfterArchive.body.count);
  if (activeAfterArchive.body.count !== 2) {
    throw new Error(`Expected 2 active notes after archive, got ${activeAfterArchive.body.count}`);
  }

  // 12. Query archived notes
  const archivedNotesRes = await request('GET', '/api/notes?isArchived=true', null, token);
  console.log('12. Archived notes count:', archivedNotesRes.body.count);
  if (archivedNotesRes.body.count !== 1) {
    throw new Error(`Expected 1 archived note, got ${archivedNotesRes.body.count}`);
  }

  // 13. Check dashboard metrics for notes
  const dashboardRes = await request('GET', '/api/dashboard/summary', null, token);
  console.log('13. Dashboard notes metrics:', {
    totalNotesCount: dashboardRes.body.data?.totalNotesCount,
    pinnedNotesCount: dashboardRes.body.data?.pinnedNotesCount,
  });

  // 14. Delete Note 3
  const deleteRes = await request('DELETE', `/api/notes/${note3Id}`, null, token);
  console.log('14. Delete Note 3:', deleteRes.status, deleteRes.body.success);

  console.log('\n>>> All Phase 6 Notes & Documents backend tests PASSED successfully! <<<');
}

runTests().catch((err) => {
  console.error('Test failed:', err);
  process.exit(1);
});
