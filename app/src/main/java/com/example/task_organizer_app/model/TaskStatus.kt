package com.example.task_organizer_app.model

import androidx.compose.ui.graphics.Color

enum class TaskStatus(val displayName: String, val color: Long) {
    ONGOING("Ongoing", 0xFF2196F3),        // Blue
    DONE("Done", 0xFF4CAF50),              // Green
    ACCOMPLISHED("Accomplished", 0xFF8E24AA), // Purple
    NOT_ACCOMPLISHED("Not Accomplished", 0xFFF44336); // Red

    companion object {
        fun fromString(value: String?): TaskStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: ONGOING
        }
    }
}
