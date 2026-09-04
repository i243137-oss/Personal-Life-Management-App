package com.example.ui.screens.loans

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LoanDto
import com.example.ui.theme.AccentCardBackground
import com.example.ui.theme.AccentExpense
import com.example.ui.theme.AccentIncome
import com.example.ui.theme.AccentLoan
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.LoanViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    loanViewModel: LoanViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by loanViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAddSheet by remember { mutableStateOf(false) }
    var loanForRepayment by remember { mutableStateOf<LoanDto?>(null) }
    var loanForReminder by remember { mutableStateOf<LoanDto?>(null) }
    var loanToDelete by remember { mutableStateOf<LoanDto?>(null) }

    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val repaySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Handle snackbar messages
    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            loanViewModel.clearMessages()
        }
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            loanViewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Add Loan") },
                text = { Text("New Udhar", fontWeight = FontWeight.SemiBold) },
                containerColor = EmeraldPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_loan_fab")
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { loanViewModel.loadData(isRefresh = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                item {
                    Column {
                        Text(
                            text = "Loans & Udhar",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Track who owes you and who you owe with instant wallet sync",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Summary Dashboard Card
                item {
                    LoanSummaryCard(
                        totalLent = uiState.summary.totalLent,
                        totalBorrowed = uiState.summary.totalBorrowed,
                        netPosition = uiState.summary.netPosition,
                        activeLentCount = uiState.summary.activeLentCount,
                        activeBorrowedCount = uiState.summary.activeBorrowedCount,
                        settledCount = uiState.summary.settledCount,
                        overdueCount = uiState.summary.overdueCount
                    )
                }

                // Search & Filter Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Search bar
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { loanViewModel.setSearchQuery(it) },
                            placeholder = { Text("Search by name, note, or phone...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { loanViewModel.setSearchQuery("") }) {
                                        Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_loans_input")
                        )

                        // Filter Chips Row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = uiState.selectedFilter == null,
                                    onClick = { loanViewModel.setFilter(null) },
                                    label = { Text("All (${uiState.summary.totalCount})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                        selectedLabelColor = EmeraldPrimary
                                    )
                                )
                            }
                            item {
                                FilterChip(
                                    selected = uiState.selectedFilter == "lent",
                                    onClick = { loanViewModel.setFilter("lent") },
                                    label = { Text("They Owe Me (${uiState.summary.activeLentCount})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentIncome.copy(alpha = 0.2f),
                                        selectedLabelColor = AccentIncome
                                    )
                                )
                            }
                            item {
                                FilterChip(
                                    selected = uiState.selectedFilter == "borrowed",
                                    onClick = { loanViewModel.setFilter("borrowed") },
                                    label = { Text("I Owe (${uiState.summary.activeBorrowedCount})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentExpense.copy(alpha = 0.2f),
                                        selectedLabelColor = AccentExpense
                                    )
                                )
                            }
                            item {
                                FilterChip(
                                    selected = uiState.selectedFilter == "settled",
                                    onClick = { loanViewModel.setFilter("settled") },
                                    label = { Text("Settled (${uiState.summary.settledCount})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }

                // Loading or Empty State or Loans List
                if (uiState.isLoading && !uiState.isRefreshing) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = EmeraldPrimary)
                        }
                    }
                } else if (uiState.loans.isEmpty()) {
                    item {
                        EmptyLoansCard(
                            filter = uiState.selectedFilter,
                            onAddClick = { showAddSheet = true }
                        )
                    }
                } else {
                    items(uiState.loans, key = { it.id }) { loan ->
                        LoanCardItem(
                            loan = loan,
                            onRepaymentClick = { loanForRepayment = loan },
                            onMarkPaidClick = { loanViewModel.markAsPaid(loan.id) },
                            onReminderClick = { loanForReminder = loan },
                            onDeleteClick = { loanToDelete = loan }
                        )
                    }
                }
            }
        }
    }

    // Add Loan Bottom Sheet
    if (showAddSheet) {
        AddLoanSheet(
            sheetState = addSheetState,
            isSubmitting = uiState.isMutating,
            onDismiss = { showAddSheet = false },
            onSubmit = { personName, phoneNumber, type, amount, dueDate, notes, affectBalance ->
                loanViewModel.createLoan(
                    personName = personName,
                    phoneNumber = phoneNumber,
                    type = type,
                    amount = amount,
                    dueDate = dueDate,
                    notes = notes,
                    affectBalance = affectBalance,
                    onSuccess = {
                        scope.launch { addSheetState.hide() }
                        showAddSheet = false
                    }
                )
            }
        )
    }

    // Repayment Bottom Sheet
    loanForRepayment?.let { loan ->
        RepaymentSheet(
            loan = loan,
            sheetState = repaySheetState,
            isSubmitting = uiState.isMutating,
            onDismiss = { loanForRepayment = null },
            onSubmit = { amount, notes ->
                loanViewModel.recordRepayment(
                    loanId = loan.id,
                    amount = amount,
                    notes = notes,
                    onSuccess = {
                        scope.launch { repaySheetState.hide() }
                        loanForRepayment = null
                    }
                )
            }
        )
    }

    // Reminder Dialog
    loanForReminder?.let { loan ->
        ReminderDialog(
            loan = loan,
            onDismiss = { loanForReminder = null }
        )
    }

    // Delete Confirmation Dialog
    loanToDelete?.let { loan ->
        AlertDialog(
            onDismissRequest = { loanToDelete = null },
            title = { Text("Delete Udhar Record?") },
            text = {
                Text(
                    "Are you sure you want to delete the udhar record for ${loan.personName} of Rs. ${loan.remainingAmount.toInt()}?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        loanViewModel.deleteLoan(loan.id)
                        loanToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentExpense)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { loanToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LoanSummaryCard(
    totalLent: Double,
    totalBorrowed: Double,
    netPosition: Double,
    activeLentCount: Int,
    activeBorrowedCount: Int,
    settledCount: Int,
    overdueCount: Int
) {
    val pkrFormat = NumberFormat.getNumberInstance(Locale.US)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AccentCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("loan_summary_card")
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            EmeraldDark,
                            EmeraldPrimary.copy(alpha = 0.85f),
                            EmeraldLight.copy(alpha = 0.65f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            // Title and Net Position Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Udhar Position",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (netPosition >= 0) Color(0xFF047857) else Color(0xFFB91C1C)
                ) {
                    Text(
                        text = if (netPosition >= 0) "+Rs. ${pkrFormat.format(netPosition.toLong())} (Surplus)" else "-Rs. ${pkrFormat.format((-netPosition).toLong())} (Deficit)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Two-column layout: Lent vs Borrowed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Total Lent (They Owe Me)
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34D399).copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CallMade,
                                contentDescription = "Lent",
                                tint = Color(0xFFA7F3D0),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "They Owe Me",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Rs. ${pkrFormat.format(totalLent.toLong())}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "$activeLentCount pending debtors",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Total Borrowed (I Owe)
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF87171).copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CallReceived,
                                contentDescription = "Borrowed",
                                tint = Color(0xFFFECACA),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "I Owe Them",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Rs. ${pkrFormat.format(totalBorrowed.toLong())}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFED7AA)
                    )
                    Text(
                        text = "$activeBorrowedCount pending creditors",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            if (overdueCount > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF7F1D1D).copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$overdueCount loan(s) past due date!",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFEE2E2)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoanCardItem(
    loan: LoanDto,
    onRepaymentClick: () -> Unit,
    onMarkPaidClick: () -> Unit,
    onReminderClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val pkrFormat = NumberFormat.getNumberInstance(Locale.US)
    val isLent = loan.type == "lent"
    val isSettled = loan.status == "paid" || loan.remainingAmount <= 0.0

    val initial = loan.personName.firstOrNull()?.uppercase() ?: "?"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("loan_card_${loan.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Avatar, Name, Type Badge, Delete Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Initial Circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isLent) AccentIncome.copy(alpha = 0.15f) else AccentExpense.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isLent) AccentIncome else AccentExpense
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = loan.personName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Type badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isLent) AccentIncome.copy(alpha = 0.15f) else AccentExpense.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isLent) "THEY OWE" else "YOU OWE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isLent) AccentIncome else AccentExpense,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (!loan.phoneNumber.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = "Phone",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = loan.phoneNumber,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Amount & Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = if (isSettled) "Settled Amount" else "Remaining Due",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Rs. ${pkrFormat.format(if (isSettled) loan.amount.toLong() else loan.remainingAmount.toLong())}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSettled) MaterialTheme.colorScheme.onSurfaceVariant else if (isLent) AccentIncome else AccentExpense
                    )
                    if (!isSettled && loan.remainingAmount < loan.amount) {
                        Text(
                            text = "Original: Rs. ${pkrFormat.format(loan.amount.toLong())}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isSettled -> Color(0xFF10B981).copy(alpha = 0.15f)
                        loan.status == "partially_paid" -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                        else -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (isSettled) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Paid",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = when {
                                isSettled -> "Settled"
                                loan.status == "partially_paid" -> "Partially Paid"
                                else -> "Pending"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isSettled -> Color(0xFF059669)
                                loan.status == "partially_paid" -> Color(0xFF2563EB)
                                else -> Color(0xFFD97706)
                            }
                        )
                    }
                }
            }

            // Repayment Progress Bar if partially paid
            if (!isSettled && loan.amount > 0 && loan.remainingAmount < loan.amount) {
                val paidProgress = ((loan.amount - loan.remainingAmount) / loan.amount).toFloat().coerceIn(0f, 1f)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { paidProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isLent) AccentIncome else AccentExpense,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Notes and Due Date
            if (!loan.notes.isNullOrBlank() || loan.dueDate != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!loan.notes.isNullOrBlank()) {
                        Text(
                            text = loan.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    if (loan.dueDate != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = "Due Date",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Due: ${loan.dueDate.take(10)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Action Buttons Row (Only when not settled)
            if (!isSettled) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Record Payment
                    Button(
                        onClick = onRepaymentClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(38.dp)
                            .testTag("loan_repay_button_${loan.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Payment,
                            contentDescription = "Pay",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isLent) "Received" else "Repay",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Mark as Paid
                    OutlinedButton(
                        onClick = onMarkPaidClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("loan_mark_paid_button_${loan.id}")
                    ) {
                        Text(
                            text = "Full Settle",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Send Reminder
                    if (isLent) {
                        OutlinedButton(
                            onClick = onReminderClick,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(38.dp)
                                .testTag("loan_remind_button_${loan.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = "Remind",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Remind",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyLoansCard(
    filter: String?,
    onAddClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Payment,
                    contentDescription = "No loans",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = when (filter) {
                    "lent" -> "No pending debtor records"
                    "borrowed" -> "No pending creditor records"
                    "settled" -> "No settled udhar records yet"
                    else -> "No Udhar records yet"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Keep track of money you've given or taken from friends and family.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add First Udhar")
            }
        }
    }
}
