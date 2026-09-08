package com.example.ui.screens.loans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentExpense
import com.example.ui.theme.AccentIncome
import com.example.ui.theme.EmeraldPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanSheet(
    sheetState: SheetState,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        personName: String,
        phoneNumber: String?,
        type: String,
        amount: Double,
        dueDate: String?,
        notes: String?,
        affectBalance: Boolean
    ) -> Unit
) {
    var selectedType by remember { mutableStateOf("lent") } // "lent" = They Owe Me, "borrowed" = I Owe Them
    var personName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<String?>(null) }
    var affectBalance by remember { mutableStateOf(true) }

    var personNameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

    val quickAmounts = listOf(500, 1000, 2000, 5000, 10000)

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Add Udhar / Loan Record",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Type Selector Segmented Row
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    // Lent (They owe me)
                    val isLentSelected = selectedType == "lent"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isLentSelected) AccentIncome else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f))
                            .clickable { selectedType = "lent" }
                            .padding(vertical = 12.dp)
                            .testTag("loan_type_lent_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CallMade,
                                contentDescription = "Lent",
                                tint = if (isLentSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "They Owe Me (Diya)",
                                fontWeight = if (isLentSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isLentSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Borrowed (I owe them)
                    val isBorrowedSelected = selectedType == "borrowed"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isBorrowedSelected) AccentExpense else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f))
                            .clickable { selectedType = "borrowed" }
                            .padding(vertical = 12.dp)
                            .testTag("loan_type_borrowed_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CallReceived,
                                contentDescription = "Borrowed",
                                tint = if (isBorrowedSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "I Owe Them (Liya)",
                                fontWeight = if (isBorrowedSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isBorrowedSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Person Name
            OutlinedTextField(
                value = personName,
                onValueChange = {
                    personName = it
                    personNameError = null
                },
                label = { Text("Person Name *") },
                placeholder = { Text("e.g., Ali Ahmed, Kashif Bhai") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Person, contentDescription = "Person")
                },
                singleLine = true,
                isError = personNameError != null,
                supportingText = personNameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loan_person_name_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Phone Number (Optional)
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number (Optional - for WhatsApp reminder)") },
                placeholder = { Text("e.g., 0300 1234567") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Phone, contentDescription = "Phone")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loan_phone_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Principal Amount
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.all { it.isDigit() || it == '.' }) {
                        amountText = input
                        amountError = null
                    }
                },
                label = { Text("Loan Amount *") },
                placeholder = { Text("0") },
                prefix = {
                    Text(
                        text = "Rs. ",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = amountError != null,
                supportingText = amountError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loan_amount_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Amount Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickAmounts) { amt ->
                    FilterChip(
                        selected = amountText == amt.toString(),
                        onClick = {
                            amountText = amt.toString()
                            amountError = null
                        },
                        label = { Text("+Rs. $amt") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Due Date Selection
            Text(
                text = "Target Due Date (Optional)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = dueDate == null,
                    onClick = { dueDate = null },
                    label = { Text("No deadline") }
                )

                FilterChip(
                    selected = dueDate != null && dueDate?.endsWith("7d") == true,
                    onClick = {
                        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
                        dueDate = dateFormat.format(cal.time)
                    },
                    label = { Text("In 7 days") }
                )

                FilterChip(
                    selected = dueDate != null && dueDate?.endsWith("30d") == true,
                    onClick = {
                        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) }
                        dueDate = dateFormat.format(cal.time)
                    },
                    label = { Text("In 30 days") }
                )
            }

            if (dueDate != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = "Due Date",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Due on: $dueDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Notes / Reason
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Reason (Optional)") },
                placeholder = { Text("e.g., University tuition, Grocery split") },
                singleLine = false,
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loan_notes_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Affect Wallet Balance Switch
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sync with Wallet Balance",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (selectedType == "lent") "Deduct this cash outflow from wallet" else "Add this borrowed cash inflow to wallet",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = affectBalance,
                        onCheckedChange = { affectBalance = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = EmeraldPrimary
                        ),
                        modifier = Modifier.testTag("loan_affect_balance_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    var hasError = false
                    if (personName.isBlank()) {
                        personNameError = "Please provide person's name"
                        hasError = true
                    }
                    val parsedAmt = amountText.toDoubleOrNull()
                    if (parsedAmt == null || parsedAmt <= 0) {
                        amountError = "Enter a valid positive amount"
                        hasError = true
                    }
                    if (hasError) return@Button

                    onSubmit(
                        personName.trim(),
                        phoneNumber.ifBlank { null },
                        selectedType,
                        parsedAmt!!,
                        dueDate,
                        notes.ifBlank { null },
                        affectBalance
                    )
                },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == "lent") AccentIncome else EmeraldPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("save_loan_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = if (selectedType == "lent") "Record Lent Udhar" else "Record Borrowed Udhar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
