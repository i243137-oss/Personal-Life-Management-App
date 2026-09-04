package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.CategoriesDataDto
import com.example.data.model.CategoryItemDto
import com.example.data.model.DashboardData
import com.example.data.model.ExpenseBreakdownItem
import com.example.data.model.LoanDto
import com.example.data.model.LoanSummaryData
import com.example.data.model.QuickPresetDto
import com.example.data.model.RepaymentDto
import com.example.data.model.TransactionDto
import com.example.data.model.TransactionStatsData
import com.example.data.model.VocabularyStatsData
import com.example.data.model.WordItemDto
import com.example.data.model.WordLookupResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Embedded Local Data Manager for offline-first resilience.
 * Ensures the app functions smoothly even when the external backend
 * (such as 10.0.2.2:5000) is unreachable from the Android streaming emulator.
 */
class LocalDataManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("personal_life_manager_local_db", Context.MODE_PRIVATE)

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val transactionListType =
        Types.newParameterizedType(List::class.java, TransactionDto::class.java)
    private val loanListType =
        Types.newParameterizedType(List::class.java, LoanDto::class.java)
    private val wordListType =
        Types.newParameterizedType(List::class.java, WordItemDto::class.java)

    private val transactionAdapter = moshi.adapter<List<TransactionDto>>(transactionListType)
    private val loanAdapter = moshi.adapter<List<LoanDto>>(loanListType)
    private val wordAdapter = moshi.adapter<List<WordItemDto>>(wordListType)

    private val _transactionsFlow = MutableStateFlow<List<TransactionDto>>(emptyList())
    val transactionsFlow: Flow<List<TransactionDto>> = _transactionsFlow.asStateFlow()

    private val _loansFlow = MutableStateFlow<List<LoanDto>>(emptyList())
    val loansFlow: Flow<List<LoanDto>> = _loansFlow.asStateFlow()

    private val _wordsFlow = MutableStateFlow<List<WordItemDto>>(emptyList())
    val wordsFlow: Flow<List<WordItemDto>> = _wordsFlow.asStateFlow()

    init {
        loadFromPrefs()
        if (_transactionsFlow.value.isEmpty() && _loansFlow.value.isEmpty()) {
            seedDefaultDemoData()
        }
    }

    private fun loadFromPrefs() {
        val txJson = prefs.getString(KEY_TRANSACTIONS, null)
        if (!txJson.isNullOrBlank()) {
            try {
                val list = transactionAdapter.fromJson(txJson) ?: emptyList()
                _transactionsFlow.value = list
            } catch (_: Exception) {
            }
        }

        val loanJson = prefs.getString(KEY_LOANS, null)
        if (!loanJson.isNullOrBlank()) {
            try {
                val list = loanAdapter.fromJson(loanJson) ?: emptyList()
                _loansFlow.value = list
            } catch (_: Exception) {
            }
        }

        val wordJson = prefs.getString(KEY_WORDS, null)
        if (!wordJson.isNullOrBlank()) {
            try {
                val list = wordAdapter.fromJson(wordJson) ?: emptyList()
                _wordsFlow.value = list
            } catch (_: Exception) {
            }
        }
    }

    private fun saveTransactions(list: List<TransactionDto>) {
        _transactionsFlow.value = list
        try {
            val json = transactionAdapter.toJson(list)
            prefs.edit().putString(KEY_TRANSACTIONS, json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveLoans(list: List<LoanDto>) {
        _loansFlow.value = list
        try {
            val json = loanAdapter.toJson(list)
            prefs.edit().putString(KEY_LOANS, json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveWords(list: List<WordItemDto>) {
        _wordsFlow.value = list
        try {
            val json = wordAdapter.toJson(list)
            prefs.edit().putString(KEY_WORDS, json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun seedDefaultDemoData() {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())

        val demoTransactions = listOf(
            TransactionDto(
                id = "tx_1",
                type = "income",
                amount = 125000.0,
                category = "Salary",
                description = "Monthly Corporate Salary",
                date = now
            ),
            TransactionDto(
                id = "tx_2",
                type = "income",
                amount = 28000.0,
                category = "Freelance",
                description = "Mobile App UI Design Project",
                date = now
            ),
            TransactionDto(
                id = "tx_3",
                type = "expense",
                amount = 16500.0,
                category = "Groceries",
                description = "Monthly Imtiaz Super Market",
                date = now
            ),
            TransactionDto(
                id = "tx_4",
                type = "expense",
                amount = 7200.0,
                category = "Transport & Fuel",
                description = "Petrol PSO refill",
                date = now
            ),
            TransactionDto(
                id = "tx_5",
                type = "expense",
                amount = 4800.0,
                category = "Dining Out",
                description = "Dinner with family",
                date = now
            ),
            TransactionDto(
                id = "tx_6",
                type = "expense",
                amount = 11400.0,
                category = "Bills & Utilities",
                description = "Electricity bill & Fiber Internet",
                date = now
            )
        )

        val demoLoans = listOf(
            LoanDto(
                id = "loan_1",
                personName = "Hamza Ali",
                phoneNumber = "03001234567",
                type = "lent",
                amount = 15000.0,
                remainingAmount = 10000.0,
                status = "partially_paid",
                dueDate = "2026-09-25",
                notes = "Shared car engine overhaul",
                repayments = listOf(
                    RepaymentDto(
                        id = "rep_1",
                        amount = 5000.0,
                        date = now,
                        notes = "JazzCash transfer"
                    )
                ),
                createdAt = now
            ),
            LoanDto(
                id = "loan_2",
                personName = "Zainab Bibi",
                phoneNumber = "03219876543",
                type = "lent",
                amount = 8000.0,
                remainingAmount = 8000.0,
                status = "pending",
                dueDate = "2026-10-05",
                notes = "University semester books",
                repayments = emptyList(),
                createdAt = now
            ),
            LoanDto(
                id = "loan_3",
                personName = "Usman Ghani",
                phoneNumber = "03335554433",
                type = "borrowed",
                amount = 12000.0,
                remainingAmount = 12000.0,
                status = "pending",
                dueDate = "2026-09-20",
                notes = "Emergency medical expense cash",
                repayments = emptyList(),
                createdAt = now
            )
        )

        val demoWords = listOf(
            WordItemDto(
                id = "word_1",
                word = "Resilience",
                mode = "meaning",
                phonetic = "/rɪˈzɪl.jəns/",
                partOfSpeech = "noun",
                shortDefinition = "The capacity to withstand or to recover quickly from difficulties; toughness.",
                fullDefinition = "The ability of an individual, organization, or system to adapt successfully to stress, adversity, trauma, or significant sources of threat, emerging stronger and more resourceful.",
                synonyms = listOf("toughness", "adaptability", "endurance", "grit", "buoyancy", "flexibility"),
                antonyms = listOf("fragility", "vulnerability", "weakness", "rigidity"),
                examples = listOf(
                    "Her mental resilience helped her overcome severe setbacks and complete her medical degree.",
                    "Building economic resilience requires diversifying revenue streams across industries.",
                    "The bamboo tree is known for its remarkable resilience during severe monsoon storms."
                ),
                keyPoints = listOf(
                    "Resilience is an active, learned behavior rather than a static genetic trait.",
                    "It involves psychological flexibility, emotional regulation, and social support networks.",
                    "Fostering resilience prevents chronic burnout and accelerates professional recovery."
                ),
                eli5Analogy = "Like a rubber ball that gets squeezed or bounced hard against the floor, but immediately pops right back into its original round shape.",
                keyTakeaway = "Challenges are inevitable, but our capacity to adapt, recover, and rebound is entirely trainable through deliberate reflection and endurance.",
                masteryStatus = "mastered",
                createdAt = now
            ),
            WordItemDto(
                id = "word_2",
                word = "Serendipity",
                mode = "meaning",
                phonetic = "/ˌser.ənˈdɪp.ə.ti/",
                partOfSpeech = "noun",
                shortDefinition = "The occurrence and development of events by chance in a happy or beneficial way.",
                fullDefinition = "The fortunate occurrence of discovering desirable, valuable, or agreeable things when least expected, often while searching for something entirely different.",
                synonyms = listOf("chance", "happy accident", "fluke", "good fortune", "providence", "luck"),
                antonyms = listOf("misfortune", "design", "deliberation", "misadventure"),
                examples = listOf(
                    "Penicillin was discovered through pure serendipity when Fleming observed mold inhibiting bacteria.",
                    "A chance meeting at a coffee shop led to a serendipitous multi-million-dollar partnership."
                ),
                keyPoints = listOf(
                    "Serendipity favors the prepared mind: observing the unexpected requires active curiosity.",
                    "You can increase your serendipity surface area by meeting diverse people and sharing ideas."
                ),
                eli5Analogy = "Looking through your winter coat pockets for a tissue, and unexpectedly pulling out a crisp 1000-rupee note you forgot you had.",
                keyTakeaway = "Keep your curiosity high; the most transformative opportunities in life frequently disguise themselves as happy accidents.",
                masteryStatus = "learning",
                createdAt = now
            ),
            WordItemDto(
                id = "word_3",
                word = "Pragmatic",
                mode = "meaning",
                phonetic = "/præɡˈmæt.ɪk/",
                partOfSpeech = "adjective",
                shortDefinition = "Dealing with things sensibly and realistically based on practical rather than theoretical considerations.",
                fullDefinition = "Evaluating theories or beliefs in terms of the success of their practical application; guided by measurable outcomes rather than rigid ideology.",
                synonyms = listOf("practical", "sensible", "realistic", "down-to-earth", "utilitarian"),
                antonyms = listOf("idealistic", "impractical", "dogmatic", "unrealistic"),
                examples = listOf(
                    "We took a pragmatic approach to the software deadline, focusing on essential features first.",
                    "A pragmatic budget prioritizes food, rent, and emergency savings before luxury upgrades."
                ),
                keyPoints = listOf(
                    "Pragmatism bridges the gap between ambitious vision and actual feasibility.",
                    "Essential for effective project management and financial stewardship."
                ),
                eli5Analogy = "If it's pouring rain outside, buying a sturdy umbrella that works right away instead of waiting weeks to design a high-tech rain suit.",
                keyTakeaway = "Actionable, sensible progress in the real world will always outvalue theoretical perfection that never gets shipped.",
                masteryStatus = "reviewing",
                createdAt = now
            )
        )

        saveTransactions(demoTransactions)
        saveLoans(demoLoans)
        saveWords(demoWords)
    }

    // --- Transactions Methods ---

    fun getTransactions(): List<TransactionDto> = _transactionsFlow.value

    fun addTransaction(
        type: String,
        amount: Double,
        category: String,
        description: String?,
        date: String?
    ): TransactionDto {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        val tx = TransactionDto(
            id = "tx_" + UUID.randomUUID().toString().take(8),
            type = type,
            amount = amount,
            category = category,
            description = description,
            date = date ?: now
        )
        val current = _transactionsFlow.value.toMutableList()
        current.add(0, tx)
        saveTransactions(current)
        return tx
    }

    fun deleteTransaction(id: String): Boolean {
        val current = _transactionsFlow.value.toMutableList()
        val removed = current.removeAll { it.effectiveId == id || it.id == id }
        if (removed) {
            saveTransactions(current)
        }
        return removed
    }

    // --- Loans Methods ---

    fun getLoans(): List<LoanDto> = _loansFlow.value

    fun addLoan(
        personName: String,
        phoneNumber: String?,
        type: String,
        amount: Double,
        dueDate: String?,
        notes: String?,
        affectBalance: Boolean
    ): LoanDto {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        val loan = LoanDto(
            id = "loan_" + UUID.randomUUID().toString().take(8),
            personName = personName,
            phoneNumber = phoneNumber,
            type = type,
            amount = amount,
            remainingAmount = amount,
            status = "pending",
            dueDate = dueDate,
            notes = notes,
            repayments = emptyList(),
            createdAt = now
        )
        val current = _loansFlow.value.toMutableList()
        current.add(0, loan)
        saveLoans(current)

        if (affectBalance) {
            val txType = if (type == "lent") "expense" else "income"
            val desc = if (type == "lent") "Udhar lent to $personName" else "Udhar borrowed from $personName"
            addTransaction(type = txType, amount = amount, category = "Loan", description = desc, date = now)
        }

        return loan
    }

    fun recordRepayment(
        loanId: String,
        amount: Double,
        notes: String?
    ): LoanDto? {
        val current = _loansFlow.value.toMutableList()
        val idx = current.indexOfFirst { it.id == loanId }
        if (idx == -1) return null

        val old = current[idx]
        val newRemaining = (old.remainingAmount - amount).coerceAtLeast(0.0)
        val newStatus = if (newRemaining <= 0.0) "paid" else "partially_paid"
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())

        val rep = RepaymentDto(
            id = "rep_" + UUID.randomUUID().toString().take(8),
            amount = amount,
            date = now,
            notes = notes
        )
        val reps = old.repayments.toMutableList()
        reps.add(0, rep)

        val updated = old.copy(
            remainingAmount = newRemaining,
            status = newStatus,
            repayments = reps
        )
        current[idx] = updated
        saveLoans(current)

        // Automatically sync transaction balance
        val txType = if (old.type == "lent") "income" else "expense"
        val desc = if (old.type == "lent") "Udhar repayment from ${old.personName}" else "Udhar repayment to ${old.personName}"
        addTransaction(type = txType, amount = amount, category = "Loan Repayment", description = desc, date = now)

        return updated
    }

    fun updateLoanStatus(loanId: String, status: String): LoanDto? {
        val current = _loansFlow.value.toMutableList()
        val idx = current.indexOfFirst { it.id == loanId }
        if (idx == -1) return null

        val old = current[idx]
        val newRemaining = if (status == "paid") 0.0 else old.remainingAmount
        val updated = old.copy(status = status, remainingAmount = newRemaining)
        current[idx] = updated
        saveLoans(current)
        return updated
    }

    fun deleteLoan(loanId: String): Boolean {
        val current = _loansFlow.value.toMutableList()
        val removed = current.removeAll { it.id == loanId }
        if (removed) {
            saveLoans(current)
        }
        return removed
    }

    fun getLoanSummary(): LoanSummaryData {
        val loans = _loansFlow.value
        var totalLent = 0.0
        var totalBorrowed = 0.0
        var activeLent = 0
        var activeBorrowed = 0
        var settled = 0

        loans.forEach { loan ->
            val remaining = loan.remainingAmount
            if (loan.status == "paid" || remaining <= 0.0) {
                settled++
            } else {
                if (loan.type == "lent") {
                    totalLent += remaining
                    activeLent++
                } else {
                    totalBorrowed += remaining
                    activeBorrowed++
                }
            }
        }

        return LoanSummaryData(
            totalLent = totalLent,
            totalBorrowed = totalBorrowed,
            netPosition = totalLent - totalBorrowed,
            activeLentCount = activeLent,
            activeBorrowedCount = activeBorrowed,
            settledCount = settled,
            overdueCount = 0,
            totalCount = loans.size
        )
    }

    fun getCategories(): CategoriesDataDto {
        return CategoriesDataDto(
            expense = listOf(
                CategoryItemDto(name = "Groceries", icon = "shopping_bag", color = "#10B981"),
                CategoryItemDto(name = "Transport & Fuel", icon = "directions_car", color = "#3B82F6"),
                CategoryItemDto(name = "Dining Out", icon = "restaurant", color = "#F59E0B"),
                CategoryItemDto(name = "Bills & Utilities", icon = "receipt_long", color = "#EF4444"),
                CategoryItemDto(name = "Health & Medical", icon = "medical_services", color = "#EC4899"),
                CategoryItemDto(name = "Entertainment", icon = "movie", color = "#8B5CF6"),
                CategoryItemDto(name = "Shopping", icon = "shopping_cart", color = "#14B8A6"),
                CategoryItemDto(name = "Other Expense", icon = "more_horiz", color = "#64748B")
            ),
            income = listOf(
                CategoryItemDto(name = "Salary", icon = "payments", color = "#10B981"),
                CategoryItemDto(name = "Freelance", icon = "laptop", color = "#3B82F6"),
                CategoryItemDto(name = "Business", icon = "storefront", color = "#F59E0B"),
                CategoryItemDto(name = "Investment", icon = "trending_up", color = "#8B5CF6"),
                CategoryItemDto(name = "Other Income", icon = "more_horiz", color = "#64748B")
            ),
            quickPresets = listOf(
                QuickPresetDto(label = "Chai / Tea", category = "Dining Out", amount = 100.0, type = "expense"),
                QuickPresetDto(label = "Petrol Refill", category = "Transport & Fuel", amount = 1000.0, type = "expense"),
                QuickPresetDto(label = "Biryani Lunch", category = "Dining Out", amount = 450.0, type = "expense"),
                QuickPresetDto(label = "Grocery Run", category = "Groceries", amount = 2500.0, type = "expense")
            )
        )
    }

    fun getTransactionStats(): TransactionStatsData {
        val txs = _transactionsFlow.value
        var totalIncome = 0.0
        var totalExpense = 0.0
        val categoryTotals = mutableMapOf<String, Double>()

        txs.forEach { tx ->
            if (tx.type == "income") totalIncome += tx.amount
            else if (tx.type == "expense") {
                totalExpense += tx.amount
                categoryTotals[tx.category] = (categoryTotals[tx.category] ?: 0.0) + tx.amount
            }
        }

        val breakdown = categoryTotals.entries.map {
            val pct = if (totalExpense > 0) ((it.value / totalExpense) * 100).toInt() else 0
            ExpenseBreakdownItem(
                category = it.key,
                amount = it.value,
                percentage = pct
            )
        }.sortedByDescending { it.amount }

        return TransactionStatsData(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            expenseBreakdown = breakdown
        )
    }

    fun getDashboardData(): DashboardData {
        val txs = _transactionsFlow.value
        val loans = _loansFlow.value

        var totalIncome = 0.0
        var totalExpenses = 0.0
        var todayExpenses = 0.0

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        txs.forEach { tx ->
            if (tx.type == "income") {
                totalIncome += tx.amount
            } else if (tx.type == "expense") {
                totalExpenses += tx.amount
                if (tx.date?.startsWith(todayStr) == true) {
                    todayExpenses += tx.amount
                }
            }
        }

        val currentBalance = totalIncome - totalExpenses

        var youOwe = 0.0
        var othersOwe = 0.0
        loans.forEach { loan ->
            if (loan.status != "paid" && loan.remainingAmount > 0.0) {
                if (loan.type == "lent") othersOwe += loan.remainingAmount
                else youOwe += loan.remainingAmount
            }
        }

        return DashboardData(
            currentBalance = currentBalance,
            todayExpenses = todayExpenses,
            youOwe = youOwe,
            othersOwe = othersOwe,
            recentActivity = txs.take(5)
        )
    }

    // --- Phase 4: AI Smart Dictionary & Vocabulary Methods ---

    private val curatedWordsMap = mapOf(
        "resilience" to WordLookupResult(
            word = "Resilience",
            mode = "meaning",
            phonetic = "/rɪˈzɪl.jəns/",
            partOfSpeech = "noun",
            shortDefinition = "The capacity to withstand or to recover quickly from difficulties; toughness.",
            fullDefinition = "The ability of an individual, organization, or system to adapt successfully to stress, adversity, trauma, or significant sources of threat, emerging stronger and more resourceful.",
            synonyms = listOf("toughness", "adaptability", "endurance", "grit", "buoyancy", "flexibility"),
            antonyms = listOf("fragility", "vulnerability", "weakness", "rigidity"),
            examples = listOf(
                "Her mental resilience helped her overcome severe setbacks and complete her medical degree.",
                "Building economic resilience requires diversifying revenue streams across industries.",
                "The bamboo tree is known for its remarkable resilience during severe monsoon storms."
            ),
            keyPoints = listOf(
                "Resilience is an active, learned behavior rather than a static genetic trait.",
                "It involves psychological flexibility, emotional regulation, and social support networks.",
                "Fostering resilience prevents chronic burnout and accelerates professional recovery."
            ),
            eli5Analogy = "Like a rubber ball that gets squeezed or bounced hard against the floor, but immediately pops right back into its original round shape.",
            keyTakeaway = "Challenges are inevitable, but our capacity to adapt, recover, and rebound is entirely trainable through deliberate reflection and endurance."
        ),
        "serendipity" to WordLookupResult(
            word = "Serendipity",
            mode = "meaning",
            phonetic = "/ˌser.ənˈdɪp.ə.ti/",
            partOfSpeech = "noun",
            shortDefinition = "The occurrence and development of events by chance in a happy or beneficial way.",
            fullDefinition = "The fortunate occurrence of discovering desirable, valuable, or agreeable things when least expected, often while searching for something entirely different.",
            synonyms = listOf("chance", "happy accident", "fluke", "good fortune", "providence", "luck"),
            antonyms = listOf("misfortune", "design", "deliberation", "misadventure"),
            examples = listOf(
                "Penicillin was discovered through pure serendipity when Fleming observed mold inhibiting bacteria.",
                "A chance meeting at a coffee shop led to a serendipitous multi-million-dollar partnership."
            ),
            keyPoints = listOf(
                "Serendipity favors the prepared mind: observing the unexpected requires active curiosity.",
                "You can increase your serendipity surface area by meeting diverse people and sharing ideas publicly."
            ),
            eli5Analogy = "Looking through your winter coat pockets for a tissue, and unexpectedly pulling out a crisp 1000-rupee note you forgot you had.",
            keyTakeaway = "Keep your curiosity high; the most transformative opportunities in life frequently disguise themselves as happy accidents."
        ),
        "ephemeral" to WordLookupResult(
            word = "Ephemeral",
            mode = "meaning",
            phonetic = "/ɪˈfem.ər.əl/",
            partOfSpeech = "adjective",
            shortDefinition = "Lasting for a very short time; transient or fleeting.",
            fullDefinition = "Existing, lasting, or recurring for only a brief period of time; possessing a temporary or momentary existence.",
            synonyms = listOf("fleeting", "transient", "momentary", "evanescent", "short-lived", "impermanent"),
            antonyms = listOf("permanent", "enduring", "eternal", "perpetual", "everlasting"),
            examples = listOf(
                "The ephemeral beauty of cherry blossoms draws millions of admirers each spring.",
                "Fame on social media can be extraordinarily ephemeral without lasting craftsmanship.",
                "Morning dew on the lawn is an ephemeral phenomenon that vanishes under the sunrise."
            ),
            keyPoints = listOf(
                "Derived from the Greek word 'ephemeros' meaning 'lasting only a day'.",
                "In art and literature, ephemerality often intensifies emotional poignancy and value.",
                "Understanding that unpleasant moments are ephemeral helps maintain emotional perspective."
            ),
            eli5Analogy = "Blowing soap bubbles in the afternoon breeze: they shine with gorgeous rainbow colors, but pop in just a few seconds.",
            keyTakeaway = "Embrace the present moment; recognizing the fleeting nature of life makes genuine experiences all the more precious."
        ),
        "pragmatic" to WordLookupResult(
            word = "Pragmatic",
            mode = "meaning",
            phonetic = "/præɡˈmæt.ɪk/",
            partOfSpeech = "adjective",
            shortDefinition = "Dealing with things sensibly and realistically based on practical rather than theoretical considerations.",
            fullDefinition = "Evaluating theories or beliefs in terms of the success of their practical application; guided by measurable outcomes rather than rigid ideology.",
            synonyms = listOf("practical", "sensible", "realistic", "down-to-earth", "utilitarian"),
            antonyms = listOf("idealistic", "impractical", "dogmatic", "unrealistic"),
            examples = listOf(
                "We took a pragmatic approach to the software deadline, focusing on essential features first.",
                "A pragmatic budget prioritizes food, rent, and emergency savings before luxury upgrades."
            ),
            keyPoints = listOf(
                "Pragmatism bridges the gap between ambitious vision and actual feasibility.",
                "Focuses on 'what actually works' in the real world rather than what sounds perfect on paper.",
                "Essential for effective project management and financial stewardship."
            ),
            eli5Analogy = "If it's pouring rain outside, buying a sturdy umbrella that works right away instead of waiting weeks to design a high-tech rain suit.",
            keyTakeaway = "Actionable, sensible progress in the real world will always outvalue theoretical perfection that never gets shipped."
        ),
        "eloquent" to WordLookupResult(
            word = "Eloquent",
            mode = "meaning",
            phonetic = "/ˈel.ə.kwənt/",
            partOfSpeech = "adjective",
            shortDefinition = "Fluent or persuasive in speaking or writing; clearly expressing feelings or meaning.",
            fullDefinition = "Characterized by forceful, fluent, and expressive language that touches, inspires, or convinces listeners and readers.",
            synonyms = listOf("articulate", "expressive", "fluent", "persuasive", "poignant", "vivid"),
            antonyms = listOf("inarticulate", "tongue-tied", "clumsy", "hesitant"),
            examples = listOf(
                "The leader delivered an eloquent speech that moved the entire audience to tears.",
                "Her silence was far more eloquent than any words could have possibly conveyed."
            ),
            keyPoints = listOf(
                "Eloquence is not just about big words; it is about choosing the exact right words with emotional resonance.",
                "Timing and vocal cadence play as large a role as written vocabulary."
            ),
            eli5Analogy = "Telling a bedtime story so vividly and smoothly that everyone listening can picture the dragons and castles in their head.",
            keyTakeaway = "True eloquence is clarity combined with heart; express complex ideas simply and sincerely."
        ),
        "tenacity" to WordLookupResult(
            word = "Tenacity",
            mode = "meaning",
            phonetic = "/təˈnæs.ə.ti/",
            partOfSpeech = "noun",
            shortDefinition = "The quality or fact of being able to grip something firmly; determination and perseverance.",
            fullDefinition = "The mental or moral strength to resist opposition, danger, or fatigue; persistently clinging to an objective despite discouragement.",
            synonyms = listOf("determination", "persistence", "perseverance", "grit", "stubbornness", "resolve"),
            antonyms = listOf("irresolution", "hesitation", "wavering", "surrender"),
            examples = listOf(
                "His sheer tenacity carried him through years of unfunded research to the final patent.",
                "You need unrelenting tenacity to build a sustainable business in competitive markets."
            ),
            keyPoints = listOf(
                "Tenacity differs from stubbornness because it remains focused on the goal while adapting methods.",
                "Drives long-term compounding across personal, athletic, and cognitive endeavors."
            ),
            eli5Analogy = "A little puppy holding onto a tug-of-war rope with both paws, refusing to let go no matter how hard you gently pull.",
            keyTakeaway = "Talent provides the launchpad, but unwavering tenacity is what actually crosses the finish line."
        )
    )

    fun lookupWord(rawWord: String, mode: String = "meaning"): WordLookupResult {
        val wordTrimmed = rawWord.trim()
        val key = wordTrimmed.lowercase()

        val baseResult = curatedWordsMap[key] ?: run {
            val capitalized = wordTrimmed.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            WordLookupResult(
                word = capitalized,
                mode = mode,
                phonetic = "/$key/",
                partOfSpeech = "noun / concept",
                shortDefinition = "A meaningful term representing $key and its practical manifestations.",
                fullDefinition = "$capitalized represents a core conceptual framework within modern language, frequently analyzed across intellectual, professional, and personal growth contexts.",
                synonyms = listOf("concept", "principle", "construct", "framework", "facet"),
                antonyms = listOf("counterpart", "antithesis"),
                examples = listOf(
                    "Mastering the principles of $key provides structured clarity when evaluating decisions.",
                    "The team explored $key to establish standard practices across the project."
                ),
                keyPoints = listOf(
                    "Provides foundational perspective when navigating nuanced situations.",
                    "Connects conceptual theory with actionable real-world execution."
                ),
                eli5Analogy = "Like having a magnifying glass that lets you see tiny details in a big puzzle you couldn't spot before.",
                keyTakeaway = "Deep comprehension of $key elevates how you express ideas and solve practical problems."
            )
        }

        // Check if user has saved this word
        val savedWord = _wordsFlow.value.find { it.word.equals(wordTrimmed, ignoreCase = true) }

        return baseResult.copy(
            mode = mode,
            isSaved = savedWord != null,
            savedWordId = savedWord?.id,
            masteryStatus = savedWord?.masteryStatus
        )
    }

    fun saveWord(
        word: String,
        mode: String,
        phonetic: String?,
        partOfSpeech: String?,
        shortDefinition: String?,
        fullDefinition: String?,
        synonyms: List<String>,
        antonyms: List<String>,
        examples: List<String>,
        keyPoints: List<String>,
        eli5Analogy: String?,
        keyTakeaway: String?,
        masteryStatus: String = "learning",
        personalNotes: String? = null
    ): WordItemDto {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        val current = _wordsFlow.value.toMutableList()
        val idx = current.indexOfFirst { it.word.equals(word.trim(), ignoreCase = true) }

        val wordItem = if (idx != -1) {
            val existing = current[idx]
            val updated = existing.copy(
                mode = mode,
                phonetic = phonetic ?: existing.phonetic,
                partOfSpeech = partOfSpeech ?: existing.partOfSpeech,
                shortDefinition = shortDefinition ?: existing.shortDefinition,
                fullDefinition = fullDefinition ?: existing.fullDefinition,
                synonyms = if (synonyms.isNotEmpty()) synonyms else existing.synonyms,
                antonyms = if (antonyms.isNotEmpty()) antonyms else existing.antonyms,
                examples = if (examples.isNotEmpty()) examples else existing.examples,
                keyPoints = if (keyPoints.isNotEmpty()) keyPoints else existing.keyPoints,
                eli5Analogy = eli5Analogy ?: existing.eli5Analogy,
                keyTakeaway = keyTakeaway ?: existing.keyTakeaway,
                masteryStatus = masteryStatus,
                personalNotes = personalNotes ?: existing.personalNotes
            )
            current[idx] = updated
            updated
        } else {
            val newItem = WordItemDto(
                id = "word_" + UUID.randomUUID().toString().take(8),
                word = word.trim(),
                mode = mode,
                phonetic = phonetic,
                partOfSpeech = partOfSpeech,
                shortDefinition = shortDefinition,
                fullDefinition = fullDefinition,
                synonyms = synonyms,
                antonyms = antonyms,
                examples = examples,
                keyPoints = keyPoints,
                eli5Analogy = eli5Analogy,
                keyTakeaway = keyTakeaway,
                masteryStatus = masteryStatus,
                personalNotes = personalNotes,
                createdAt = now
            )
            current.add(0, newItem)
            newItem
        }

        saveWords(current)
        return wordItem
    }

    fun getLearnedWords(status: String? = null, search: String? = null): List<WordItemDto> {
        var list = _wordsFlow.value
        if (!status.isNullOrBlank() && status != "all") {
            list = list.filter { it.masteryStatus.equals(status, ignoreCase = true) }
        }
        if (!search.isNullOrBlank()) {
            val q = search.lowercase()
            list = list.filter {
                it.word.lowercase().contains(q) ||
                (it.shortDefinition?.lowercase()?.contains(q) == true) ||
                (it.partOfSpeech?.lowercase()?.contains(q) == true)
            }
        }
        return list
    }

    fun updateWordMastery(id: String, status: String, personalNotes: String? = null): WordItemDto? {
        val current = _wordsFlow.value.toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx == -1) return null

        val old = current[idx]
        val updated = old.copy(
            masteryStatus = status,
            personalNotes = personalNotes ?: old.personalNotes
        )
        current[idx] = updated
        saveWords(current)
        return updated
    }

    fun deleteLearnedWord(id: String): Boolean {
        val current = _wordsFlow.value.toMutableList()
        val removed = current.removeAll { it.id == id }
        if (removed) {
            saveWords(current)
        }
        return removed
    }

    fun getVocabularyStats(): VocabularyStatsData {
        val list = _wordsFlow.value
        val total = list.size
        val mastered = list.count { it.masteryStatus.equals("mastered", ignoreCase = true) }
        val reviewing = list.count { it.masteryStatus.equals("reviewing", ignoreCase = true) }
        val learning = list.count { it.masteryStatus.equals("learning", ignoreCase = true) }
        val percentage = if (total > 0) ((mastered.toDouble() / total) * 100).toInt() else 0

        return VocabularyStatsData(
            total = total,
            mastered = mastered,
            reviewing = reviewing,
            learning = learning,
            masteryPercentage = percentage
        )
    }

    companion object {
        private const val KEY_TRANSACTIONS = "local_transactions_v1"
        private const val KEY_LOANS = "local_loans_v1"
        private const val KEY_WORDS = "local_words_v1"
    }
}
