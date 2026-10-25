package com.example.task_organizer_app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.task_organizer_app.model.AppUpdateInfo

@Composable
fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {
            if (!updateInfo.isForceUpdate) {
                onDismiss()
            }
        },
        title = {
            Text(if (updateInfo.isForceUpdate) "Mandatory App Update" else "New Version Available!")
        },
        text = {
            Text(
                "Version ${updateInfo.latestVersionName} is now available.\n\n" +
                        "Release Notes:\n${updateInfo.releaseNotes}"
            )
        },
        confirmButton = {
            Button(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.updateUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }) {
                Text("Update Now")
            }
        },
        dismissButton = {
            if (!updateInfo.isForceUpdate) {
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            }
        }
    )
}
