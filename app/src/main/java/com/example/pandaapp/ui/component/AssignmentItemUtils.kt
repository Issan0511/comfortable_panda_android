package com.example.pandaapp.ui.component

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.widget.RemoteViews
import com.example.pandaapp.R
import com.example.pandaapp.data.model.Assignment
import com.example.pandaapp.util.formatEpochSecondsToShort

/**
 * 締め切りまでの時間に応じて色コードを取得
 */
fun getDeadlineColorInt(dueTimeSeconds: Long?): Int {
    if (dueTimeSeconds == null) {
        return Color.GRAY
    }

    val now = System.currentTimeMillis() / 1000
    val remainingSeconds = dueTimeSeconds - now
    val remainingHours = remainingSeconds / 3600.0

    return when {
        remainingSeconds <= 0 -> {
            // 締め切り経過 → 青
            Color.parseColor("#2196F3")
        }
        remainingHours < 24 -> {
            // 24時間以内 → 赤
            Color.RED
        }
        remainingHours < 120 -> {
            // 120時間以内 → 黄色
            Color.parseColor("#FFC107")
        }
        remainingHours < 336 -> {
            // 336時間以内 → 緑
            Color.parseColor("#4CAF50")
        }
        else -> {
            // それ以上 → 灰色
            Color.GRAY
        }
    }
}

fun createAssignmentRemoteViews(
    context: Context,
    assignment: Assignment
): RemoteViews {
    val remoteViews = RemoteViews(context.packageName, R.layout.widget_assignment_item)

    remoteViews.setTextViewText(R.id.widget_course_name, assignment.courseName)
    remoteViews.setTextViewText(R.id.widget_assignment_title, assignment.title)

    val dueLabel = assignment.dueTimeSeconds?.let {
        val remainingTime = formatRemainingTime(it)
        val dateTime = formatEpochSecondsToShort(it)
        
        // Spannableで太字を適用
        val spannableString = SpannableString("$remainingTime($dateTime)")
        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            remainingTime.length,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString
    } ?: "期限: -"

    remoteViews.setTextViewText(R.id.widget_assignment_due, dueLabel)

    // 締め切り色を背景に適用（透明度付き）
    val backgroundColor = getDeadlineColorInt(assignment.dueTimeSeconds)
    val colorWithAlpha = (0x26 shl 24) or (backgroundColor and 0xFFFFFF)
    remoteViews.setInt(R.id.widget_item_root, "setBackgroundColor", colorWithAlpha)

    return remoteViews
}

/**
 * 課題リストをソートするロジック
 * 期限未来の課題を期限昇順、過去の課題を期限降順で表示
 */
fun sortAssignments(assignments: List<Assignment>): List<Assignment> {
    val now = System.currentTimeMillis() / 1000
    val (futureAssignments, pastAssignments) = assignments.partition {
        it.dueTimeSeconds != null && it.dueTimeSeconds > now
    }

    return futureAssignments.sortedBy { it.dueTimeSeconds } +
        pastAssignments.sortedByDescending { it.dueTimeSeconds }
}
