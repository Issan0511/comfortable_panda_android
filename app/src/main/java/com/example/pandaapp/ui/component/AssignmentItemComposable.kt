package com.example.pandaapp.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pandaapp.data.model.Assignment
import com.example.pandaapp.util.formatEpochSecondsToShort

/**
 * 課題アイテムを表示するComposable
 * mainとwidgetで共通して使用
 */
@Composable
fun AssignmentItemComposable(
    assignment: Assignment,
    onClick: (Assignment) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val deadlineColor = getDeadlineColor(assignment.dueTimeSeconds)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(
                color = deadlineColor.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.small
            )
            .clickable { onClick(assignment) }
            .padding(12.dp)
    ) {
        val submissionLabel = if (assignment.isSubmitted) "提出済み" else "未提出"
        val submissionColor = if (assignment.isSubmitted) Color(0xFF2E7D32) else Color(0xFFD32F2F)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = assignment.courseName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = submissionLabel,
                style = MaterialTheme.typography.bodySmall,
                color = submissionColor
            )
        }
        Text(text = assignment.title, style = MaterialTheme.typography.bodyLarge)
        assignment.dueTimeSeconds?.let {
            val formattedDate = formatEpochSecondsToShort(it)
            val remainingTime = formatRemainingTime(it)
            
            val annotatedString = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(remainingTime)
                }
                append("($formattedDate)")
            }
            
            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black,
                fontSize = 16.sp
            )
        } ?: run {
            Text(text = "期限: -", style = MaterialTheme.typography.bodySmall)
        }
        Divider(modifier = Modifier.padding(top = 8.dp))
    }
}
