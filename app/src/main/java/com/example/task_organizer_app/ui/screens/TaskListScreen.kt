package com.example.task_organizer_app.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.task_organizer_app.model.TaskItem
import com.example.task_organizer_app.model.TaskStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskListScreen(
    tasks: List<TaskItem>,
    allTasks: List<TaskItem>,
    selectedStatus: TaskStatus?,
    selectedMilestone: String?,
    onStatusSelected: (TaskStatus?) -> Unit,
    onMilestoneSelected: (String?) -> Unit,
    onTaskCreatedOrUpdated: (
        id: String?,
        title: String,
        description: String,
        status: TaskStatus,
        milestone: String,
        deadlineMillis: Long?,
        reminderMillis: Long?
    ) -> Unit,
    onTaskStatusChanged: (TaskItem, TaskStatus) -> Unit,
    onTaskDeleted: (String) -> Unit
) {
    var showTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskItem?>(null) }

    // Unique Milestones extracted from all tasks
    val milestones = remember(allTasks) {
        allTasks.map { it.milestone }.distinct().filter { it.isNotBlank() }
    }

    val tabs = remember { listOf<TaskStatus?>(null) + TaskStatus.entries }
    val currentTabIndex = remember(selectedStatus) {
        tabs.indexOf(selectedStatus).coerceAtLeast(0)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Milestone filter chips
            if (milestones.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedMilestone == null,
                            onClick = { onMilestoneSelected(null) },
                            label = { Text("All Milestones") }
                        )
                    }
                    items(milestones) { m ->
                        FilterChip(
                            selected = selectedMilestone.equals(m, ignoreCase = true),
                            onClick = {
                                if (selectedMilestone.equals(m, ignoreCase = true)) {
                                    onMilestoneSelected(null)
                                } else {
                                    onMilestoneSelected(m)
                                }
                            },
                            label = { Text(m) }
                        )
                    }
                }
            }

            // Task Status Tabs
            ScrollableTabRow(
                selectedTabIndex = currentTabIndex,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, status ->
                    Tab(
                        selected = index == currentTabIndex,
                        onClick = { onStatusSelected(status) },
                        text = {
                            val count = if (status == null) allTasks.size else allTasks.count { it.status == status }
                            Text(text = "${status?.displayName ?: "All"} ($count)")
                        }
                    )
                }
            }

            // Task Items List
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks found.\nTap '+' to create a new task!",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onEdit = {
                                taskToEdit = task
                                showTaskDialog = true
                            },
                            onStatusChange = { newStatus -> onTaskStatusChanged(task, newStatus) },
                            onDelete = { onTaskDeleted(task.id) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                taskToEdit = null
                showTaskDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task")
        }

        if (showTaskDialog) {
            TaskDialog(
                initialTask = taskToEdit,
                onDismiss = { showTaskDialog = false },
                onSave = { id, title, desc, status, milestone, deadline, reminder ->
                    onTaskCreatedOrUpdated(id, title, desc, status, milestone, deadline, reminder)
                }
            )
        }
    }
}

@Composable
fun TaskCard(
    task: TaskItem,
    onEdit: () -> Unit,
    onStatusChange: (TaskStatus) -> Unit,
    onDelete: () -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val reminderFormat = remember { SimpleDateFormat("MMM dd HH:mm", Locale.getDefault()) }
    val isOverdue = remember(task.deadlineEpochMillis) {
        task.deadlineEpochMillis?.let { it < System.currentTimeMillis() && task.status != TaskStatus.DONE && task.status != TaskStatus.ACCOMPLISHED } ?: false
    }

    val statusColor = Color(task.status.color)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Milestone Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = task.milestone,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status Badge
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(statusColor)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = task.status.displayName,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box {
                        IconButton(onClick = { isMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Task Actions")
                        }
                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Task") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    isMenuExpanded = false
                                    onEdit()
                                }
                            )
                            TaskStatus.entries.forEach { s ->
                                if (s != task.status) {
                                    DropdownMenuItem(
                                        text = { Text("Mark as ${s.displayName}") },
                                        onClick = {
                                            isMenuExpanded = false
                                            onStatusChange(s)
                                        }
                                    )
                                }
                            }
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color.Red) },
                                leadingIcon = { Icon(Icons.Default.Delete, tint = Color.Red, contentDescription = null) },
                                onClick = {
                                    isMenuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = task.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Deadline & Reminder Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (task.deadlineEpochMillis != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = if (isOverdue) Color.Red else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.height(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Due: ${dateFormat.format(Date(task.deadlineEpochMillis))}" + if (isOverdue) " (Overdue)" else "",
                            fontSize = 12.sp,
                            color = if (isOverdue) Color.Red else MaterialTheme.colorScheme.outline,
                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                if (task.reminderEpochMillis != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.height(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = reminderFormat.format(Date(task.reminderEpochMillis)),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
