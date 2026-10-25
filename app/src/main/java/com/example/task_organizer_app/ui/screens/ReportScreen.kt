package com.example.task_organizer_app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.task_organizer_app.model.TaskItem
import com.example.task_organizer_app.model.TaskStatus

@Composable
fun ReportScreen(
    tasks: List<TaskItem>,
    onSharePdf: () -> Unit,
    onSavePdf: () -> Unit
) {
    val totalTasks = tasks.size
    val ongoingCount = remember(tasks) { tasks.count { it.status == TaskStatus.ONGOING } }
    val doneCount = remember(tasks) { tasks.count { it.status == TaskStatus.DONE } }
    val accomplishedCount = remember(tasks) { tasks.count { it.status == TaskStatus.ACCOMPLISHED } }
    val notAccomplishedCount = remember(tasks) { tasks.count { it.status == TaskStatus.NOT_ACCOMPLISHED } }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // PDF Action Buttons Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "PDF Summary Report",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Generate and export a PDF report containing task metrics, pie charts, and bar charts.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onSharePdf,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share PDF")
                    }
                    OutlinedButton(
                        onClick = onSavePdf,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save PDF")
                    }
                }
            }
        }

        // Summary Statistics Grid
        Text(
            text = "Statistics Dashboard",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(modifier = Modifier.weight(1f), label = "Total", count = totalTasks, color = Color.Gray)
            StatCard(modifier = Modifier.weight(1f), label = "Ongoing", count = ongoingCount, color = Color(TaskStatus.ONGOING.color))
            StatCard(modifier = Modifier.weight(1f), label = "Done", count = doneCount, color = Color(TaskStatus.DONE.color))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(modifier = Modifier.weight(1f), label = "Accomplished", count = accomplishedCount, color = Color(TaskStatus.ACCOMPLISHED.color))
            StatCard(modifier = Modifier.weight(1f), label = "Not Accomplished", count = notAccomplishedCount, color = Color(TaskStatus.NOT_ACCOMPLISHED.color))
        }

        // Pie Chart Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Status Distribution (Pie Chart)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Compose Canvas Pie Chart
                    ComposePieChart(
                        ongoing = ongoingCount,
                        done = doneCount,
                        accomplished = accomplishedCount,
                        notAccomplished = notAccomplishedCount,
                        modifier = Modifier.size(150.dp)
                    )

                    // Pie Chart Legend
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LegendItem("Ongoing", ongoingCount, totalTasks, Color(TaskStatus.ONGOING.color))
                        LegendItem("Done", doneCount, totalTasks, Color(TaskStatus.DONE.color))
                        LegendItem("Accomplished", accomplishedCount, totalTasks, Color(TaskStatus.ACCOMPLISHED.color))
                        LegendItem("Not Accomplished", notAccomplishedCount, totalTasks, Color(TaskStatus.NOT_ACCOMPLISHED.color))
                    }
                }
            }
        }

        // Bar Chart Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Task Status Counts (Bar Chart)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                ComposeBarChart(
                    ongoing = ongoingCount,
                    done = doneCount,
                    accomplished = accomplishedCount,
                    notAccomplished = notAccomplishedCount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, label: String, count: Int, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$count", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun LegendItem(label: String, count: Int, total: Int, color: Color) {
    val pct = if (total > 0) (count * 100) / total else 0
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "$label: $count ($pct%)", fontSize = 12.sp)
    }
}

@Composable
fun ComposePieChart(
    ongoing: Int,
    done: Int,
    accomplished: Int,
    notAccomplished: Int,
    modifier: Modifier = Modifier
) {
    val total = (ongoing + done + accomplished + notAccomplished).toFloat()
    val ongoingColor = Color(TaskStatus.ONGOING.color)
    val doneColor = Color(TaskStatus.DONE.color)
    val accomplishedColor = Color(TaskStatus.ACCOMPLISHED.color)
    val notAccomplishedColor = Color(TaskStatus.NOT_ACCOMPLISHED.color)

    Canvas(modifier = modifier) {
        if (total == 0f) {
            drawCircle(color = Color.LightGray, style = Fill)
            return@Canvas
        }

        var startAngle = -90f

        val slices = listOf(
            ongoing.toFloat() to ongoingColor,
            done.toFloat() to doneColor,
            accomplished.toFloat() to accomplishedColor,
            notAccomplished.toFloat() to notAccomplishedColor
        )

        for ((count, color) in slices) {
            if (count > 0f) {
                val sweepAngle = (count / total) * 360f
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = Size(size.width, size.height)
                )
                startAngle += sweepAngle
            }
        }
    }
}

@Composable
fun ComposeBarChart(
    ongoing: Int,
    done: Int,
    accomplished: Int,
    notAccomplished: Int,
    modifier: Modifier = Modifier
) {
    val maxVal = maxOf(ongoing, done, accomplished, notAccomplished, 1).toFloat()

    val ongoingColor = Color(TaskStatus.ONGOING.color)
    val doneColor = Color(TaskStatus.DONE.color)
    val accomplishedColor = Color(TaskStatus.ACCOMPLISHED.color)
    val notAccomplishedColor = Color(TaskStatus.NOT_ACCOMPLISHED.color)

    val bars = listOf(
        "Ongoing" to (ongoing to ongoingColor),
        "Done" to (done to doneColor),
        "Accomp." to (accomplished to accomplishedColor),
        "Not Acc." to (notAccomplished to notAccomplishedColor)
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = width / 8f
        val gap = width / 6f

        // Draw Baseline
        drawLine(
            color = Color.Gray,
            start = Offset(0f, height - 30f),
            end = Offset(width, height - 30f),
            strokeWidth = 2f
        )

        bars.forEachIndexed { index, (label, data) ->
            val (count, color) = data
            val barHeight = (count.toFloat() / maxVal) * (height - 60f)
            val left = (index + 0.5f) * gap

            // Bar Rect
            drawRect(
                color = color,
                topLeft = Offset(left, height - 30f - barHeight),
                size = Size(barWidth, barHeight)
            )
        }
    }
}
