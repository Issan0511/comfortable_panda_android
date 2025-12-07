package com.example.pandaapp.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatEpochSecondsToJst(epochSeconds: Long): String {
    val instant = Instant.ofEpochSecond(epochSeconds)
    val zoneId = ZoneId.of("Asia/Tokyo")
    val zonedDateTime = instant.atZone(zoneId)
    val formatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
    return formatter.format(zonedDateTime)
}

fun formatEpochSecondsToShort(epochSeconds: Long): String {
    val instant = Instant.ofEpochSecond(epochSeconds)
    val zoneId = ZoneId.of("Asia/Tokyo")
    val zonedDateTime = instant.atZone(zoneId)
    val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
    return formatter.format(zonedDateTime)
}
