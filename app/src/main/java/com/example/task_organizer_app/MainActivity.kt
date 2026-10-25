package com.example.task_organizer_app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import com.example.task_organizer_app.notification.NotificationHelper
import com.example.task_organizer_app.ui.screens.CalendarScreen
import com.example.task_organizer_app.ui.screens.ReportScreen
import com.example.task_organizer_app.ui.screens.SettingsScreen
import com.example.task_organizer_app.ui.screens.TaskListScreen
import com.example.task_organizer_app.ui.screens.UpdateDialog
import com.example.task_organizer_app.ui.theme.Task_organizer_appTheme
import com.example.task_organizer_app.ui.viewmodel.TaskViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TaskViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notification permission is required for task reminders.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            Task_organizer_appTheme {
                TaskOrganizerMainScreen(viewModel = viewModel)
            }
        }
    }
}

enum class NavScreen(val title: String, val icon: ImageVector) {
    TASKS("Tasks", Icons.Default.List),
    CALENDAR("Calendar", Icons.Default.CalendarMonth),
    REPORT("Reports", Icons.Default.Assessment),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskOrganizerMainScreen(viewModel: TaskViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val filteredTasks by viewModel.tasks.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val selectedStatus by viewModel.selectedStatusFilter.collectAsState()
    val selectedMilestone by viewModel.selectedMilestoneFilter.collectAsState()

    val selectedDateMillis by viewModel.selectedCalendarDateMillis.collectAsState()
    val tasksOnSelectedDate by viewModel.tasksForSelectedDate.collectAsState()

    val appUpdateInfo by viewModel.appUpdateInfo.collectAsState()
    val autoCheckUpdates by viewModel.autoCheckUpdates.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = NavScreen.entries[selectedTab].title) }
            )
        },
        bottomBar = {
            NavigationBar {
                NavScreen.entries.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> TaskListScreen(
                    tasks = filteredTasks,
                    allTasks = allTasks,
                    selectedStatus = selectedStatus,
                    selectedMilestone = selectedMilestone,
                    onStatusSelected = { viewModel.selectedStatusFilter.value = it },
                    onMilestoneSelected = { viewModel.selectedMilestoneFilter.value = it },
                    onTaskCreatedOrUpdated = { id, title, desc, status, milestone, deadline, reminder ->
                        viewModel.saveTask(id, title, desc, status, milestone, deadline, reminder)
                    },
                    onTaskStatusChanged = { task, newStatus ->
                        viewModel.updateTaskStatus(task, newStatus)
                    },
                    onTaskDeleted = { taskId ->
                        viewModel.deleteTask(taskId)
                    }
                )

                1 -> CalendarScreen(
                    selectedDateMillis = selectedDateMillis,
                    tasksOnDate = tasksOnSelectedDate,
                    onDateSelected = { dateMillis ->
                        viewModel.setSelectedCalendarDate(dateMillis)
                    },
                    onTaskStatusChanged = { task, newStatus ->
                        viewModel.updateTaskStatus(task, newStatus)
                    }
                )

                2 -> ReportScreen(
                    tasks = allTasks,
                    onSharePdf = { viewModel.sharePdfReport() },
                    onSavePdf = { viewModel.savePdfReport() }
                )

                3 -> SettingsScreen(
                    currentVersionName = "1.0.0",
                    currentVersionCode = 1,
                    appUpdateInfo = appUpdateInfo,
                    autoCheckUpdates = autoCheckUpdates,
                    onAutoCheckToggled = { viewModel.setAutoCheckUpdates(it) },
                    onCheckForUpdatesClicked = { viewModel.checkAppUpdates() }
                )
            }

            // Show Remote Config update dialog if update is available
            appUpdateInfo?.let { updateInfo ->
                if (updateInfo.isUpdateAvailable) {
                    UpdateDialog(
                        updateInfo = updateInfo,
                        onDismiss = { viewModel.dismissUpdatePrompt() }
                    )
                }
            }
        }
    }
}
