package com.example.task_organizer_app.model

import java.util.UUID

data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val status: TaskStatus = TaskStatus.ONGOING,
    val milestone: String = "General",
    val deadlineEpochMillis: Long? = null,
    val reminderEpochMillis: Long? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "title" to title,
            "description" to description,
            "status" to status.name,
            "milestone" to milestone,
            "deadlineEpochMillis" to deadlineEpochMillis,
            "reminderEpochMillis" to reminderEpochMillis,
            "createdAtEpochMillis" to createdAtEpochMillis
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): TaskItem {
            return TaskItem(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                title = map["title"] as? String ?: "",
                description = map["description"] as? String ?: "",
                status = TaskStatus.fromString(map["status"] as? String),
                milestone = map["milestone"] as? String ?: "General",
                deadlineEpochMillis = (map["deadlineEpochMillis"] as? Number)?.toLong(),
                reminderEpochMillis = (map["reminderEpochMillis"] as? Number)?.toLong(),
                createdAtEpochMillis = (map["createdAtEpochMillis"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
