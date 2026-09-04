package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.VocabularyStatsData
import com.example.data.model.WordItemDto
import com.example.data.model.WordLookupResult
import com.example.data.repository.DictionaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DictionaryUiState(
    val selectedTab: Int = 0, // 0 = Search & Learn, 1 = Learned Notebook
    val searchQuery: String = "",
    val activeMode: String = "meaning", // "meaning" or "explain"
    val isLookingUp: Boolean = false,
    val lookupResult: WordLookupResult? = null,
    val lookupError: String? = null,
    val actionMessage: String? = null,
    // Notebook
    val filterStatus: String = "all", // "all", "learning", "reviewing", "mastered"
    val notebookSearch: String = "",
    val learnedWords: List<WordItemDto> = emptyList(),
    val stats: VocabularyStatsData = VocabularyStatsData(),
    val isLoadingNotebook: Boolean = false,
    // Quiz / Flashcard Mode
    val isQuizActive: Boolean = false,
    val quizWords: List<WordItemDto> = emptyList(),
    val currentQuizIndex: Int = 0,
    val isCardFlipped: Boolean = false,
    val quizCompleted: Boolean = false,
    val quizLearnedInSession: Int = 0
)

class DictionaryViewModel(
    private val dictionaryRepository: DictionaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DictionaryUiState())
    val uiState: StateFlow<DictionaryUiState> = _uiState.asStateFlow()

    init {
        // Collect live changes from repository
        viewModelScope.launch {
            dictionaryRepository.learnedWordsFlow.collect {
                refreshNotebookData()
            }
        }
        // Initial lookup for demonstration
        lookupWord("Resilience", "meaning")
    }

    fun selectTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (tab == 1) {
            refreshNotebookData()
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query, lookupError = null) }
    }

    fun setMode(mode: String) {
        _uiState.update { it.copy(activeMode = mode) }
        val current = _uiState.value.lookupResult
        if (current != null) {
            lookupWord(current.word, mode)
        }
    }

    fun lookupWord(wordToLookup: String? = null, modeToUse: String? = null) {
        val word = (wordToLookup ?: _uiState.value.searchQuery).trim()
        if (word.isBlank()) {
            _uiState.update { it.copy(lookupError = "Please enter a word to search") }
            return
        }

        val mode = modeToUse ?: _uiState.value.activeMode
        _uiState.update {
            it.copy(
                isLookingUp = true,
                lookupError = null,
                searchQuery = word,
                activeMode = mode
            )
        }

        viewModelScope.launch {
            val result = dictionaryRepository.lookupWord(word, mode)
            result.fold(
                onSuccess = { data ->
                    _uiState.update {
                        it.copy(
                            isLookingUp = false,
                            lookupResult = data,
                            lookupError = null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLookingUp = false,
                            lookupError = err.message ?: "Failed to look up definition"
                        )
                    }
                }
            )
        }
    }

    fun saveCurrentLookup(masteryStatus: String = "learning") {
        val result = _uiState.value.lookupResult ?: return
        viewModelScope.launch {
            val saveResult = dictionaryRepository.saveWord(
                word = result.word,
                mode = result.mode,
                phonetic = result.phonetic,
                partOfSpeech = result.partOfSpeech,
                shortDefinition = result.shortDefinition,
                fullDefinition = result.fullDefinition,
                synonyms = result.synonyms,
                antonyms = result.antonyms,
                examples = result.examples,
                keyPoints = result.keyPoints,
                eli5Analogy = result.eli5Analogy,
                keyTakeaway = result.keyTakeaway,
                masteryStatus = masteryStatus
            )
            saveResult.fold(
                onSuccess = { savedDto ->
                    _uiState.update {
                        it.copy(
                            lookupResult = result.copy(
                                isSaved = true,
                                savedWordId = savedDto.id,
                                masteryStatus = savedDto.masteryStatus
                            ),
                            actionMessage = "\"${result.word}\" saved to Vocabulary Notebook"
                        )
                    }
                    refreshNotebookData()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(actionMessage = "Failed to save: ${err.message}") }
                }
            )
        }
    }

    fun removeCurrentLookupFromNotebook() {
        val result = _uiState.value.lookupResult ?: return
        val id = result.savedWordId ?: return
        viewModelScope.launch {
            dictionaryRepository.deleteLearnedWord(id)
            _uiState.update {
                it.copy(
                    lookupResult = result.copy(
                        isSaved = false,
                        savedWordId = null,
                        masteryStatus = null
                    ),
                    actionMessage = "Removed from Vocabulary Notebook"
                )
            }
            refreshNotebookData()
        }
    }

    fun setNotebookFilter(status: String) {
        _uiState.update { it.copy(filterStatus = status) }
        refreshNotebookData()
    }

    fun setNotebookSearch(query: String) {
        _uiState.update { it.copy(notebookSearch = query) }
        refreshNotebookData()
    }

    fun updateWordMastery(id: String, status: String, notes: String? = null) {
        viewModelScope.launch {
            val res = dictionaryRepository.updateMastery(id, status, notes)
            res.onSuccess {
                refreshNotebookData()
                val currentLookup = _uiState.value.lookupResult
                if (currentLookup?.savedWordId == id) {
                    _uiState.update { it.copy(lookupResult = currentLookup.copy(masteryStatus = status)) }
                }
            }
        }
    }

    fun deleteWordFromNotebook(id: String) {
        viewModelScope.launch {
            dictionaryRepository.deleteLearnedWord(id)
            refreshNotebookData()
            val currentLookup = _uiState.value.lookupResult
            if (currentLookup?.savedWordId == id) {
                _uiState.update {
                    it.copy(lookupResult = currentLookup.copy(isSaved = false, savedWordId = null, masteryStatus = null))
                }
            }
        }
    }

    fun refreshNotebookData() {
        viewModelScope.launch {
            val filter = _uiState.value.filterStatus
            val search = _uiState.value.notebookSearch
            val wordsRes = dictionaryRepository.getLearnedWords(
                status = if (filter == "all") null else filter,
                search = if (search.isBlank()) null else search
            )
            val statsRes = dictionaryRepository.getVocabularyStats()

            wordsRes.onSuccess { words ->
                _uiState.update { it.copy(learnedWords = words) }
            }
            statsRes.onSuccess { stats ->
                _uiState.update { it.copy(stats = stats) }
            }
        }
    }

    // --- Flashcards / Quiz Mode ---

    fun startQuiz() {
        val allWords = _uiState.value.learnedWords
        if (allWords.isEmpty()) {
            _uiState.update { it.copy(actionMessage = "Add words to your notebook first to practice!") }
            return
        }
        _uiState.update {
            it.copy(
                isQuizActive = true,
                quizWords = allWords.shuffled(),
                currentQuizIndex = 0,
                isCardFlipped = false,
                quizCompleted = false,
                quizLearnedInSession = 0
            )
        }
    }

    fun flipQuizCard() {
        _uiState.update { it.copy(isCardFlipped = !it.isCardFlipped) }
    }

    fun answerQuizCard(mastered: Boolean) {
        val state = _uiState.value
        val currentWord = state.quizWords.getOrNull(state.currentQuizIndex) ?: return

        if (mastered) {
            updateWordMastery(currentWord.id, "mastered")
        }

        val nextIndex = state.currentQuizIndex + 1
        if (nextIndex >= state.quizWords.size) {
            _uiState.update {
                it.copy(
                    quizCompleted = true,
                    quizLearnedInSession = it.quizLearnedInSession + (if (mastered) 1 else 0)
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    currentQuizIndex = nextIndex,
                    isCardFlipped = false,
                    quizLearnedInSession = it.quizLearnedInSession + (if (mastered) 1 else 0)
                )
            }
        }
    }

    fun exitQuiz() {
        _uiState.update { it.copy(isQuizActive = false) }
        refreshNotebookData()
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }
}

class DictionaryViewModelFactory(
    private val dictionaryRepository: DictionaryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DictionaryViewModel::class.java)) {
            return DictionaryViewModel(dictionaryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
