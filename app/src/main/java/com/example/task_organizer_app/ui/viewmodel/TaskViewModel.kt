package com.example.task_organizer_app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.task_organizer_app.model.AppUpdateInfo
import com.example.task_organizer_app.model.ReminderItem
import com.example.task_organizer_app.model.TaskItem
import com.example.task_organizer_app.model.TaskStatus
import com.example.task_organizer_app.pdf.PdfReportGenerator
import com.example.task_organizer_app.repository.FirestoreTaskRepository
import com.example.task_organizer_app.repository.RealtimeReminderRepository
import com.example.task_organizer_app.repository.RemoteConfigManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val taskRepository = FirestoreTaskRepository()
    private val reminderRepository = RealtimeReminderRepository(application)
    private val remoteConfigManager = RemoteConfigManager()

    private val _rawTasks = MutableStateFlow<List<TaskItem>>(emptyList())
    
    val selectedStatusFilter = MutableStateFlow<TaskStatus?>(null)
    val selectedMilestoneFilter = MutableStateFlow<String?>(null)

    val tasks: StateFlow<List<TaskItem>> = combine(
        _rawTasks,
        selectedStatusFilter,
        selectedMilestoneFilter
    ) { allTasks, status, milestone ->
        allTasks.filter { task ->
            (status == null || task.status == status) &&
                    (milestone == null || task.milestone.equals(milestone, ignoreCase = true))
        }
    }.asStateFlow(viewModelScope, _rawTasks.value)

    val allTasks: StateFlow<List<TaskItem>> = _rawTasks.asStateFlow()

    private val _selectedCalendarDateMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedCalendarDateMillis: StateFlow<Long> = _selectedCalendarDateMillis.asStateFlow()

    val tasksForSelectedDate: StateFlow<List<TaskItem>> = combine(
        _rawTasks,
        _selectedCalendarDateMillis
    ) { allTasks, dateMillis ->
        val calSelected = Calendar.getInstance().apply { timeInMillis = dateMillis }
        allTasks.filter { task ->
            val taskDate = task.deadlineEpochMillis ?: task.reminderEpochMillis ?: task.createdAtEpochMillis
            val calTask = Calendar.getInstance().apply { timeInMillis = taskDate }
            calSelected.get(Calendar.YEAR) == calTask.get(Calendar.YEAR) &&
                    calSelected.get(Calendar.DAY_OF_YEAR) == calTask.get(Calendar.DAY_OF_YEAR)
        }
    }.asStateFlow(viewModelScope, emptyList())

    private val _appUpdateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val appUpdateInfo: StateFlow<AppUpdateInfo?> = _appUpdateInfo.asStateFlow()

    private val _autoCheckUpdates = MutableStateFlow(true)
    val autoCheckUpdates: StateFlow<Boolean> = _autoCheckUpdates.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    init {
        // Collect real-time tasks from Firestore
        viewModelScope.launch {
            taskRepository.getTasksFlow().collect { list ->
                _rawTasks.value = list
            }
        }

        // Auto update check on launch if enabled
        checkAppUpdates()
    }

    fun setSelectedCalendarDate(dateMillis: Long) {
        _selectedCalendarDateMillis.value = dateMillis
    }

    fun setAutoCheckUpdates(enabled: Boolean) {
        _autoCheckUpdates.value = enabled
    }

    fun checkAppUpdates() {
        viewModelScope.launch {
            try {
                val packageInfo = getApplication<Application>().packageManager
                    .getPackageInfo(getApplication<Application>().packageName, 0)
                val currentCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode
                }

                val updateInfo = remoteConfigManager.checkForUpdates(currentCode)
                _appUpdateInfo.value = updateInfo

                if (updateInfo.isUpdateAvailable) {
                    _userMessage.emit("New update available: v${updateInfo.latestVersionName}")
                }
            } catch (e: Exception) {
                _userMessage.emit("Failed to check for updates")
            }
        }
    }

    fun saveTask(
        id: String? = null,
        title: String,
        description: String,
        status: TaskStatus,
        milestone: String,
        deadlineMillis: Long?,
        reminderMillis: Long?
    ) {
        viewModelScope.launch {
            val task = TaskItem(
                id = id ?: java.util.UUID.randomUUID().toString(),
                title = title.trim(),
                description = description.trim(),
                status = status,
                milestone = milestone.ifBlank { "General" },
                deadlineEpochMillis = deadlineMillis,
                reminderEpochMillis = reminderMillis
            )

            val success = taskRepository.saveTask(task)
            if (success) {
                if (reminderMillis != null && reminderMillis > System.currentTimeMillis()) {
                    val reminderItem = ReminderItem(
                        reminderId = "rem_${task.id}",
                        taskId = task.id,
                        taskTitle = task.title,
                        reminderTimeMillis = reminderMillis,
                        isTriggered = false
                    )
                    reminderRepository.addOrUpdateReminder(reminderItem)
                }
                _userMessage.emit("Task saved successfully!")
            } else {
                _userMessage.emit("Failed to save task.")
            }
        }
    }

    fun updateTaskStatus(task: TaskItem, newStatus: TaskStatus) {
        viewModelScope.launch {
            val updated = task.copy(status = newStatus)
            taskRepository.saveTask(updated)
            _userMessage.emit("Status updated to ${newStatus.displayName}")
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
            reminderRepository.removeReminder("rem_$taskId", taskId)
            _userMessage.emit("Task deleted.")
        }
    }

    fun sharePdfReport() {
        viewModelScope.launch {
            val currentTasks = _rawTasks.value
            if (currentTasks.isEmpty()) {
                _userMessage.emit("No tasks available to generate report.")
                return@launch
            }
            try {
                val pdfFile = PdfReportGenerator.generatePdfReport(getApplication(), currentTasks)
                PdfReportGenerator.sharePdf(getApplication(), pdfFile)
            } catch (e: Exception) {
                _userMessage.emit("Error generating PDF report")
            }
        }
    }

    fun savePdfReport() {
        viewModelScope.launch {
            val currentTasks = _rawTasks.value
            if (currentTasks.isEmpty()) {
                _userMessage.emit("No tasks available to generate report.")
                return@launch
            }
            try {
                val pdfFile = PdfReportGenerator.generatePdfReport(getApplication(), currentTasks)
                val savedPath = PdfReportGenerator.savePdfToDownloads(getApplication(), pdfFile)
                if (savedPath != null) {
                    _userMessage.emit("PDF report saved to Downloads: $savedPath")
                } else {
                    _userMessage.emit("Failed to save PDF to Downloads")
                }
            } catch (e: Exception) {
                _userMessage.emit("Error saving PDF report")
            }
        }
    }

    fun dismissUpdatePrompt() {
        _appUpdateInfo.value = _appUpdateInfo.value?.copy(isUpdateAvailable = false)
    }
}

// Extension to turn Flow into StateFlow in ViewModel scope cleanly
private fun <T> kotlinx.coroutines.flow.Flow<T>.asStateFlow(
    scope: kotlinx.coroutines.CoroutineScope,
    initialValue: T
): StateFlow<T> {
    val state = MutableStateFlow(initialValue)
    scope.launch {
        collect { state.value = it }
    }
    return state.asStateFlow()
}
