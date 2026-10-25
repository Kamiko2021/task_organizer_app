package com.example.task_organizer_app.model

data class AppUpdateInfo(
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0.0",
    val minRequiredVersionCode: Int = 1,
    val updateUrl: String = "https://play.google.com/store/apps",
    val releaseNotes: String = "Bug fixes and performance improvements.",
    val isUpdateAvailable: Boolean = false,
    val isForceUpdate: Boolean = false
)
