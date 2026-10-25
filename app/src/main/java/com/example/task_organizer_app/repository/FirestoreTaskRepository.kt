package com.example.task_organizer_app.repository

import android.util.Log
import com.example.task_organizer_app.model.TaskItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirestoreTaskRepository {
    private val TAG = "FirestoreTaskRepo"
    private val localBackup = MutableStateFlow<List<TaskItem>>(emptyList())

    private val firestore: FirebaseFirestore? by lazy {
        runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    }

    fun getTasksFlow(): Flow<List<TaskItem>> = callbackFlow {
        val db = firestore
        if (db == null) {
            val job = launch {
                localBackup.collect { items ->
                    trySend(items)
                }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        val listener = db.collection("tasks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching tasks", error)
                    trySend(localBackup.value)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val tasks = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { TaskItem.fromMap(it) }
                    }
                    localBackup.value = tasks
                    trySend(tasks)
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun saveTask(task: TaskItem): Boolean {
        // Update local memory backup immediately
        val currentList = localBackup.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == task.id }
        if (index >= 0) {
            currentList[index] = task
        } else {
            currentList.add(0, task)
        }
        localBackup.value = currentList

        val db = firestore ?: return true
        return try {
            db.collection("tasks").document(task.id).set(task.toMap()).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save task to Firestore", e)
            false
        }
    }

    suspend fun deleteTask(taskId: String): Boolean {
        localBackup.value = localBackup.value.filterNot { it.id == taskId }

        val db = firestore ?: return true
        return try {
            db.collection("tasks").document(taskId).delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete task from Firestore", e)
            false
        }
    }
}
