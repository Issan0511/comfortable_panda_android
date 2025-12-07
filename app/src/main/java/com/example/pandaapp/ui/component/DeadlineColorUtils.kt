package com.example.pandaapp.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 締め切りまでの時間に応じて色を決定
 * - 提出済み → 青
 * - 締め切り経過（未提出） → 灰色
 * - 24時間以内 → 赤
 * - 120時間以内（5日以内） → 黄色
 * - 336時間以内（14日以内） → 緑
 * - それ以上 → 無色（透明）
 */
@Composable
fun getDeadlineColor(dueTimeSeconds: Long?, isSubmitted: Boolean): Color {
    if (isSubmitted) {
        return Color(0xFF2196F3) // Blue for submitted
    }
    if (dueTimeSeconds == null) {
        return Color.Transparent
    }

    val now = System.currentTimeMillis() / 1000
    val remainingSeconds = dueTimeSeconds - now
    val remainingHours = remainingSeconds / 3600.0

    return when {
        remainingSeconds <= 0 -> {
            // 締め切り経過（未提出） → 灰色
            MaterialTheme.colorScheme.outline
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
            // それ以上 → 無色（透明）
            Color.Transparent
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

    if (remainingSeconds <= 0) return "締め切り経過"

    val days = remainingSeconds / 86400
    val hours = (remainingSeconds % 86400) / 3600
    val minutes = (remainingSeconds % 3600) / 60

    val parts = mutableListOf<String>()
    if (days > 0) parts.add("${days}日")
    if (hours > 0) parts.add("${hours}時間")
    if (minutes > 0) parts.add("${minutes}分")

    return when {
        parts.isEmpty() -> "0分"
        else -> parts.joinToString(separator = "")
    }
}
