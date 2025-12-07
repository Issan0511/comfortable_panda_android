package com.example.pandaapp.ui.component

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.widget.RemoteViews
import com.example.pandaapp.R
import com.example.pandaapp.data.model.Assignment
import com.example.pandaapp.util.formatEpochSecondsToJst

/**
 * 締め切りまでの時間に応じて色コードを取得
 */
fun getDeadlineColorInt(dueTimeSeconds: Long?, isSubmitted: Boolean): Int {
    if (dueTimeSeconds == null) {
        return Color.TRANSPARENT
    }

    val now = System.currentTimeMillis() / 1000
    val remainingSeconds = dueTimeSeconds - now
    val remainingHours = remainingSeconds / 3600.0

    return when {
        isSubmitted -> {
            // 提出済み → 青
            Color.parseColor("#2196F3")
        }
        remainingSeconds <= 0 -> {
            // 締め切り経過（未提出） → 灰色
            Color.GRAY
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
            // それ以上 → 無色（透明）
            Color.TRANSPARENT
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
    val submissionLabel = if (assignment.isSubmitted) "提出済み" else "未提出"
    val submissionColor = if (assignment.isSubmitted) Color.parseColor("#2196F3") else Color.parseColor("#D32F2F")
    remoteViews.setTextViewText(R.id.widget_submission_status, submissionLabel)
    remoteViews.setTextColor(R.id.widget_submission_status, submissionColor)

    val dueLabel = assignment.dueTimeSeconds?.let {
        val remainingTime = formatRemainingTime(it)
        val dateTime = formatEpochSecondsToJst(it)
        
        // Spannableで太字を適用（残り時間は強調）
        val spannableString = SpannableString("$remainingTime($dateTime)")
        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            remainingTime.length,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        // 時刻の分部を太字にする（`HH:mm` の : の次の2桁）
        val colonIndex = dateTime.lastIndexOf(":")
        if (colonIndex >= 0) {
            val minuteStart = remainingTime.length + 1 + colonIndex + 1 // plus '(' and portion before ':'
            val minuteEnd = minuteStart + 2
            if (minuteStart >= 0 && minuteEnd <= spannableString.length) {
                spannableString.setSpan(
                    StyleSpan(Typeface.BOLD),
                    minuteStart,
                    minuteEnd,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        spannableString
    } ?: "期限: -"

    remoteViews.setTextViewText(R.id.widget_assignment_due, dueLabel)

    // 締め切り色を背景に適用（透明度付き）
    val backgroundColor = getDeadlineColorInt(assignment.dueTimeSeconds, assignment.isSubmitted)
    if (backgroundColor != Color.TRANSPARENT) {
        val colorWithAlpha = (0x26 shl 24) or (backgroundColor and 0xFFFFFF)
        remoteViews.setInt(R.id.widget_item_root, "setBackgroundColor", colorWithAlpha)
    }

    return remoteViews
}

/**
 * 課題リストをソートするロジック
 * 未提出/提出 × 期限未来/期限過ぎ の4グループで並べる
 * それぞれのグループは期限の早い順（過去も含め昇順）
 */
fun sortAssignments(assignments: List<Assignment>): List<Assignment> {
    val now = System.currentTimeMillis() / 1000

    fun isFuture(a: Assignment): Boolean =
        a.dueTimeSeconds?.let { it > now } ?: true

    fun timeKey(a: Assignment): Long =
        a.dueTimeSeconds ?: Long.MAX_VALUE

    val notSubmittedFuture = assignments.filter { !it.isSubmitted && isFuture(it) }
        .sortedBy { timeKey(it) }
    val notSubmittedPast = assignments.filter { !it.isSubmitted && !isFuture(it) }
        .sortedBy { timeKey(it) }
    val submittedFuture = assignments.filter { it.isSubmitted && isFuture(it) }
        .sortedBy { timeKey(it) }
    val submittedPast = assignments.filter { it.isSubmitted && !isFuture(it) }
        .sortedBy { timeKey(it) }

    return notSubmittedFuture + notSubmittedPast + submittedFuture + submittedPast
}
