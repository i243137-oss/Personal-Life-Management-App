package com.example.ui.screens.dictionary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiAssistantData
import com.example.data.model.WordItemDto
import com.example.data.model.WordLookupResult
import com.example.ui.viewmodel.DictionaryViewModel

private fun playAudio(context: Context, url: String?) {
    if (url.isNullOrBlank()) {
        Toast.makeText(context, "No audio pronunciation available", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val mp = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .build()
            )
            setDataSource(url)
            setOnPreparedListener { start() }
            setOnCompletionListener { release() }
            setOnErrorListener { _, _, _ ->
                release()
                true
            }
            prepareAsync()
        }
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to play audio", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DictionaryScreen(
    dictionaryViewModel: DictionaryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by dictionaryViewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            dictionaryViewModel.clearActionMessage()
        }
    }

    if (uiState.isQuizActive) {
        QuizFlashcardDialog(
            quizWords = uiState.quizWords,
            currentIndex = uiState.currentQuizIndex,
            isCardFlipped = uiState.isCardFlipped,
            isCompleted = uiState.quizCompleted,
            masteredInSession = uiState.quizLearnedInSession,
            onFlip = { dictionaryViewModel.flipQuizCard() },
            onAnswer = { mastered -> dictionaryViewModel.answerQuizCard(mastered) },
            onDismiss = { dictionaryViewModel.exitQuiz() }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Smart Dictionary",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Gemini",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Gemini AI",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        Text(
                            text = "AI-powered vocabulary lookup & concept mastery",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.learnedWords.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { dictionaryViewModel.startQuiz() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("dict_start_quiz_header_button")
                        ) {
                            Icon(imageVector = Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Practice", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Switcher
                TabRow(
                    selectedTabIndex = uiState.selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = uiState.selectedTab == 0,
                        onClick = { dictionaryViewModel.selectTab(0) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Search & Learn")
                            }
                        },
                        modifier = Modifier.testTag("tab_search_learn")
                    )

                    Tab(
                        selected = uiState.selectedTab == 1,
                        onClick = { dictionaryViewModel.selectTab(1) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BadgedBox(
                                    badge = {
                                        if (uiState.learnedWords.isNotEmpty()) {
                                            Badge { Text("${uiState.learnedWords.size}") }
                                        }
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Learned Words")
                            }
                        },
                        modifier = Modifier.testTag("tab_learned_words")
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.selectedTab == 0) {
                SearchAndLearnTab(
                    uiState = uiState,
                    dictionaryViewModel = dictionaryViewModel,
                    focusManager = focusManager
                )
            } else {
                LearnedWordsTab(
                    uiState = uiState,
                    dictionaryViewModel = dictionaryViewModel
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchAndLearnTab(
    uiState: com.example.ui.viewmodel.DictionaryUiState,
    dictionaryViewModel: DictionaryViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { dictionaryViewModel.setSearchQuery(it) },
                placeholder = { Text("Search word or concept (e.g. Resilience, Serendipity)...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { dictionaryViewModel.setSearchQuery("") },
                            modifier = Modifier.testTag("dict_search_clear_button")
                        ) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        dictionaryViewModel.lookupWord()
                    }
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dict_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Mode Selector: Meaning Mode vs Explain Mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isMeaning = uiState.activeMode == "meaning"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isMeaning) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { dictionaryViewModel.setMode("meaning") }
                        .padding(vertical = 10.dp)
                        .testTag("mode_meaning_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = if (isMeaning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Meaning Mode",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isMeaning) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isMeaning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val isExplain = uiState.activeMode == "explain"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isExplain) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { dictionaryViewModel.setMode("explain") }
                        .padding(vertical = 10.dp)
                        .testTag("mode_explain_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = if (isExplain) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Explain Mode",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isExplain) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isExplain) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Word Inspiration Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val suggestions = listOf("Resilience", "Serendipity", "Ephemeral", "Pragmatic", "Eloquent", "Tenacity")
                suggestions.forEach { word ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                focusManager.clearFocus()
                                dictionaryViewModel.lookupWord(word)
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = word,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Loading or Error or Result Display
        item {
            if (uiState.isLookingUp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Consulting Gemini AI dictionary...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (uiState.lookupError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dictionary_error_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = uiState.lookupError ?: "Please connect to internet",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { dictionaryViewModel.lookupWord() },
                            modifier = Modifier.testTag("dictionary_retry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Try Again")
                        }
                    }
                }
            } else if (uiState.lookupResult != null) {
                WordResultCard(
                    result = uiState.lookupResult,
                    activeMode = uiState.activeMode,
                    aiAssistantData = uiState.aiAssistantData,
                    isAiAssistantLoading = uiState.isAiAssistantLoading,
                    activeAiFeature = uiState.activeAiFeature,
                    onFetchAiAssistant = { feature -> dictionaryViewModel.getAiAssistant(feature) },
                    onSaveWord = { status, notes -> dictionaryViewModel.saveCurrentLookup(status, notes) },
                    onRemoveWord = { dictionaryViewModel.removeCurrentLookupFromNotebook() },
                    onLookupRelated = { rel -> dictionaryViewModel.lookupWord(rel) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordResultCard(
    result: WordLookupResult,
    activeMode: String,
    aiAssistantData: AiAssistantData?,
    isAiAssistantLoading: Boolean,
    activeAiFeature: String,
    onFetchAiAssistant: (String) -> Unit,
    onSaveWord: (String, String?) -> Unit,
    onRemoveWord: () -> Unit,
    onLookupRelated: (String) -> Unit
) {
    val context = LocalContext.current
    var showMasteryMenu by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteInput by remember(result.word) { mutableStateOf(result.personalNotes ?: "") }

    val audioUrl = result.audioUrl ?: result.dictionary?.audio?.firstOrNull()?.url
    val syllables = result.dictionary?.syllables?.takeIf { it.isNotBlank() } ?: result.word
    val writtenPron = result.dictionary?.pronunciation?.written?.takeIf { it.isNotBlank() } ?: result.phonetic
    val ipaPron = result.dictionary?.pronunciation?.ipa?.takeIf { it.isNotBlank() }
    val etymologyText = result.etymology?.takeIf { it.isNotBlank() } ?: result.dictionary?.etymology?.takeIf { it.isNotBlank() }
    val sourceProvider = result.source?.provider ?: if (result.dictionary != null) "merriam_webster" else "local"
    val isMwSource = sourceProvider == "merriam_webster" || result.dictionary != null
    val isGeminiSource = sourceProvider == "gemini"
    val sourceLabel = when {
        isMwSource -> "Merriam-Webster Collegiate® Authoritative"
        isGeminiSource -> "Gemini AI Knowledge Source"
        else -> "Curated Lexicon (Offline)"
    }
    val sourceColor = when {
        isMwSource -> Color(0xFF1E3A8A)
        isGeminiSource -> MaterialTheme.colorScheme.primary
        else -> Color(0xFF4B5563)
    }

    if (showNoteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Personal Study Note") },
            text = {
                Column {
                    Text("Add your own context, translation, or mnemonic for '${result.word}':", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        placeholder = { Text("E.g. Used in research papers; remember to pronounce with stress on 2nd syllable.") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNoteDialog = false
                        onSaveWord(result.masteryStatus ?: "learning", noteInput)
                    }
                ) {
                    Text("Save Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Authoritative Source Header Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = sourceColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, sourceColor.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isMwSource) Icons.Default.Check else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = sourceColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sourceLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = sourceColor
                    )
                }
            }

            if (result.thesaurus != null || isMwSource) {
                Surface(
                    color = Color(0xFF0D9488).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Thesaurus Linked",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF0F766E),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Main Word Title & Pronunciation Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = result.word,
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!audioUrl.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { playAudio(context, audioUrl) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .testTag("dict_audio_pronunciation_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Listen to pronunciation",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Syllables and written/IPA pronunciation
                        FlowRow(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            if (syllables != result.word) {
                                Text(
                                    text = syllables,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!writtenPron.isNullOrBlank()) {
                                Text(
                                    text = "\\ $writtenPron \\",
                                    style = MaterialTheme.typography.titleSmall.copy(fontStyle = FontStyle.Italic),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (!ipaPron.isNullOrBlank()) {
                                Text(
                                    text = "[$ipaPron]",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!result.partOfSpeech.isNullOrBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = result.partOfSpeech.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Save / Bookmark Action
                    Box {
                        if (result.isSaved) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showMasteryMenu = true }
                                    .testTag("dict_saved_status_button"),
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = "Saved",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = (result.masteryStatus ?: "Saved").replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showMasteryMenu,
                                onDismissRequest = { showMasteryMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Status: Learning") },
                                    leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFFF59E0B)) },
                                    onClick = {
                                        showMasteryMenu = false
                                        onSaveWord("learning", noteInput)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Status: Reviewing") },
                                    leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFF3B82F6)) },
                                    onClick = {
                                        showMasteryMenu = false
                                        onSaveWord("reviewing", noteInput)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Status: Mastered") },
                                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF10B981)) },
                                    onClick = {
                                        showMasteryMenu = false
                                        onSaveWord("mastered", noteInput)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit Personal Note") },
                                    leadingIcon = { Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showMasteryMenu = false
                                        showNoteDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Remove from Notebook") },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMasteryMenu = false
                                        onRemoveWord()
                                    }
                                )
                            }
                        } else {
                            Button(
                                onClick = { onSaveWord("learning", null) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("dict_save_word_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkAdd,
                                    contentDescription = "Add to learned words",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Word", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // Short Definition Callout
                if (!result.shortDefinition.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = result.shortDefinition,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }

                // Personal Note if saved
                if (result.isSaved && !result.personalNotes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFFFEF9C3).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().clickable { showNoteDialog = true }
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Note: ${result.personalNotes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF78350F)
                            )
                        }
                    }
                }
            }
        }

        // Detailed Merriam-Webster Definitions Card
        val structuredDefinitions = result.dictionary?.definitions
        if (!structuredDefinitions.isNullOrEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Merriam-Webster Collegiate® Definitions",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${structuredDefinitions.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    structuredDefinitions.forEachIndexed { idx, defItem ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (!defItem.partOfSpeech.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "[${defItem.partOfSpeech}]",
                                        style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = defItem.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 21.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (defItem.examples.isNotEmpty()) {
                                Column(modifier = Modifier.padding(start = 28.dp, top = 4.dp)) {
                                    defItem.examples.forEach { ex ->
                                        Text(
                                            text = "“$ex”",
                                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (!result.fullDefinition.isNullOrBlank()) {
            // Fallback Full Definition & Nuances Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Detailed Definition & Nuances",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = result.fullDefinition,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Collegiate Thesaurus Section: Synonyms, Antonyms, and Related Words
        if (result.synonyms.isNotEmpty() || result.antonyms.isNotEmpty() || result.relatedWords.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Collegiate Thesaurus Insights",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• tap word to look up",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (result.synonyms.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Synonyms (${result.synonyms.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            result.synonyms.forEach { syn ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onLookupRelated(syn) },
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = syn,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (result.antonyms.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Antonyms (${result.antonyms.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            result.antonyms.forEach { ant ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onLookupRelated(ant) },
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = ant,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (result.relatedWords.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Related Words (${result.relatedWords.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF0F766E)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            result.relatedWords.forEach { rel ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onLookupRelated(rel) },
                                    color = Color(0xFFCCFBF1),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = rel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF0F766E),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Word Origin & Etymology Card
        if (!etymologyText.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🏛️ Word Origin & Etymology",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF92400E)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = etymologyText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF78350F),
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Gemini AI Learning Companion Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD6FE))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gemini AI Learning Companion",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF5B21B6)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pedagogical insights, academic framing, and Urdu explanations:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6D28D9)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Feature buttons
                val aiFeatures = listOf(
                    "simple_explanation" to "Simple English",
                    "urdu_explanation" to "Urdu (اردو)",
                    "academic_context" to "Academic Context",
                    "collocations" to "Collocations",
                    "common_mistakes" to "Common Mistakes",
                    "memory_tricks" to "Memory Trick",
                    "quiz" to "Quiz Me"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    aiFeatures.forEach { (featKey, label) ->
                        val isSelected = activeAiFeature == featKey && aiAssistantData != null
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onFetchAiAssistant(featKey) },
                            color = if (isSelected) Color(0xFF7C3AED) else Color.White,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF7C3AED) else Color(0xFFDDD6FE))
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) Color.White else Color(0xFF6D28D9),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                // AI Response Content or Loading
                if (isAiAssistantLoading) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF7C3AED)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Gemini is preparing learning insight...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6D28D9)
                        )
                    }
                } else if (aiAssistantData != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEDE9FE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val currentTitle = aiFeatures.find { it.first == aiAssistantData.feature }?.second ?: "Learning Insight"
                                Text(
                                    text = currentTitle,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF5B21B6)
                                )
                                if (aiAssistantData.isFallback) {
                                    Surface(color = Color(0xFFF3F4F6), shape = RoundedCornerShape(4.dp)) {
                                        Text("Offline Guide", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(4.dp, 2.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = aiAssistantData.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF374151),
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }
        }

        // Real-World Examples
        if (result.examples.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Real-World Examples",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    result.examples.forEachIndexed { idx, example ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = "\"$example\"",
                                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (idx < result.examples.lastIndex) {
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }

        // Explain Mode Specific: ELI5 Analogy Card
        if (!result.eli5Analogy.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7).copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Analogy",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Explain Like I'm 5 (Analogy)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF92400E)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = result.eli5Analogy,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF78350F),
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }

        // Key Points / Nuances
        if (result.keyPoints.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (activeMode == "explain") "Key Concepts & Principles" else "Practical Usage Notes",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    result.keyPoints.forEach { point ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = point,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Key Takeaway Banner
        if (!result.keyTakeaway.isNullOrBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Core Takeaway",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = result.keyTakeaway,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun LearnedWordsTab(
    uiState: com.example.ui.viewmodel.DictionaryUiState,
    dictionaryViewModel: DictionaryViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            // Mastery Overview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Vocabulary Mastery",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${uiState.stats.total} words saved in your notebook",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${uiState.stats.masteryPercentage}% Mastered",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { (uiState.stats.masteryPercentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF10B981),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Breakdown counts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MasteryCountPill(label = "Learning", count = uiState.stats.learning, color = Color(0xFFF59E0B))
                        MasteryCountPill(label = "Reviewing", count = uiState.stats.reviewing, color = Color(0xFF3B82F6))
                        MasteryCountPill(label = "Mastered", count = uiState.stats.mastered, color = Color(0xFF10B981))
                    }

                    if (uiState.learnedWords.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { dictionaryViewModel.startQuiz() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("dict_start_quiz_card_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Launch Flashcard Practice", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "all" to "All (${uiState.stats.total})",
                    "learning" to "Learning (${uiState.stats.learning})",
                    "reviewing" to "Reviewing (${uiState.stats.reviewing})",
                    "mastered" to "Mastered (${uiState.stats.mastered})"
                ).forEach { (filterKey, label) ->
                    val selected = uiState.filterStatus == filterKey
                    FilterChip(
                        selected = selected,
                        onClick = { dictionaryViewModel.setNotebookFilter(filterKey) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search in Notebook
            OutlinedTextField(
                value = uiState.notebookSearch,
                onValueChange = { dictionaryViewModel.setNotebookSearch(it) },
                placeholder = { Text("Filter saved words...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (uiState.notebookSearch.isNotEmpty()) {
                        IconButton(onClick = { dictionaryViewModel.setNotebookSearch("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notebook_search_input")
            )
        }

        if (uiState.learnedWords.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No saved words found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Search any word in 'Search & Learn' and tap 'Save Word' to build your vocabulary notebook.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.learnedWords, key = { it.id }) { wordItem ->
                LearnedWordCard(
                    word = wordItem,
                    onOpenWord = {
                        dictionaryViewModel.selectTab(0)
                        dictionaryViewModel.lookupWord(wordItem.word, wordItem.mode)
                    },
                    onUpdateMastery = { status -> dictionaryViewModel.updateWordMastery(wordItem.id, status) },
                    onDelete = { dictionaryViewModel.deleteWordFromNotebook(wordItem.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MasteryCountPill(label: String, count: Int, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$count $label",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = color
            )
        }
    }
}

@Composable
private fun LearnedWordCard(
    word: WordItemDto,
    onOpenWord: () -> Unit,
    onUpdateMastery: (String) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val badgeColor = when (word.masteryStatus.lowercase()) {
        "mastered" -> Color(0xFF10B981)
        "reviewing" -> Color(0xFF3B82F6)
        else -> Color(0xFFF59E0B)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenWord() }
            .testTag("learned_word_${word.word}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = word.word,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!word.partOfSpeech.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = word.partOfSpeech.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (!word.audioUrl.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { playAudio(context, word.audioUrl) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Listen",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Mastery Status Chip (Clickable to change)
                Box {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showMenu = true },
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = word.masteryStatus.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mark as Learning") },
                            leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFFF59E0B)) },
                            onClick = {
                                showMenu = false
                                onUpdateMastery("learning")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Mark as Reviewing") },
                            leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFF3B82F6)) },
                            onClick = {
                                showMenu = false
                                onUpdateMastery("reviewing")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Mark as Mastered") },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF10B981)) },
                            onClick = {
                                showMenu = false
                                onUpdateMastery("mastered")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Word") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            if (!word.shortDefinition.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = word.shortDefinition,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!word.personalNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFFFEF9C3).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(6.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Note: ${word.personalNotes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF78350F),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!word.phonetic.isNullOrBlank()) {
                    Text(
                        text = word.phonetic,
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "View details",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
