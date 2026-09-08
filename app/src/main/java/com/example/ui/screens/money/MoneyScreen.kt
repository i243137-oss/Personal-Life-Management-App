package com.example.ui.screens.money

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionDto
import com.example.ui.theme.AccentExpense
import com.example.ui.theme.AccentIncome
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.MoneyViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(
    moneyViewModel: MoneyViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by moneyViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddSheet by remember { mutableStateOf(false) }
    var addSheetInitialType by remember { mutableStateOf("expense") }
    var showBreakdownDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<TransactionDto?>(null) }

    val currencyFormatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 0
        }
    }

    LaunchedEffect(uiState.actionSuccessMessage) {
        uiState.actionSuccessMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            moneyViewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { err ->
            snackbarHostState.showSnackbar(err)
            moneyViewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    addSheetInitialType = "expense"
                    showAddSheet = true
                },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_transaction")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Transaction")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Entry", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("money_screen_scroll"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Money & Expenses",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Track income, daily spends & cashflow",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row {
                        IconButton(
                            onClick = { showBreakdownDialog = true },
                            modifier = Modifier.testTag("insights_button")
                        ) {
                            Icon(
                                Icons.Filled.PieChart,
                                contentDescription = "Spending Breakdown",
                                tint = EmeraldPrimary
                            )
                        }

                        IconButton(
                            onClick = { moneyViewModel.loadData() },
                            modifier = Modifier.testTag("refresh_money_button")
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Overview Wallet Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("money_balance_card"),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(EmeraldDark, EmeraldPrimary, EmeraldLight)
                                )
                            )
                            .padding(22.dp)
                    ) {
                        Column {
                            Text(
                                text = "Current Net Balance",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.85f),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Rs. ${currencyFormatter.format(uiState.currentBalance)}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Inflow
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.ArrowDownward,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Total Income",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Rs. ${currencyFormatter.format(uiState.totalIncome)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Outflow
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.ArrowUpward,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Total Expenses",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Rs. ${currencyFormatter.format(uiState.totalExpense)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Add Presets Bar
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Spend Shortcuts",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap to record instantly",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.categories.quickPresets) { preset ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .clickable {
                                        moneyViewModel.quickAddExpense(
                                            label = preset.label,
                                            category = preset.category,
                                            amount = preset.amount,
                                            type = preset.type
                                        )
                                    }
                                    .testTag("quick_preset_${preset.label.replace(" ", "_")}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Rs. ${preset.amount.toInt()}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (preset.type == "income") AccentIncome else AccentExpense
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dual Fast Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            addSheetInitialType = "income"
                            showAddSheet = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_add_income"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentIncome.copy(alpha = 0.15f),
                            contentColor = AccentIncome
                        )
                    ) {
                        Icon(Icons.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Add Income", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            addSheetInitialType = "expense"
                            showAddSheet = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_add_expense"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentExpense.copy(alpha = 0.15f),
                            contentColor = AccentExpense
                        )
                    ) {
                        Icon(Icons.Filled.TrendingDown, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("- Add Expense", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Search and Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Search box
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { moneyViewModel.setSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_transactions_input"),
                        placeholder = { Text("Search transactions, notes...") },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { moneyViewModel.setSearchQuery("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Type segmented chips: All | Income | Expense
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("all" to "All Records", "expense" to "Expenses", "income" to "Income").forEach { (typeKey, label) ->
                            val isSelected = uiState.selectedTypeFilter == typeKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { moneyViewModel.setTypeFilter(typeKey) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = EmeraldPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Category filter row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.selectedCategoryFilter == "all",
                                onClick = { moneyViewModel.setCategoryFilter("all") },
                                label = { Text("All Categories") }
                            )
                        }

                        val activeCatList = if (uiState.selectedTypeFilter == "income") {
                            uiState.categories.income
                        } else {
                            uiState.categories.expense
                        }

                        items(activeCatList) { category ->
                            FilterChip(
                                selected = uiState.selectedCategoryFilter == category.name,
                                onClick = {
                                    moneyViewModel.setCategoryFilter(
                                        if (uiState.selectedCategoryFilter == category.name) "all" else category.name
                                    )
                                },
                                label = { Text(category.name) }
                            )
                        }
                    }
                }
            }

            // Transaction Stream List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions (${uiState.filteredTransactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                }
            }

            if (uiState.filteredTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (uiState.searchQuery.isNotEmpty() || uiState.selectedCategoryFilter != "all") {
                                    "No transactions match your filter criteria."
                                } else {
                                    "No transactions recorded yet."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Use '+ Add Income' or '- Add Expense' above to start tracking.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            } else {
                items(uiState.filteredTransactions, key = { it.effectiveId }) { tx ->
                    TransactionCardItem(
                        transaction = tx,
                        currencyFormatter = currencyFormatter,
                        onDelete = { transactionToDelete = tx }
                    )
                }
            }
        }
    }

    // Add Transaction Sheet Modal
    if (showAddSheet) {
        AddTransactionSheet(
            initialType = addSheetInitialType,
            currentBalance = uiState.currentBalance,
            categoriesData = uiState.categories,
            isSubmitting = uiState.isSubmitting,
            overdraftWarning = uiState.overdraftPrompt,
            onDismiss = {
                showAddSheet = false
                moneyViewModel.clearMessages()
            },
            onSubmit = { type, amount, category, description, allowOverdraft ->
                moneyViewModel.createTransaction(
                    type = type,
                    amount = amount,
                    category = category,
                    description = description,
                    allowOverdraft = allowOverdraft,
                    onSuccess = {
                        showAddSheet = false
                    }
                )
            }
        )
    }

    // Spending Breakdown Dialog
    if (showBreakdownDialog) {
        SpendingBreakdownDialog(
            stats = uiState.stats,
            onDismiss = { showBreakdownDialog = false }
        )
    }

    // Delete Confirmation Dialog
    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction?") },
            text = {
                Text(
                    "Are you sure you want to delete this ${transactionToDelete!!.category} transaction of Rs. ${transactionToDelete!!.amount.toInt()}? Your balance will be recalculated."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        transactionToDelete?.let {
                            moneyViewModel.deleteTransaction(it.effectiveId)
                        }
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TransactionCardItem(
    transaction: TransactionDto,
    currencyFormatter: NumberFormat,
    onDelete: () -> Unit
) {
    val isIncome = transaction.type == "income"
    val accentColor = if (isIncome) AccentIncome else AccentExpense

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tx_item_${transaction.effectiveId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!transaction.description.isNullOrBlank()) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount & Delete Action
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isIncome) "+" else "-"}Rs. ${currencyFormatter.format(transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = accentColor
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("delete_tx_${transaction.effectiveId}")
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
