package com.example.ui.screens.luggage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        destination: String,
        bagType: String,
        departureDate: String,
        returnDate: String,
        maxWeightKg: Double,
        colorHex: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var bagType by remember { mutableStateOf("Cabin Bag") }
    var bagExpanded by remember { mutableStateOf(false) }
    var departureDate by remember { mutableStateOf("") }
    var returnDate by remember { mutableStateOf("") }
    var maxWeightText by remember { mutableStateOf("7.0") }
    var selectedColor by remember { mutableStateOf("#2563EB") }
    var titleError by remember { mutableStateOf(false) }

    val bagTypes = listOf("Cabin Bag", "Checked Suitcase", "Backpack", "Duffel Bag", "Tech Pouch")
    val colorOptions = listOf(
        "#2563EB" to "Blue",
        "#059669" to "Green",
        "#7C3AED" to "Purple",
        "#D97706" to "Amber",
        "#DC2626" to "Red",
        "#0891B2" to "Cyan"
    )
    val weightPresets = listOf(7.0, 10.0, 15.0, 20.0, 23.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Luggage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("New Luggage Trip", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = it.isBlank()
                    },
                    label = { Text("Trip Title *") },
                    placeholder = { Text("e.g., Dubai Tech Summit") },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("Trip title is required") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trip_title_input")
                )

                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Destination") },
                    placeholder = { Text("e.g., Dubai, UAE") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trip_destination_input")
                )

                ExposedDropdownMenuBox(
                    expanded = bagExpanded,
                    onExpandedChange = { bagExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = bagType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Bag Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bagExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("trip_bag_type_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = bagExpanded,
                        onDismissRequest = { bagExpanded = false }
                    ) {
                        bagTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    bagType = type
                                    bagExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = departureDate,
                        onValueChange = { departureDate = it },
                        label = { Text("Depart Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("trip_departure_date_input")
                    )
                    OutlinedTextField(
                        value = returnDate,
                        onValueChange = { returnDate = it },
                        label = { Text("Return Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("trip_return_date_input")
                    )
                }

                Column {
                    OutlinedTextField(
                        value = maxWeightText,
                        onValueChange = { maxWeightText = it },
                        label = { Text("Weight Limit (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trip_weight_limit_input")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        weightPresets.forEach { preset ->
                            FilterChip(
                                selected = maxWeightText == preset.toString(),
                                onClick = { maxWeightText = preset.toString() },
                                label = { Text("${preset.toInt()}kg", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = "Trip Theme Color",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        colorOptions.forEach { (hex, _) ->
                            val color = try {
                                Color(android.graphics.Color.parseColor(hex))
                            } catch (_: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                            val isSelected = selectedColor.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = hex }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        return@Button
                    }
                    val weight = maxWeightText.toDoubleOrNull() ?: 7.0
                    onConfirm(
                        title.trim(),
                        destination.trim(),
                        bagType,
                        departureDate.trim(),
                        returnDate.trim(),
                        weight,
                        selectedColor
                    )
                },
                modifier = Modifier.testTag("confirm_create_trip_btn")
            ) {
                Text("Create Trip")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_create_trip_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}
