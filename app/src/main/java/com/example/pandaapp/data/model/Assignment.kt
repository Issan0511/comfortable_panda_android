package com.example.pandaapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AssignmentResponse(
    @SerialName("assignment_collection")
    val assignments: List<AssignmentItem> = emptyList()
)

@Serializable
data class AssignmentItem(
    val id: String = "",
    val title: String = "",
    @SerialName("dueTime") val dueTime: DueTime? = null,
    val status: String? = null,
    val submissions: List<Submission> = emptyList()
)

@Serializable
data class DueTime(
    @SerialName("epochSecond") val epochSecond: Long? = null
)

@Serializable
data class Submission(
    @SerialName("userSubmission") val userSubmission: Boolean = false
)

@Serializable
data class Assignment(
    val id: String,
    val title: String,
    val dueTimeSeconds: Long?,
    val status: String?,
    val courseName: String,
    val courseId: String,
    val isSubmitted: Boolean = false
)
