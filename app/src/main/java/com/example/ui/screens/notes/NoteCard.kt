package com.example.ui.screens.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChecklistItemDto
import com.example.data.model.NoteDto

fun parseNoteColor(hex: String, fallback: Color = Color(0xFFFEF3C7)): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = when (clean.length) {
            6 -> 0xFF000000.toLong() or clean.toLong(16)
            8 -> clean.toLong(16)
            else -> return fallback
        }
        Color(colorInt)
    } catch (_: Exception) {
        fallback
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteCard(
    note: NoteDto,
    onCardClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
    onToggleChecklistItem: (itemId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val cardColor = parseNoteColor(note.colorHex)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onCardClick)
            .testTag("note_card_${note.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Category badge, Pin button, Options menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Category Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.08f),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = note.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp
                        ),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pin toggle
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("pin_button_${note.id}")
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.isPinned) "Unpin note" else "Pin note",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // More Menu
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("more_menu_${note.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More actions",
                                tint = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Note") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onCardClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (note.isPinned) "Unpin" else "Pin to Top") },
                                leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onTogglePin()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (note.isArchived) "Unarchive" else "Archive") },
                                leadingIcon = {
                                    Icon(
                                        if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onToggleArchive()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Title
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black.copy(alpha = 0.88f)
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Content snippet
            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Black.copy(alpha = 0.72f),
                        lineHeight = 20.sp
                    ),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Checklist Preview
            if (note.checklist.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val previewItems = note.checklist.take(3)
                    previewItems.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleChecklistItem(item.id) }
                                .padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (item.isDone) MaterialTheme.colorScheme.primary
                                        else Color.Black.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.isDone) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (item.isDone) Color.Black.copy(alpha = 0.45f)
                                    else Color.Black.copy(alpha = 0.85f),
                                    textDecoration = if (item.isDone) TextDecoration.LineThrough else null
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (note.checklist.size > 3) {
                        Text(
                            text = "+${note.checklist.size - 3} more items",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.Black.copy(alpha = 0.5f),
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(start = 24.dp, top = 2.dp, bottom = 2.dp)
                        )
                    }
                }
            }

            // Tags
            if (note.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    note.tags.forEach { tag ->
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.Black.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.06f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Bottom Date / Status Indicator
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dateDisplay = note.updatedAt?.take(10) ?: note.createdAt?.take(10) ?: "Saved"
                Text(
                    text = dateDisplay,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.Black.copy(alpha = 0.45f),
                        fontSize = 11.sp
                    )
                )

                if (note.checklist.isNotEmpty()) {
                    val completed = note.checklist.count { it.isDone }
                    val total = note.checklist.size
                    Text(
                        text = "$completed/$total done",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.Black.copy(alpha = 0.6f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}
