package com.example.pandaapp.util

import com.example.pandaapp.data.model.Assignment

data class AssignmentUpdateResult(
    val savedAssignments: List<Assignment>,
    val newAssignments: List<Assignment>,
    val submittedAssignments: List<Assignment>,
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

        // 締め切りが近い新規課題を抽出（重複通知防止のため通常通知から除外）
        val urgentAssignments = newAssignments.filter { isUrgent(it) }
        val regularNewAssignments = newAssignments - urgentAssignments

        // 提出状態が変化した課題を検出
        val submittedAssignments = detectSubmittedAssignments(stored.assignments, distinctAssignments)

        val now = currentEpochSeconds()
        assignmentStore.save(distinctAssignments, lastUpdatedEpochSeconds = now)

        if (regularNewAssignments.isNotEmpty()) {
            notifier.notify(regularNewAssignments)
        }

        if (urgentAssignments.isNotEmpty()) {
            notifier.notifyUrgent(urgentAssignments)
        }

        if (submittedAssignments.isNotEmpty()) {
            notifier.notifySubmission(submittedAssignments)
        }

        return AssignmentUpdateResult(
            savedAssignments = distinctAssignments,
            newAssignments = newAssignments,
            submittedAssignments = submittedAssignments,
            lastUpdatedEpochSeconds = now
        )
    }

    /**
     * 提出状態が「未提出」から「提出済み」に変わった課題を検出
     */
    private fun detectSubmittedAssignments(
        oldAssignments: List<Assignment>,
        newAssignments: List<Assignment>
    ): List<Assignment> {
        val oldMap = oldAssignments.associateBy { it.id }
        return newAssignments.filter { newAssignment ->
            val oldAssignment = oldMap[newAssignment.id]
            // 以前は未提出で、現在は提出済みの場合
            oldAssignment != null && !oldAssignment.isSubmitted && newAssignment.isSubmitted
        }
    }

    private fun currentEpochSeconds(): Long = System.currentTimeMillis() / 1000

    private fun isUrgent(assignment: Assignment): Boolean {
        val dueSeconds = assignment.dueTimeSeconds ?: return false
        val diff = dueSeconds - currentEpochSeconds()
        return diff in 1..URGENT_THRESHOLD_SECONDS
    }

    private companion object {
        const val URGENT_THRESHOLD_SECONDS = 3 * 60 * 60 // 3 hours
    }
}
