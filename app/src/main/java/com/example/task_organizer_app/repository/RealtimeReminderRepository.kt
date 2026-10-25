package com.example.task_organizer_app.repository

import android.content.Context
import android.util.Log
import com.example.task_organizer_app.model.ReminderItem
import com.example.task_organizer_app.notification.ReminderScheduler
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class RealtimeReminderRepository(private val context: Context) {
    private val TAG = "RealtimeReminderRepo"

    private val realtimeDb: FirebaseDatabase? by lazy {
        runCatching { FirebaseDatabase.getInstance() }.getOrNull()
    }

    suspend fun addOrUpdateReminder(reminder: ReminderItem) {
        // First schedule locally via AlarmManager
        if (reminder.reminderTimeMillis > System.currentTimeMillis() && !reminder.isTriggered) {
            ReminderScheduler.scheduleReminder(
                context = context,
                taskId = reminder.taskId,
                taskTitle = reminder.taskTitle,
                triggerTimeMillis = reminder.reminderTimeMillis
            )
        }

        // Then sync to Firebase Realtime Database
        val db = realtimeDb ?: return
        try {
            db.getReference("reminders")
                .child(reminder.reminderId)
                .setValue(reminder.toMap())
                .await()
            Log.d(TAG, "Reminder synced to Realtime DB: ${reminder.reminderId}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync reminder to Realtime DB", e)
        }
    }

    suspend fun removeReminder(reminderId: String, taskId: String) {
        ReminderScheduler.cancelReminder(context, taskId)

        val db = realtimeDb ?: return
        try {
            db.getReference("reminders")
                .child(reminderId)
                .removeValue()
                .await()
            Log.d(TAG, "Reminder removed from Realtime DB: $reminderId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove reminder from Realtime DB", e)
        }
    }
}
