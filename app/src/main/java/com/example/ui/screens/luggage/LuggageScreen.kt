package com.example.ui.screens.luggage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LuggageItemDto
import com.example.data.model.LuggageTripDto
import com.example.ui.viewmodel.LuggageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuggageScreen(
    luggageViewModel: LuggageViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by luggageViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateTripDialog by remember { mutableStateOf(false) }
    var showEditTripDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<LuggageItemDto?>(null) }
    var itemToDelete by remember { mutableStateOf<LuggageItemDto?>(null) }
    var tripToDelete by remember { mutableStateOf<LuggageTripDto?>(null) }
    var showTemplateSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            luggageViewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            luggageViewModel.clearMessages()
        }
    }

    val selectedTrip = uiState.selectedTrip
    val categories = listOf("All", "Clothing", "Electronics", "Toiletries", "Documents", "Medication", "Valuables", "Other")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTrip != null) {
                ExtendedFloatingActionButton(
                    onClick = { showAddItemDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Item") },
                    modifier = Modifier.testTag("fab_add_item")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 84.dp)
            ) {
                // Header Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Luggage & Packing",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Smart travel packing & luggage weight tracker",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { luggageViewModel.loadTrips(isRefresh = true) },
                                modifier = Modifier.testTag("refresh_luggage_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }

                            Button(
                                onClick = { showCreateTripDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("create_new_trip_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Trip", fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Trips Horizontal Selector (if > 1 trips)
                if (uiState.trips.size > 1) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.trips.forEach { trip ->
                                val isSelected = trip.id == selectedTrip?.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { luggageViewModel.selectTrip(trip.id) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Luggage,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "${trip.title} (${trip.packedItems}/${trip.totalItems})",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.testTag("chip_trip_${trip.id}")
                                )
                            }
                        }
                    }
                }

                // If trips are loading and empty
                if (uiState.isLoading && uiState.trips.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (selectedTrip == null) {
                    // Empty State: No Trips
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Luggage,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "No Luggage Trips Yet",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Create your upcoming flight, road trip, or business trip to organize packing and keep weight strictly under bag limits.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                                )

                                Button(
                                    onClick = { showCreateTripDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("empty_create_trip_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Create Your First Trip")
                                }
                            }
                        }
                    }
                } else {
                    // Active Trip Card
                    item {
                        TripCard(
                            trip = selectedTrip,
                            onPackAll = { luggageViewModel.packAll(selectedTrip.id) },
                            onResetPacking = { luggageViewModel.resetPacking(selectedTrip.id) },
                            onOpenTemplates = { showTemplateSheet = true },
                            onEditTrip = { showEditTripDialog = true },
                            onDeleteTrip = { tripToDelete = selectedTrip }
                        )
                    }

                    // Search & Filter Controls
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Search bar
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { luggageViewModel.setSearchQuery(it) },
                                placeholder = { Text("Search items in trip...") },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                },
                                trailingIcon = {
                                    if (uiState.searchQuery.isNotBlank()) {
                                        IconButton(onClick = { luggageViewModel.setSearchQuery("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_luggage_items_input")
                            )

                            // Packed Filter Segment: All | Unpacked | Packed
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val packedCount = selectedTrip.packedItems
                                val unpackedCount = selectedTrip.totalItems - packedCount

                                FilterChip(
                                    selected = uiState.filterPacked == null,
                                    onClick = { luggageViewModel.setFilterPacked(null) },
                                    label = { Text("All (${selectedTrip.totalItems})") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = uiState.filterPacked == false,
                                    onClick = { luggageViewModel.setFilterPacked(false) },
                                    label = { Text("To Pack ($unpackedCount)") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = uiState.filterPacked == true,
                                    onClick = { luggageViewModel.setFilterPacked(true) },
                                    label = { Text("Packed ($packedCount)") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Category Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                categories.forEach { cat ->
                                    val isSelected = if (cat == "All") uiState.selectedCategory == null else uiState.selectedCategory == cat
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            luggageViewModel.selectCategory(if (cat == "All") null else cat)
                                        },
                                        label = { Text(cat, fontSize = 12.sp) },
                                        modifier = Modifier.testTag("chip_category_$cat")
                                    )
                                }
                            }
                        }
                    }

                    // Items List
                    val filteredItems = uiState.filteredItems

                    if (filteredItems.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (selectedTrip.items.isEmpty()) "Checklist is Empty" else "No matching items",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (selectedTrip.items.isEmpty())
                                            "Load a pre-made template or tap '+ Add Item' to start packing."
                                        else
                                            "Try changing your search filter or category selection.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                                    )

                                    if (selectedTrip.items.isEmpty()) {
                                        FilledTonalButton(
                                            onClick = { showTemplateSheet = true },
                                            modifier = Modifier.testTag("empty_apply_template_btn")
                                        ) {
                                            Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Browse Packing Templates")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(
                            items = filteredItems,
                            key = { it.id }
                        ) { item ->
                            LuggageItemRow(
                                item = item,
                                onTogglePacked = {
                                    luggageViewModel.toggleItem(selectedTrip.id, item.id)
                                },
                                onEditClick = {
                                    editingItem = item
                                },
                                onDeleteClick = {
                                    itemToDelete = item
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Create Trip Dialog
    if (showCreateTripDialog) {
        CreateTripDialog(
            onDismiss = { showCreateTripDialog = false },
            onConfirm = { title, dest, bag, depart, ret, weight, color ->
                luggageViewModel.createTrip(title, dest, bag, depart, ret, weight, color)
                showCreateTripDialog = false
            }
        )
    }

    // Edit Trip Dialog
    if (showEditTripDialog && selectedTrip != null) {
        CreateTripDialog(
            onDismiss = { showEditTripDialog = false },
            onConfirm = { title, dest, bag, depart, ret, weight, color ->
                luggageViewModel.updateTrip(
                    id = selectedTrip.id,
                    title = title,
                    destination = dest,
                    bagType = bag,
                    departureDate = depart,
                    returnDate = ret,
                    maxWeightKg = weight,
                    colorHex = color
                )
                showEditTripDialog = false
            }
        )
    }

    // Add Luggage Item Dialog
    if (showAddItemDialog && selectedTrip != null) {
        AddLuggageItemDialog(
            onDismiss = { showAddItemDialog = false },
            onConfirm = { name, category, quantity, isEssential, weight, notes ->
                luggageViewModel.addItem(
                    tripId = selectedTrip.id,
                    name = name,
                    category = category,
                    quantity = quantity,
                    isEssential = isEssential,
                    weightKg = weight,
                    notes = notes
                )
                showAddItemDialog = false
            }
        )
    }

    // Edit Luggage Item Dialog
    if (editingItem != null && selectedTrip != null) {
        AddLuggageItemDialog(
            initialItem = editingItem,
            onDismiss = { editingItem = null },
            onConfirm = { name, category, quantity, isEssential, weight, notes ->
                luggageViewModel.updateItem(
                    tripId = selectedTrip.id,
                    itemId = editingItem!!.id,
                    name = name,
                    category = category,
                    quantity = quantity,
                    isEssential = isEssential,
                    weightKg = weight,
                    notes = notes
                )
                editingItem = null
            }
        )
    }

    // Template Picker Bottom Sheet
    if (showTemplateSheet && selectedTrip != null) {
        TemplatePickerBottomSheet(
            sheetState = sheetState,
            onDismiss = { showTemplateSheet = false },
            onApplyTemplate = { key ->
                luggageViewModel.applyTemplate(selectedTrip.id, key)
            }
        )
    }

    // Delete Item Confirmation Dialog
    if (itemToDelete != null && selectedTrip != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Item") },
            text = { Text("Are you sure you want to remove '${itemToDelete!!.name}' from your packing list?") },
            confirmButton = {
                Button(
                    onClick = {
                        luggageViewModel.deleteItem(selectedTrip.id, itemToDelete!!.id)
                        itemToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_item_btn")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Trip Confirmation Dialog
    if (tripToDelete != null) {
        AlertDialog(
            onDismissRequest = { tripToDelete = null },
            title = { Text("Delete Trip") },
            text = { Text("Are you sure you want to delete '${tripToDelete!!.title}' and all its packed items?") },
            confirmButton = {
                Button(
                    onClick = {
                        luggageViewModel.deleteTrip(tripToDelete!!.id)
                        tripToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_trip_btn")
                ) {
                    Text("Delete Trip")
                }
            },
            dismissButton = {
                TextButton(onClick = { tripToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
