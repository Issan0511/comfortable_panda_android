package com.example.pandaapp.util

import com.example.pandaapp.data.model.Assignment

data class AssignmentUpdateResult(
    val savedAssignments: List<Assignment>,
    val newAssignments: List<Assignment>,
    val lastUpdatedEpochSeconds: Long
)

/**
 * Detects newly fetched assignments, persists them, and triggers notifications.
 */
class AssignmentUpdateProcessor(
    private val assignmentStore: AssignmentStore,
    private val notifier: NewAssignmentNotifier
) {

    fun process(assignments: List<Assignment>): AssignmentUpdateResult {
        val stored = assignmentStore.load()
        val savedIds = stored.assignments.map { it.id }.toSet()
        val distinctAssignments = assignments.distinctBy { it.id }
        val newAssignments = distinctAssignments.filterNot { it.id in savedIds }

        val now = currentEpochSeconds()
        assignmentStore.save(distinctAssignments, lastUpdatedEpochSeconds = now)

        if (newAssignments.isNotEmpty()) {
            notifier.notify(newAssignments)
        }

        return AssignmentUpdateResult(
            savedAssignments = distinctAssignments,
            newAssignments = newAssignments,
            lastUpdatedEpochSeconds = now
        )
    }

    private fun currentEpochSeconds(): Long = System.currentTimeMillis() / 1000
}
