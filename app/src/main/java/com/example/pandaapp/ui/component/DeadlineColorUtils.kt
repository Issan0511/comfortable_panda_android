package com.example.pandaapp.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 締め切りまでの時間に応じて色を決定
 * - 締め切り経過 → 青
 * - 24時間以内 → 赤
 * - 120時間以内 → 黄色
 * - 336時間以内 → 緑
 * - それ以上 → 灰色
 */
@Composable
fun getDeadlineColor(dueTimeSeconds: Long?): Color {
    if (dueTimeSeconds == null) {
        return MaterialTheme.colorScheme.outline
    }

    val now = System.currentTimeMillis() / 1000
    val remainingSeconds = dueTimeSeconds - now
    val remainingHours = remainingSeconds / 3600.0

    return when {
        remainingSeconds <= 0 -> {
            // 締め切り経過 → 青
            Color(0xFF2196F3) // Material Blue
        }
        remainingHours < 24 -> {
            // 24時間以内 → 赤
            MaterialTheme.colorScheme.error
        }
        remainingHours < 120 -> {
            // 120時間以内 → 黄色
            Color(0xFFFFC107) // Material Amber
        }
        remainingHours < 336 -> {
            // 336時間以内 → 緑
            Color(0xFF4CAF50) // Material Green
        }
        else -> {
            // それ以上 → 灰色
            MaterialTheme.colorScheme.outline
        }
    }
}

/**
 * 締め切りまでの時間をフォーマットした文字列を取得
 * 返り値例: "5日2時間", "23時間", "30分"
 */
fun formatRemainingTime(dueTimeSeconds: Long?): String {
    if (dueTimeSeconds == null) {
        return "-"
    }

    val now = System.currentTimeMillis() / 1000
    val remainingSeconds = dueTimeSeconds - now

    return when {
        remainingSeconds <= 0 -> "締め切り経過"
        remainingSeconds < 3600 -> {
            val minutes = remainingSeconds / 60
            "${minutes}分"
        }
        remainingSeconds < 86400 -> {
            val hours = remainingSeconds / 3600
            "${hours}時間"
        }
        else -> {
            val days = remainingSeconds / 86400
            val hours = (remainingSeconds % 86400) / 3600
            "${days}日${hours}時間"
        }
    }
}
