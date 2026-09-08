package com.example.ui.screens.loans

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LoanDto
import com.example.ui.theme.EmeraldPrimary
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ReminderDialog(
    loan: LoanDto,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val pkrFormat = NumberFormat.getNumberInstance(Locale.US)
    val formattedRemaining = pkrFormat.format(loan.remainingAmount.toLong())
    val dueDateText = loan.dueDate?.take(10) ?: "soon"

    val reminderMessage = "Assalam-o-Alaikum ${loan.personName}, this is a gentle reminder regarding the pending Udhar of Rs. $formattedRemaining (Original: Rs. ${pkrFormat.format(loan.amount.toLong())}). Please let me know when convenient. JazakAllah khair!"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = "Reminder Icon",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Send Reminder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close"
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Friendly, polite reminder message prepared for ${loan.personName}:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = reminderMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                if (!loan.phoneNumber.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Recipient phone: ${loan.phoneNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Udhar Reminder", reminderMessage)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Reminder copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copy_reminder_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy")
                }

                Button(
                    onClick = {
                        try {
                            val phone = loan.phoneNumber?.filter { it.isDigit() }
                            val intent = if (!phone.isNullOrBlank()) {
                                Intent(Intent.ACTION_VIEW).apply {
                                    val formattedPhone = if (phone.startsWith("0")) "92" + phone.substring(1) else phone
                                    data = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(reminderMessage)}")
                                }
                            } else {
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, reminderMessage)
                                }
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, reminderMessage)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Reminder"))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_whatsapp_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Share",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share / WA")
                }
            }
        },
        dismissButton = null
    )
}
