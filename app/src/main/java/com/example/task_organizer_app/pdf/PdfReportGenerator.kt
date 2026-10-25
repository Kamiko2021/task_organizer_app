package com.example.task_organizer_app.pdf

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.task_organizer_app.model.TaskItem
import com.example.task_organizer_app.model.TaskStatus
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    fun generatePdfReport(context: Context, tasks: List<TaskItem>): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size in points
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        var currentY = 40f

        // --- Header ---
        paint.color = Color.parseColor("#1565C0")
        paint.textSize = 22f
        paint.isFakeBoldText = true
        canvas.drawText("Task Organizer - Summary Report", 40f, currentY, paint)

        currentY += 20f
        paint.color = Color.DKGRAY
        paint.textSize = 10f
        paint.isFakeBoldText = false
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        canvas.drawText("Generated on: ${dateFormat.format(Date())}", 40f, currentY, paint)

        currentY += 15f
        paint.color = Color.LTGRAY
        paint.strokeWidth = 1f
        canvas.drawLine(40f, currentY, 555f, currentY, paint)
        currentY += 25f

        // --- Task Statistics Summary ---
        val total = tasks.size
        val ongoing = tasks.count { it.status == TaskStatus.ONGOING }
        val done = tasks.count { it.status == TaskStatus.DONE }
        val accomplished = tasks.count { it.status == TaskStatus.ACCOMPLISHED }
        val notAccomplished = tasks.count { it.status == TaskStatus.NOT_ACCOMPLISHED }

        paint.color = Color.BLACK
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Task Summary Overview", 40f, currentY, paint)
        currentY += 20f

        paint.textSize = 11f
        paint.isFakeBoldText = false
        canvas.drawText("Total Tasks: $total | Ongoing: $ongoing | Done: $done | Accomplished: $accomplished | Not Accomplished: $notAccomplished", 40f, currentY, paint)
        currentY += 30f

        // --- Pie Chart & Bar Chart Section ---
        val chartTopY = currentY
        val pieBounds = RectF(40f, chartTopY, 180f, chartTopY + 140f)

        // Draw Pie Chart
        val statusCounts = listOf(
            TaskStatus.ONGOING to ongoing,
            TaskStatus.DONE to done,
            TaskStatus.ACCOMPLISHED to accomplished,
            TaskStatus.NOT_ACCOMPLISHED to notAccomplished
        )

        val colors = mapOf(
            TaskStatus.ONGOING to Color.parseColor("#2196F3"),
            TaskStatus.DONE to Color.parseColor("#4CAF50"),
            TaskStatus.ACCOMPLISHED to Color.parseColor("#8E24AA"),
            TaskStatus.NOT_ACCOMPLISHED to Color.parseColor("#F44336")
        )

        var startAngle = 0f
        val piePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        if (total > 0) {
            for ((status, count) in statusCounts) {
                if (count > 0) {
                    val sweepAngle = (count.toFloat() / total) * 360f
                    piePaint.color = colors[status] ?: Color.GRAY
                    canvas.drawArc(pieBounds, startAngle, sweepAngle, true, piePaint)
                    startAngle += sweepAngle
                }
            }
        } else {
            piePaint.color = Color.LTGRAY
            canvas.drawOval(pieBounds, piePaint)
        }

        // Pie Chart Legend
        var legendY = chartTopY + 15f
        for ((status, count) in statusCounts) {
            val pct = if (total > 0) (count * 100) / total else 0
            piePaint.color = colors[status] ?: Color.GRAY
            canvas.drawRect(200f, legendY - 10f, 215f, legendY, piePaint)

            paint.color = Color.BLACK
            paint.textSize = 10f
            canvas.drawText("${status.displayName}: $count ($pct%)", 225f, legendY, paint)
            legendY += 22f
        }

        // Draw Bar Chart on the Right
        val barStartX = 360f
        val barBottomY = chartTopY + 130f
        val barWidth = 30f
        val maxCount = maxOf(statusCounts.maxOfOrNull { it.second } ?: 1, 1)

        // Draw Bar Chart Axes
        paint.color = Color.GRAY
        paint.strokeWidth = 1.5f
        canvas.drawLine(barStartX, barBottomY, barStartX + 180f, barBottomY, paint) // X Axis
        canvas.drawLine(barStartX, barBottomY - 100f, barStartX, barBottomY, paint) // Y Axis

        var xOffset = barStartX + 15f
        for ((status, count) in statusCounts) {
            val barHeight = (count.toFloat() / maxCount) * 80f
            val barRect = RectF(xOffset, barBottomY - barHeight, xOffset + barWidth, barBottomY)

            piePaint.color = colors[status] ?: Color.GRAY
            canvas.drawRect(barRect, piePaint)

            // Bar Count Label
            paint.color = Color.BLACK
            paint.textSize = 9f
            canvas.drawText("$count", xOffset + (barWidth / 4), barBottomY - barHeight - 4f, paint)

            xOffset += barWidth + 12f
        }

        currentY = chartTopY + 160f
        paint.color = Color.LTGRAY
        canvas.drawLine(40f, currentY, 555f, currentY, paint)
        currentY += 25f

        // --- Task Breakdown Table ---
        paint.color = Color.BLACK
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Detailed Task List", 40f, currentY, paint)
        currentY += 20f

        // Table Header
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("Title", 40f, currentY, paint)
        canvas.drawText("Milestone", 200f, currentY, paint)
        canvas.drawText("Status", 340f, currentY, paint)
        canvas.drawText("Deadline", 450f, currentY, paint)

        currentY += 8f
        paint.color = Color.GRAY
        canvas.drawLine(40f, currentY, 555f, currentY, paint)
        currentY += 15f

        // Table Rows
        paint.isFakeBoldText = false
        paint.color = Color.DKGRAY

        val taskDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val displayTasks = tasks.take(18) // Fit on single page for clean report

        for (task in displayTasks) {
            val titleText = if (task.title.length > 22) task.title.take(20) + "..." else task.title
            val milestoneText = if (task.milestone.length > 18) task.milestone.take(16) + "..." else task.milestone
            val deadlineText = task.deadlineEpochMillis?.let { taskDateFormat.format(Date(it)) } ?: "None"

            canvas.drawText(titleText, 40f, currentY, paint)
            canvas.drawText(milestoneText, 200f, currentY, paint)
            canvas.drawText(task.status.displayName, 340f, currentY, paint)
            canvas.drawText(deadlineText, 450f, currentY, paint)

            currentY += 18f
            if (currentY > 800f) break
        }

        pdfDocument.finishPage(page)

        // Save file to cache directory
        val fileName = "Task_Organizer_Report_${System.currentTimeMillis()}.pdf"
        val outputFile = File(context.cacheDir, fileName)
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }

    fun sharePdf(context: Context, pdfFile: File) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Task Organizer Report")
            putExtra(Intent.EXTRA_TEXT, "Attached is the latest Task Organizer summary report.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share Task Report PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun savePdfToDownloads(context: Context, pdfFile: File): String? {
        val fileName = pdfFile.name
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return null

            resolver.openOutputStream(uri)?.use { out: OutputStream ->
                pdfFile.inputStream().copyTo(out)
            }
            return Environment.DIRECTORY_DOWNLOADS + "/" + fileName
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destFile = File(downloadsDir, fileName)
            pdfFile.copyTo(destFile, overwrite = true)
            return destFile.absolutePath
        }
    }
}
