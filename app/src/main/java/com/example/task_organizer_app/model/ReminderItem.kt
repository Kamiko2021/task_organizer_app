package com.example.task_organizer_app.model

data class ReminderItem(
    val reminderId: String = "",
    val taskId: String = "",
    val taskTitle: String = "",
    val reminderTimeMillis: Long = 0L,
    val isTriggered: Boolean = false
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "reminderId" to reminderId,
            "taskId" to taskId,
            "taskTitle" to taskTitle,
            "reminderTimeMillis" to reminderTimeMillis,
            "isTriggered" to isTriggered
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): ReminderItem {
            return ReminderItem(
                reminderId = map["reminderId"] as? String ?: "",
                taskId = map["taskId"] as? String ?: "",
                taskTitle = map["taskTitle"] as? String ?: "",
                reminderTimeMillis = (map["reminderTimeMillis"] as? Number)?.toLong() ?: 0L,
                isTriggered = map["isTriggered"] as? Boolean ?: false
            )
        }
    }
}
