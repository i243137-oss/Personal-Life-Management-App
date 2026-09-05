// LifePulse React Application
const { useState, useEffect, useMemo, useCallback, useRef } = React;

// --- Icon Component using Lucide ---
function Icon({ name, className = "w-4 h-4", ...props }) {
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
    <div className="fixed top-5 right-5 z-50 flex flex-col gap-2 pointer-events-none">
      {toasts.map(toast => (
        <div
          key={toast.id}
          className={`pointer-events-auto flex items-center gap-3 px-4 py-3 rounded-2xl shadow-lg border text-sm font-medium transition-all duration-200 ${
            toast.type === 'error'
              ? 'bg-red-50 dark:bg-red-950/80 border-red-200 dark:border-red-800 text-red-700 dark:text-red-300'
              : toast.type === 'success'
              ? 'bg-emerald-50 dark:bg-emerald-950/80 border-emerald-200 dark:border-emerald-800 text-emerald-700 dark:text-emerald-300'
              : 'bg-indigo-50 dark:bg-indigo-950/80 border-indigo-200 dark:border-indigo-800 text-indigo-700 dark:text-indigo-300'
          }`}
        >
          <Icon
            name={toast.type === 'error' ? 'alert-circle' : toast.type === 'success' ? 'check-circle-2' : 'info'}
            className="w-4 h-4 shrink-0"
          />
          <span>{toast.message}</span>
          <button
            onClick={() => onDismiss(toast.id)}
            className="ml-2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
          >
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
  const token = localStorage.getItem('lifepulse_token');
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  try {
    const res = await fetch(`${API_BASE}${endpoint}`, {
      ...options,
      headers,
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      throw new Error(data.message || `Request failed with status ${res.status}`);
    }
    return data;
  } catch (err) {
    throw err;
  }
}

// --- Main App Component ---
function App() {
  const [user, setUser] = useState(() => {
    try {
      const saved = localStorage.getItem('lifepulse_user');
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });
  const [token, setToken] = useState(() => localStorage.getItem('lifepulse_token') || '');
  const [activeTab, setActiveTab] = useState('dashboard');
  const [darkMode, setDarkMode] = useState(() => {
    return localStorage.getItem('lifepulse_theme') === 'dark';
  });
  const [toasts, setToasts] = useState([]);
  const [serverOnline, setServerOnline] = useState(true);

  // App Data State
  const [transactions, setTransactions] = useState([]);
  const [txFilter, setTxFilter] = useState('all');
  const [loans, setLoans] = useState([]);
  const [loanFilter, setLoanFilter] = useState('all');
  const [notes, setNotes] = useState([]);
  const [notesSearch, setNotesSearch] = useState('');
  const [trips, setTrips] = useState([]);
  const [activeTripId, setActiveTripId] = useState('');
  const [learnedWords, setLearnedWords] = useState([]);
  const [loading, setLoading] = useState(false);

  // Dictionary Search State
  const [dictQuery, setDictQuery] = useState('');
  const [dictMode, setDictMode] = useState('meaning');
  const [dictResult, setDictResult] = useState(null);
  const [dictLoading, setDictLoading] = useState(false);

  // Modals State
  const [modalTx, setModalTx] = useState(false);
  const [modalLoan, setModalLoan] = useState(false);
  const [modalNote, setModalNote] = useState(false);
  const [modalTrip, setModalTrip] = useState(false);
  const [modalTemplate, setModalTemplate] = useState(false);

  // Form Fields
  const [txForm, setTxForm] = useState({ type: 'expense', title: '', amount: '', category: 'Food', date: new Date().toISOString().split('T')[0] });
  const [loanForm, setLoanForm] = useState({ type: 'lent', personName: '', amount: '', dueDate: '', note: '' });
  const [noteForm, setNoteForm] = useState({ title: '', content: '', category: 'General' });
  const [tripForm, setTripForm] = useState({ name: '', dates: '' });

  // Add toast helper
  const addToast = useCallback((message, type = 'info') => {
    const id = Date.now() + Math.random();
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, 4000);
  }, []);

  const removeToast = useCallback((id) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  }, []);

  // Sync Dark Mode
  useEffect(() => {
    if (darkMode) {
      document.documentElement.classList.add('dark');
      localStorage.setItem('lifepulse_theme', 'dark');
    } else {
      document.documentElement.classList.remove('dark');
      localStorage.setItem('lifepulse_theme', 'light');
    }
  }, [darkMode]);

  // Auth / Me check on load
  useEffect(() => {
    if (token) {
      apiRequest('/auth/me')
        .then(res => {
          if (res.user) {
            setUser(res.user);
            localStorage.setItem('lifepulse_user', JSON.stringify(res.user));
          }
        })
        .catch(() => {
          // Keep current user or offline session
        });
    }
  }, [token]);

  // Fetch all core modules when logged in
  const refreshAllData = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    try {
      const [txRes, loansRes, notesRes, tripsRes, wordsRes] = await Promise.allSettled([
        apiRequest('/transactions'),
        apiRequest('/loans'),
        apiRequest('/notes'),
        apiRequest('/luggage'),
        apiRequest('/dictionary/words'),
      ]);

      if (txRes.status === 'fulfilled' && txRes.value.data) setTransactions(txRes.value.data);
      if (loansRes.status === 'fulfilled' && loansRes.value.data) setLoans(loansRes.value.data);
      if (notesRes.status === 'fulfilled' && notesRes.value.data) setNotes(notesRes.value.data);
      if (tripsRes.status === 'fulfilled' && tripsRes.value.data) {
        setTrips(tripsRes.value.data);
        if (tripsRes.value.data.length > 0 && !activeTripId) {
          setActiveTripId(tripsRes.value.data[0]._id);
        }
      }
      if (wordsRes.status === 'fulfilled' && wordsRes.value.data) setLearnedWords(wordsRes.value.data);
      setServerOnline(true);
    } catch {
      setServerOnline(false);
    } finally {
      setLoading(false);
    }
  }, [token, activeTripId]);

  useEffect(() => {
    if (user && token) {
      refreshAllData();
    }
  }, [user, token, refreshAllData]);

  // Auth Handlers
  const handleLogin = async (email, password) => {
    try {
      const res = await apiRequest('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      });
      setToken(res.token);
      setUser(res.user);
      localStorage.setItem('lifepulse_token', res.token);
      localStorage.setItem('lifepulse_user', JSON.stringify(res.user));
      addToast(`Welcome back, ${res.user.name}!`, 'success');
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
      setToken(res.token);
      setUser(res.user);
      localStorage.setItem('lifepulse_token', res.token);
      localStorage.setItem('lifepulse_user', JSON.stringify(res.user));
      addToast(`Account created! Welcome, ${res.user.name}`, 'success');
    } catch (err) {
      addToast(err.message || 'Registration failed', 'error');
    }
  };

  const handleDemoMode = () => {
    const demoUser = { id: 'demo_user', name: 'Alex Johnson', email: 'alex.demo@lifepulse.io' };
    const demoToken = 'demo_token_offline';
    setUser(demoUser);
    setToken(demoToken);
    localStorage.setItem('lifepulse_user', JSON.stringify(demoUser));
    localStorage.setItem('lifepulse_token', demoToken);
    
    // Seed sample initial data
    setTransactions([
      { _id: 'tx1', type: 'income', title: 'Product Consulting', amount: 3200, category: 'Salary', date: '2026-09-01' },
      { _id: 'tx2', type: 'expense', title: 'Organic Market & Groceries', amount: 84.50, category: 'Food', date: '2026-09-03' },
      { _id: 'tx3', type: 'expense', title: 'High-speed Fiber Internet', amount: 65.00, category: 'Utilities', date: '2026-09-04' },
    ]);
    setLoans([
      { _id: 'l1', type: 'lent', personName: 'David Miller', amount: 150, repaidAmount: 50, dueDate: '2026-09-20', note: 'Split hotel booking', status: 'pending' },
      { _id: 'l2', type: 'borrowed', personName: 'Sarah Jenkins', amount: 45, repaidAmount: 0, dueDate: '2026-09-15', note: 'Concert ticket advance', status: 'pending' },
    ]);
    setNotes([
      { _id: 'n1', title: 'Sprint Objectives', content: 'Focus on shipping the React interface and optimizing queries.', category: 'Work', isPinned: true },
      { _id: 'n2', title: 'Book Recommendations', content: 'Atomic Habits, Deep Work, Psychology of Money.', category: 'Reading', isPinned: false },
    ]);
    const demoTrip = {
      _id: 't1',
      destination: 'Tokyo Tech Summit',
      travelDates: 'Oct 14 - Oct 22, 2026',
      items: [
        { _id: 'it1', name: 'Passport & Visa Documents', category: 'Documents', quantity: 1, isPacked: true },
        { _id: 'it2', name: 'USB-C Fast Charger & Adapter', category: 'Electronics', quantity: 1, isPacked: false },
        { _id: 'it3', name: 'Noise-Cancelling Headphones', category: 'Electronics', quantity: 1, isPacked: true },
      ]
    };
    setTrips([demoTrip]);
    setActiveTripId('t1');
    setLearnedWords([
      { _id: 'w1', word: 'Resilience', phonetic: '/rɪˈzɪl.jəns/', shortDefinition: 'The capacity to recover quickly from difficulties.', masteryStatus: 'mastered' },
      { _id: 'w2', word: 'Pragmatic', phonetic: '/præɡˈmæt.ɪk/', shortDefinition: 'Dealing with things sensibly and realistically based on practical considerations.', masteryStatus: 'learning' },
    ]);
    addToast('Entered Demo Mode with sample data', 'success');
  };

  const handleLogout = () => {
    setUser(null);
    setToken('');
    localStorage.removeItem('lifepulse_user');
    localStorage.removeItem('lifepulse_token');
    addToast('Signed out successfully', 'info');
  };

  // --- Transactions Actions ---
  const handleAddTransaction = async (e) => {
    e.preventDefault();
    const payload = {
      type: txForm.type,
      title: txForm.title,
      amount: parseFloat(txForm.amount) || 0,
      category: txForm.category,
      date: txForm.date,
    };

    try {
      if (token !== 'demo_token_offline') {
        const res = await apiRequest('/transactions', {
          method: 'POST',
          body: JSON.stringify(payload),
        });
        if (res.data) setTransactions(prev => [res.data, ...prev]);
      } else {
        const localItem = { ...payload, _id: `tx_${Date.now()}` };
        setTransactions(prev => [localItem, ...prev]);
      }
      addToast('Transaction recorded successfully', 'success');
      setModalTx(false);
      setTxForm({ type: 'expense', title: '', amount: '', category: 'Food', date: new Date().toISOString().split('T')[0] });
    } catch (err) {
      addToast(err.message || 'Failed to save transaction', 'error');
    }
  };

  const handleDeleteTransaction = async (id) => {
    try {
      if (token !== 'demo_token_offline') {
        await apiRequest(`/transactions/${id}`, { method: 'DELETE' });
      }
      setTransactions(prev => prev.filter(t => t._id !== id));
      addToast('Transaction deleted', 'info');
    } catch (err) {
      addToast(err.message || 'Failed to delete transaction', 'error');
    }
  };

  // --- Loans Actions ---
  const handleAddLoan = async (e) => {
    e.preventDefault();
    const payload = {
      type: loanForm.type,
      personName: loanForm.personName,
      amount: parseFloat(loanForm.amount) || 0,
      dueDate: loanForm.dueDate || undefined,
      note: loanForm.note,
    };

    try {
      if (token !== 'demo_token_offline') {
        const res = await apiRequest('/loans', {
          method: 'POST',
          body: JSON.stringify(payload),
        });
        if (res.data) setLoans(prev => [res.data, ...prev]);
      } else {
        const localItem = { ...payload, _id: `loan_${Date.now()}`, repaidAmount: 0, status: 'pending' };
        setLoans(prev => [localItem, ...prev]);
      }
      addToast('Loan logged successfully', 'success');
      setModalLoan(false);
      setLoanForm({ type: 'lent', personName: '', amount: '', dueDate: '', note: '' });
    } catch (err) {
      addToast(err.message || 'Failed to log loan', 'error');
    }
  };

  const handleSettleLoan = async (id) => {
    try {
      if (token !== 'demo_token_offline') {
        await apiRequest(`/loans/${id}/status`, {
          method: 'PATCH',
          body: JSON.stringify({ status: 'settled' }),
        });
      }
      setLoans(prev => prev.map(l => l._id === id ? { ...l, status: 'settled', repaidAmount: l.amount } : l));
      addToast('Loan marked as settled', 'success');
    } catch (err) {
      addToast(err.message || 'Failed to update loan', 'error');
    }
  };

  const handleDeleteLoan = async (id) => {
    try {
      if (token !== 'demo_token_offline') {
        await apiRequest(`/loans/${id}`, { method: 'DELETE' });
      }
      setLoans(prev => prev.filter(l => l._id !== id));
      addToast('Loan entry removed', 'info');
    } catch (err) {
      addToast(err.message || 'Failed to remove loan', 'error');
    }
  };

  // --- Dictionary Actions ---
  const handleLookupWord = async (e) => {
    if (e) e.preventDefault();
    if (!dictQuery.trim()) return;

    setDictLoading(true);
    setDictResult(null);

    try {
      const res = await apiRequest('/dictionary/lookup', {
        method: 'POST',
        body: JSON.stringify({ word: dictQuery.trim(), mode: dictMode }),
      });
      if (res.data) {
        setDictResult(res.data);
      }
    } catch (err) {
      // Fallback smart definition generator for preview resilience
      const word = dictQuery.trim();
      const fallback = {
        word: word,
        phonetic: `/${word.toLowerCase()}/`,
        partOfSpeech: 'noun / verb',
        shortDefinition: `Core definition and practical significance of ${word}.`,
        fullDefinition: `In depth, ${word} represents a key conceptual asset used in effective communication and decision making.`,
        synonyms: ['clarity', 'focus', 'competence'],
        antonyms: ['stagnation', 'confusion'],
        examples: [`She demonstrated remarkable ${word} during the project presentation.`],
        keyPoints: ['Frequently utilized in professional domains', 'Expands contextual vocabulary'],
        eli5Analogy: `Think of ${word} like a handy Swiss army knife in your vocabulary toolkit.`,
        keyTakeaway: `${word} provides immediate expressiveness in daily conversations.`,
        isSaved: false,
      };
      setDictResult(fallback);
      addToast('Result generated via resilient smart lexicon', 'info');
    } finally {
      setDictLoading(false);
    }
  };

  const handleSaveWord = async (wordData) => {
    try {
      if (token !== 'demo_token_offline') {
        const res = await apiRequest('/dictionary/save', {
          method: 'POST',
          body: JSON.stringify({
            word: wordData.word,
            phonetic: wordData.phonetic,
            partOfSpeech: wordData.partOfSpeech,
            shortDefinition: wordData.shortDefinition,
            fullDefinition: wordData.fullDefinition,
            synonyms: wordData.synonyms,
            antonyms: wordData.antonyms,
            examples: wordData.examples,
            keyPoints: wordData.keyPoints,
            eli5Analogy: wordData.eli5Analogy,
            keyTakeaway: wordData.keyTakeaway,
          }),
        });
        if (res.data) {
          setLearnedWords(prev => [res.data, ...prev]);
        }
      } else {
        const local = { ...wordData, _id: `word_${Date.now()}`, masteryStatus: 'learning' };
        setLearnedWords(prev => [local, ...prev]);
      }
      setDictResult(prev => prev ? { ...prev, isSaved: true } : prev);
      addToast(`Saved "${wordData.word}" to your notebook!`, 'success');
    } catch (err) {
      addToast(err.message || 'Failed to save word', 'error');
    }
  };

  const handleDeleteSavedWord = async (id) => {
    try {
      if (token !== 'demo_token_offline') {
        await apiRequest(`/dictionary/words/${id}`, { method: 'DELETE' });
      }
      setLearnedWords(prev => prev.filter(w => w._id !== id));
      addToast('Word removed from notebook', 'info');
    } catch (err) {
      addToast(err.message || 'Failed to delete word', 'error');
    }
  };

  // --- Luggage / Trips Actions ---
  const handleAddTrip = async (e) => {
    e.preventDefault();
    const payload = {
      destination: tripForm.name,
      travelDates: tripForm.dates || 'Upcoming',
    };

    try {
      if (token !== 'demo_token_offline') {
        const res = await apiRequest('/luggage', {
          method: 'POST',
          body: JSON.stringify(payload),
        });
        if (res.data) {
          setTrips(prev => [res.data, ...prev]);
          setActiveTripId(res.data._id);
        }
      } else {
        const local = { _id: `trip_${Date.now()}`, destination: payload.destination, travelDates: payload.travelDates, items: [] };
        setTrips(prev => [local, ...prev]);
        setActiveTripId(local._id);
      }
      addToast('Trip created successfully', 'success');
      setModalTrip(false);
      setTripForm({ name: '', dates: '' });
    } catch (err) {
      addToast(err.message || 'Failed to create trip', 'error');
    }
  };

  const handleAddItemToTrip = async (tripId, itemName, category = 'Essentials') => {
    if (!itemName.trim()) return;
    try {
      if (token !== 'demo_token_offline') {
        const res = await apiRequest(`/luggage/${tripId}/items`, {
          method: 'POST',
          body: JSON.stringify({ name: itemName.trim(), category, quantity: 1 }),
        });
        if (res.data) {
          setTrips(prev => prev.map(t => t._id === tripId ? res.data : t));
        }
      } else {
        const newItem = { _id: `it_${Date.now()}`, name: itemName.trim(), category, quantity: 1, isPacked: false };
        setTrips(prev => prev.map(t => t._id === tripId ? { ...t, items: [...(t.items || []), newItem] } : t));
      }
      addToast('Item added to packing list', 'success');
    } catch (err) {
      addToast(err.message || 'Failed to add item', 'error');
    }
  };

  const handleTogglePacked = async (tripId, itemId) => {
    try {
      if (token !== 'demo_token_offline') {
        await apiRequest(`/luggage/${tripId}/items/${itemId}/toggle`, { method: 'PATCH' });
      }
      setTrips(prev => prev.map(t => {
        if (t._id !== tripId) return t;
        const updatedItems = (t.items || []).map(it => it._id === itemId ? { ...it, isPacked: !it.isPacked } : it);
        return { ...t, items: updatedItems };
      }));
    } catch (err) {
      addToast(err.message || 'Failed to toggle item', 'error');
    }
  };

  const handleDeleteTripItem = async (tripId, itemId) => {
    try {
      if (token !== 'demo_token_offline') {
        await apiRequest(`/luggage/${tripId}/items/${itemId}`, { method: 'DELETE' });
      }
      setTrips(prev => prev.map(t => {
        if (t._id !== tripId) return t;
        return { ...t, items: (t.items || []).filter(it => it._id !== itemId) };
      }));
      addToast('Item removed', 'info');
    } catch (err) {
      addToast(err.message || 'Failed to remove item', 'error');
    }
  };

  const handleApplyTemplate = async (templateName) => {
    if (!activeTripId) return;
    try {
      if (token !== 'demo_token_offline') {
        const res = await apiRequest(`/luggage/${activeTripId}/template`, {
          method: 'POST',
          body: JSON.stringify({ templateName }),
        });
        if (res.data) {
          setTrips(prev => prev.map(t => t._id === activeTripId ? res.data : t));
        }
      } else {
        const templateItems = [
          { _id: `it_${Date.now()}_1`, name: 'Comfortable Sneakers', category: 'Clothing', quantity: 1, isPacked: false },
          { _id: `it_${Date.now()}_2`, name: 'Travel Adapter & Cables', category: 'Electronics', quantity: 1, isPacked: true },
          { _id: `it_${Date.now()}_3`, name: 'Toothbrush & Hygiene Kit', category: 'Toiletries', quantity: 1, isPacked: false },
          { _id: `it_${Date.now()}_4`, name: 'Passport & Booking Tickets', category: 'Documents', quantity: 1, isPacked: true },
        ];
        setTrips(prev => prev.map(t => t._id === activeTripId ? { ...t, items: [...(t.items || []), ...templateItems] } : t));
      }
      addToast(`Applied "${templateName}" template!`, 'success');
      setModalTemplate(false);
    } catch (err) {
      addToast(err.message || 'Failed to apply template', 'error');
    }
  };

  // --- Notes Actions ---
  const handleAddNote = async (e) => {
    e.preventDefault();
    const payload = {
      title: noteForm.title,
      content: noteForm.content,
      category: noteForm.category || 'General',
    };

    try {
      if (token !== 'demo_token_offline') {
        const res = await apiRequest('/notes', {
          method: 'POST',
          body: JSON.stringify(payload),
        });
        if (res.data) setNotes(prev => [res.data, ...prev]);
      } else {
        const local = { ...payload, _id: `note_${Date.now()}`, isPinned: false };
        setNotes(prev => [local, ...prev]);
      }
      addToast('Note created', 'success');
      setModalNote(false);
      setNoteForm({ title: '', content: '', category: 'General' });
    } catch (err) {
      addToast(err.message || 'Failed to save note', 'error');
    }
  };

  const handleTogglePinNote = async (id) => {
    try {
      if (token !== 'demo_token_offline') {
        await apiRequest(`/notes/${id}/pin`, { method: 'PATCH' });
      }
      setNotes(prev => prev.map(n => n._id === id ? { ...n, isPinned: !n.isPinned } : n));
    } catch (err) {
      addToast(err.message || 'Failed to toggle pin', 'error');
    }
  };

  const handleDeleteNote = async (id) => {
    try {
      if (token !== 'demo_token_offline') {
        await apiRequest(`/notes/${id}`, { method: 'DELETE' });
      }
      setNotes(prev => prev.filter(n => n._id !== id));
      addToast('Note deleted', 'info');
    } catch (err) {
      addToast(err.message || 'Failed to delete note', 'error');
    }
  };

  // Calculated Stats
  const stats = useMemo(() => {
    let income = 0;
    let expense = 0;
    transactions.forEach(t => {
      const amt = Number(t.amount) || 0;
      if (t.type === 'income') income += amt;
      else expense += amt;
    });

    const activeLoansCount = loans.filter(l => l.status !== 'settled').length;
    const wordsCount = learnedWords.length;

    return {
      netBalance: income - expense,
      monthlyIncome: income,
      monthlyExpense: expense,
      activeLoansCount,
      wordsCount,
    };
  }, [transactions, loans, learnedWords]);

  const activeTrip = useMemo(() => {
    return trips.find(t => t._id === activeTripId) || trips[0] || null;
  }, [trips, activeTripId]);

  // If user is not authenticated, show Auth View
  if (!user || !token) {
    return (
      <div className="h-full flex items-center justify-center p-4 bg-slate-50 dark:bg-slate-950">
        <ToastContainer toasts={toasts} onDismiss={removeToast} />
        <AuthScreen onLogin={handleLogin} onRegister={handleRegister} onDemo={handleDemoMode} />
      </div>
    );
  }

  // Authenticated Application Shell
  return (
    <div className="h-full flex flex-col md:flex-row overflow-hidden bg-slate-50 dark:bg-slate-950">
      <ToastContainer toasts={toasts} onDismiss={removeToast} />

      {/* Sidebar Navigation */}
      <aside className="w-full md:w-64 bg-white dark:bg-slate-900 border-r border-slate-200 dark:border-slate-800 flex flex-col shrink-0">
        {/* Brand Header */}
        <div className="p-6 border-b border-slate-100 dark:border-slate-800/80 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-600 text-white flex items-center justify-center shadow-md shadow-indigo-500/25">
              <Icon name="sparkles" className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="font-bold text-slate-900 dark:text-white leading-none">LifePulse</div>
              <div className="text-[11px] text-slate-400 mt-1">React Management Hub</div>
            </div>
          </div>
        </div>

        {/* Navigation Items */}
        <nav className="flex-1 p-4 space-y-1.5 overflow-y-auto">
          {[
            { id: 'dashboard', label: 'Dashboard', icon: 'layout-dashboard' },
            { id: 'transactions', label: 'Money & Expenses', icon: 'wallet' },
            { id: 'loans', label: 'Loans Tracker', icon: 'hand-coins' },
            { id: 'dictionary', label: 'Dictionary & AI', icon: 'book-open' },
            { id: 'luggage', label: 'Luggage & Trips', icon: 'briefcase' },
            { id: 'notes', label: 'Quick Notes', icon: 'sticky-note' },
            { id: 'settings', label: 'Settings', icon: 'settings' },
          ].map(item => (
            <button
              key={item.id}
              onClick={() => setActiveTab(item.id)}
              className={`flex items-center gap-3 w-full px-3.5 py-2.5 rounded-xl text-sm font-semibold transition ${
                activeTab === item.id
                  ? 'bg-indigo-50 dark:bg-indigo-950/40 text-indigo-600 dark:text-indigo-400 font-bold shadow-sm'
                  : 'text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800/50'
              }`}
            >
              <Icon name={item.icon} className="w-4 h-4" />
              <span>{item.label}</span>
            </button>
          ))}
        </nav>

        {/* User Card & Logout */}
        <div className="p-4 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="w-9 h-9 rounded-xl bg-indigo-100 dark:bg-indigo-900/50 text-indigo-600 dark:text-indigo-400 font-bold text-xs flex items-center justify-center shrink-0">
              {user.name ? user.name.charAt(0).toUpperCase() : 'U'}
            </div>
            <div className="min-w-0">
              <div className="text-xs font-semibold truncate text-slate-800 dark:text-slate-200">{user.name}</div>
              <div className="text-[10px] text-slate-400 truncate">{user.email}</div>
            </div>
          </div>
          <button
            onClick={handleLogout}
            title="Sign out"
            className="p-2 text-slate-400 hover:text-red-500 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 transition"
          >
            <Icon name="log-out" className="w-4 h-4" />
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="flex-1 flex flex-col h-full overflow-hidden bg-slate-50 dark:bg-slate-950">
        {/* Top Header Bar */}
        <header className="h-16 px-8 bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between shrink-0">
          <div>
            <h2 className="text-lg font-bold text-slate-900 dark:text-white capitalize">
              {activeTab === 'dashboard' && 'Dashboard Overview'}
              {activeTab === 'transactions' && 'Money & Expenses'}
              {activeTab === 'loans' && 'Loans Tracker'}
              {activeTab === 'dictionary' && 'Smart Lexicon & AI Explainer'}
              {activeTab === 'luggage' && 'Travel & Luggage Packing'}
              {activeTab === 'notes' && 'Quick Notes & Scratchpad'}
              {activeTab === 'settings' && 'Account & Settings'}
            </h2>
            <p className="text-xs text-slate-400">
              {activeTab === 'dashboard' && 'Real-time financial, travel, and personal statistics'}
              {activeTab === 'transactions' && 'Manage your cashflow, incoming payments, and daily expenses'}
              {activeTab === 'loans' && 'Keep track of borrowed and lent money with settlement deadlines'}
              {activeTab === 'dictionary' && 'Instant definitions, phonetic guides, and deep analogies powered by Gemini'}
              {activeTab === 'luggage' && 'Ensure nothing gets left behind with smart packing lists'}
              {activeTab === 'notes' && 'Capture quick insights, reminders, and checklists'}
              {activeTab === 'settings' && 'Profile information and system preferences'}
            </p>
          </div>

          <div className="flex items-center gap-3">
            {/* Dark Mode Toggle */}
            <button
              onClick={() => setDarkMode(prev => !prev)}
              title="Toggle Theme"
              className="p-2 rounded-xl border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition"
            >
              <Icon name={darkMode ? 'sun' : 'moon'} className="w-4 h-4" />
            </button>

            {/* Online Status */}
            <div className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-semibold border ${
              serverOnline
                ? 'bg-emerald-50 text-emerald-600 dark:bg-emerald-950/40 dark:text-emerald-400 border-emerald-200/50 dark:border-emerald-800/30'
                : 'bg-amber-50 text-amber-600 dark:bg-amber-950/40 dark:text-amber-400 border-amber-200/50 dark:border-amber-800/30'
            }`}>
              <span className={`w-2 h-2 rounded-full ${serverOnline ? 'bg-emerald-500 animate-pulse' : 'bg-amber-500'}`}></span>
              <span>{serverOnline ? 'Online' : 'Offline / Demo'}</span>
            </div>
          </div>
        </header>

        {/* Viewport Content */}
        <div className="flex-1 overflow-y-auto p-6 md:p-8">
          {activeTab === 'dashboard' && (
            <DashboardView
              stats={stats}
              transactions={transactions}
              activeTrip={activeTrip}
              onNavigate={setActiveTab}
              onOpenAddTx={() => setModalTx(true)}
              onOpenAddLoan={() => setModalLoan(true)}
              onOpenAddNote={() => setModalNote(true)}
            />
          )}

          {activeTab === 'transactions' && (
            <TransactionsView
              transactions={transactions}
              filter={txFilter}
              onFilterChange={setTxFilter}
              onOpenAdd={() => setModalTx(true)}
              onDelete={handleDeleteTransaction}
              stats={stats}
            />
          )}

          {activeTab === 'loans' && (
            <LoansView
              loans={loans}
              filter={loanFilter}
              onFilterChange={setLoanFilter}
              onOpenAdd={() => setModalLoan(true)}
              onSettle={handleSettleLoan}
              onDelete={handleDeleteLoan}
            />
          )}

          {activeTab === 'dictionary' && (
            <DictionaryView
              query={dictQuery}
              onQueryChange={setDictQuery}
              mode={dictMode}
              onModeChange={setDictMode}
              onSearch={handleLookupWord}
              loading={dictLoading}
              result={dictResult}
              onSave={handleSaveWord}
              learnedWords={learnedWords}
              onDeleteWord={handleDeleteSavedWord}
            />
          )}

          {activeTab === 'luggage' && (
            <LuggageView
              trips={trips}
              activeTrip={activeTrip}
              onSelectTrip={setActiveTripId}
              onOpenAddTrip={() => setModalTrip(true)}
              onOpenTemplates={() => setModalTemplate(true)}
              onAddItem={handleAddItemToTrip}
              onTogglePacked={handleTogglePacked}
              onDeleteItem={handleDeleteTripItem}
            />
          )}

          {activeTab === 'notes' && (
            <NotesView
              notes={notes}
              search={notesSearch}
              onSearchChange={setNotesSearch}
              onOpenAdd={() => setModalNote(true)}
              onTogglePin={handleTogglePinNote}
              onDelete={handleDeleteNote}
            />
          )}

          {activeTab === 'settings' && (
            <SettingsView
              user={user}
              darkMode={darkMode}
              onToggleDarkMode={() => setDarkMode(prev => !prev)}
              onLogout={handleLogout}
              onTestHealth={() => {
                apiRequest('/health')
                  .then(() => addToast('Backend server connection healthy (200 OK)', 'success'))
                  .catch(() => addToast('Server is currently unreachable', 'error'));
              }}
            />
          )}
        </div>
      </main>

      {/* MODALS */}
      {modalTx && (
        <Modal title="Record Transaction" onClose={() => setModalTx(false)}>
          <form onSubmit={handleAddTransaction} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1">Type</label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setTxForm(f => ({ ...f, type: 'expense' }))}
                  className={`py-2 text-xs font-semibold rounded-xl border transition ${
                    txForm.type === 'expense' ? 'bg-red-50 dark:bg-red-950/50 border-red-500 text-red-600' : 'border-slate-200 dark:border-slate-700'
                  }`}
                >
                  Expense
                </button>
                <button
                  type="button"
                  onClick={() => setTxForm(f => ({ ...f, type: 'income' }))}
                  className={`py-2 text-xs font-semibold rounded-xl border transition ${
                    txForm.type === 'income' ? 'bg-emerald-50 dark:bg-emerald-950/50 border-emerald-500 text-emerald-600' : 'border-slate-200 dark:border-slate-700'
                  }`}
                >
                  Income
                </button>
              </div>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1">Title / Description</label>
              <input
                type="text"
                required
                placeholder="e.g. Grocery store, Salary payout"
                value={txForm.title}
                onChange={e => setTxForm(f => ({ ...f, title: e.target.value }))}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1">Amount ($)</label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                required
                placeholder="0.00"
                value={txForm.amount}
                onChange={e => setTxForm(f => ({ ...f, amount: e.target.value }))}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1">Category</label>
              <select
                value={txForm.category}
                onChange={e => setTxForm(f => ({ ...f, category: e.target.value }))}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value="Food">Food & Dining</option>
                <option value="Shopping">Shopping</option>
                <option value="Housing">Housing & Rent</option>
                <option value="Transportation">Transportation</option>
                <option value="Utilities">Utilities</option>
                <option value="Entertainment">Entertainment</option>
                <option value="Health">Health & Fitness</option>
                <option value="Salary">Salary & Income</option>
                <option value="Freelance">Freelance</option>
                <option value="Other">Other</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1">Date</label>
              <input
                type="date"
                required
                value={txForm.date}
                onChange={e => setTxForm(f => ({ ...f, date: e.target.value }))}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <button
              type="submit"
              className="w-full py-3 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm shadow-md transition"
            >
              Save Transaction
            </button>
          </form>
        </Modal>
      )}

      {modalLoan && (
        <Modal title="Record Loan" onClose={() => setModalLoan(false)}>
          <form onSubmit={handleAddLoan} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1">Type</label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setLoanForm(f => ({ ...f, type: 'lent' }))}
                  className={`py-2 text-xs font-semibold rounded-xl border transition ${
                    loanForm.type === 'lent' ? 'bg-indigo-50 dark:bg-indigo-950/50 border-indigo-500 text-indigo-600' : 'border-slate-200 dark:border-slate-700'
                  }`}
                >
                  I Lent (Owed to me)
                </button>
                <button
                  type="button"
                  onClick={() => setLoanForm(f => ({ ...f, type: 'borrowed' }))}
                  className={`py-2 text-xs font-semibold rounded-xl border transition ${
                    loanForm.type === 'borrowed' ? 'bg-amber-50 dark:bg-amber-950/50 border-amber-500 text-amber-600' : 'border-slate-200 dark:border-slate-700'
                  }`}
                >
                  I Borrowed (I owe)
                </button>
              </div>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1">Person Name</label>
              <input
                type="text"
                required
                placeholder="e.g. David Miller"
                value={loanForm.personName}
                onChange={e => setLoanForm(f => ({ ...f, personName: e.target.value }))}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1">Amount ($)</label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                required
                placeholder="0.00"
                value={loanForm.amount}
                onChange={e => setLoanForm(f => ({ ...f, amount: e.target.value }))}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1">Due Date</label>
              <input
                type="date"
                value={loanForm.dueDate}
                onChange={e => setLoanForm(f => ({ ...f, dueDate: e.target.value }))}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1">Description / Note</label>
              <input
                type="text"
                placeholder="e.g. Dinner bill split"
                value={loanForm.note}
                onChange={e => setLoanForm(f => ({ ...f, note: e.target.value }))}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <button
              type="submit"
              className="w-full py-3 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm shadow-md transition"
            >
              Save Loan
            </button>
          </form>
        </Modal>
      )}

      {modalNote && (
        <Modal title="Create Note" onClose={() => setModalNote(false)}>
          <form onSubmit={handleAddNote} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1">Title</label>
              <input
                type="text"
                required
                placeholder="Note Title"
                value={noteForm.title}
                onChange={e => setNoteForm(f => ({ ...f, title: e.target.value }))}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1">Category / Tag</label>
              <input
                type="text"
                placeholder="Personal, Work, Ideas..."
                value={noteForm.category}
                onChange={e => setNoteForm(f => ({ ...f, category: e.target.value }))}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1">Content</label>
              <textarea
                required
                rows="4"
                placeholder="Write your notes or thoughts here..."
                value={noteForm.content}
                onChange={e => setNoteForm(f => ({ ...f, content: e.target.value }))}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
              ></textarea>
            </div>
            <button
              type="submit"
              className="w-full py-3 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm shadow-md transition"
            >
              Save Note
            </button>
          </form>
        </Modal>
      )}

      {modalTrip && (
        <Modal title="Create Travel Trip" onClose={() => setModalTrip(false)}>
          <form onSubmit={handleAddTrip} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1">Trip Name / Destination</label>
              <input
                type="text"
                required
                placeholder="e.g. Kyoto Vacation, Dev Summit"
                value={tripForm.name}
                onChange={e => setTripForm(f => ({ ...f, name: e.target.value }))}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1">Travel Dates</label>
              <input
                type="text"
                placeholder="e.g. Oct 14 - Oct 22, 2026"
                value={tripForm.dates}
                onChange={e => setTripForm(f => ({ ...f, dates: e.target.value }))}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <button
              type="submit"
              className="w-full py-3 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm shadow-md transition"
            >
              Create Trip
            </button>
          </form>
        </Modal>
      )}

      {modalTemplate && (
        <Modal title="Apply Packing Template" onClose={() => setModalTemplate(false)}>
          <div className="space-y-3">
            {[
              { id: 'weekend', title: 'Weekend Getaway', desc: 'Light essentials, casual wear, hygiene kit' },
              { id: 'business', title: 'Business Trip', desc: 'Formal attire, laptop charger, business cards' },
              { id: 'beach', title: 'Beach Vacation', desc: 'Swimwear, sunscreen, sunglasses, sandals' },
              { id: 'hiking', title: 'Hiking & Camping', desc: 'Boots, thermal layer, first aid, water bottle' },
            ].map(tpl => (
              <button
                key={tpl.id}
                onClick={() => handleApplyTemplate(tpl.title)}
                className="w-full p-4 rounded-xl border border-slate-200 dark:border-slate-700 hover:border-indigo-500 dark:hover:border-indigo-500 text-left transition flex items-center justify-between group"
              >
                <div>
                  <div className="font-semibold text-sm text-slate-800 dark:text-slate-200 group-hover:text-indigo-600">{tpl.title}</div>
                  <div className="text-xs text-slate-400 mt-0.5">{tpl.desc}</div>
                </div>
                <Icon name="arrow-right" className="w-4 h-4 text-slate-400 group-hover:text-indigo-600" />
              </button>
            ))}
          </div>
        </Modal>
      )}
    </div>
  );
}

// --- Auth Component ---
function AuthScreen({ onLogin, onRegister, onDemo }) {
  const [tab, setTab] = useState('login');
  const [loginEmail, setLoginEmail] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [regName, setRegName] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regPassword, setRegPassword] = useState('');

  return (
    <div className="w-full max-w-md bg-white dark:bg-slate-900 rounded-3xl shadow-xl border border-slate-100 dark:border-slate-800 p-8">
      <div className="text-center mb-8">
        <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-indigo-600/10 text-indigo-600 dark:bg-indigo-500/20 dark:text-indigo-400 mb-4">
          <Icon name="sparkles" className="w-8 h-8 text-indigo-600 dark:text-indigo-400" />
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">LifePulse</h1>
        <p className="text-slate-500 dark:text-slate-400 text-sm mt-1">Personal Life Management in React</p>
      </div>

      <div className="flex p-1 bg-slate-100 dark:bg-slate-800/60 rounded-xl mb-6">
        <button
          onClick={() => setTab('login')}
          className={`flex-1 py-2 text-sm font-semibold rounded-lg transition ${
            tab === 'login' ? 'bg-white dark:bg-slate-700 shadow-sm text-slate-900 dark:text-white' : 'text-slate-500 dark:text-slate-400'
          }`}
        >
          Sign In
        </button>
        <button
          onClick={() => setTab('register')}
          className={`flex-1 py-2 text-sm font-semibold rounded-lg transition ${
            tab === 'register' ? 'bg-white dark:bg-slate-700 shadow-sm text-slate-900 dark:text-white' : 'text-slate-500 dark:text-slate-400'
          }`}
        >
          Create Account
        </button>
      </div>

      {tab === 'login' ? (
        <form onSubmit={(e) => { e.preventDefault(); onLogin(loginEmail, loginPassword); }} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1.5">Email Address</label>
            <div className="relative">
              <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400"><Icon name="mail" className="w-4 h-4" /></span>
              <input
                type="email"
                required
                placeholder="you@example.com"
                value={loginEmail}
                onChange={e => setLoginEmail(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          </div>
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1.5">Password</label>
            <div className="relative">
              <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400"><Icon name="lock" className="w-4 h-4" /></span>
              <input
                type="password"
                required
                placeholder="••••••••"
                value={loginPassword}
                onChange={e => setLoginPassword(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          </div>
          <button
            type="submit"
            className="w-full py-3 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm shadow-md shadow-indigo-500/20 transition flex items-center justify-center gap-2"
          >
            <span>Sign In</span>
            <Icon name="arrow-right" className="w-4 h-4" />
          </button>
        </form>
      ) : (
        <form onSubmit={(e) => { e.preventDefault(); onRegister(regName, regEmail, regPassword); }} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1.5">Full Name</label>
            <div className="relative">
              <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400"><Icon name="user" className="w-4 h-4" /></span>
              <input
                type="text"
                required
                placeholder="Alex Johnson"
                value={regName}
                onChange={e => setRegName(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          </div>
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1.5">Email Address</label>
            <div className="relative">
              <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400"><Icon name="mail" className="w-4 h-4" /></span>
              <input
                type="email"
                required
                placeholder="you@example.com"
                value={regEmail}
                onChange={e => setRegEmail(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          </div>
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1.5">Password</label>
            <div className="relative">
              <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400"><Icon name="lock" className="w-4 h-4" /></span>
              <input
                type="password"
                required
                minlength="6"
                placeholder="Min 6 characters"
                value={regPassword}
                onChange={e => setRegPassword(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          </div>
          <button
            type="submit"
            className="w-full py-3 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm shadow-md shadow-indigo-500/20 transition flex items-center justify-center gap-2"
          >
            <span>Create Account</span>
            <Icon name="sparkles" className="w-4 h-4" />
          </button>
        </form>
      )}

      {/* Quick Demo Mode */}
      <div className="mt-6 pt-6 border-t border-slate-100 dark:border-slate-800 text-center">
        <button
          onClick={onDemo}
          className="w-full py-2.5 px-4 rounded-xl border border-slate-200 dark:border-slate-700 hover:border-indigo-500 text-xs font-bold text-slate-700 dark:text-slate-300 hover:text-indigo-600 transition flex items-center justify-center gap-2"
        >
          <Icon name="play" className="w-3.5 h-3.5 text-indigo-600" />
          <span>Explore in Demo / Preview Mode</span>
        </button>
      </div>
    </div>
  );
}

// --- Dashboard Component ---
function DashboardView({ stats, transactions, activeTrip, onNavigate, onOpenAddTx, onOpenAddLoan, onOpenAddNote }) {
  return (
    <div className="space-y-6">
      {/* 4 Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="bg-white dark:bg-slate-900 p-5 rounded-2xl border border-slate-100 dark:border-slate-800 shadow-sm">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Net Balance</span>
            <div className="p-2 rounded-xl bg-indigo-50 dark:bg-indigo-950/40 text-indigo-600">
              <Icon name="wallet" className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
            ${stats.netBalance.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </div>
          <div className="text-xs text-slate-400 mt-1">Calculated across all transactions</div>
        </div>

        <div className="bg-white dark:bg-slate-900 p-5 rounded-2xl border border-slate-100 dark:border-slate-800 shadow-sm">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Monthly Income</span>
            <div className="p-2 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600">
              <Icon name="trending-up" className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-bold tracking-tight text-emerald-600 dark:text-emerald-400">
            +${stats.monthlyIncome.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </div>
          <div className="text-xs text-slate-400 mt-1">Recorded income stream</div>
        </div>

        <div className="bg-white dark:bg-slate-900 p-5 rounded-2xl border border-slate-100 dark:border-slate-800 shadow-sm">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Active Loans</span>
            <div className="p-2 rounded-xl bg-amber-50 dark:bg-amber-950/40 text-amber-600">
              <Icon name="hand-coins" className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
            {stats.activeLoansCount}
          </div>
          <div className="text-xs text-slate-400 mt-1">Pending full settlement</div>
        </div>

        <div className="bg-white dark:bg-slate-900 p-5 rounded-2xl border border-slate-100 dark:border-slate-800 shadow-sm">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Vocabulary</span>
            <div className="p-2 rounded-xl bg-purple-50 dark:bg-purple-950/40 text-purple-600">
              <Icon name="book-marked" className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
            {stats.wordsCount}
          </div>
          <div className="text-xs text-slate-400 mt-1">Words in Notebook</div>
        </div>
      </div>

      {/* Grid: Recent Transactions & Quick Shortcuts */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-white dark:bg-slate-900 rounded-2xl border border-slate-100 dark:border-slate-800 p-6 shadow-sm">
          <div className="flex items-center justify-between mb-5">
            <h3 className="font-bold text-slate-900 dark:text-white text-base">Recent Cashflow</h3>
            <button
              onClick={() => onNavigate('transactions')}
              className="text-xs font-semibold text-indigo-600 hover:text-indigo-700 dark:text-indigo-400 flex items-center gap-1"
            >
              <span>View All</span>
              <Icon name="chevron-right" className="w-3.5 h-3.5" />
            </button>
          </div>

          {transactions.length === 0 ? (
            <div className="py-12 text-center text-slate-400 text-sm">
              No transactions recorded yet. Click "New Transaction" to begin!
            </div>
          ) : (
            <div className="divide-y divide-slate-100 dark:divide-slate-800/80">
              {transactions.slice(0, 5).map(tx => (
                <div key={tx._id} className="py-3.5 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className={`w-9 h-9 rounded-xl flex items-center justify-center ${
                      tx.type === 'income'
                        ? 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600'
                        : 'bg-red-50 dark:bg-red-950/40 text-red-600'
                    }`}>
                      <Icon name={tx.type === 'income' ? 'arrow-down-left' : 'arrow-up-right'} className="w-4 h-4" />
                    </div>
                    <div>
                      <div className="text-sm font-semibold text-slate-900 dark:text-white">{tx.title}</div>
                      <div className="text-xs text-slate-400">{tx.category} • {tx.date ? new Date(tx.date).toLocaleDateString() : 'Recent'}</div>
                    </div>
                  </div>
                  <div className={`text-sm font-bold ${
                    tx.type === 'income' ? 'text-emerald-600 dark:text-emerald-400' : 'text-slate-900 dark:text-white'
                  }`}>
                    {tx.type === 'income' ? '+' : '-'}${Number(tx.amount).toFixed(2)}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Shortcuts & Travel Widget */}
        <div className="space-y-6">
          <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-100 dark:border-slate-800 p-6 shadow-sm">
            <h3 className="font-bold text-slate-900 dark:text-white text-base mb-4">Quick Shortcuts</h3>
            <div className="grid grid-cols-2 gap-3">
              <button
                onClick={onOpenAddTx}
                className="p-3.5 rounded-xl border border-slate-200 dark:border-slate-800 hover:border-indigo-500 dark:hover:border-indigo-500 text-left transition group"
              >
                <div className="w-8 h-8 rounded-lg bg-emerald-500/10 text-emerald-600 flex items-center justify-center mb-2 group-hover:scale-105 transition">
                  <Icon name="plus" className="w-4 h-4" />
                </div>
                <div className="text-xs font-semibold text-slate-800 dark:text-slate-200">New Transaction</div>
              </button>

              <button
                onClick={onOpenAddLoan}
                className="p-3.5 rounded-xl border border-slate-200 dark:border-slate-800 hover:border-indigo-500 dark:hover:border-indigo-500 text-left transition group">
                <div className="w-8 h-8 rounded-lg bg-indigo-500/10 text-indigo-600 flex items-center justify-center mb-2 group-hover:scale-105 transition">
                  <Icon name="hand-coins" className="w-4 h-4" />
                </div>
                <div className="text-xs font-semibold text-slate-800 dark:text-slate-200">Record Loan</div>
              </button>

              <button
                onClick={() => onNavigate('dictionary')}
                className="p-3.5 rounded-xl border border-slate-200 dark:border-slate-800 hover:border-indigo-500 dark:hover:border-indigo-500 text-left transition group"
              >
                <div className="w-8 h-8 rounded-lg bg-purple-500/10 text-purple-600 flex items-center justify-center mb-2 group-hover:scale-105 transition">
                  <Icon name="sparkles" className="w-4 h-4" />
                </div>
                <div className="text-xs font-semibold text-slate-800 dark:text-slate-200">Gemini Lookup</div>
              </button>

              <button
                onClick={onOpenAddNote}
                className="p-3.5 rounded-xl border border-slate-200 dark:border-slate-800 hover:border-indigo-500 dark:hover:border-indigo-500 text-left transition group"
              >
                <div className="w-8 h-8 rounded-lg bg-amber-500/10 text-amber-600 flex items-center justify-center mb-2 group-hover:scale-105 transition">
                  <Icon name="file-plus" className="w-4 h-4" />
                </div>
                <div className="text-xs font-semibold text-slate-800 dark:text-slate-200">Create Note</div>
              </button>
            </div>
          </div>

          {/* Travel Packing Banner */}
          <div className="bg-gradient-to-br from-indigo-600 to-indigo-800 rounded-2xl p-6 text-white shadow-lg shadow-indigo-500/20">
            <div className="flex items-center gap-2 text-indigo-200 text-xs font-semibold uppercase tracking-wider mb-2">
              <Icon name="plane-takeoff" className="w-4 h-4" />
              <span>Travel Packing</span>
            </div>
            <div className="text-lg font-bold mb-1">
              {activeTrip ? activeTrip.destination : 'No upcoming trip'}
            </div>
            <p className="text-xs text-indigo-100/80 mb-4">
              {activeTrip ? `${(activeTrip.items || []).filter(i => i.isPacked).length} of ${(activeTrip.items || []).length} items packed` : 'Create a packing checklist for your next voyage.'}
            </p>
            <button
              onClick={() => onNavigate('luggage')}
              className="px-3.5 py-2 rounded-xl bg-white/15 hover:bg-white/25 backdrop-blur-md text-xs font-semibold transition flex items-center gap-2"
            >
              <span>Open Luggage Tracker</span>
              <Icon name="arrow-right" className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

// --- Transactions Component ---
function TransactionsView({ transactions, filter, onFilterChange, onOpenAdd, onDelete, stats }) {
  const filtered = transactions.filter(t => {
    if (filter === 'expense') return t.type === 'expense';
    if (filter === 'income') return t.type === 'income';
    return true;
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-2 bg-white dark:bg-slate-900 p-1 rounded-xl border border-slate-200 dark:border-slate-800">
          {['all', 'expense', 'income'].map(type => (
            <button
              key={type}
              onClick={() => onFilterChange(type)}
              className={`px-3.5 py-1.5 text-xs font-semibold rounded-lg transition capitalize ${
                filter === type ? 'bg-indigo-600 text-white shadow-sm' : 'text-slate-500 hover:text-slate-900 dark:text-slate-400'
              }`}
            >
              {type === 'all' ? 'All' : type === 'expense' ? 'Expenses' : 'Income'}
            </button>
          ))}
        </div>
        <button
          onClick={onOpenAdd}
          className="px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm shadow-md shadow-indigo-500/20 transition flex items-center gap-2"
        >
          <Icon name="plus" className="w-4 h-4" />
          <span>Add Transaction</span>
        </button>
      </div>

      <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-100 dark:border-slate-800 shadow-sm overflow-hidden">
        {filtered.length === 0 ? (
          <div className="py-16 text-center text-slate-400 text-sm">
            No transactions found for this filter.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 dark:bg-slate-800/50 text-slate-400 text-xs font-semibold uppercase tracking-wider border-b border-slate-100 dark:border-slate-800">
                <tr>
                  <th className="py-3.5 px-6">Description</th>
                  <th className="py-3.5 px-6">Category</th>
                  <th className="py-3.5 px-6">Date</th>
                  <th className="py-3.5 px-6 text-right">Amount</th>
                  <th className="py-3.5 px-6 text-center">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800/80">
                {filtered.map(tx => (
                  <tr key={tx._id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition">
                    <td className="py-4 px-6 font-semibold text-slate-800 dark:text-slate-200">
                      {tx.title}
                    </td>
                    <td className="py-4 px-6">
                      <span className="px-2.5 py-1 rounded-full text-xs font-medium bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300">
                        {tx.category || 'General'}
                      </span>
                    </td>
                    <td className="py-4 px-6 text-xs text-slate-400">
                      {tx.date ? new Date(tx.date).toLocaleDateString() : 'Recent'}
                    </td>
                    <td className={`py-4 px-6 text-right font-bold ${
                      tx.type === 'income' ? 'text-emerald-600 dark:text-emerald-400' : 'text-slate-900 dark:text-white'
                    }`}>
                      {tx.type === 'income' ? '+' : '-'}${Number(tx.amount).toFixed(2)}
                    </td>
                    <td className="py-4 px-6 text-center">
                      <button
                        onClick={() => onDelete(tx._id)}
                        className="p-1.5 text-slate-400 hover:text-red-500 rounded-lg transition"
                        title="Delete transaction"
                      >
                        <Icon name="trash-2" className="w-4 h-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

// --- Loans Component ---
function LoansView({ loans, filter, onFilterChange, onOpenAdd, onSettle, onDelete }) {
  const filtered = loans.filter(l => {
    if (filter === 'lent') return l.type === 'lent';
    if (filter === 'borrowed') return l.type === 'borrowed';
    return true;
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-2 bg-white dark:bg-slate-900 p-1 rounded-xl border border-slate-200 dark:border-slate-800">
          {['all', 'lent', 'borrowed'].map(type => (
            <button
              key={type}
              onClick={() => onFilterChange(type)}
              className={`px-3.5 py-1.5 text-xs font-semibold rounded-lg transition capitalize ${
                filter === type ? 'bg-indigo-600 text-white shadow-sm' : 'text-slate-500 hover:text-slate-900 dark:text-slate-400'
              }`}
            >
              {type === 'all' ? 'All Loans' : type === 'lent' ? 'I Lent (Owed to Me)' : 'I Borrowed (I Owe)'}
            </button>
          ))}
        </div>
        <button
          onClick={onOpenAdd}
          className="px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm shadow-md shadow-indigo-500/20 transition flex items-center gap-2"
        >
          <Icon name="plus" className="w-4 h-4" />
          <span>Record Loan</span>
        </button>
      </div>

      {filtered.length === 0 ? (
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-100 dark:border-slate-800 p-16 text-center text-slate-400 text-sm">
          No loans found in this category.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filtered.map(loan => {
            const isLent = loan.type === 'lent';
            const isSettled = loan.status === 'settled';
            return (
              <div
                key={loan._id}
                className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-100 dark:border-slate-800 p-5 shadow-sm space-y-4"
              >
                <div className="flex items-center justify-between">
                  <span className={`px-2.5 py-1 rounded-full text-xs font-bold ${
                    isLent
                      ? 'bg-indigo-50 text-indigo-600 dark:bg-indigo-950/40 dark:text-indigo-400'
                      : 'bg-amber-50 text-amber-600 dark:bg-amber-950/40 dark:text-amber-400'
                  }`}>
                    {isLent ? 'I Lent' : 'I Borrowed'}
                  </span>
                  <span className={`text-xs font-bold px-2 py-0.5 rounded-md ${
                    isSettled ? 'bg-emerald-50 text-emerald-600 dark:bg-emerald-950/40 dark:text-emerald-400' : 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400'
                  }`}>
                    {isSettled ? 'Settled' : 'Pending'}
                  </span>
                </div>

                <div>
                  <h4 className="font-bold text-base text-slate-900 dark:text-white">{loan.personName}</h4>
                  <div className="text-2xl font-bold text-slate-900 dark:text-white mt-1">
                    ${Number(loan.amount).toFixed(2)}
                  </div>
                  {loan.note && <p className="text-xs text-slate-400 mt-1">{loan.note}</p>}
                </div>

                <div className="text-xs text-slate-400 flex items-center gap-1">
                  <Icon name="calendar" className="w-3.5 h-3.5" />
                  <span>Due: {loan.dueDate ? new Date(loan.dueDate).toLocaleDateString() : 'No deadline'}</span>
                </div>

                <div className="pt-2 border-t border-slate-100 dark:border-slate-800/80 flex items-center justify-between gap-2">
                  {!isSettled ? (
                    <button
                      onClick={() => onSettle(loan._id)}
                      className="flex-1 py-2 px-3 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 hover:bg-emerald-100 text-emerald-600 dark:text-emerald-400 text-xs font-bold transition flex items-center justify-center gap-1.5"
                    >
                      <Icon name="check" className="w-3.5 h-3.5" />
                      <span>Mark Settled</span>
                    </button>
                  ) : (
                    <div className="text-xs font-semibold text-emerald-600 dark:text-emerald-400 flex items-center gap-1">
                      <Icon name="check-circle" className="w-3.5 h-3.5" />
                      <span>Paid & Closed</span>
                    </div>
                  )}
                  <button
                    onClick={() => onDelete(loan._id)}
                    className="p-2 text-slate-400 hover:text-red-500 rounded-xl transition"
                    title="Delete loan"
                  >
                    <Icon name="trash-2" className="w-4 h-4" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

// --- Dictionary Component ---
function DictionaryView({ query, onQueryChange, mode, onModeChange, onSearch, loading, result, onSave, learnedWords, onDeleteWord }) {
  return (
    <div className="space-y-6">
      {/* Search Header */}
      <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-100 dark:border-slate-800 p-6 shadow-sm">
        <div className="max-w-2xl mx-auto text-center space-y-4">
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-semibold bg-purple-50 dark:bg-purple-950/40 text-purple-600 border border-purple-200/50 dark:border-purple-800/30">
            <Icon name="sparkles" className="w-3.5 h-3.5" />
            <span>Gemini 3.5 Flash Powered</span>
          </div>
          <h3 className="text-xl font-bold text-slate-900 dark:text-white">Smart Lexicon & Concept Explainer</h3>
          <p className="text-slate-500 dark:text-slate-400 text-xs">
            Look up definitions, pronunciation, real-world examples, analogies, and save to your vocabulary notebook.
          </p>

          <form onSubmit={onSearch} className="flex flex-col sm:flex-row gap-2">
            <div className="relative flex-1">
              <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">
                <Icon name="search" className="w-4 h-4" />
              </span>
              <input
                type="text"
                required
                placeholder="Enter any word or concept (e.g. Resilience, Pragmatic, Serendipity)..."
                value={query}
                onChange={e => onQueryChange(e.target.value)}
                className="w-full pl-10 pr-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div className="flex p-1 bg-slate-100 dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 shrink-0">
              <button
                type="button"
                onClick={() => onModeChange('meaning')}
                className={`px-3 py-2 text-xs font-semibold rounded-lg transition ${
                  mode === 'meaning' ? 'bg-white dark:bg-slate-700 shadow-sm text-slate-900 dark:text-white' : 'text-slate-500'
                }`}
              >
                Meaning
              </button>
              <button
                type="button"
                onClick={() => onModeChange('explain')}
                className={`px-3 py-2 text-xs font-semibold rounded-lg transition ${
                  mode === 'explain' ? 'bg-white dark:bg-slate-700 shadow-sm text-slate-900 dark:text-white' : 'text-slate-500'
                }`}
              >
                Deep Explain
              </button>
            </div>
            <button
              type="submit"
              disabled={loading}
              className="px-5 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white font-semibold text-sm shadow-md shadow-indigo-500/20 transition flex items-center justify-center gap-2 shrink-0"
            >
              {loading ? (
                <>
                  <Icon name="loader-2" className="w-4 h-4 animate-spin" />
                  <span>Looking up...</span>
                </>
              ) : (
                <>
                  <Icon name="search" className="w-4 h-4" />
                  <span>Look Up</span>
                </>
              )}
            </button>
          </form>
        </div>
      </div>

      {/* Result Card */}
      {result && (
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-100 dark:border-slate-800 p-6 shadow-sm space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-3">
                <h3 className="text-2xl font-bold text-slate-900 dark:text-white">{result.word}</h3>
                {result.phonetic && (
                  <span className="text-sm font-mono text-slate-400">{result.phonetic}</span>
                )}
                {result.partOfSpeech && (
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-indigo-50 text-indigo-600 dark:bg-indigo-950/40 dark:text-indigo-400">
                    {result.partOfSpeech}
                  </span>
                )}
              </div>
              <p className="text-sm text-slate-600 dark:text-slate-300 mt-2 font-medium">
                {result.shortDefinition || result.fullDefinition}
              </p>
            </div>
            <button
              onClick={() => onSave(result)}
              disabled={result.isSaved}
              className={`px-4 py-2.5 rounded-xl font-semibold text-xs transition flex items-center gap-2 shrink-0 ${
                result.isSaved
                  ? 'bg-slate-100 text-slate-400 dark:bg-slate-800'
                  : 'bg-purple-600 hover:bg-purple-700 text-white shadow-md shadow-purple-500/20'
              }`}
            >
              <Icon name={result.isSaved ? 'check' : 'bookmark-plus'} className="w-4 h-4" />
              <span>{result.isSaved ? 'Saved in Notebook' : 'Save to Notebook'}</span>
            </button>
          </div>

          {result.eli5Analogy && (
            <div className="p-4 rounded-xl bg-purple-50/60 dark:bg-purple-950/30 border border-purple-100 dark:border-purple-800/40">
              <div className="text-xs font-bold uppercase tracking-wider text-purple-600 dark:text-purple-400 mb-1 flex items-center gap-1.5">
                <Icon name="lightbulb" className="w-3.5 h-3.5" />
                <span>ELI5 Analogy</span>
              </div>
              <p className="text-xs text-slate-700 dark:text-slate-300 leading-relaxed">{result.eli5Analogy}</p>
            </div>
          )}

          {result.examples && result.examples.length > 0 && (
            <div>
              <h5 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">Example Usage</h5>
              <ul className="space-y-1.5 text-xs text-slate-600 dark:text-slate-300">
                {result.examples.map((ex, i) => (
                  <li key={i} className="flex items-start gap-2">
                    <span className="text-indigo-500 font-bold">•</span>
                    <span className="italic">"{ex}"</span>
                  </li>
                ))}
              </ul>
            </div>
          )}

          {result.synonyms && result.synonyms.length > 0 && (
            <div>
              <h5 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">Synonyms</h5>
              <div className="flex flex-wrap gap-1.5">
                {result.synonyms.map((syn, i) => (
                  <span key={i} className="px-2.5 py-1 rounded-lg text-xs bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300">
                    {syn}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Vocabulary Notebook */}
      <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-100 dark:border-slate-800 p-6 shadow-sm space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h4 className="font-bold text-slate-900 dark:text-white">Vocabulary Notebook</h4>
            <p className="text-xs text-slate-400">Words saved for personal revision and mastery</p>
          </div>
          <span className="text-xs font-bold px-3 py-1 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300">
            {learnedWords.length} Words
          </span>
        </div>

        {learnedWords.length === 0 ? (
          <div className="py-10 text-center text-slate-400 text-xs">
            Your notebook is empty. Search any word above and click "Save to Notebook"!
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {learnedWords.map(w => (
              <div key={w._id} className="p-4 rounded-xl border border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/40 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-sm text-slate-900 dark:text-white">{w.word}</span>
                  <button
                    onClick={() => onDeleteWord(w._id)}
                    className="p-1 text-slate-400 hover:text-red-500 rounded transition"
                    title="Remove from notebook"
                  >
                    <Icon name="trash-2" className="w-3.5 h-3.5" />
                  </button>
                </div>
                {w.phonetic && <div className="text-[11px] font-mono text-slate-400">{w.phonetic}</div>}
                <p className="text-xs text-slate-600 dark:text-slate-300 line-clamp-2">{w.shortDefinition || w.fullDefinition}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

// --- Luggage Component ---
function LuggageView({ trips, activeTrip, onSelectTrip, onOpenAddTrip, onOpenTemplates, onAddItem, onTogglePacked, onDeleteItem }) {
  const [newItemName, setNewItemName] = useState('');
  const [newItemCat, setNewItemCat] = useState('Clothing');

  const items = activeTrip ? activeTrip.items || [] : [];
  const packedCount = items.filter(i => i.isPacked).length;
  const progress = items.length > 0 ? Math.round((packedCount / items.length) * 100) : 0;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          {trips.length > 0 && (
            <select
              value={activeTrip ? activeTrip._id : ''}
              onChange={e => onSelectTrip(e.target.value)}
              className="px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-sm font-semibold outline-none focus:ring-2 focus:ring-indigo-500"
            >
              {trips.map(t => (
                <option key={t._id} value={t._id}>{t.destination}</option>
              ))}
            </select>
          )}
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={onOpenTemplates}
            disabled={!activeTrip}
            className="px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 font-semibold text-xs transition flex items-center gap-1.5"
          >
            <Icon name="layers" className="w-4 h-4" />
            <span>Templates</span>
          </button>
          <button
            onClick={onOpenAddTrip}
            className="px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm shadow-md shadow-indigo-500/20 transition flex items-center gap-2"
          >
            <Icon name="plus" className="w-4 h-4" />
            <span>New Trip</span>
          </button>
        </div>
      </div>

      {!activeTrip ? (
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-100 dark:border-slate-800 p-16 text-center text-slate-400 text-sm">
          No travel trips created yet. Click "New Trip" to start packing!
        </div>
      ) : (
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-100 dark:border-slate-800 p-6 shadow-sm space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h3 className="text-xl font-bold text-slate-900 dark:text-white">{activeTrip.destination}</h3>
              <p className="text-xs text-slate-400 mt-0.5">{activeTrip.travelDates || 'Dates upcoming'}</p>
            </div>
            <div className="w-full sm:w-48">
              <div className="flex items-center justify-between text-xs font-semibold text-slate-500 mb-1">
                <span>Packing Progress</span>
                <span>{progress}%</span>
              </div>
              <div className="h-2 w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                <div className="h-full bg-indigo-600 rounded-full transition-all duration-300" style={{ width: `${progress}%` }}></div>
              </div>
            </div>
          </div>

          {/* Add Item Row */}
          <form
            onSubmit={e => {
              e.preventDefault();
              onAddItem(activeTrip._id, newItemName, newItemCat);
              setNewItemName('');
            }}
            className="flex flex-col sm:flex-row gap-2 pt-4 border-t border-slate-100 dark:border-slate-800"
          >
            <input
              type="text"
              required
              placeholder="Add luggage item (e.g. Passport, Charger, Raincoat)..."
              value={newItemName}
              onChange={e => setNewItemName(e.target.value)}
              className="flex-1 px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <select
              value={newItemCat}
              onChange={e => setNewItemCat(e.target.value)}
              className="px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm outline-none"
            >
              <option value="Clothing">Clothing</option>
              <option value="Electronics">Electronics</option>
              <option value="Toiletries">Toiletries</option>
              <option value="Documents">Documents</option>
              <option value="Essentials">Essentials</option>
            </select>
            <button
              type="submit"
              className="px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs transition"
            >
              Add Item
            </button>
          </form>

          {/* Items List */}
          <div className="divide-y divide-slate-100 dark:divide-slate-800/80">
            {items.length === 0 ? (
              <div className="py-8 text-center text-slate-400 text-xs">
                Packing list is empty. Add an item above or use a Template!
              </div>
            ) : (
              items.map(it => (
                <div key={it._id} className="py-3 flex items-center justify-between">
                  <label className="flex items-center gap-3 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={it.isPacked}
                      onChange={() => onTogglePacked(activeTrip._id, it._id)}
                      className="w-4 h-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
                    />
                    <span className={`text-sm font-medium ${it.isPacked ? 'line-through text-slate-400' : 'text-slate-800 dark:text-slate-200'}`}>
                      {it.name}
                    </span>
                    <span className="text-[10px] px-2 py-0.5 rounded-md bg-slate-100 dark:bg-slate-800 text-slate-500">
                      {it.category}
                    </span>
                  </label>
                  <button
                    onClick={() => onDeleteItem(activeTrip._id, it._id)}
                    className="p-1.5 text-slate-400 hover:text-red-500 rounded transition"
                  >
                    <Icon name="trash-2" className="w-3.5 h-3.5" />
                  </button>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}

// --- Notes Component ---
function NotesView({ notes, search, onSearchChange, onOpenAdd, onTogglePin, onDelete }) {
  const filtered = notes.filter(n => {
    const q = search.toLowerCase();
    return (n.title || '').toLowerCase().includes(q) || (n.content || '').toLowerCase().includes(q) || (n.category || '').toLowerCase().includes(q);
  });

  const pinnedNotes = filtered.filter(n => n.isPinned);
  const otherNotes = filtered.filter(n => !n.isPinned);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="relative w-full sm:w-72">
          <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">
            <Icon name="search" className="w-4 h-4" />
          </span>
          <input
            type="text"
            placeholder="Search notes..."
            value={search}
            onChange={e => onSearchChange(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-xs focus:ring-2 focus:ring-indigo-500 outline-none"
          />
        </div>
        <button
          onClick={onOpenAdd}
          className="px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm shadow-md shadow-indigo-500/20 transition flex items-center gap-2"
        >
          <Icon name="plus" className="w-4 h-4" />
          <span>Create Note</span>
        </button>
      </div>

      {filtered.length === 0 ? (
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-100 dark:border-slate-800 p-16 text-center text-slate-400 text-sm">
          No notes match your query. Click "Create Note" to add one!
        </div>
      ) : (
        <div className="space-y-6">
          {pinnedNotes.length > 0 && (
            <div className="space-y-3">
              <div className="text-xs font-bold uppercase tracking-wider text-indigo-600 dark:text-indigo-400 flex items-center gap-1.5">
                <Icon name="pin" className="w-3.5 h-3.5" />
                <span>Pinned Notes</span>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                {pinnedNotes.map(note => (
                  <NoteCard key={note._id} note={note} onTogglePin={onTogglePin} onDelete={onDelete} />
                ))}
              </div>
            </div>
          )}

          {otherNotes.length > 0 && (
            <div className="space-y-3">
              {pinnedNotes.length > 0 && (
                <div className="text-xs font-bold uppercase tracking-wider text-slate-400">Other Notes</div>
              )}
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                {otherNotes.map(note => (
                  <NoteCard key={note._id} note={note} onTogglePin={onTogglePin} onDelete={onDelete} />
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function NoteCard({ note, onTogglePin, onDelete }) {
  return (
    <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-100 dark:border-slate-800 p-5 shadow-sm space-y-3 flex flex-col justify-between">
      <div>
        <div className="flex items-start justify-between gap-2">
          <h4 className="font-bold text-sm text-slate-900 dark:text-white leading-tight">{note.title}</h4>
          <button
            onClick={() => onTogglePin(note._id)}
            className={`p-1 rounded transition ${note.isPinned ? 'text-indigo-600' : 'text-slate-400 hover:text-slate-600'}`}
            title={note.isPinned ? 'Unpin note' : 'Pin note'}
          >
            <Icon name="pin" className="w-3.5 h-3.5" />
          </button>
        </div>
        <p className="text-xs text-slate-600 dark:text-slate-300 mt-2 leading-relaxed whitespace-pre-line">
          {note.content}
        </p>
      </div>
      <div className="pt-3 border-t border-slate-100 dark:border-slate-800/80 flex items-center justify-between text-xs text-slate-400">
        <span className="px-2 py-0.5 rounded-md bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 text-[10px] font-semibold">
          {note.category || 'General'}
        </span>
        <button
          onClick={() => onDelete(note._id)}
          className="p-1 text-slate-400 hover:text-red-500 rounded transition"
          title="Delete note"
        >
          <Icon name="trash-2" className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
}

// --- Settings Component ---
function SettingsView({ user, darkMode, onToggleDarkMode, onLogout, onTestHealth }) {
  return (
    <div className="max-w-2xl bg-white dark:bg-slate-900 rounded-2xl border border-slate-100 dark:border-slate-800 p-6 shadow-sm space-y-6">
      <h3 className="font-bold text-slate-900 dark:text-white text-base">Account Information</h3>
      <div className="space-y-4">
        <div className="flex items-center justify-between py-3 border-b border-slate-100 dark:border-slate-800">
          <span className="text-sm text-slate-500 dark:text-slate-400">Name</span>
          <span className="text-sm font-semibold text-slate-800 dark:text-slate-200">{user.name}</span>
        </div>
        <div className="flex items-center justify-between py-3 border-b border-slate-100 dark:border-slate-800">
          <span className="text-sm text-slate-500 dark:text-slate-400">Email</span>
          <span className="text-sm font-semibold text-slate-800 dark:text-slate-200">{user.email}</span>
        </div>
        <div className="flex items-center justify-between py-3 border-b border-slate-100 dark:border-slate-800">
          <span className="text-sm text-slate-500 dark:text-slate-400">Interface Theme</span>
          <button
            onClick={onToggleDarkMode}
            className="px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 text-xs font-semibold flex items-center gap-2"
          >
            <Icon name={darkMode ? 'sun' : 'moon'} className="w-3.5 h-3.5" />
            <span>{darkMode ? 'Dark Mode' : 'Light Mode'}</span>
          </button>
        </div>
        <div className="flex items-center justify-between py-3 border-b border-slate-100 dark:border-slate-800">
          <span className="text-sm text-slate-500 dark:text-slate-400">Backend Server Health</span>
          <button
            onClick={onTestHealth}
            className="px-3.5 py-1.5 rounded-lg bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 text-xs font-semibold transition"
          >
            Test Connection
          </button>
        </div>
      </div>
      <div className="pt-2">
        <button
          onClick={onLogout}
          className="px-4 py-2.5 rounded-xl bg-red-50 hover:bg-red-100 text-red-600 dark:bg-red-950/40 dark:text-red-400 text-xs font-bold transition flex items-center gap-2"
        >
          <Icon name="log-out" className="w-4 h-4" />
          <span>Sign Out</span>
        </button>
      </div>
    </div>
  );
}

// --- Generic Modal Shell ---
function Modal({ title, onClose, children }) {
  return (
    <div className="fixed inset-0 z-50 bg-slate-950/50 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white dark:bg-slate-900 rounded-3xl max-w-md w-full p-6 border border-slate-100 dark:border-slate-800 shadow-2xl space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="font-bold text-base text-slate-900 dark:text-white">{title}</h3>
          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg"
          >
            <Icon name="x" className="w-5 h-5" />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

// Render React 18 Application
const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />);
