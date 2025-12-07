package com.example.pandaapp.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.pandaapp.R
import com.example.pandaapp.data.model.Assignment
import com.example.pandaapp.util.AssignmentStore
import com.example.pandaapp.util.formatEpochSecondsToJst

class AssignmentWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return AssignmentRemoteViewsFactory(applicationContext, intent)
    }
}

private class AssignmentRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId: Int = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0)
    private val assignmentStore = AssignmentStore(context)
    private var assignments: List<Assignment> = emptyList()

    override fun onCreate() {
        loadAssignments()
    }

    override fun onDataSetChanged() {
        loadAssignments()
    }

    override fun onDestroy() {
        assignments = emptyList()
    }

    override fun getCount(): Int = assignments.size

    override fun getViewAt(position: Int): RemoteViews {
        val assignment = assignments[position]
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_assignment_item)

        remoteViews.setTextViewText(R.id.widget_course_name, assignment.courseName)
        remoteViews.setTextViewText(R.id.widget_assignment_title, assignment.title)

        val dueLabel = assignment.dueTimeSeconds?.let { "期限: ${formatEpochSecondsToJst(it)}" } ?: "期限: -"
        val statusLabel = assignment.status?.let { "状態: $it" } ?: "状態: -"

        remoteViews.setTextViewText(R.id.widget_assignment_due, dueLabel)
        remoteViews.setTextViewText(R.id.widget_assignment_status, statusLabel)

        val fillInIntent = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        remoteViews.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)

        return remoteViews
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = assignments[position].id.hashCode().toLong()

    override fun hasStableIds(): Boolean = true

    private fun loadAssignments() {
        val storedAssignments = assignmentStore.load().assignments
        val now = System.currentTimeMillis() / 1000
        val (futureAssignments, pastAssignments) = storedAssignments.partition {
            it.dueTimeSeconds != null && it.dueTimeSeconds > now
        }

        val sorted = futureAssignments.sortedBy { it.dueTimeSeconds } +
            pastAssignments.sortedByDescending { it.dueTimeSeconds }

        assignments = sorted
    }
}
