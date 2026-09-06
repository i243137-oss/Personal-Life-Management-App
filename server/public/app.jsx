// Personal Life Manager - Android Web Frontend
const { useState, useEffect, useMemo, useCallback, useRef } = React;

// --- Icon Component using Lucide ---
function Icon({ name, className = "w-5 h-5", ...props }) {
  const spanRef = useRef(null);
  useEffect(() => {
    if (spanRef.current && window.lucide) {
      spanRef.current.innerHTML = `<i data-lucide="${name}" class="${className}"></i>`;
      window.lucide.createIcons({ root: spanRef.current });
    }
  }, [name, className]);
  return <span ref={spanRef} className="inline-flex items-center justify-center shrink-0" {...props} />;
}

// --- Toast System ---
function ToastContainer({ toasts, onDismiss }) {
  return (
    <div className="fixed top-4 right-4 z-50 flex flex-col gap-2 max-w-sm pointer-events-none">
      {toasts.map(toast => (
        <div
          key={toast.id}
          className={`pointer-events-auto flex items-center gap-2.5 px-4 py-3 rounded-2xl shadow-lg border text-sm font-medium transition-all ${
            toast.type === 'error'
              ? 'bg-red-50 dark:bg-red-950/90 border-red-200 dark:border-red-900 text-red-800 dark:text-red-200'
              : toast.type === 'success'
              ? 'bg-[#EBF7F2] dark:bg-[#082B21] border-[#A6F2D6] dark:border-[#0F6D54] text-[#002117] dark:text-[#A6F2D6]'
              : 'bg-[#F6FBF7] dark:bg-[#191D1B] border-[#DCE5DF] dark:border-[#404944] text-[#191C1B] dark:text-[#E1E3DF]'
          }`}
        >
          <Icon
            name={toast.type === 'error' ? 'alert-circle' : toast.type === 'success' ? 'check-circle-2' : 'info'}
            className="w-4 h-4 shrink-0 text-[#0F6D54]"
          />
          <span className="flex-1">{toast.message}</span>
          <button onClick={() => onDismiss(toast.id)} className="text-gray-400 hover:text-gray-600">
            <Icon name="x" className="w-3.5 h-3.5" />
          </button>
        </div>
      ))}
    </div>
  );
}

// --- API Service Helper ---
const API_BASE = '/api';

async function apiRequest(endpoint, options = {}) {
  const token = localStorage.getItem('plm_token');
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };
  const res = await fetch(`${API_BASE}${endpoint}`, { ...options, headers });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(data.message || `Request failed with status ${res.status}`);
  }
  return data;
}

// Format Currency matching Android App (PKR)
function formatCurrency(amount) {
  const num = Number(amount) || 0;
  return 'Rs. ' + Math.abs(num).toLocaleString('en-US');
}

// --- Main App Component ---
function App() {
  const [user, setUser] = useState(() => {
    try {
      const saved = localStorage.getItem('plm_user');
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });
  const [token, setToken] = useState(() => localStorage.getItem('plm_token') || '');
  const [activeTab, setActiveTab] = useState('home'); // 'home' | 'money' | 'loans' | 'luggage' | 'more'
  const [activeSubscreen, setActiveSubscreen] = useState(null); // 'dictionary' | 'notes' | null
  const [isPhoneFrame, setIsPhoneFrame] = useState(true);
  const [darkMode, setDarkMode] = useState(() => localStorage.getItem('plm_theme') === 'dark');
  const [toasts, setToasts] = useState([]);
  const [loading, setLoading] = useState(false);

  // App Data State
  const [transactions, setTransactions] = useState([]);
  const [loans, setLoans] = useState([]);
  const [notes, setNotes] = useState([]);
  const [trips, setTrips] = useState([]);
  const [activeTripId, setActiveTripId] = useState('');
  const [learnedWords, setLearnedWords] = useState([]);

  // Filter & Search states
  const [moneySearch, setMoneySearch] = useState('');
  const [moneyFilterType, setMoneyFilterType] = useState('all'); // 'all' | 'expense' | 'income'
  const [moneyFilterCategory, setMoneyFilterCategory] = useState('all');
  const [loansFilterType, setLoansFilterType] = useState('all'); // 'all' | 'lent' | 'borrowed' | 'settled'
  const [notesSearch, setNotesSearch] = useState('');
  const [notesCategory, setNotesCategory] = useState('all');

  // Modals
  const [modalTx, setModalTx] = useState(false);
  const [txFormType, setTxFormType] = useState('expense');
  const [modalLoan, setModalLoan] = useState(false);
  const [modalNote, setModalNote] = useState(false);
  const [modalTrip, setModalTrip] = useState(false);
  const [modalItem, setModalItem] = useState(false);

  // Forms
  const [txForm, setTxForm] = useState({ category: 'Meal & Food', amount: '', description: '', date: new Date().toISOString().split('T')[0] });
  const [loanForm, setLoanForm] = useState({ type: 'lent', personName: '', amount: '', dueDate: '', note: '', phoneNumber: '' });
  const [noteForm, setNoteForm] = useState({ title: '', content: '', category: 'General' });
  const [tripForm, setTripForm] = useState({ destination: '', travelDates: '' });
  const [itemForm, setItemForm] = useState({ name: '', category: 'Clothes', weightKg: 1 });

  // Dictionary state
  const [dictWord, setDictWord] = useState('');
  const [dictMode, setDictMode] = useState('meaning');
  const [dictResult, setDictResult] = useState(null);
  const [dictLoading, setDictLoading] = useState(false);
  const [dictActiveTab, setDictActiveTab] = useState('search');
  const [dictSavedWords, setDictSavedWords] = useState([]);
  const [dictSavedWordsLoading, setDictSavedWordsLoading] = useState(false);
  const [dictAiAssistantData, setDictAiAssistantData] = useState(null);
  const [dictAiAssistantLoading, setDictAiAssistantLoading] = useState(false);
  const [dictAiActiveFeature, setDictAiActiveFeature] = useState(null);
  const [geminiTestResult, setGeminiTestResult] = useState(null);
  const [geminiTesting, setGeminiTesting] = useState(false);

  // Helper: Toast
  const addToast = useCallback((message, type = 'info') => {
    const id = Date.now() + Math.random();
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 4000);
  }, []);

  const removeToast = useCallback(id => {
    setToasts(prev => prev.filter(t => t.id !== id));
  }, []);

  // Sync Dark Mode
  useEffect(() => {
    if (darkMode) {
      document.documentElement.classList.add('dark');
      localStorage.setItem('plm_theme', 'dark');
    } else {
      document.documentElement.classList.remove('dark');
      localStorage.setItem('plm_theme', 'light');
    }
  }, [darkMode]);

  // Load Data
  const refreshAllData = useCallback(async (tokenOverride = null) => {
    const currentToken = tokenOverride || token || localStorage.getItem('plm_token');
    if (!currentToken) return;
    setLoading(true);
    try {
      const [txRes, loansRes, notesRes, luggageRes, wordsRes] = await Promise.allSettled([
        apiRequest('/transactions', { headers: { Authorization: `Bearer ${currentToken}` } }),
        apiRequest('/loans', { headers: { Authorization: `Bearer ${currentToken}` } }),
        apiRequest('/notes', { headers: { Authorization: `Bearer ${currentToken}` } }),
        apiRequest('/luggage', { headers: { Authorization: `Bearer ${currentToken}` } }),
        apiRequest('/dictionary/words', { headers: { Authorization: `Bearer ${currentToken}` } }),
      ]);

      if (txRes.status === 'fulfilled' && txRes.value.data) setTransactions(txRes.value.data);
      if (loansRes.status === 'fulfilled' && loansRes.value.data) setLoans(loansRes.value.data);
      if (notesRes.status === 'fulfilled' && notesRes.value.data) setNotes(notesRes.value.data);
      if (luggageRes.status === 'fulfilled' && luggageRes.value.data) {
        setTrips(luggageRes.value.data);
        if (luggageRes.value.data.length > 0 && !activeTripId) {
          setActiveTripId(luggageRes.value.data[0]._id);
        }
      }
      if (wordsRes.status === 'fulfilled' && wordsRes.value.data) setLearnedWords(wordsRes.value.data);
    } catch (err) {
      console.warn('Refresh error:', err);
    } finally {
      setLoading(false);
    }
  }, [token, activeTripId]);

  useEffect(() => {
    if (token) refreshAllData(token);
  }, [token, refreshAllData]);

  // Auth Handlers
  const handleLogin = async (email, password) => {
    try {
      const res = await apiRequest('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      });
      localStorage.setItem('plm_token', res.token);
      localStorage.setItem('plm_user', JSON.stringify(res.user));
      setToken(res.token);
      setUser(res.user);
      addToast(`Welcome back, ${res.user.name}!`, 'success');
      refreshAllData(res.token);
    } catch (err) {
      addToast(err.message || 'Login failed', 'error');
    }
  };

  const handleRegister = async (name, email, password) => {
    try {
      const res = await apiRequest('/auth/register', {
        method: 'POST',
        body: JSON.stringify({ name, email, password }),
      });
      localStorage.setItem('plm_token', res.token);
      localStorage.setItem('plm_user', JSON.stringify(res.user));
      setToken(res.token);
      setUser(res.user);
      addToast(`Welcome to Personal Life Manager, ${res.user.name}!`, 'success');
      refreshAllData(res.token);
    } catch (err) {
      addToast(err.message || 'Registration failed', 'error');
    }
  };

  const handleDemoUser = () => {
    const demo = { id: 'demo_user', name: 'Alex Johnson', email: 'alex.manager@personal.app' };
    const demoToken = 'demo_token_mobile';
    setUser(demo);
    setToken(demoToken);
    localStorage.setItem('plm_user', JSON.stringify(demo));
    localStorage.setItem('plm_token', demoToken);

    setTransactions([
      { _id: 't1', type: 'income', category: 'Salary', amount: 95000, description: 'Monthly direct deposit', date: '2026-09-01' },
      { _id: 't2', type: 'expense', category: 'Meal & Food', amount: 450, description: 'Lunch with team', date: '2026-09-05' },
      { _id: 't3', type: 'expense', category: 'Transport', amount: 1200, description: 'Fuel refuel', date: '2026-09-04' },
      { _id: 't4', type: 'expense', category: 'Bills & Utilities', amount: 3500, description: 'High-speed fiber optic', date: '2026-09-03' }
    ]);
    setLoans([
      { _id: 'l1', type: 'lent', personName: 'Bilal Khan', amount: 15000, repaidAmount: 5000, dueDate: '2026-09-25', status: 'pending', notes: 'Emergency medical aid' },
      { _id: 'l2', type: 'borrowed', personName: 'Usman Tariq', amount: 8000, repaidAmount: 8000, dueDate: '2026-09-10', status: 'paid', notes: 'Shared apartment deposit' }
    ]);
    setTrips([
      {
        _id: 'trip1',
        destination: 'Hunza Valley & Skardu',
        travelDates: 'Sep 15 - Sep 22, 2026',
        items: [
          { _id: 'i1', name: 'Warm Jacket & Fleece', category: 'Clothes', packed: true, weightKg: 1.5 },
          { _id: 'i2', name: 'DSLR Camera & Batteries', category: 'Electronics', packed: true, weightKg: 2.0 },
          { _id: 'i3', name: 'CNIC & Trekking Permits', category: 'Documents', packed: true, weightKg: 0.1 },
          { _id: 'i4', name: 'First Aid & Altitude Meds', category: 'Toiletries', packed: false, weightKg: 0.5 },
          { _id: 'i5', name: 'Trekking Boots & Socks', category: 'Gear', packed: false, weightKg: 1.8 }
        ]
      }
    ]);
    setActiveTripId('trip1');
    setNotes([
      { _id: 'n1', title: 'Monthly Budget Rules', content: '50% essentials, 30% savings & investments, 20% personal development.', category: 'Finance', isPinned: true, createdAt: '2026-09-01' },
      { _id: 'n2', title: 'Travel Packing Checklist', content: 'Verify power bank capacity (under 20,000mAh for flights) and warm gloves.', category: 'Travel', isPinned: false, createdAt: '2026-09-03' }
    ]);
    addToast('Logged in as Alex Johnson (Personal Manager)', 'success');
  };

  const handleLogout = () => {
    setUser(null);
    setToken('');
    localStorage.removeItem('plm_user');
    localStorage.removeItem('plm_token');
    addToast('Signed out', 'info');
  };

  // Financial Calculations
  const calculatedBalance = useMemo(() => {
    let bal = 0;
    transactions.forEach(t => {
      const amt = Number(t.amount) || 0;
      if (t.type === 'income' || t.type === 'loan_received') bal += amt;
      else if (t.type === 'expense' || t.type === 'loan_given') bal -= amt;
    });
    return bal;
  }, [transactions]);

  const totalIncome = useMemo(() => {
    return transactions.filter(t => t.type === 'income').reduce((acc, t) => acc + (Number(t.amount) || 0), 0);
  }, [transactions]);

  const totalExpenses = useMemo(() => {
    return transactions.filter(t => t.type === 'expense').reduce((acc, t) => acc + (Number(t.amount) || 0), 0);
  }, [transactions]);

  const youOwe = useMemo(() => {
    return loans.filter(l => l.type === 'borrowed' && l.status !== 'paid')
      .reduce((acc, l) => acc + ((Number(l.amount) || 0) - (Number(l.repaidAmount) || 0)), 0);
  }, [loans]);

  const othersOwe = useMemo(() => {
    return loans.filter(l => l.type === 'lent' && l.status !== 'paid')
      .reduce((acc, l) => acc + ((Number(l.amount) || 0) - (Number(l.repaidAmount) || 0)), 0);
  }, [loans]);

  // Trip stats
  const activeTrip = useMemo(() => trips.find(t => t._id === activeTripId) || trips[0] || null, [trips, activeTripId]);
  const pendingPackingCount = useMemo(() => {
    let pending = 0;
    trips.forEach(t => {
      (t.items || []).forEach(item => {
        if (!item.packed) pending++;
      });
    });
    return pending;
  }, [trips]);

  // Greeting based on time
  const greeting = useMemo(() => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good Morning';
    if (hour < 17) return 'Good Afternoon';
    return 'Good Evening';
  }, []);

  // Quick Action Click
  const handleQuickAction = (key) => {
    if (key === 'add_money') {
      setActiveSubscreen(null);
      setActiveTab('money');
      setTxFormType('income');
      setModalTx(true);
    } else if (key === 'add_expense') {
      setActiveSubscreen(null);
      setActiveTab('money');
      setTxFormType('expense');
      setModalTx(true);
    } else if (key === 'add_loan') {
      setActiveSubscreen(null);
      setActiveTab('loans');
      setModalLoan(true);
    } else if (key === 'dictionary') {
      setActiveSubscreen('dictionary');
    } else if (key === 'add_note') {
      setActiveSubscreen('notes');
      setModalNote(true);
    } else if (key === 'luggage') {
      setActiveSubscreen(null);
      setActiveTab('luggage');
    }
  };

  // Transaction submission
  const handleAddTransaction = async (e) => {
    e.preventDefault();
    if (!txForm.amount) return;
    const payload = {
      type: txFormType,
      category: txForm.category,
      amount: Number(txForm.amount),
      description: txForm.description,
      date: txForm.date || new Date().toISOString().split('T')[0],
      allowOverdraft: true
    };
    try {
      if (token !== 'demo_token_mobile') {
        const res = await apiRequest('/transactions', { method: 'POST', body: JSON.stringify(payload) });
        if (res.data) setTransactions(prev => [res.data, ...prev]);
      } else {
        const mock = { ...payload, _id: 't_' + Date.now() };
        setTransactions(prev => [mock, ...prev]);
      }
      setModalTx(false);
      setTxForm({ category: 'Meal & Food', amount: '', description: '', date: new Date().toISOString().split('T')[0] });
      addToast(`${txFormType === 'income' ? 'Income' : 'Expense'} recorded`, 'success');
    } catch (err) {
      addToast(err.message || 'Error creating transaction', 'error');
    }
  };

  const handleDeleteTransaction = async (id) => {
    try {
      if (token !== 'demo_token_mobile') {
        await apiRequest(`/transactions/${id}`, { method: 'DELETE' });
      }
      setTransactions(prev => prev.filter(t => t._id !== id));
      addToast('Transaction removed', 'info');
    } catch (err) {
      addToast(err.message || 'Failed to delete transaction', 'error');
    }
  };

  // Quick Preset Add
  const handleQuickPreset = async (label, category, amount, type) => {
    const payload = {
      type,
      category,
      amount,
      description: `Quick record: ${label}`,
      date: new Date().toISOString().split('T')[0],
      allowOverdraft: true
    };
    try {
      if (token !== 'demo_token_mobile') {
        const res = await apiRequest('/transactions', { method: 'POST', body: JSON.stringify(payload) });
        if (res.data) setTransactions(prev => [res.data, ...prev]);
      } else {
        setTransactions(prev => [{ ...payload, _id: 't_' + Date.now() }, ...prev]);
      }
      addToast(`Recorded ${label} (Rs. ${amount})`, 'success');
    } catch (err) {
      addToast(err.message || 'Failed to record preset', 'error');
    }
  };

  // Loan Add
  const handleAddLoan = async (e) => {
    e.preventDefault();
    if (!loanForm.personName || !loanForm.amount) return;
    const payload = {
      type: loanForm.type,
      personName: loanForm.personName,
      amount: Number(loanForm.amount),
      dueDate: loanForm.dueDate || undefined,
      notes: loanForm.note,
      phoneNumber: loanForm.phoneNumber
    };
    try {
      if (token !== 'demo_token_mobile') {
        const res = await apiRequest('/loans', { method: 'POST', body: JSON.stringify(payload) });
        if (res.data) setLoans(prev => [res.data, ...prev]);
      } else {
        setLoans(prev => [{ ...payload, _id: 'l_' + Date.now(), status: 'pending', repaidAmount: 0 }, ...prev]);
      }
      setModalLoan(false);
      setLoanForm({ type: 'lent', personName: '', amount: '', dueDate: '', note: '', phoneNumber: '' });
      addToast('Loan recorded', 'success');
    } catch (err) {
      addToast(err.message || 'Failed to create loan', 'error');
    }
  };

  const handleSettleLoan = async (id) => {
    try {
      if (token !== 'demo_token_mobile') {
        await apiRequest(`/loans/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status: 'paid' }) });
      }
      setLoans(prev => prev.map(l => l._id === id ? { ...l, status: 'paid', repaidAmount: l.amount } : l));
      addToast('Loan marked as settled', 'success');
    } catch (err) {
      addToast(err.message || 'Failed to update loan', 'error');
    }
  };

  const handleDeleteLoan = async (id) => {
    try {
      if (token !== 'demo_token_mobile') {
        await apiRequest(`/loans/${id}`, { method: 'DELETE' });
      }
      setLoans(prev => prev.filter(l => l._id !== id));
      addToast('Loan removed', 'info');
    } catch (err) {
      addToast(err.message || 'Failed to delete loan', 'error');
    }
  };

  // Note Add
  const handleAddNote = async (e) => {
    e.preventDefault();
    if (!noteForm.title) return;
    const payload = {
      title: noteForm.title,
      content: noteForm.content,
      category: noteForm.category || 'General'
    };
    try {
      if (token !== 'demo_token_mobile') {
        const res = await apiRequest('/notes', { method: 'POST', body: JSON.stringify(payload) });
        if (res.data) setNotes(prev => [res.data, ...prev]);
      } else {
        setNotes(prev => [{ ...payload, _id: 'n_' + Date.now(), isPinned: false, createdAt: new Date().toISOString() }, ...prev]);
      }
      setModalNote(false);
      setNoteForm({ title: '', content: '', category: 'General' });
      addToast('Note created', 'success');
    } catch (err) {
      addToast(err.message || 'Failed to save note', 'error');
    }
  };

  const handleTogglePinNote = async (id) => {
    try {
      if (token !== 'demo_token_mobile') {
        await apiRequest(`/notes/${id}/pin`, { method: 'PATCH' });
      }
      setNotes(prev => prev.map(n => n._id === id ? { ...n, isPinned: !n.isPinned } : n));
    } catch (err) {
      addToast(err.message || 'Failed to update note', 'error');
    }
  };

  const handleDeleteNote = async (id) => {
    try {
      if (token !== 'demo_token_mobile') {
        await apiRequest(`/notes/${id}`, { method: 'DELETE' });
      }
      setNotes(prev => prev.filter(n => n._id !== id));
      addToast('Note deleted', 'info');
    } catch (err) {
      addToast(err.message || 'Failed to delete note', 'error');
    }
  };

  // Luggage item toggle
  const handleToggleLuggageItem = async (tripId, itemId) => {
    try {
      if (token !== 'demo_token_mobile') {
        await apiRequest(`/luggage/${tripId}/items/${itemId}/toggle`, { method: 'PATCH' });
      }
      setTrips(prev => prev.map(trip => {
        if (trip._id !== tripId) return trip;
        const updated = (trip.items || []).map(item => item._id === itemId ? { ...item, packed: !item.packed } : item);
        return { ...trip, items: updated };
      }));
    } catch (err) {
      addToast(err.message || 'Failed to toggle item', 'error');
    }
  };

  const handleAddLuggageItem = async (e) => {
    e.preventDefault();
    if (!itemForm.name || !activeTripId) return;
    const payload = {
      name: itemForm.name,
      category: itemForm.category,
      weightKg: Number(itemForm.weightKg) || 1
    };
    try {
      if (token !== 'demo_token_mobile') {
        const res = await apiRequest(`/luggage/${activeTripId}/items`, { method: 'POST', body: JSON.stringify(payload) });
        if (res.data) {
          setTrips(prev => prev.map(t => t._id === activeTripId ? res.data : t));
        }
      } else {
        setTrips(prev => prev.map(t => {
          if (t._id !== activeTripId) return t;
          return { ...t, items: [...(t.items || []), { ...payload, _id: 'i_' + Date.now(), packed: false }] };
        }));
      }
      setModalItem(false);
      setItemForm({ name: '', category: 'Clothes', weightKg: 1 });
      addToast('Item added to luggage', 'success');
    } catch (err) {
      addToast(err.message || 'Failed to add item', 'error');
    }
  };

  // Dictionary Lookup (Merriam-Webster + Unified Service)
  const handleLookupWord = async (e, wordOverride) => {
    if (e && e.preventDefault) e.preventDefault();
    const targetWord = (wordOverride || dictWord || '').trim();
    if (!targetWord) return;

    setDictWord(targetWord);
    setDictLoading(true);
    setDictResult(null);
    setDictAiAssistantData(null);
    setDictAiActiveFeature(null);

    try {
      // Primary search via GET /api/dictionary/:word (or fallback POST /dictionary/lookup)
      let res;
      try {
        res = await apiRequest(`/dictionary/${encodeURIComponent(targetWord)}`);
      } catch (getErr) {
        res = await apiRequest('/dictionary/lookup', {
          method: 'POST',
          body: JSON.stringify({ word: targetWord })
        });
        if (res.data) res = res.data;
      }

      if (res) {
        setDictResult(res);
        if (res.source?.thesaurusError && res.source?.dictionarySuccess) {
          addToast('Dictionary loaded (Thesaurus unavailable)', 'info');
        } else if (res.source?.dictionaryError && res.source?.thesaurusSuccess) {
          addToast('Thesaurus loaded (Dictionary unavailable)', 'info');
        } else if (res.success) {
          addToast(`Found "${res.word}" in Merriam-Webster`, 'success');
        }
      }
    } catch (err) {
      addToast(err.message || 'Dictionary lookup failed', 'error');
    } finally {
      setDictLoading(false);
    }
  };

  // Fetch Learned Words from Notebook
  const fetchSavedWords = async () => {
    setDictSavedWordsLoading(true);
    try {
      const res = await apiRequest('/dictionary/words');
      if (res.data) setDictSavedWords(res.data);
    } catch (err) {
      console.warn('Failed to load saved vocabulary:', err.message);
    } finally {
      setDictSavedWordsLoading(false);
    }
  };

  // Save Current Word to Vocabulary Notebook
  const handleSaveWordToNotebook = async (status = 'learning', userNote = '') => {
    if (!dictResult || !dictResult.word) return;
    try {
      const payload = {
        word: dictResult.word,
        phonetic: dictResult.phonetic || dictResult.dictionary?.pronunciation?.written || '',
        partOfSpeech: dictResult.partOfSpeech || (dictResult.dictionary?.partsOfSpeech || [])[0] || '',
        shortDefinition: dictResult.shortDefinition || dictResult.dictionary?.definitions?.[0]?.text || '',
        fullDefinition: dictResult.fullDefinition || '',
        synonyms: dictResult.thesaurus?.synonyms || dictResult.synonyms || [],
        antonyms: dictResult.thesaurus?.antonyms || dictResult.antonyms || [],
        relatedWords: dictResult.thesaurus?.relatedWords || [],
        examples: dictResult.examples || dictResult.dictionary?.examples || [],
        etymology: dictResult.dictionary?.etymology || '',
        audioUrl: dictResult.dictionary?.audio?.[0]?.url || '',
        masteryStatus: status,
        personalNotes: userNote
      };

      const res = await apiRequest('/dictionary/save', {
        method: 'POST',
        body: JSON.stringify(payload)
      });

      if (res.data) {
        setDictResult(prev => ({
          ...prev,
          isSaved: true,
          savedWordId: res.data._id,
          masteryStatus: status,
          personalNotes: userNote
        }));
        addToast(`"${dictResult.word}" saved to your Notebook (${status})`, 'success');
        fetchSavedWords();
      }
    } catch (err) {
      addToast(err.message || 'Failed to save word', 'error');
    }
  };

  // Update Mastery Status for a saved word
  const handleUpdateWordMastery = async (wordId, status, personalNotes) => {
    try {
      await apiRequest(`/dictionary/words/${wordId}/mastery`, {
        method: 'PATCH',
        body: JSON.stringify({ status, personalNotes })
      });
      addToast(`Status updated to ${status}`, 'success');
      if (dictResult && dictResult.savedWordId === wordId) {
        setDictResult(prev => ({ ...prev, masteryStatus: status, personalNotes: personalNotes !== undefined ? personalNotes : prev.personalNotes }));
      }
      fetchSavedWords();
    } catch (err) {
      addToast(err.message || 'Failed to update status', 'error');
    }
  };

  // Delete Word from Notebook
  const handleDeleteSavedWord = async (wordId) => {
    try {
      await apiRequest(`/dictionary/words/${wordId}`, { method: 'DELETE' });
      addToast('Word removed from notebook', 'info');
      if (dictResult && dictResult.savedWordId === wordId) {
        setDictResult(prev => ({ ...prev, isSaved: false, savedWordId: null }));
      }
      fetchSavedWords();
    } catch (err) {
      addToast(err.message || 'Failed to remove word', 'error');
    }
  };

  // Call Gemini Learning Assistant for auxiliary educational guidance
  const handleCallAiAssistant = async (feature) => {
    if (!dictResult || !dictResult.word) return;
    setDictAiAssistantLoading(true);
    setDictAiActiveFeature(feature);
    try {
      const res = await apiRequest('/dictionary/ai-assistant', {
        method: 'POST',
        body: JSON.stringify({
          word: dictResult.word,
          feature,
          definition: dictResult.dictionary?.definitions?.[0]?.text || dictResult.shortDefinition
        })
      });
      if (res.data) {
        setDictAiAssistantData(res.data);
      }
    } catch (err) {
      addToast('AI Assistant: ' + err.message, 'error');
    } finally {
      setDictAiAssistantLoading(false);
    }
  };

  // Test Gemini API connectivity
  const handleTestGemini = async () => {
    setGeminiTesting(true);
    setGeminiTestResult(null);
    try {
      const res = await apiRequest('/dictionary/test-gemini');
      setGeminiTestResult(res);
      addToast('Gemini API test completed successfully!', 'success');
    } catch (err) {
      setGeminiTestResult({
        success: false,
        message: err.message || 'Connection failed'
      });
      addToast('Gemini test failed: ' + err.message, 'error');
    } finally {
      setGeminiTesting(false);
    }
  };

  // -------------------------------------------------------------
  // If user is not authenticated: Show Android M3 Login / Register Screen
  // -------------------------------------------------------------
  if (!user || !token) {
    return (
      <div className="h-full flex items-center justify-center p-4 bg-[#EBF3ED] dark:bg-[#0C0F0E]">
        <ToastContainer toasts={toasts} onDismiss={removeToast} />
        <div className="w-full max-w-sm bg-white dark:bg-[#191D1B] rounded-3xl p-7 shadow-xl border border-[#DCE5DF] dark:border-[#404944] text-center">
          {/* Logo Branding Hub */}
          <div className="w-16 h-16 rounded-full bg-[#A6F2D6] dark:bg-[#00513E] text-[#0F6D54] dark:text-[#A6F2D6] flex items-center justify-center mx-auto mb-4 shadow-sm">
            <Icon name="award" className="w-8 h-8 text-[#0F6D54] dark:text-[#A6F2D6]" />
          </div>

          <h1 className="text-2xl font-bold text-[#191C1B] dark:text-[#E1E3DF] tracking-tight">Personal Life Manager</h1>
          <p className="text-xs text-[#404944] dark:text-[#C0C9C3] mt-1 mb-6">Finances • Loans • Dictionary • Travel Luggage • Notes</p>

          <AuthForm onLogin={handleLogin} onRegister={handleRegister} onDemo={handleDemoUser} />
        </div>
      </div>
    );
  }

  // -------------------------------------------------------------
  // Authenticated Shell: Android Material 3 Mobile Container
  // -------------------------------------------------------------
  return (
    <div className={`h-full w-full flex items-center justify-center ${isPhoneFrame ? 'p-0 sm:p-4 bg-[#D7E3DC] dark:bg-[#090C0B]' : 'bg-[#F6FBF7] dark:bg-[#101413]'}`}>
      <ToastContainer toasts={toasts} onDismiss={removeToast} />

      {/* Main Container: Android Phone Shell or Full Screen */}
      <div className={`${isPhoneFrame ? 'phone-frame bg-[#F6FBF7] dark:bg-[#101413]' : 'w-full h-full bg-[#F6FBF7] dark:bg-[#101413]'} flex flex-col relative overflow-hidden`}>

        {/* Android Status Bar & Global Controls */}
        <div className="h-10 px-5 bg-white/70 dark:bg-[#191D1B]/70 backdrop-blur-md border-b border-[#DCE5DF]/60 dark:border-[#404944]/40 flex items-center justify-between shrink-0 select-none z-20">
          <div className="flex items-center gap-1.5 text-xs font-semibold text-[#191C1B] dark:text-[#E1E3DF]">
            <span>9:41</span>
          </div>

          {/* Camera Punchhole on Mobile frame */}
          {isPhoneFrame && (
            <div className="w-3 h-3 rounded-full bg-black/80 dark:bg-black border border-white/20 -ml-2" />
          )}

          <div className="flex items-center gap-2 text-[#404944] dark:text-[#C0C9C3]">
            {/* Toggle Phone Frame vs Full Screen */}
            <button
              onClick={() => setIsPhoneFrame(!isPhoneFrame)}
              title={isPhoneFrame ? "Expand to Full Screen Web View" : "Switch to Mobile Device Frame"}
              className="p-1 rounded-lg hover:bg-[#DCE5DF]/50 dark:hover:bg-[#404944]/50 text-xs flex items-center gap-1"
            >
              <Icon name={isPhoneFrame ? "maximize-2" : "smartphone"} className="w-3.5 h-3.5" />
            </button>

            {/* Dark Mode Toggle */}
            <button
              onClick={() => setDarkMode(!darkMode)}
              className="p-1 rounded-lg hover:bg-[#DCE5DF]/50 dark:hover:bg-[#404944]/50"
              title="Toggle Dark Mode"
            >
              <Icon name={darkMode ? "sun" : "moon"} className="w-3.5 h-3.5" />
            </button>

            <Icon name="wifi" className="w-3.5 h-3.5" />
            <Icon name="battery-charging" className="w-4 h-4 text-[#0F6D54] dark:text-[#8AD5BB]" />
          </div>
        </div>

        {/* Main Content Area */}
        <div className="flex-1 overflow-y-auto pb-20 select-text">
          {activeSubscreen === 'dictionary' ? (
            <DictionarySubscreen
              dictWord={dictWord}
              setDictWord={setDictWord}
              dictResult={dictResult}
              dictLoading={dictLoading}
              onLookup={handleLookupWord}
              onBack={() => setActiveSubscreen(null)}
              dictActiveTab={dictActiveTab}
              setDictActiveTab={setDictActiveTab}
              dictSavedWords={dictSavedWords}
              dictSavedWordsLoading={dictSavedWordsLoading}
              onSaveWord={handleSaveWordToNotebook}
              onUpdateMastery={handleUpdateWordMastery}
              onDeleteWord={handleDeleteSavedWord}
              onFetchSavedWords={fetchSavedWords}
              dictAiAssistantData={dictAiAssistantData}
              dictAiAssistantLoading={dictAiAssistantLoading}
              dictAiActiveFeature={dictAiActiveFeature}
              onCallAiAssistant={handleCallAiAssistant}
              geminiTestResult={geminiTestResult}
              geminiTesting={geminiTesting}
              onTestGemini={handleTestGemini}
            />
          ) : activeSubscreen === 'notes' ? (
            <NotesSubscreen
              notes={notes}
              notesSearch={notesSearch}
              setNotesSearch={setNotesSearch}
              notesCategory={notesCategory}
              setNotesCategory={setNotesCategory}
              onPin={handleTogglePinNote}
              onDelete={handleDeleteNote}
              onOpenAdd={() => setModalNote(true)}
              onBack={() => setActiveSubscreen(null)}
            />
          ) : activeTab === 'home' ? (
            <DashboardView
              greeting={greeting}
              userName={user.name || "Personal Manager"}
              calculatedBalance={calculatedBalance}
              todayExpenses={totalExpenses}
              youOwe={youOwe}
              othersOwe={othersOwe}
              activeTrip={activeTrip}
              pendingPackingCount={pendingPackingCount}
              activeLuggageTrips={trips.length}
              totalNotesCount={notes.length}
              pinnedNotesCount={notes.filter(n => n.isPinned).length}
              recentTransactions={transactions.slice(0, 5)}
              onQuickAction={handleQuickAction}
              onRefresh={refreshAllData}
              loading={loading}
            />
          ) : activeTab === 'money' ? (
            <MoneyView
              calculatedBalance={calculatedBalance}
              totalIncome={totalIncome}
              totalExpenses={totalExpenses}
              transactions={transactions}
              searchQuery={moneySearch}
              setSearchQuery={setMoneySearch}
              filterType={moneyFilterType}
              setFilterType={setMoneyFilterType}
              filterCategory={moneyFilterCategory}
              setFilterCategory={setMoneyFilterCategory}
              onQuickPreset={handleQuickPreset}
              onOpenAddModal={(type) => {
                setTxFormType(type);
                setModalTx(true);
              }}
              onDeleteTransaction={handleDeleteTransaction}
              onRefresh={refreshAllData}
              loading={loading}
            />
          ) : activeTab === 'loans' ? (
            <LoansView
              loans={loans}
              youOwe={youOwe}
              othersOwe={othersOwe}
              filterType={loansFilterType}
              setFilterType={setLoansFilterType}
              onOpenAddModal={() => setModalLoan(true)}
              onSettleLoan={handleSettleLoan}
              onDeleteLoan={handleDeleteLoan}
              onRefresh={refreshAllData}
              loading={loading}
            />
          ) : activeTab === 'luggage' ? (
            <LuggageView
              trips={trips}
              activeTripId={activeTripId}
              setActiveTripId={setActiveTripId}
              onToggleItem={handleToggleLuggageItem}
              onOpenAddTrip={() => setModalTrip(true)}
              onOpenAddItem={() => setModalItem(true)}
              onRefresh={refreshAllData}
              loading={loading}
            />
          ) : (
            <MoreView
              user={user}
              onNavigateToDictionary={() => setActiveSubscreen('dictionary')}
              onNavigateToNotes={() => setActiveSubscreen('notes')}
              onNavigateToLuggage={() => {
                setActiveSubscreen(null);
                setActiveTab('luggage');
              }}
              onTestGemini={handleTestGemini}
              geminiTesting={geminiTesting}
              geminiTestResult={geminiTestResult}
              darkMode={darkMode}
              setDarkMode={setDarkMode}
              onLogout={handleLogout}
            />
          )}
        </div>

        {/* Android Material 3 Bottom Navigation Bar */}
        <nav className="absolute bottom-0 inset-x-0 h-16 bg-white/95 dark:bg-[#191D1B]/95 backdrop-blur-md border-t border-[#DCE5DF]/70 dark:border-[#404944]/50 flex items-center justify-around px-2 z-30 select-none">
          {[
            { id: 'home', label: 'Home', icon: 'home' },
            { id: 'money', label: 'Money', icon: 'wallet' },
            { id: 'loans', label: 'Loans', icon: 'hand-coins' },
            { id: 'luggage', label: 'Luggage', icon: 'luggage' },
            { id: 'more', label: 'More', icon: 'more-horizontal' },
          ].map(item => {
            const isSelected = activeSubscreen === null && activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => {
                  setActiveSubscreen(null);
                  setActiveTab(item.id);
                }}
                className="flex flex-col items-center justify-center flex-1 h-full py-1 text-xs transition-colors"
              >
                <div
                  className={`m3-nav-pill flex items-center justify-center rounded-2xl mb-1 ${
                    isSelected ? 'active' : 'text-[#404944] dark:text-[#C0C9C3]'
                  }`}
                >
                  <Icon
                    name={item.icon}
                    className={`w-5 h-5 ${isSelected ? 'text-[#002117] dark:text-[#A6F2D6]' : 'text-[#404944] dark:text-[#C0C9C3]'}`}
                  />
                </div>
                <span
                  className={`text-[11px] ${
                    isSelected
                      ? 'font-bold text-[#0F6D54] dark:text-[#8AD5BB]'
                      : 'font-medium text-[#404944] dark:text-[#C0C9C3]'
                  }`}
                >
                  {item.label}
                </span>
              </button>
            );
          })}
        </nav>

        {/* Floating Action Buttons / Modals */}
        {modalTx && (
          <Modal title={`${txFormType === 'income' ? 'Add Income' : 'Add Expense'}`} onClose={() => setModalTx(false)}>
            <form onSubmit={handleAddTransaction} className="space-y-4">
              <div className="flex rounded-xl bg-gray-100 dark:bg-[#191D1B] p-1">
                <button
                  type="button"
                  onClick={() => setTxFormType('expense')}
                  className={`flex-1 py-1.5 text-xs font-bold rounded-lg transition ${txFormType === 'expense' ? 'bg-[#DC2626] text-white shadow' : 'text-gray-600 dark:text-gray-400'}`}
                >
                  Expense
                </button>
                <button
                  type="button"
                  onClick={() => setTxFormType('income')}
                  className={`flex-1 py-1.5 text-xs font-bold rounded-lg transition ${txFormType === 'income' ? 'bg-[#16A34A] text-white shadow' : 'text-gray-600 dark:text-gray-400'}`}
                >
                  Income
                </button>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">Amount (Rs.)</label>
                <input
                  type="number"
                  required
                  placeholder="0"
                  value={txForm.amount}
                  onChange={e => setTxForm({ ...txForm, amount: e.target.value })}
                  className="w-full text-xl font-bold p-3 rounded-xl border border-gray-300 dark:border-gray-700 bg-white dark:bg-[#101413] text-[#191C1B] dark:text-[#E1E3DF]"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">Category</label>
                <select
                  value={txForm.category}
                  onChange={e => setTxForm({ ...txForm, category: e.target.value })}
                  className="w-full p-2.5 rounded-xl border border-gray-300 dark:border-gray-700 bg-white dark:bg-[#101413] text-sm"
                >
                  {txFormType === 'expense' ? (
                    <>
                      <option value="Meal & Food">Meal & Food</option>
                      <option value="Transport">Transport</option>
                      <option value="Bills & Utilities">Bills & Utilities</option>
                      <option value="Shopping">Shopping</option>
                      <option value="Fee">Fee / Education</option>
                      <option value="Health & Medical">Health & Medical</option>
                      <option value="Other Expense">Other Expense</option>
                    </>
                  ) : (
                    <>
                      <option value="Salary">Salary</option>
                      <option value="Pocket Money">Pocket Money</option>
                      <option value="Freelance & Gig">Freelance & Gig</option>
                      <option value="Gift / Cash Inflow">Gift / Cash Inflow</option>
                      <option value="Investment / Profit">Investment / Profit</option>
                      <option value="Other Income">Other Income</option>
                    </>
                  )}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">Description / Note</label>
                <input
                  type="text"
                  placeholder="Optional details"
                  value={txForm.description}
                  onChange={e => setTxForm({ ...txForm, description: e.target.value })}
                  className="w-full p-2.5 rounded-xl border border-gray-300 dark:border-gray-700 bg-white dark:bg-[#101413] text-sm"
                />
              </div>

              <button
                type="submit"
                className={`w-full py-3 rounded-xl font-bold text-white shadow-md ${txFormType === 'income' ? 'bg-[#16A34A]' : 'bg-[#DC2626]'}`}
              >
                Save {txFormType === 'income' ? 'Income' : 'Expense'}
              </button>
            </form>
          </Modal>
        )}

        {modalLoan && (
          <Modal title="Record Loan / Borrowing" onClose={() => setModalLoan(false)}>
            <form onSubmit={handleAddLoan} className="space-y-4">
              <div className="flex rounded-xl bg-gray-100 dark:bg-[#191D1B] p-1">
                <button
                  type="button"
                  onClick={() => setLoanForm({ ...loanForm, type: 'lent' })}
                  className={`flex-1 py-1.5 text-xs font-bold rounded-lg transition ${loanForm.type === 'lent' ? 'bg-[#16A34A] text-white shadow' : 'text-gray-600 dark:text-gray-400'}`}
                >
                  I Lent (They owe me)
                </button>
                <button
                  type="button"
                  onClick={() => setLoanForm({ ...loanForm, type: 'borrowed' })}
                  className={`flex-1 py-1.5 text-xs font-bold rounded-lg transition ${loanForm.type === 'borrowed' ? 'bg-[#D97706] text-white shadow' : 'text-gray-600 dark:text-gray-400'}`}
                >
                  I Borrowed (I owe them)
                </button>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">Person Name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g., Bilal Khan"
                  value={loanForm.personName}
                  onChange={e => setLoanForm({ ...loanForm, personName: e.target.value })}
                  className="w-full p-2.5 rounded-xl border border-gray-300 dark:border-gray-700 bg-white dark:bg-[#101413] text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">Amount (Rs.)</label>
                <input
                  type="number"
                  required
                  placeholder="0"
                  value={loanForm.amount}
                  onChange={e => setLoanForm({ ...loanForm, amount: e.target.value })}
                  className="w-full text-lg font-bold p-2.5 rounded-xl border border-gray-300 dark:border-gray-700 bg-white dark:bg-[#101413]"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">Due Date</label>
                <input
                  type="date"
                  value={loanForm.dueDate}
                  onChange={e => setLoanForm({ ...loanForm, dueDate: e.target.value })}
                  className="w-full p-2 rounded-xl border border-gray-300 dark:border-gray-700 bg-white dark:bg-[#101413] text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">Note / Reason</label>
                <input
                  type="text"
                  placeholder="e.g. Shared bill split"
                  value={loanForm.note}
                  onChange={e => setLoanForm({ ...loanForm, note: e.target.value })}
                  className="w-full p-2.5 rounded-xl border border-gray-300 dark:border-gray-700 bg-white dark:bg-[#101413] text-sm"
                />
              </div>

              <button
                type="submit"
                className="w-full py-3 rounded-xl font-bold text-white bg-[#0F6D54] hover:bg-[#074836] shadow-md"
              >
                Save Loan Record
              </button>
            </form>
          </Modal>
        )}

        {modalNote && (
          <Modal title="New Note or Document" onClose={() => setModalNote(false)}>
            <form onSubmit={handleAddNote} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">Title</label>
                <input
                  type="text"
                  required
                  placeholder="Note title"
                  value={noteForm.title}
                  onChange={e => setNoteForm({ ...noteForm, title: e.target.value })}
                  className="w-full p-2.5 rounded-xl border border-gray-300 dark:border-gray-700 bg-white dark:bg-[#101413] font-bold text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">Category</label>
                <input
                  type="text"
                  placeholder="e.g., Work, Finance, Ideas"
                  value={noteForm.category}
                  onChange={e => setNoteForm({ ...noteForm, category: e.target.value })}
                  className="w-full p-2 rounded-xl border border-gray-300 dark:border-gray-700 bg-white dark:bg-[#101413] text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">Content</label>
                <textarea
                  rows={4}
                  placeholder="Write note content or checklist..."
                  value={noteForm.content}
                  onChange={e => setNoteForm({ ...noteForm, content: e.target.value })}
                  className="w-full p-2.5 rounded-xl border border-gray-300 dark:border-gray-700 bg-white dark:bg-[#101413] text-sm"
                />
              </div>
              <button
                type="submit"
                className="w-full py-3 rounded-xl font-bold text-white bg-[#0F6D54] hover:bg-[#074836] shadow-md"
              >
                Save Note
              </button>
            </form>
          </Modal>
        )}

        {modalItem && (
          <Modal title="Add Luggage Item" onClose={() => setModalItem(false)}>
            <form onSubmit={handleAddLuggageItem} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">Item Name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g., Power Bank, Passport"
                  value={itemForm.name}
                  onChange={e => setItemForm({ ...itemForm, name: e.target.value })}
                  className="w-full p-2.5 rounded-xl border border-gray-300 dark:border-gray-700 bg-white dark:bg-[#101413] text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">Category</label>
                <select
                  value={itemForm.category}
                  onChange={e => setItemForm({ ...itemForm, category: e.target.value })}
                  className="w-full p-2.5 rounded-xl border border-gray-300 dark:border-gray-700 bg-white dark:bg-[#101413] text-sm"
                >
                  <option value="Clothes">Clothes</option>
                  <option value="Electronics">Electronics</option>
                  <option value="Documents">Documents</option>
                  <option value="Toiletries">Toiletries</option>
                  <option value="Gear">Gear & Essentials</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">Approx Weight (kg)</label>
                <input
                  type="number"
                  step="0.1"
                  value={itemForm.weightKg}
                  onChange={e => setItemForm({ ...itemForm, weightKg: e.target.value })}
                  className="w-full p-2.5 rounded-xl border border-gray-300 dark:border-gray-700 bg-white dark:bg-[#101413] text-sm"
                />
              </div>
              <button
                type="submit"
                className="w-full py-3 rounded-xl font-bold text-white bg-[#0F6D54] hover:bg-[#074836] shadow-md"
              >
                Add Item
              </button>
            </form>
          </Modal>
        )}

      </div>
    </div>
  );
}

// -------------------------------------------------------------
// Screen 1: Dashboard View (matching DashboardScreen.kt)
// -------------------------------------------------------------
function DashboardView({
  greeting,
  userName,
  calculatedBalance,
  todayExpenses,
  youOwe,
  othersOwe,
  activeTrip,
  pendingPackingCount,
  activeLuggageTrips,
  totalNotesCount,
  pinnedNotesCount,
  recentTransactions,
  onQuickAction,
  onRefresh,
  loading
}) {
  return (
    <div className="p-5 space-y-4">
      {/* Header Greeting */}
      <div className="flex items-center justify-between pt-1">
        <div>
          <div className="text-xs font-medium text-[#404944] dark:text-[#C0C9C3]">{greeting}</div>
          <div className="text-xl font-bold text-[#191C1B] dark:text-[#E1E3DF] tracking-tight">{userName}</div>
        </div>
        <button
          onClick={onRefresh}
          className="w-9 h-9 rounded-full bg-white dark:bg-[#191D1B] border border-[#DCE5DF] dark:border-[#404944] flex items-center justify-center text-[#0F6D54] dark:text-[#8AD5BB] shadow-sm active:scale-95"
          title="Refresh Dashboard"
        >
          <Icon name="refresh-cw" className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {/* Signature Primary Balance Card (Gradient matching Android AccentCardBackground) */}
      <div className="rounded-3xl p-6 text-white shadow-lg bg-gradient-to-br from-[#0F6D54] to-[#074837] relative overflow-hidden elevation-3">
        <div className="flex items-center gap-2.5 mb-3">
          <div className="w-9 h-9 rounded-full bg-white/20 flex items-center justify-center backdrop-blur-sm">
            <Icon name="wallet" className="w-5 h-5 text-white" />
          </div>
          <span className="text-sm font-medium text-[#D0F0E3]">Current Balance</span>
        </div>

        <div className="text-3xl font-extrabold tracking-tight mb-1">
          {formatCurrency(calculatedBalance)}
        </div>
        <div className="text-[11px] text-[#A2DFC7] flex items-center gap-1.5">
          <span className="w-1.5 h-1.5 rounded-full bg-[#A6F2D6] animate-pulse"></span>
          Real-time balance from MongoDB source of truth
        </div>
      </div>

      {/* 3 Metric Cards Row */}
      <div className="grid grid-cols-3 gap-2.5">
        <div className="bg-white dark:bg-[#191D1B] rounded-2xl p-3 border border-[#DCE5DF]/70 dark:border-[#404944]/50 shadow-sm elevation-1 flex flex-col justify-between">
          <span className="text-[11px] font-medium text-[#404944] dark:text-[#C0C9C3] leading-tight line-clamp-1">Today's Spends</span>
          <div className="text-xs font-bold text-[#DC2626] mt-2 truncate">
            {formatCurrency(todayExpenses)}
          </div>
        </div>

        <div className="bg-white dark:bg-[#191D1B] rounded-2xl p-3 border border-[#DCE5DF]/70 dark:border-[#404944]/50 shadow-sm elevation-1 flex flex-col justify-between">
          <span className="text-[11px] font-medium text-[#404944] dark:text-[#C0C9C3] leading-tight line-clamp-1">You Owe</span>
          <div className="text-xs font-bold text-[#D97706] mt-2 truncate">
            {formatCurrency(youOwe)}
          </div>
        </div>

        <div className="bg-white dark:bg-[#191D1B] rounded-2xl p-3 border border-[#DCE5DF]/70 dark:border-[#404944]/50 shadow-sm elevation-1 flex flex-col justify-between">
          <span className="text-[11px] font-medium text-[#404944] dark:text-[#C0C9C3] leading-tight line-clamp-1">Others Owe</span>
          <div className="text-xs font-bold text-[#16A34A] mt-2 truncate">
            {formatCurrency(othersOwe)}
          </div>
        </div>
      </div>

      {/* Travel Packing Summary Banner */}
      {activeLuggageTrips > 0 && (
        <div
          onClick={() => onQuickAction('luggage')}
          className="bg-white dark:bg-[#191D1B] rounded-2xl p-3.5 border border-[#DCE5DF]/70 dark:border-[#404944]/50 shadow-sm elevation-1 flex items-center justify-between cursor-pointer active:scale-98 transition"
        >
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-[#059669]/15 flex items-center justify-center text-[#059669]">
              <Icon name="luggage" className="w-5 h-5 text-[#059669]" />
            </div>
            <div>
              <div className="text-sm font-bold text-[#191C1B] dark:text-[#E1E3DF]">Travel Packing</div>
              <div className={`text-xs ${pendingPackingCount > 0 ? 'text-[#D97706]' : 'text-[#059669]'}`}>
                {pendingPackingCount > 0
                  ? `${pendingPackingCount} items pending across ${activeLuggageTrips} trip(s)`
                  : `All items packed for upcoming travel!`}
              </div>
            </div>
          </div>
          <Icon name="chevron-right" className="w-4 h-4 text-[#404944] dark:text-[#C0C9C3]" />
        </div>
      )}

      {/* Notes & Documents Banner */}
      {totalNotesCount > 0 && (
        <div
          onClick={() => onQuickAction('add_note')}
          className="bg-white dark:bg-[#191D1B] rounded-2xl p-3.5 border border-[#DCE5DF]/70 dark:border-[#404944]/50 shadow-sm elevation-1 flex items-center justify-between cursor-pointer active:scale-98 transition"
        >
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-[#F59E0B]/15 flex items-center justify-center text-[#F59E0B]">
              <Icon name="file-text" className="w-5 h-5 text-[#F59E0B]" />
            </div>
            <div>
              <div className="text-sm font-bold text-[#191C1B] dark:text-[#E1E3DF]">Notes & Documents</div>
              <div className="text-xs text-[#404944] dark:text-[#C0C9C3]">
                {totalNotesCount} note(s) saved ({pinnedNotesCount} pinned)
              </div>
            </div>
          </div>
          <Icon name="chevron-right" className="w-4 h-4 text-[#404944] dark:text-[#C0C9C3]" />
        </div>
      )}

      {/* Quick Actions 6-Grid (matching QuickActionsGrid in Android) */}
      <div className="pt-1">
        <div className="text-sm font-bold text-[#191C1B] dark:text-[#E1E3DF] mb-2.5">Quick Actions</div>
        <div className="grid grid-cols-2 gap-2">
          {[
            { label: '+ Money', key: 'add_money', icon: 'plus-circle' },
            { label: '+ Expense', key: 'add_expense', icon: 'receipt' },
            { label: '+ Loan', key: 'add_loan', icon: 'hand-coins' },
            { label: 'Dictionary', key: 'dictionary', icon: 'book-open' },
            { label: '+ Note', key: 'add_note', icon: 'edit-3' },
            { label: 'Luggage', key: 'luggage', icon: 'briefcase' }
          ].map(action => (
            <button
              key={action.key}
              onClick={() => onQuickAction(action.key)}
              className="bg-white/80 dark:bg-[#191D1B]/80 hover:bg-white dark:hover:bg-[#191D1B] p-3 rounded-2xl border border-[#DCE5DF]/80 dark:border-[#404944]/60 flex items-center gap-2.5 shadow-sm active:scale-95 transition"
            >
              <div className="w-8 h-8 rounded-xl bg-[#A6F2D6] dark:bg-[#00513E] text-[#0F6D54] dark:text-[#A6F2D6] flex items-center justify-center">
                <Icon name={action.icon} className="w-4 h-4 text-[#0F6D54] dark:text-[#A6F2D6]" />
              </div>
              <span className="text-xs font-semibold text-[#191C1B] dark:text-[#E1E3DF]">{action.label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Recent Activity List */}
      <div className="pt-2">
        <div className="text-sm font-bold text-[#191C1B] dark:text-[#E1E3DF] mb-2.5">Recent Activity</div>
        {recentTransactions.length === 0 ? (
          <div className="bg-white dark:bg-[#191D1B] rounded-2xl p-6 text-center border border-[#DCE5DF]/70 dark:border-[#404944]/50">
            <Icon name="receipt" className="w-8 h-8 text-gray-300 dark:text-gray-600 mx-auto mb-2" />
            <div className="text-xs font-semibold text-gray-600 dark:text-gray-300">No transactions yet</div>
            <div className="text-[10px] text-gray-400 mt-0.5">Use Quick Actions to add an income or expense</div>
          </div>
        ) : (
          <div className="space-y-2">
            {recentTransactions.map(tx => {
              const isIncome = tx.type === 'income' || tx.type === 'loan_received';
              return (
                <div
                  key={tx._id}
                  className="bg-white dark:bg-[#191D1B] rounded-2xl p-3 border border-[#DCE5DF]/70 dark:border-[#404944]/50 shadow-sm flex items-center justify-between"
                >
                  <div className="flex items-center gap-3">
                    <div className={`w-9 h-9 rounded-full flex items-center justify-center ${isIncome ? 'bg-[#16A34A]/15 text-[#16A34A]' : 'bg-[#DC2626]/15 text-[#DC2626]'}`}>
                      <Icon name={isIncome ? 'arrow-down-left' : 'arrow-up-right'} className="w-4 h-4" />
                    </div>
                    <div>
                      <div className="text-xs font-bold text-[#191C1B] dark:text-[#E1E3DF]">{tx.category}</div>
                      {tx.description && <div className="text-[10px] text-gray-400 truncate max-w-[150px]">{tx.description}</div>}
                    </div>
                  </div>
                  <div className={`text-xs font-bold ${isIncome ? 'text-[#16A34A]' : 'text-[#DC2626]'}`}>
                    {isIncome ? '+ ' : '- '}{formatCurrency(tx.amount)}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

// -------------------------------------------------------------
// Screen 2: Money & Expenses View (matching MoneyScreen.kt)
// -------------------------------------------------------------
function MoneyView({
  calculatedBalance,
  totalIncome,
  totalExpenses,
  transactions,
  searchQuery,
  setSearchQuery,
  filterType,
  setFilterType,
  filterCategory,
  setFilterCategory,
  onQuickPreset,
  onOpenAddModal,
  onDeleteTransaction,
  onRefresh,
  loading
}) {
  const filtered = useMemo(() => {
    return transactions.filter(t => {
      if (filterType === 'expense' && t.type !== 'expense') return false;
      if (filterType === 'income' && t.type !== 'income') return false;
      if (filterCategory !== 'all' && t.category !== filterCategory) return false;
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const inCat = (t.category || '').toLowerCase().includes(q);
        const inDesc = (t.description || '').toLowerCase().includes(q);
        if (!inCat && !inDesc) return false;
      }
      return true;
    });
  }, [transactions, filterType, filterCategory, searchQuery]);

  return (
    <div className="p-5 space-y-4">
      {/* Title */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-[#191C1B] dark:text-[#E1E3DF] tracking-tight">Money & Expenses</h2>
          <p className="text-xs text-[#404944] dark:text-[#C0C9C3]">Track income, daily spends & cashflow</p>
        </div>
        <button onClick={onRefresh} className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 text-[#0F6D54]">
          <Icon name="refresh-cw" className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {/* Wallet Card with Gradient */}
      <div className="rounded-3xl p-5 text-white shadow-lg bg-gradient-to-r from-[#074836] via-[#0F6D54] to-[#1E9B78]">
        <div className="text-xs uppercase tracking-wider text-white/80">Current Net Balance</div>
        <div className="text-2xl font-extrabold mt-1 mb-4">{formatCurrency(calculatedBalance)}</div>

        <div className="flex items-center justify-between pt-2 border-t border-white/20">
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-full bg-white/20 flex items-center justify-center">
              <Icon name="arrow-down-left" className="w-3.5 h-3.5 text-white" />
            </div>
            <div>
              <div className="text-[10px] text-white/70">Total Income</div>
              <div className="text-xs font-bold text-white">{formatCurrency(totalIncome)}</div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-full bg-white/20 flex items-center justify-center">
              <Icon name="arrow-up-right" className="w-3.5 h-3.5 text-white" />
            </div>
            <div>
              <div className="text-[10px] text-white/70">Total Expenses</div>
              <div className="text-xs font-bold text-white">{formatCurrency(totalExpenses)}</div>
            </div>
          </div>
        </div>
      </div>

      {/* Quick Spend Shortcuts (Tea, Lunch, Transport, Pocket Money) */}
      <div>
        <div className="flex items-center justify-between text-xs font-bold text-[#191C1B] dark:text-[#E1E3DF] mb-2">
          <span>Quick Spend Shortcuts</span>
          <span className="text-[10px] font-normal text-gray-400">1-Tap Record</span>
        </div>
        <div className="flex gap-2 overflow-x-auto pb-1 no-scrollbar">
          {[
            { label: 'Tea / Chai', cat: 'Meal & Food', amount: 80, type: 'expense' },
            { label: 'Lunch', cat: 'Meal & Food', amount: 450, type: 'expense' },
            { label: 'Transport', cat: 'Transport', amount: 300, type: 'expense' },
            { label: 'Mobile Pkg', cat: 'Bills & Utilities', amount: 800, type: 'expense' },
            { label: 'Freelance', cat: 'Freelance & Gig', amount: 25000, type: 'income' },
          ].map(p => (
            <button
              key={p.label}
              onClick={() => onQuickPreset(p.label, p.cat, p.amount, p.type)}
              className="px-3 py-2 rounded-2xl bg-white dark:bg-[#191D1B] border border-[#DCE5DF] dark:border-[#404944] shadow-sm flex items-center gap-1.5 shrink-0 text-xs font-medium active:scale-95 transition"
            >
              <span>{p.label}</span>
              <span className={`font-bold ${p.type === 'income' ? 'text-[#16A34A]' : 'text-[#DC2626]'}`}>
                Rs. {p.amount}
              </span>
            </button>
          ))}
        </div>
      </div>

      {/* Dual Fast Action Buttons */}
      <div className="grid grid-cols-2 gap-2.5">
        <button
          onClick={() => onOpenAddModal('income')}
          className="py-2.5 px-3 rounded-2xl bg-[#16A34A]/15 border border-[#16A34A]/30 text-[#16A34A] font-bold text-xs flex items-center justify-center gap-2 active:scale-95 transition"
        >
          <Icon name="trending-up" className="w-4 h-4" />
          <span>+ Add Income</span>
        </button>

        <button
          onClick={() => onOpenAddModal('expense')}
          className="py-2.5 px-3 rounded-2xl bg-[#DC2626]/15 border border-[#DC2626]/30 text-[#DC2626] font-bold text-xs flex items-center justify-center gap-2 active:scale-95 transition"
        >
          <Icon name="trending-down" className="w-4 h-4" />
          <span>- Add Expense</span>
        </button>
      </div>

      {/* Search & Filter Chips */}
      <div className="space-y-2">
        <div className="relative">
          <input
            type="text"
            placeholder="Search records, descriptions..."
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-8 py-2 rounded-2xl border border-[#DCE5DF] dark:border-[#404944] bg-white dark:bg-[#191D1B] text-xs"
          />
          <Icon name="search" className="w-4 h-4 text-gray-400 absolute left-3 top-2.5" />
          {searchQuery && (
            <button onClick={() => setSearchQuery('')} className="absolute right-2.5 top-2.5 text-gray-400">
              <Icon name="x" className="w-3.5 h-3.5" />
            </button>
          )}
        </div>

        {/* Type Filter Chips */}
        <div className="flex gap-1.5">
          {[
            { id: 'all', label: 'All Records' },
            { id: 'expense', label: 'Expenses' },
            { id: 'income', label: 'Income' },
          ].map(chip => (
            <button
              key={chip.id}
              onClick={() => setFilterType(chip.id)}
              className={`flex-1 py-1.5 rounded-xl text-xs font-semibold border transition ${
                filterType === chip.id
                  ? 'bg-[#A6F2D6] dark:bg-[#00513E] text-[#002117] dark:text-[#A6F2D6] border-transparent font-bold'
                  : 'bg-white dark:bg-[#191D1B] text-[#404944] dark:text-[#C0C9C3] border-[#DCE5DF] dark:border-[#404944]'
              }`}
            >
              {chip.label}
            </button>
          ))}
        </div>
      </div>

      {/* Transactions List */}
      <div>
        <div className="text-xs font-bold text-[#191C1B] dark:text-[#E1E3DF] mb-2">
          Transactions ({filtered.length})
        </div>

        {filtered.length === 0 ? (
          <div className="bg-white dark:bg-[#191D1B] rounded-2xl p-6 text-center border border-[#DCE5DF]/70 dark:border-[#404944]/50">
            <Icon name="receipt" className="w-8 h-8 text-gray-300 dark:text-gray-600 mx-auto mb-2" />
            <div className="text-xs font-semibold text-gray-600 dark:text-gray-300">No matching transactions</div>
            <div className="text-[10px] text-gray-400 mt-0.5">Adjust filter or tap buttons above to add entries</div>
          </div>
        ) : (
          <div className="space-y-2">
            {filtered.map(tx => {
              const isIncome = tx.type === 'income' || tx.type === 'loan_received';
              return (
                <div
                  key={tx._id}
                  className="bg-white dark:bg-[#191D1B] rounded-2xl p-3 border border-[#DCE5DF]/70 dark:border-[#404944]/50 shadow-sm flex items-center justify-between"
                >
                  <div className="flex items-center gap-3">
                    <div className={`w-9 h-9 rounded-full flex items-center justify-center ${isIncome ? 'bg-[#16A34A]/15 text-[#16A34A]' : 'bg-[#DC2626]/15 text-[#DC2626]'}`}>
                      <Icon name={isIncome ? 'arrow-down-left' : 'arrow-up-right'} className="w-4 h-4" />
                    </div>
                    <div>
                      <div className="text-xs font-bold text-[#191C1B] dark:text-[#E1E3DF]">{tx.category}</div>
                      <div className="text-[10px] text-gray-400 flex items-center gap-2">
                        <span>{tx.date ? new Date(tx.date).toLocaleDateString() : 'Recent'}</span>
                        {tx.description && <span className="truncate max-w-[120px]">• {tx.description}</span>}
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    <div className={`text-xs font-bold ${isIncome ? 'text-[#16A34A]' : 'text-[#DC2626]'}`}>
                      {isIncome ? '+ ' : '- '}{formatCurrency(tx.amount)}
                    </div>
                    <button
                      onClick={() => onDeleteTransaction(tx._id)}
                      className="p-1 rounded text-gray-300 hover:text-red-500 transition"
                      title="Delete Transaction"
                    >
                      <Icon name="trash-2" className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

// -------------------------------------------------------------
// Screen 3: Loans Tracker View (matching LoansScreen.kt)
// -------------------------------------------------------------
function LoansView({ loans, youOwe, othersOwe, filterType, setFilterType, onOpenAddModal, onSettleLoan, onDeleteLoan, onRefresh, loading }) {
  const filteredLoans = useMemo(() => {
    return loans.filter(l => {
      if (filterType === 'lent' && l.type !== 'lent') return false;
      if (filterType === 'borrowed' && l.type !== 'borrowed') return false;
      if (filterType === 'settled' && l.status !== 'paid') return false;
      return true;
    });
  }, [loans, filterType]);

  return (
    <div className="p-5 space-y-4">
      {/* Title */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-[#191C1B] dark:text-[#E1E3DF] tracking-tight">Loans Tracker</h2>
          <p className="text-xs text-[#404944] dark:text-[#C0C9C3]">Debts, borrowings & settlements</p>
        </div>
        <button onClick={onRefresh} className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 text-[#0F6D54]">
          <Icon name="refresh-cw" className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {/* Position Cards */}
      <div className="grid grid-cols-2 gap-3">
        <div className="bg-white dark:bg-[#191D1B] rounded-2xl p-4 border border-[#DCE5DF]/70 dark:border-[#404944]/50 shadow-sm">
          <div className="flex items-center gap-2 text-xs font-semibold text-[#16A34A] mb-1">
            <Icon name="arrow-up-right" className="w-4 h-4" />
            <span>Others Owe You</span>
          </div>
          <div className="text-lg font-bold text-[#191C1B] dark:text-[#E1E3DF]">{formatCurrency(othersOwe)}</div>
          <div className="text-[10px] text-gray-400 mt-1">Given to friends / family</div>
        </div>

        <div className="bg-white dark:bg-[#191D1B] rounded-2xl p-4 border border-[#DCE5DF]/70 dark:border-[#404944]/50 shadow-sm">
          <div className="flex items-center gap-2 text-xs font-semibold text-[#D97706] mb-1">
            <Icon name="arrow-down-left" className="w-4 h-4" />
            <span>You Owe</span>
          </div>
          <div className="text-lg font-bold text-[#191C1B] dark:text-[#E1E3DF]">{formatCurrency(youOwe)}</div>
          <div className="text-[10px] text-gray-400 mt-1">Borrowed / pending payoff</div>
        </div>
      </div>

      {/* Add Loan Button */}
      <button
        onClick={onOpenAddModal}
        className="w-full py-3 rounded-2xl bg-[#0F6D54] hover:bg-[#074836] text-white font-bold text-xs flex items-center justify-center gap-2 shadow-md active:scale-95 transition"
      >
        <Icon name="plus" className="w-4 h-4" />
        <span>+ Record New Loan</span>
      </button>

      {/* Filter Chips */}
      <div className="flex gap-1.5 overflow-x-auto pb-1 no-scrollbar">
        {[
          { id: 'all', label: 'All Loans' },
          { id: 'lent', label: 'To Collect' },
          { id: 'borrowed', label: 'To Pay' },
          { id: 'settled', label: 'Settled' },
        ].map(chip => (
          <button
            key={chip.id}
            onClick={() => setFilterType(chip.id)}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold border transition shrink-0 ${
              filterType === chip.id
                ? 'bg-[#A6F2D6] dark:bg-[#00513E] text-[#002117] dark:text-[#A6F2D6] border-transparent font-bold'
                : 'bg-white dark:bg-[#191D1B] text-[#404944] dark:text-[#C0C9C3] border-[#DCE5DF] dark:border-[#404944]'
            }`}
          >
            {chip.label}
          </button>
        ))}
      </div>

      {/* Loans List */}
      <div className="space-y-2.5">
        {filteredLoans.length === 0 ? (
          <div className="bg-white dark:bg-[#191D1B] rounded-2xl p-6 text-center border border-[#DCE5DF]/70 dark:border-[#404944]/50">
            <Icon name="hand-coins" className="w-8 h-8 text-gray-300 dark:text-gray-600 mx-auto mb-2" />
            <div className="text-xs font-semibold text-gray-600 dark:text-gray-300">No loans found</div>
            <div className="text-[10px] text-gray-400 mt-0.5">Use the button above to record borrowings or lendings</div>
          </div>
        ) : (
          filteredLoans.map(loan => {
            const isLent = loan.type === 'lent';
            const isPaid = loan.status === 'paid';
            return (
              <div
                key={loan._id}
                className="bg-white dark:bg-[#191D1B] rounded-2xl p-4 border border-[#DCE5DF]/70 dark:border-[#404944]/50 shadow-sm space-y-2.5"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className={`px-2 py-0.5 rounded-md text-[10px] font-bold ${isLent ? 'bg-[#16A34A]/15 text-[#16A34A]' : 'bg-[#D97706]/15 text-[#D97706]'}`}>
                      {isLent ? 'Lent' : 'Borrowed'}
                    </span>
                    <span className="text-xs font-bold text-[#191C1B] dark:text-[#E1E3DF]">{loan.personName}</span>
                  </div>

                  <span className={`text-xs font-bold ${isPaid ? 'text-gray-400 line-through' : isLent ? 'text-[#16A34A]' : 'text-[#D97706]'}`}>
                    {formatCurrency(loan.amount)}
                  </span>
                </div>

                {loan.notes && <div className="text-xs text-gray-500 dark:text-gray-400">{loan.notes}</div>}

                <div className="flex items-center justify-between pt-1 text-[11px] text-gray-400 border-t border-gray-100 dark:border-gray-800">
                  <span>Due: {loan.dueDate ? new Date(loan.dueDate).toLocaleDateString() : 'Flexible'}</span>

                  <div className="flex items-center gap-2">
                    {!isPaid && (
                      <button
                        onClick={() => onSettleLoan(loan._id)}
                        className="px-2.5 py-1 rounded-lg bg-[#A6F2D6] dark:bg-[#00513E] text-[#002117] dark:text-[#A6F2D6] text-[10px] font-bold hover:opacity-90"
                      >
                        Settle
                      </button>
                    )}
                    {isPaid && (
                      <span className="text-xs text-[#16A34A] font-bold flex items-center gap-1">
                        <Icon name="check" className="w-3.5 h-3.5" /> Paid
                      </span>
                    )}
                    <button
                      onClick={() => onDeleteLoan(loan._id)}
                      className="text-gray-300 hover:text-red-500 p-1"
                    >
                      <Icon name="trash-2" className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}

// -------------------------------------------------------------
// Screen 4: Luggage & Trips View (matching LuggageScreen.kt)
// -------------------------------------------------------------
function LuggageView({ trips, activeTripId, setActiveTripId, onToggleItem, onOpenAddTrip, onOpenAddItem, onRefresh, loading }) {
  const currentTrip = useMemo(() => trips.find(t => t._id === activeTripId) || trips[0] || null, [trips, activeTripId]);

  const items = currentTrip ? currentTrip.items || [] : [];
  const packedCount = items.filter(i => i.packed).length;
  const totalCount = items.length;
  const progressPercent = totalCount > 0 ? Math.round((packedCount / totalCount) * 100) : 0;

  return (
    <div className="p-5 space-y-4">
      {/* Title */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-[#191C1B] dark:text-[#E1E3DF] tracking-tight">Luggage & Packing</h2>
          <p className="text-xs text-[#404944] dark:text-[#C0C9C3]">Travel checklists & baggage limits</p>
        </div>
        <button onClick={onRefresh} className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 text-[#0F6D54]">
          <Icon name="refresh-cw" className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {/* Trip Switcher / Banner */}
      {currentTrip ? (
        <div className="rounded-3xl p-5 bg-gradient-to-br from-[#0F6D54] to-[#074837] text-white shadow-lg space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Icon name="map-pin" className="w-4 h-4 text-[#A6F2D6]" />
              <span className="text-sm font-bold truncate max-w-[200px]">{currentTrip.destination}</span>
            </div>
            <span className="text-[11px] text-white/80">{currentTrip.travelDates || 'Upcoming'}</span>
          </div>

          <div>
            <div className="flex items-center justify-between text-xs text-[#D0F0E3] mb-1 font-semibold">
              <span>Packing Progress</span>
              <span>{packedCount} / {totalCount} packed ({progressPercent}%)</span>
            </div>
            <div className="w-full h-2 rounded-full bg-black/20 overflow-hidden">
              <div className="h-full bg-[#A6F2D6] transition-all duration-300" style={{ width: `${progressPercent}%` }} />
            </div>
          </div>
        </div>
      ) : (
        <div className="bg-white dark:bg-[#191D1B] rounded-2xl p-6 text-center border border-[#DCE5DF]">
          <Icon name="luggage" className="w-8 h-8 text-gray-300 mx-auto mb-2" />
          <div className="text-xs font-bold text-gray-700 dark:text-gray-300">No active trips</div>
          <button
            onClick={onOpenAddTrip}
            className="mt-3 px-4 py-2 rounded-xl bg-[#0F6D54] text-white font-bold text-xs"
          >
            + Create Trip
          </button>
        </div>
      )}

      {/* Add Item Action */}
      {currentTrip && (
        <div className="flex gap-2">
          <button
            onClick={onOpenAddItem}
            className="flex-1 py-2.5 rounded-2xl bg-[#0F6D54] text-white font-bold text-xs flex items-center justify-center gap-2 shadow-sm active:scale-95 transition"
          >
            <Icon name="plus" className="w-4 h-4" />
            <span>Add Item</span>
          </button>
        </div>
      )}

      {/* Checklist Items */}
      {currentTrip && (
        <div className="space-y-2">
          <div className="text-xs font-bold text-[#191C1B] dark:text-[#E1E3DF]">
            Items ({items.length})
          </div>

          {items.length === 0 ? (
            <div className="bg-white dark:bg-[#191D1B] rounded-2xl p-5 text-center text-xs text-gray-400">
              No items in packing list. Tap '+ Add Item' above!
            </div>
          ) : (
            items.map(item => (
              <div
                key={item._id}
                onClick={() => onToggleItem(currentTrip._id, item._id)}
                className={`p-3 rounded-2xl border flex items-center justify-between cursor-pointer transition select-none ${
                  item.packed
                    ? 'bg-[#EBF7F2] dark:bg-[#00382A]/30 border-[#A6F2D6]/70 dark:border-[#0F6D54]/50'
                    : 'bg-white dark:bg-[#191D1B] border-[#DCE5DF] dark:border-[#404944]'
                }`}
              >
                <div className="flex items-center gap-3">
                  <div
                    className={`w-5 h-5 rounded-lg flex items-center justify-center border transition ${
                      item.packed ? 'bg-[#0F6D54] border-[#0F6D54] text-white' : 'border-gray-300 dark:border-gray-600'
                    }`}
                  >
                    {item.packed && <Icon name="check" className="w-3.5 h-3.5" />}
                  </div>
                  <div>
                    <div className={`text-xs font-semibold ${item.packed ? 'line-through text-gray-400' : 'text-[#191C1B] dark:text-[#E1E3DF]'}`}>
                      {item.name}
                    </div>
                    <div className="text-[10px] text-gray-400">{item.category} • {item.weightKg || 1} kg</div>
                  </div>
                </div>

                <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${item.packed ? 'bg-[#A6F2D6] text-[#002117]' : 'bg-gray-100 dark:bg-gray-800 text-gray-500'}`}>
                  {item.packed ? 'Packed' : 'Pending'}
                </span>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}

// -------------------------------------------------------------
// Screen 5: More & Settings Screen (matching SettingsScreen.kt)
// -------------------------------------------------------------
function MoreView({ user, onNavigateToDictionary, onNavigateToNotes, onNavigateToLuggage, onTestGemini, geminiTesting, geminiTestResult, darkMode, setDarkMode, onLogout }) {
  return (
    <div className="p-5 space-y-4">
      <h2 className="text-xl font-bold text-[#191C1B] dark:text-[#E1E3DF] tracking-tight">Settings & Modules</h2>

      {/* Profile Card */}
      <div className="bg-white dark:bg-[#191D1B] rounded-2xl p-4 border border-[#DCE5DF]/70 dark:border-[#404944]/50 shadow-sm flex items-center gap-3.5">
        <div className="w-12 h-12 rounded-full bg-[#A6F2D6] dark:bg-[#00513E] text-[#0F6D54] dark:text-[#A6F2D6] flex items-center justify-center text-lg font-bold">
          {user.name ? user.name.charAt(0).toUpperCase() : 'U'}
        </div>
        <div className="flex-1 min-w-0">
          <div className="text-sm font-bold text-[#191C1B] dark:text-[#E1E3DF] truncate">{user.name || 'Personal User'}</div>
          <div className="text-xs text-gray-400 truncate">{user.email || 'user@manager.app'}</div>
        </div>
      </div>

      {/* App Modules */}
      <div>
        <div className="text-xs font-bold text-[#0F6D54] dark:text-[#8AD5BB] uppercase tracking-wider mb-2">App Modules & Features</div>
        <div className="bg-white dark:bg-[#191D1B] rounded-2xl border border-[#DCE5DF]/70 dark:border-[#404944]/50 overflow-hidden divide-y divide-gray-100 dark:divide-gray-800">
          <button
            onClick={onNavigateToDictionary}
            className="w-full p-3.5 flex items-center justify-between hover:bg-gray-50 dark:hover:bg-gray-800/50 text-left transition"
          >
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-blue-50 dark:bg-blue-900/30 text-blue-600 flex items-center justify-center">
                <Icon name="book-open" className="w-5 h-5" />
              </div>
              <div>
                <div className="text-xs font-bold text-[#191C1B] dark:text-[#E1E3DF]">AI Smart Dictionary</div>
                <div className="text-[10px] text-gray-400">Word definitions, analogies & Gemini test</div>
              </div>
            </div>
            <Icon name="chevron-right" className="w-4 h-4 text-gray-400" />
          </button>

          <button
            onClick={onNavigateToNotes}
            className="w-full p-3.5 flex items-center justify-between hover:bg-gray-50 dark:hover:bg-gray-800/50 text-left transition"
          >
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-amber-50 dark:bg-amber-900/30 text-amber-600 flex items-center justify-center">
                <Icon name="file-text" className="w-5 h-5" />
              </div>
              <div>
                <div className="text-xs font-bold text-[#191C1B] dark:text-[#E1E3DF]">Notes & Documents</div>
                <div className="text-[10px] text-gray-400">Personal notes, checklists & pinned docs</div>
              </div>
            </div>
            <Icon name="chevron-right" className="w-4 h-4 text-gray-400" />
          </button>

          <button
            onClick={onNavigateToLuggage}
            className="w-full p-3.5 flex items-center justify-between hover:bg-gray-50 dark:hover:bg-gray-800/50 text-left transition"
          >
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-emerald-50 dark:bg-emerald-900/30 text-emerald-600 flex items-center justify-center">
                <Icon name="luggage" className="w-5 h-5" />
              </div>
              <div>
                <div className="text-xs font-bold text-[#191C1B] dark:text-[#E1E3DF]">Luggage & Packing</div>
                <div className="text-[10px] text-gray-400">Trip checklists and baggage management</div>
              </div>
            </div>
            <Icon name="chevron-right" className="w-4 h-4 text-gray-400" />
          </button>
        </div>
      </div>

      {/* Gemini AI Live Status & Verification Card */}
      <div>
        <div className="text-xs font-bold text-[#0F6D54] dark:text-[#8AD5BB] uppercase tracking-wider mb-2">Gemini AI Engine</div>
        <div className="bg-white dark:bg-[#191D1B] rounded-2xl p-4 border border-[#DCE5DF]/70 dark:border-[#404944]/50 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Icon name="sparkles" className="w-4 h-4 text-[#0F6D54] dark:text-[#8AD5BB]" />
              <span className="text-xs font-bold text-[#191C1B] dark:text-[#E1E3DF]">Gemini Integration</span>
            </div>
            <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-[#A6F2D6] text-[#002117]">
              gemini-3.6-flash
            </span>
          </div>

          <p className="text-[11px] text-gray-500 dark:text-gray-400 leading-relaxed">
            Verify real-time communication between Personal Life Manager and Google Gemini API.
          </p>

          <button
            onClick={onTestGemini}
            disabled={geminiTesting}
            className="w-full py-2.5 rounded-xl bg-[#0F6D54] hover:bg-[#074836] text-white font-bold text-xs flex items-center justify-center gap-2 shadow-sm transition disabled:opacity-50"
          >
            <Icon name="zap" className={`w-3.5 h-3.5 ${geminiTesting ? 'animate-bounce' : ''}`} />
            <span>{geminiTesting ? 'Testing Gemini API...' : 'Test Gemini AI Live'}</span>
          </button>

          {geminiTestResult && (
            <div className={`p-3 rounded-xl text-xs ${geminiTestResult.success ? 'bg-[#EBF7F2] text-[#002117] border border-[#A6F2D6]' : 'bg-red-50 text-red-700 border border-red-200'}`}>
              <div className="font-bold flex items-center gap-1.5 mb-1">
                <Icon name={geminiTestResult.success ? "check-circle" : "alert-circle"} className="w-4 h-4" />
                <span>{geminiTestResult.success ? 'Gemini API is WORKING!' : 'Gemini Test Returned Error'}</span>
              </div>
              <div className="text-[11px] opacity-90">
                {geminiTestResult.reply || geminiTestResult.message}
              </div>
              {geminiTestResult.maskedKey && (
                <div className="text-[9px] text-gray-400 mt-1">Key: {geminiTestResult.maskedKey}</div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* System Configuration */}
      <div>
        <div className="text-xs font-bold text-[#0F6D54] dark:text-[#8AD5BB] uppercase tracking-wider mb-2">Configuration</div>
        <div className="bg-white dark:bg-[#191D1B] rounded-2xl p-4 border border-[#DCE5DF]/70 dark:border-[#404944]/50 space-y-3">
          <div className="flex items-center justify-between text-xs">
            <span className="font-medium text-[#191C1B] dark:text-[#E1E3DF]">Default Currency</span>
            <span className="font-bold text-[#0F6D54] dark:text-[#8AD5BB]">PKR (Rs.)</span>
          </div>

          <div className="flex items-center justify-between text-xs pt-2 border-t border-gray-100 dark:border-gray-800">
            <span className="font-medium text-[#191C1B] dark:text-[#E1E3DF]">Dark Appearance</span>
            <button
              onClick={() => setDarkMode(!darkMode)}
              className={`w-10 h-6 rounded-full p-1 transition-colors ${darkMode ? 'bg-[#0F6D54]' : 'bg-gray-300'}`}
            >
              <div className={`w-4 h-4 rounded-full bg-white transition-transform ${darkMode ? 'translate-x-4' : 'translate-x-0'}`} />
            </button>
          </div>
        </div>
      </div>

      {/* Logout Button */}
      <button
        onClick={onLogout}
        className="w-full py-3 rounded-2xl bg-red-50 dark:bg-red-950/40 text-red-600 dark:text-red-400 font-bold text-xs flex items-center justify-center gap-2 border border-red-200 dark:border-red-900/50 hover:bg-red-100 transition"
      >
        <Icon name="log-out" className="w-4 h-4" />
        <span>Sign Out</span>
      </button>
    </div>
  );
}

// -------------------------------------------------------------
// Subscreen: Dictionary & Academic Lexicon (Merriam-Webster + Gemini AI)
// -------------------------------------------------------------
function DictionarySubscreen({
  dictWord,
  setDictWord,
  dictResult,
  dictLoading,
  onLookup,
  onBack,
  dictActiveTab,
  setDictActiveTab,
  dictSavedWords,
  dictSavedWordsLoading,
  onSaveWord,
  onUpdateMastery,
  onDeleteWord,
  onFetchSavedWords,
  dictAiAssistantData,
  dictAiAssistantLoading,
  dictAiActiveFeature,
  onCallAiAssistant,
  geminiTestResult,
  geminiTesting,
  onTestGemini
}) {
  const [saveModalOpen, setSaveModalOpen] = useState(false);
  const [selectedMastery, setSelectedMastery] = useState('learning');
  const [personalNoteText, setPersonalNoteText] = useState('');
  const [notebookFilter, setNotebookFilter] = useState('all');
  const [notebookSearch, setNotebookSearch] = useState('');
  const [playingAudio, setPlayingAudio] = useState(false);

  // Suggested academic words from prompt
  const academicPills = ['mitigate', 'ephemeral', 'ubiquitous', 'exacerbate', 'paradigm'];

  // Audio player helper
  const playAudio = (url, fallbackText) => {
    if (url) {
      setPlayingAudio(true);
      const audio = new Audio(url);
      audio.play().catch(e => {
        console.warn('Audio play error, falling back to speech synthesis:', e);
        if ('speechSynthesis' in window) {
          const utt = new SpeechSynthesisUtterance(fallbackText || dictResult?.word);
          window.speechSynthesis.speak(utt);
        }
      }).finally(() => {
        setTimeout(() => setPlayingAudio(false), 1200);
      });
    } else if ('speechSynthesis' in window && (fallbackText || dictResult?.word)) {
      const utt = new SpeechSynthesisUtterance(fallbackText || dictResult?.word);
      window.speechSynthesis.speak(utt);
    }
  };

  // Load saved words whenever notebook tab is clicked
  useEffect(() => {
    if (dictActiveTab === 'notebook' && onFetchSavedWords) {
      onFetchSavedWords();
    }
  }, [dictActiveTab]);

  const filteredSavedWords = useMemo(() => {
    return (dictSavedWords || []).filter(w => {
      if (notebookFilter !== 'all' && w.masteryStatus !== notebookFilter) return false;
      if (notebookSearch.trim()) {
        const q = notebookSearch.toLowerCase();
        return (w.word || '').toLowerCase().includes(q) ||
               (w.shortDefinition || '').toLowerCase().includes(q) ||
               (w.personalNotes || '').toLowerCase().includes(q);
      }
      return true;
    });
  }, [dictSavedWords, notebookFilter, notebookSearch]);

  return (
    <div className="p-4 sm:p-5 space-y-4 max-w-3xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <button onClick={onBack} className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 transition">
            <Icon name="arrow-left" className="w-5 h-5 text-[#0F6D54] dark:text-[#8AD5BB]" />
          </button>
          <div>
            <h2 className="text-xl font-bold text-[#191C1B] dark:text-[#E1E3DF] tracking-tight">
              Collegiate Lexicon & AI
            </h2>
            <p className="text-xs text-[#404944] dark:text-[#C0C9C3]">
              Merriam-Webster® Collegiate Dictionary & Thesaurus
            </p>
          </div>
        </div>

        {/* Tab Switcher */}
        <div className="flex bg-gray-100 dark:bg-[#191D1B] p-1 rounded-2xl border border-[#DCE5DF] dark:border-[#404944]">
          <button
            onClick={() => setDictActiveTab('search')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition flex items-center gap-1.5 ${
              dictActiveTab === 'search'
                ? 'bg-[#0F6D54] text-white shadow-sm'
                : 'text-gray-500 hover:text-gray-900 dark:hover:text-gray-100'
            }`}
          >
            <Icon name="search" className="w-3.5 h-3.5" />
            <span>Search</span>
          </button>
          <button
            onClick={() => setDictActiveTab('notebook')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition flex items-center gap-1.5 ${
              dictActiveTab === 'notebook'
                ? 'bg-[#0F6D54] text-white shadow-sm'
                : 'text-gray-500 hover:text-gray-900 dark:hover:text-gray-100'
            }`}
          >
            <Icon name="bookmark" className="w-3.5 h-3.5" />
            <span>Notebook</span>
            {dictSavedWords?.length > 0 && (
              <span className="ml-1 px-1.5 py-0.2 rounded-full text-[10px] bg-[#A6F2D6] text-[#002117] font-extrabold">
                {dictSavedWords.length}
              </span>
            )}
          </button>
        </div>
      </div>

      {dictActiveTab === 'search' ? (
        <div className="space-y-4">
          {/* Search Form */}
          <form onSubmit={onLookup} className="space-y-2.5">
            <div className="relative">
              <input
                type="text"
                placeholder="Search Collegiate Dictionary (e.g., mitigate, paradigm)..."
                value={dictWord}
                onChange={e => setDictWord(e.target.value)}
                className="w-full pl-10 pr-24 py-3.5 rounded-2xl border border-[#DCE5DF] dark:border-[#404944] bg-white dark:bg-[#191D1B] text-sm font-semibold shadow-sm focus:outline-none focus:ring-2 focus:ring-[#0F6D54]"
              />
              <Icon name="search" className="w-4 h-4 text-gray-400 absolute left-3.5 top-4" />
              {dictWord && (
                <button
                  type="button"
                  onClick={() => setDictWord('')}
                  className="absolute right-20 top-3.5 text-gray-400 hover:text-gray-600 p-1"
                >
                  <Icon name="x" className="w-3.5 h-3.5" />
                </button>
              )}
              <button
                type="submit"
                disabled={dictLoading}
                className="absolute right-2 top-2 px-4 py-2 rounded-xl bg-[#0F6D54] hover:bg-[#0b5340] text-white font-bold text-xs shadow transition flex items-center gap-1"
              >
                {dictLoading ? (
                  <>
                    <span className="animate-spin text-xs">⟳</span>
                    <span>Looking...</span>
                  </>
                ) : (
                  <span>Lookup</span>
                )}
              </button>
            </div>

            {/* Academic Word Recommendations */}
            <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar py-0.5">
              <span className="text-[11px] font-bold text-gray-400 uppercase tracking-wider shrink-0 mr-1">
                Explore:
              </span>
              {academicPills.map(pw => (
                <button
                  key={pw}
                  type="button"
                  onClick={() => {
                    setDictWord(pw);
                    onLookup(null, pw);
                  }}
                  className="px-2.5 py-1 rounded-xl text-xs font-semibold bg-white dark:bg-[#191D1B] text-[#0F6D54] dark:text-[#8AD5BB] border border-[#DCE5DF] dark:border-[#404944] hover:bg-[#EBF7F2] dark:hover:bg-[#082B21] shrink-0 transition"
                >
                  {pw}
                </button>
              ))}
            </div>
          </form>

          {/* Invalid Word or Suggestions View */}
          {dictResult && !dictResult.success && (
            <div className="p-4 rounded-2xl bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-900/50 space-y-2">
              <div className="flex items-center gap-2 text-amber-800 dark:text-amber-300 font-bold text-sm">
                <Icon name="alert-triangle" className="w-4 h-4 text-amber-600" />
                <span>Word Not Found</span>
              </div>
              <p className="text-xs text-amber-900 dark:text-amber-200">
                {dictResult.message || 'We could not find an entry for that exact spelling.'}
              </p>
              {dictResult.suggestions && dictResult.suggestions.length > 0 && (
                <div className="pt-2">
                  <span className="text-[11px] font-bold text-amber-900 dark:text-amber-200 uppercase">
                    Did you mean:
                  </span>
                  <div className="flex flex-wrap gap-1.5 mt-1.5">
                    {dictResult.suggestions.map(sug => (
                      <button
                        key={sug}
                        type="button"
                        onClick={() => {
                          setDictWord(sug);
                          onLookup(null, sug);
                        }}
                        className="px-2.5 py-1 rounded-lg bg-white dark:bg-[#191D1B] border border-amber-300 text-xs font-semibold text-amber-900 dark:text-amber-200 hover:bg-amber-100 dark:hover:bg-amber-900/30"
                      >
                        {sug}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Word Result Card */}
          {dictResult && dictResult.success && (
            <div className="space-y-4">
              {/* Main Card */}
              <div className="bg-white dark:bg-[#191D1B] rounded-3xl p-5 sm:p-6 border border-[#DCE5DF] dark:border-[#404944] shadow-sm space-y-5">
                {/* Header & Pronunciation */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-gray-100 dark:border-gray-800">
                  <div>
                    <div className="flex items-center gap-2.5 flex-wrap">
                      <h1 className="text-3xl font-extrabold text-[#0F6D54] dark:text-[#8AD5BB] tracking-tight font-serif">
                        {dictResult.dictionary?.syllables || dictResult.word}
                      </h1>
                      {dictResult.partOfSpeech && (
                        <span className="px-2.5 py-0.5 rounded-full text-xs font-bold uppercase tracking-wider bg-[#EBF7F2] dark:bg-[#082B21] text-[#0F6D54] dark:text-[#8AD5BB] border border-[#A6F2D6] dark:border-[#0F6D54]">
                          {dictResult.partOfSpeech}
                        </span>
                      )}
                    </div>
                    {/* Phonetic & IPA */}
                    <div className="flex items-center gap-2 text-xs text-gray-500 dark:text-gray-400 font-mono mt-1">
                      {dictResult.dictionary?.pronunciation?.written && (
                        <span>\{dictResult.dictionary.pronunciation.written}\</span>
                      )}
                      {dictResult.dictionary?.pronunciation?.ipa && (
                        <span className="text-gray-400">({dictResult.dictionary.pronunciation.ipa})</span>
                      )}
                      {dictResult.phonetic && !dictResult.dictionary?.pronunciation?.written && (
                        <span>{dictResult.phonetic}</span>
                      )}
                    </div>
                  </div>

                  {/* Actions: Audio & Save */}
                  <div className="flex items-center gap-2 self-start sm:self-center">
                    <button
                      onClick={() => playAudio(dictResult.dictionary?.audio?.[0]?.url, dictResult.word)}
                      className={`px-3 py-2 rounded-xl border flex items-center gap-1.5 text-xs font-bold transition ${
                        playingAudio
                          ? 'bg-[#A6F2D6] text-[#002117] border-transparent'
                          : 'bg-gray-50 dark:bg-gray-800 text-[#0F6D54] dark:text-[#8AD5BB] border-gray-200 dark:border-gray-700 hover:bg-gray-100'
                      }`}
                      title="Listen to American English pronunciation"
                    >
                      <Icon name="volume-2" className="w-4 h-4" />
                      <span>{playingAudio ? 'Playing...' : 'Audio'}</span>
                    </button>

                    <button
                      onClick={() => {
                        setSelectedMastery(dictResult.masteryStatus || 'learning');
                        setPersonalNoteText(dictResult.personalNotes || '');
                        setSaveModalOpen(true);
                      }}
                      className={`px-3 py-2 rounded-xl text-xs font-bold flex items-center gap-1.5 transition ${
                        dictResult.isSaved
                          ? 'bg-[#A6F2D6] dark:bg-[#00513E] text-[#002117] dark:text-[#A6F2D6]'
                          : 'bg-[#0F6D54] text-white hover:bg-[#0b5340]'
                      }`}
                    >
                      <Icon name={dictResult.isSaved ? "bookmark-check" : "bookmark"} className="w-4 h-4" />
                      <span>{dictResult.isSaved ? 'Saved in Notebook' : 'Save Word'}</span>
                    </button>
                  </div>
                </div>

                {/* Merriam-Webster Attribution Badge */}
                <div className="flex items-center justify-between text-[11px] text-gray-400 border-b border-gray-100 dark:border-gray-800 pb-2">
                  <div className="flex items-center gap-1.5">
                    <span className="font-semibold text-gray-600 dark:text-gray-300">
                      Merriam-Webster Collegiate®
                    </span>
                    <span>• Authoritative Reference</span>
                  </div>
                  {dictResult.source?.cached && (
                    <span className="px-1.5 py-0.5 rounded text-[10px] bg-gray-100 dark:bg-gray-800 text-gray-500">
                      Cached
                    </span>
                  )}
                </div>

                {/* DEFINITIONS SECTION */}
                <div className="space-y-3">
                  <div className="text-xs font-bold text-gray-400 uppercase tracking-wider flex items-center gap-1.5">
                    <Icon name="book-open" className="w-3.5 h-3.5 text-[#0F6D54]" />
                    <span>Definitions</span>
                  </div>
                  <div className="space-y-3 pl-1">
                    {(dictResult.dictionary?.definitions || [
                      { text: dictResult.shortDefinition || dictResult.fullDefinition, examples: dictResult.examples }
                    ]).map((def, idx) => (
                      <div key={idx} className="space-y-1">
                        <div className="text-sm font-medium text-[#191C1B] dark:text-[#E1E3DF] leading-relaxed flex items-start gap-2">
                          <span className="text-[#0F6D54] font-bold shrink-0">{idx + 1}.</span>
                          <div>
                            {def.partOfSpeech && (
                              <span className="text-xs italic text-gray-500 dark:text-gray-400 mr-1.5">
                                [{def.partOfSpeech}]
                              </span>
                            )}
                            <span>{def.text}</span>
                          </div>
                        </div>

                        {/* Verbal Illustrations / Examples */}
                        {def.examples && def.examples.length > 0 && (
                          <div className="pl-6 space-y-1">
                            {def.examples.map((ex, eIdx) => (
                              <div key={eIdx} className="text-xs italic text-gray-600 dark:text-gray-400 flex items-start gap-1">
                                <span className="text-gray-400 select-none">“</span>
                                <span>{ex}</span>
                                <span className="text-gray-400 select-none">”</span>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>

                {/* SYNONYMS (Merriam-Webster Collegiate Thesaurus) */}
                {((dictResult.thesaurus?.synonyms && dictResult.thesaurus.synonyms.length > 0) ||
                  (dictResult.synonyms && dictResult.synonyms.length > 0)) && (
                  <div className="space-y-2 pt-2 border-t border-gray-100 dark:border-gray-800">
                    <div className="text-xs font-bold text-gray-400 uppercase tracking-wider flex items-center gap-1.5">
                      <Icon name="check-circle" className="w-3.5 h-3.5 text-emerald-600" />
                      <span>Synonyms</span>
                    </div>
                    <div className="flex flex-wrap gap-1.5">
                      {(dictResult.thesaurus?.synonyms || dictResult.synonyms || []).map((syn, idx) => (
                        <button
                          key={idx}
                          type="button"
                          onClick={() => {
                            setDictWord(syn);
                            onLookup(null, syn);
                          }}
                          className="px-2.5 py-1 rounded-xl text-xs font-semibold bg-emerald-50 dark:bg-emerald-950/40 text-emerald-800 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800 hover:bg-emerald-100 transition flex items-center gap-1"
                          title={`Look up "${syn}"`}
                        >
                          <span>{syn}</span>
                          <span className="text-emerald-400 text-[10px]">↗</span>
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                {/* ANTONYMS */}
                {((dictResult.thesaurus?.antonyms && dictResult.thesaurus.antonyms.length > 0) ||
                  (dictResult.antonyms && dictResult.antonyms.length > 0)) && (
                  <div className="space-y-2 pt-2 border-t border-gray-100 dark:border-gray-800">
                    <div className="text-xs font-bold text-gray-400 uppercase tracking-wider flex items-center gap-1.5">
                      <Icon name="x-circle" className="w-3.5 h-3.5 text-rose-600" />
                      <span>Antonyms</span>
                    </div>
                    <div className="flex flex-wrap gap-1.5">
                      {(dictResult.thesaurus?.antonyms || dictResult.antonyms || []).map((ant, idx) => (
                        <button
                          key={idx}
                          type="button"
                          onClick={() => {
                            setDictWord(ant);
                            onLookup(null, ant);
                          }}
                          className="px-2.5 py-1 rounded-xl text-xs font-semibold bg-rose-50 dark:bg-rose-950/40 text-rose-800 dark:text-rose-300 border border-rose-200 dark:border-rose-800 hover:bg-rose-100 transition flex items-center gap-1"
                          title={`Look up "${ant}"`}
                        >
                          <span>{ant}</span>
                          <span className="text-rose-400 text-[10px]">↗</span>
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                {/* RELATED WORDS */}
                {dictResult.thesaurus?.relatedWords && dictResult.thesaurus.relatedWords.length > 0 && (
                  <div className="space-y-2 pt-2 border-t border-gray-100 dark:border-gray-800">
                    <div className="text-xs font-bold text-gray-400 uppercase tracking-wider flex items-center gap-1.5">
                      <Icon name="share-2" className="w-3.5 h-3.5 text-blue-600" />
                      <span>Related Words</span>
                    </div>
                    <div className="flex flex-wrap gap-1.5">
                      {dictResult.thesaurus.relatedWords.map((rw, idx) => (
                        <button
                          key={idx}
                          type="button"
                          onClick={() => {
                            setDictWord(rw);
                            onLookup(null, rw);
                          }}
                          className="px-2.5 py-1 rounded-xl text-xs font-semibold bg-blue-50 dark:bg-blue-950/40 text-blue-800 dark:text-blue-300 border border-blue-200 dark:border-blue-800 hover:bg-blue-100 transition"
                        >
                          {rw}
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                {/* ETYMOLOGY */}
                {dictResult.dictionary?.etymology && (
                  <div className="p-3.5 rounded-2xl bg-amber-50/60 dark:bg-amber-950/20 border border-amber-200/60 dark:border-amber-900/40 space-y-1">
                    <div className="text-[11px] font-bold text-amber-800 dark:text-amber-300 uppercase tracking-wider flex items-center gap-1">
                      <Icon name="compass" className="w-3.5 h-3.5" />
                      <span>Etymology & Origin</span>
                    </div>
                    <p className="text-xs text-amber-900 dark:text-amber-200 font-serif leading-relaxed">
                      {dictResult.dictionary.etymology}
                    </p>
                  </div>
                )}

                {/* IDIOMS & PHRASES */}
                {dictResult.dictionary?.idioms && dictResult.dictionary.idioms.length > 0 && (
                  <div className="space-y-2 pt-2 border-t border-gray-100 dark:border-gray-800">
                    <div className="text-xs font-bold text-gray-400 uppercase tracking-wider">
                      Phrases & Run-on Idioms
                    </div>
                    <div className="space-y-1.5">
                      {dictResult.dictionary.idioms.map((idm, idx) => (
                        <div key={idx} className="text-xs text-gray-700 dark:text-gray-300">
                          <span className="font-bold text-[#0F6D54]">{idm.phrase}:</span> {idm.definition}
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {/* AI LEARNING ASSISTANT (Gemini Integration) */}
              <div className="bg-gradient-to-br from-[#F4FAF7] to-white dark:from-[#09231B] dark:to-[#191D1B] rounded-3xl p-5 border border-[#A6F2D6] dark:border-[#0F6D54] space-y-3.5">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="p-1.5 rounded-xl bg-[#0F6D54] text-white">
                      <Icon name="sparkles" className="w-4 h-4" />
                    </div>
                    <div>
                      <h3 className="text-sm font-bold text-[#002117] dark:text-[#A6F2D6]">
                        Gemini AI Learning Companion
                      </h3>
                      <p className="text-[11px] text-[#404944] dark:text-[#C0C9C3]">
                        Educational aids for academic research, textbooks & reading
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={onTestGemini}
                    disabled={geminiTesting}
                    className="text-[11px] font-bold text-[#0F6D54] dark:text-[#8AD5BB] hover:underline"
                  >
                    {geminiTesting ? 'Testing...' : 'Test AI Key'}
                  </button>
                </div>

                {/* AI Feature Selector Pills */}
                <div className="flex flex-wrap gap-1.5">
                  {[
                    { id: 'simple_explanation', label: 'Simple English', icon: 'smile' },
                    { id: 'urdu_explanation', label: 'Urdu Context (اردو)', icon: 'globe' },
                    { id: 'academic_context', label: 'Academic Usage', icon: 'file-text' },
                    { id: 'collocations', label: 'Collocations', icon: 'link' },
                    { id: 'common_mistakes', label: 'Common Mistakes', icon: 'alert-circle' },
                    { id: 'memory_tricks', label: 'Memory Trick', icon: 'zap' },
                    { id: 'quiz', label: 'Quiz Me', icon: 'help-circle' }
                  ].map(feat => (
                    <button
                      key={feat.id}
                      type="button"
                      disabled={dictAiAssistantLoading}
                      onClick={() => onCallAiAssistant(feat.id)}
                      className={`px-3 py-1.5 rounded-xl text-xs font-semibold border transition flex items-center gap-1.5 ${
                        dictAiActiveFeature === feat.id
                          ? 'bg-[#0F6D54] text-white border-[#0F6D54] shadow-sm'
                          : 'bg-white dark:bg-[#191D1B] text-gray-700 dark:text-gray-200 border-gray-200 dark:border-gray-700 hover:border-[#0F6D54]'
                      }`}
                    >
                      <Icon name={feat.icon} className="w-3.5 h-3.5" />
                      <span>{feat.label}</span>
                    </button>
                  ))}
                </div>

                {/* AI Result Presentation */}
                {dictAiAssistantLoading && (
                  <div className="p-4 rounded-2xl bg-white dark:bg-[#191D1B] border border-gray-200 dark:border-gray-800 text-center space-y-2">
                    <span className="inline-block animate-spin text-lg text-[#0F6D54]">⟳</span>
                    <p className="text-xs text-gray-500 font-medium">
                      Gemini is generating educational insights for "{dictResult.word}"...
                    </p>
                  </div>
                )}

                {dictAiAssistantData && !dictAiAssistantLoading && (
                  <div className="p-4 rounded-2xl bg-white dark:bg-[#191D1B] border border-[#A6F2D6] dark:border-[#0F6D54] space-y-2 text-xs">
                    <div className="flex items-center justify-between text-[11px] font-bold text-[#0F6D54] dark:text-[#8AD5BB] uppercase">
                      <span>{dictAiAssistantData.feature.replace('_', ' ')}:</span>
                      {dictAiAssistantData.isFallback && (
                        <span className="text-[10px] text-gray-400 lowercase">(curated guide)</span>
                      )}
                    </div>
                    <div className="text-gray-800 dark:text-gray-200 whitespace-pre-line leading-relaxed font-sans">
                      {dictAiAssistantData.content}
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      ) : (
        /* VOCABULARY NOTEBOOK TAB */
        <div className="space-y-4">
          <div className="flex flex-col sm:flex-row gap-2.5 items-stretch sm:items-center justify-between">
            {/* Search saved words */}
            <div className="relative flex-1">
              <input
                type="text"
                placeholder="Search saved vocabulary or notes..."
                value={notebookSearch}
                onChange={e => setNotebookSearch(e.target.value)}
                className="w-full pl-9 pr-4 py-2.5 rounded-2xl border border-[#DCE5DF] dark:border-[#404944] bg-white dark:bg-[#191D1B] text-xs font-semibold"
              />
              <Icon name="search" className="w-3.5 h-3.5 text-gray-400 absolute left-3 top-3.5" />
            </div>

            {/* Filter pills */}
            <div className="flex gap-1 overflow-x-auto no-scrollbar">
              {['all', 'learning', 'reviewing', 'mastered'].map(st => (
                <button
                  key={st}
                  onClick={() => setNotebookFilter(st)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold capitalize transition shrink-0 ${
                    notebookFilter === st
                      ? 'bg-[#0F6D54] text-white shadow-sm'
                      : 'bg-white dark:bg-[#191D1B] text-gray-500 border border-gray-200 dark:border-gray-800'
                  }`}
                >
                  {st}
                </button>
              ))}
            </div>
          </div>

          {/* Words List */}
          {dictSavedWordsLoading ? (
            <div className="p-8 text-center text-xs text-gray-400">Loading your vocabulary notebook...</div>
          ) : filteredSavedWords.length === 0 ? (
            <div className="p-8 text-center bg-white dark:bg-[#191D1B] rounded-3xl border border-dashed border-gray-300 dark:border-gray-700 space-y-2">
              <Icon name="bookmark" className="w-8 h-8 text-gray-300 mx-auto" />
              <div className="text-sm font-bold text-gray-600 dark:text-gray-300">No words found</div>
              <p className="text-xs text-gray-400 max-w-xs mx-auto">
                Look up words in the search tab and click "Save Word" to curate your vocabulary notebook.
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {filteredSavedWords.map(w => (
                <div
                  key={w._id}
                  className="p-4 rounded-3xl bg-white dark:bg-[#191D1B] border border-[#DCE5DF] dark:border-[#404944] shadow-sm space-y-3"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => {
                          setDictActiveTab('search');
                          setDictWord(w.word);
                          onLookup(null, w.word);
                        }}
                        className="text-lg font-bold text-[#0F6D54] dark:text-[#8AD5BB] font-serif hover:underline text-left"
                      >
                        {w.word}
                      </button>
                      <span className="text-xs text-gray-400">{w.phonetic}</span>
                      {w.partOfSpeech && (
                        <span className="text-[10px] uppercase font-bold text-gray-500 px-2 py-0.5 rounded-full bg-gray-100 dark:bg-gray-800">
                          {w.partOfSpeech}
                        </span>
                      )}
                    </div>

                    <div className="flex items-center gap-2">
                      {/* Mastery selector */}
                      <select
                        value={w.masteryStatus || 'learning'}
                        onChange={e => onUpdateMastery(w._id, e.target.value, w.personalNotes)}
                        className="px-2 py-1 rounded-xl text-[11px] font-bold border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-[#0F6D54] dark:text-[#8AD5BB]"
                      >
                        <option value="learning">Learning</option>
                        <option value="reviewing">Reviewing</option>
                        <option value="mastered">Mastered</option>
                      </select>

                      <button
                        onClick={() => onDeleteWord(w._id)}
                        className="p-1.5 rounded-lg text-gray-400 hover:text-red-600 transition"
                        title="Remove word"
                      >
                        <Icon name="trash-2" className="w-4 h-4" />
                      </button>
                    </div>
                  </div>

                  <p className="text-xs text-gray-700 dark:text-gray-300 line-clamp-2">
                    {w.shortDefinition || w.fullDefinition}
                  </p>

                  {/* Personal Note */}
                  {w.personalNotes && (
                    <div className="p-2.5 rounded-xl bg-gray-50 dark:bg-gray-800/60 border border-gray-200 dark:border-gray-700 text-xs text-gray-600 dark:text-gray-300">
                      <span className="font-bold text-[#0F6D54]">My Note: </span>
                      <span>{w.personalNotes}</span>
                    </div>
                  )}

                  {/* Synonyms preview */}
                  {w.synonyms && w.synonyms.length > 0 && (
                    <div className="flex flex-wrap gap-1">
                      {w.synonyms.slice(0, 4).map((s, sIdx) => (
                        <span key={sIdx} className="px-2 py-0.5 rounded-lg text-[10px] bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-300">
                          {s}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Save Word Modal */}
      {saveModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
          <div className="bg-white dark:bg-[#191D1B] rounded-3xl p-6 border border-[#DCE5DF] dark:border-[#404944] max-w-sm w-full space-y-4 shadow-xl">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-bold text-[#191C1B] dark:text-[#E1E3DF]">
                Save "{dictResult?.word}"
              </h3>
              <button onClick={() => setSaveModalOpen(false)} className="p-1 text-gray-400 hover:text-gray-600">
                <Icon name="x" className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-gray-500 uppercase">Mastery Stage</label>
              <div className="grid grid-cols-3 gap-2">
                {['learning', 'reviewing', 'mastered'].map(st => (
                  <button
                    key={st}
                    type="button"
                    onClick={() => setSelectedMastery(st)}
                    className={`py-2 rounded-xl text-xs font-bold capitalize transition border ${
                      selectedMastery === st
                        ? 'bg-[#0F6D54] text-white border-[#0F6D54]'
                        : 'bg-gray-50 dark:bg-gray-800 border-gray-200 dark:border-gray-700 text-gray-600'
                    }`}
                  >
                    {st}
                  </button>
                ))}
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-gray-500 uppercase">Personal Note (Optional)</label>
              <textarea
                rows={3}
                placeholder="Where did you read this? Personal memory trigger or context sentence..."
                value={personalNoteText}
                onChange={e => setPersonalNoteText(e.target.value)}
                className="w-full p-3 rounded-2xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-xs focus:outline-none focus:ring-1 focus:ring-[#0F6D54]"
              />
            </div>

            <div className="flex gap-2 pt-1">
              <button
                type="button"
                onClick={() => setSaveModalOpen(false)}
                className="flex-1 py-2.5 rounded-xl border border-gray-200 dark:border-gray-700 text-xs font-bold text-gray-600"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  onSaveWord(selectedMastery, personalNoteText);
                  setSaveModalOpen(false);
                }}
                className="flex-1 py-2.5 rounded-xl bg-[#0F6D54] hover:bg-[#0b5340] text-white text-xs font-bold shadow transition"
              >
                Confirm Save
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// -------------------------------------------------------------
// Subscreen: Notes & Documents (matching NotesScreen.kt)
// -------------------------------------------------------------
function NotesSubscreen({ notes, notesSearch, setNotesSearch, notesCategory, setNotesCategory, onPin, onDelete, onOpenAdd, onBack }) {
  const filtered = useMemo(() => {
    return notes.filter(n => {
      if (notesCategory !== 'all' && n.category !== notesCategory) return false;
      if (notesSearch.trim()) {
        const q = notesSearch.toLowerCase();
        return (n.title || '').toLowerCase().includes(q) || (n.content || '').toLowerCase().includes(q);
      }
      return true;
    });
  }, [notes, notesSearch, notesCategory]);

  return (
    <div className="p-5 space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <button onClick={onBack} className="p-1.5 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800">
            <Icon name="arrow-left" className="w-5 h-5 text-[#0F6D54]" />
          </button>
          <div>
            <h2 className="text-xl font-bold text-[#191C1B] dark:text-[#E1E3DF] tracking-tight">Notes & Docs</h2>
            <p className="text-xs text-[#404944] dark:text-[#C0C9C3]">Personal scratchpad & checklists</p>
          </div>
        </div>

        <button
          onClick={onOpenAdd}
          className="px-3 py-1.5 rounded-xl bg-[#0F6D54] text-white font-bold text-xs flex items-center gap-1.5 shadow-sm"
        >
          <Icon name="plus" className="w-3.5 h-3.5" />
          <span>New</span>
        </button>
      </div>

      {/* Search Input */}
      <div className="relative">
        <input
          type="text"
          placeholder="Search your notes..."
          value={notesSearch}
          onChange={e => setNotesSearch(e.target.value)}
          className="w-full pl-9 pr-4 py-2.5 rounded-2xl border border-[#DCE5DF] dark:border-[#404944] bg-white dark:bg-[#191D1B] text-xs"
        />
        <Icon name="search" className="w-4 h-4 text-gray-400 absolute left-3 top-3" />
      </div>

      {/* Notes List */}
      <div className="space-y-2.5">
        {filtered.length === 0 ? (
          <div className="bg-white dark:bg-[#191D1B] rounded-2xl p-6 text-center border border-[#DCE5DF] text-xs text-gray-400">
            No notes found. Tap 'New' above to write a note!
          </div>
        ) : (
          filtered.map(note => (
            <div
              key={note._id}
              className={`p-4 rounded-2xl border shadow-sm space-y-2 transition ${
                note.isPinned
                  ? 'bg-amber-50/50 dark:bg-amber-950/20 border-amber-200 dark:border-amber-900/40'
                  : 'bg-white dark:bg-[#191D1B] border-[#DCE5DF] dark:border-[#404944]'
              }`}
            >
              <div className="flex items-start justify-between">
                <div>
                  <span className="text-[10px] font-bold uppercase tracking-wider text-[#0F6D54] dark:text-[#8AD5BB]">
                    {note.category || 'General'}
                  </span>
                  <div className="text-sm font-bold text-[#191C1B] dark:text-[#E1E3DF]">{note.title}</div>
                </div>

                <div className="flex items-center gap-1.5">
                  <button
                    onClick={() => onPin(note._id)}
                    className={`p-1.5 rounded-lg ${note.isPinned ? 'text-amber-600' : 'text-gray-300 hover:text-amber-500'}`}
                    title={note.isPinned ? "Unpin note" : "Pin note to top"}
                  >
                    <Icon name="pin" className="w-3.5 h-3.5" />
                  </button>
                  <button
                    onClick={() => onDelete(note._id)}
                    className="p-1.5 rounded-lg text-gray-300 hover:text-red-500"
                    title="Delete Note"
                  >
                    <Icon name="trash-2" className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>

              <p className="text-xs text-gray-600 dark:text-gray-300 whitespace-pre-wrap leading-relaxed">
                {note.content}
              </p>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

// --- Generic Modal Shell ---
function Modal({ title, onClose, children }) {
  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4">
      <div className="w-full max-w-sm bg-white dark:bg-[#191D1B] rounded-t-3xl sm:rounded-3xl p-5 shadow-2xl border border-gray-200 dark:border-gray-800 animate-slide-up">
        <div className="flex items-center justify-between pb-3 border-b border-gray-100 dark:border-gray-800 mb-4">
          <h3 className="text-sm font-bold text-[#191C1B] dark:text-[#E1E3DF]">{title}</h3>
          <button onClick={onClose} className="p-1.5 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-400">
            <Icon name="x" className="w-4 h-4" />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

// --- Auth Component (Login / Register) ---
function AuthForm({ onLogin, onRegister, onDemo }) {
  const [isRegister, setIsRegister] = useState(false);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (isRegister) {
      onRegister(name, email, password);
    } else {
      onLogin(email, password);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-3.5 text-left">
      {isRegister && (
        <div>
          <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">Full Name</label>
          <div className="relative">
            <input
              type="text"
              required
              placeholder="e.g. Alex Johnson"
              value={name}
              onChange={e => setName(e.target.value)}
              className="w-full pl-9 pr-3 py-2.5 rounded-xl border border-gray-300 dark:border-gray-700 bg-white dark:bg-[#101413] text-xs font-medium text-[#191C1B] dark:text-[#E1E3DF]"
            />
            <Icon name="user" className="w-4 h-4 text-gray-400 absolute left-3 top-3" />
          </div>
        </div>
      )}

      <div>
        <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">Email Address</label>
        <div className="relative">
          <input
            type="email"
            required
            placeholder="user@example.com"
            value={email}
            onChange={e => setEmail(e.target.value)}
            className="w-full pl-9 pr-3 py-2.5 rounded-xl border border-gray-300 dark:border-gray-700 bg-white dark:bg-[#101413] text-xs font-medium text-[#191C1B] dark:text-[#E1E3DF]"
          />
          <Icon name="mail" className="w-4 h-4 text-gray-400 absolute left-3 top-3" />
        </div>
      </div>

      <div>
        <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">Password</label>
        <div className="relative">
          <input
            type={showPassword ? "text" : "password"}
            required
            placeholder="••••••••"
            value={password}
            onChange={e => setPassword(e.target.value)}
            className="w-full pl-9 pr-10 py-2.5 rounded-xl border border-gray-300 dark:border-gray-700 bg-white dark:bg-[#101413] text-xs font-medium text-[#191C1B] dark:text-[#E1E3DF]"
          />
          <Icon name="lock" className="w-4 h-4 text-gray-400 absolute left-3 top-3" />
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            className="absolute right-3 top-3 text-gray-400 hover:text-gray-600"
          >
            <Icon name={showPassword ? "eye-off" : "eye"} className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      <button
        type="submit"
        className="w-full py-3 rounded-xl bg-[#0F6D54] hover:bg-[#074836] text-white font-bold text-xs shadow-md active:scale-95 transition"
      >
        {isRegister ? 'Create Account' : 'Sign In'}
      </button>

      {/* Demo Account Button */}
      <button
        type="button"
        onClick={onDemo}
        className="w-full py-2.5 rounded-xl bg-[#A6F2D6] dark:bg-[#00513E] text-[#002117] dark:text-[#A6F2D6] font-bold text-xs hover:opacity-90 transition"
      >
        ⚡ Instant Demo Access
      </button>

      <div className="text-center pt-2">
        <button
          type="button"
          onClick={() => setIsRegister(!isRegister)}
          className="text-xs text-[#0F6D54] dark:text-[#8AD5BB] font-semibold hover:underline"
        >
          {isRegister ? 'Already have an account? Sign In' : "Don't have an account? Register"}
        </button>
      </div>
    </form>
  );
}

// Mount the React Application
const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />);
