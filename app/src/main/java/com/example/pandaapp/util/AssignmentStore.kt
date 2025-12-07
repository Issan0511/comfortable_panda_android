package com.example.pandaapp.util

import android.content.Context
import android.content.SharedPreferences
import com.example.pandaapp.data.model.Assignment
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class StoredAssignments(
    val assignments: List<Assignment>,
    val lastUpdatedEpochSeconds: Long?
)

class AssignmentStore(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    fun save(assignments: List<Assignment>, lastUpdatedEpochSeconds: Long = currentEpochSeconds()) {
        val serialized = json.encodeToString(ListSerializer(Assignment.serializer()), assignments)
        preferences.edit()
            .putString(KEY_ASSIGNMENTS, serialized)
            .putLong(KEY_LAST_UPDATED, lastUpdatedEpochSeconds)
            .apply()
    }

    fun load(): StoredAssignments {
        val stored = preferences.getString(KEY_ASSIGNMENTS, null)
        val assignments = stored?.let {
            runCatching {
                json.decodeFromString(ListSerializer(Assignment.serializer()), it)
            }.getOrElse { emptyList() }
        } ?: emptyList()

        val lastUpdatedSeconds = if (preferences.contains(KEY_LAST_UPDATED)) {
            preferences.getLong(KEY_LAST_UPDATED, 0L)
        } else {
            null
        }

        return StoredAssignments(
            assignments = assignments,
            lastUpdatedEpochSeconds = lastUpdatedSeconds
        )
    }

    fun clear() {
        preferences.edit()
            .remove(KEY_ASSIGNMENTS)
            .remove(KEY_LAST_UPDATED)
            .apply()
    }

    private fun currentEpochSeconds(): Long = System.currentTimeMillis() / 1000

    private companion object {
        const val PREF_FILE = "panda_assignments"
        const val KEY_ASSIGNMENTS = "assignments"
        const val KEY_LAST_UPDATED = "assignments_last_updated"
    }
}
