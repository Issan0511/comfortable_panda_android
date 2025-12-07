package com.example.pandaapp.util

import android.content.Context
import android.content.SharedPreferences
import com.example.pandaapp.data.model.Assignment
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AssignmentStore(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    fun save(assignments: List<Assignment>) {
        val serialized = json.encodeToString(ListSerializer(Assignment.serializer()), assignments)
        preferences.edit().putString(KEY_ASSIGNMENTS, serialized).apply()
    }

    fun load(): List<Assignment> {
        val stored = preferences.getString(KEY_ASSIGNMENTS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(Assignment.serializer()), stored)
        }.getOrElse { emptyList() }
    }

    fun clear() {
        preferences.edit().remove(KEY_ASSIGNMENTS).apply()
    }

    private companion object {
        const val PREF_FILE = "panda_assignments"
        const val KEY_ASSIGNMENTS = "assignments"
    }
}
