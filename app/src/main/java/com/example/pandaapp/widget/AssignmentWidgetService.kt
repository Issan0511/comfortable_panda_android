package com.example.pandaapp.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.pandaapp.R
import com.example.pandaapp.data.model.Assignment
import com.example.pandaapp.ui.component.createAssignmentRemoteViews
import com.example.pandaapp.ui.component.sortAssignments
import com.example.pandaapp.util.AssignmentStore

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
        val remoteViews = createAssignmentRemoteViews(context, assignment)

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
        assignments = sortAssignments(storedAssignments)
    }
}
