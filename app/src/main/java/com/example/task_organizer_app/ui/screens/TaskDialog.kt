package com.example.task_organizer_app.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.task_organizer_app.model.TaskItem
import com.example.task_organizer_app.model.TaskStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TaskDialog(
    initialTask: TaskItem? = null,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        title: String,
        description: String,
        status: TaskStatus,
        milestone: String,
        deadlineMillis: Long?,
        reminderMillis: Long?
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var status by remember { mutableStateOf(initialTask?.status ?: TaskStatus.ONGOING) }
    var milestone by remember { mutableStateOf(initialTask?.milestone ?: "Sprint 1") }
    var deadlineMillis by remember { mutableStateOf<Long?>(initialTask?.deadlineEpochMillis) }
    var reminderMillis by remember { mutableStateOf<Long?>(initialTask?.reminderEpochMillis) }

    var isStatusMenuExpanded by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialTask == null) "Add New Task" else "Edit Task")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = milestone,
                    onValueChange = { milestone = it },
                    label = { Text("Milestone (e.g., Sprint 1, Phase 2)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Status Dropdown
                Column {
                    Text("Status", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { isStatusMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(status.displayName)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = isStatusMenuExpanded,
                        onDismissRequest = { isStatusMenuExpanded = false }
                    ) {
                        TaskStatus.entries.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.displayName) },
                                onClick = {
                                    status = item
                                    isStatusMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Optional Deadline Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Optional Deadline", fontSize = 12.sp)
                        Text(
                            text = deadlineMillis?.let { dateFormat.format(Date(it)) } ?: "No deadline set",
                            fontSize = 14.sp
                        )
                    }
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        deadlineMillis?.let { cal.timeInMillis = it }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selectedCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    set(Calendar.HOUR_OF_DAY, 23)
                                    set(Calendar.MINUTE, 59)
                                }
                                deadlineMillis = selectedCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Pick Deadline")
                    }
                    if (deadlineMillis != null) {
                        TextButton(onClick = { deadlineMillis = null }) {
                            Text("Clear")
                        }
                    }
                }

                // Optional Reminder Notification Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reminder Notification", fontSize = 12.sp)
                        Text(
                            text = reminderMillis?.let { timeFormat.format(Date(it)) } ?: "No reminder set",
                            fontSize = 14.sp
                        )
                    }
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        reminderMillis?.let { cal.timeInMillis = it }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        val selectedCal = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                            set(Calendar.HOUR_OF_DAY, hourOfDay)
                                            set(Calendar.MINUTE, minute)
                                            set(Calendar.SECOND, 0)
                                        }
                                        reminderMillis = selectedCal.timeInMillis
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Pick Reminder Time")
                    }
                    if (reminderMillis != null) {
                        TextButton(onClick = { reminderMillis = null }) {
                            Text("Clear")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(
                        initialTask?.id,
                        title,
                        description,
                        status,
                        milestone,
                        deadlineMillis,
                        reminderMillis
                    )
                    onDismiss()
                }
            ) {
                Text("Save Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
