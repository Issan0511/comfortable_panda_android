package com.example.pandaapp.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.pandaapp.R
import com.example.pandaapp.data.model.Assignment
import com.example.pandaapp.ui.component.createAssignmentRemoteViews
import com.example.pandaapp.ui.component.sortAssignments
import com.example.pandaapp.util.AssignmentStore

class AssignmentWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        Log.d(TAG, "onGetViewFactory called")
        return AssignmentRemoteViewsFactory(applicationContext, intent)
    }
    
    companion object {
        private const val TAG = "WidgetService"
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
        Log.d("WidgetService", "onCreate called for widget $appWidgetId")
        loadAssignments()
    }

    override fun onDataSetChanged() {
        Log.d("WidgetService", "onDataSetChanged called for widget $appWidgetId")
        loadAssignments()
    }

    override fun onDestroy() {
        Log.d("WidgetService", "onDestroy called for widget $appWidgetId")
        assignments = emptyList()
    }

    override fun getCount(): Int {
        Log.d("WidgetService", "getCount called: ${assignments.size} assignments")
        return assignments.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        val item = assignments[position]
        Log.d("WidgetService", "getViewAt: pos=$position, title=${item.title}")

        val assignment = assignments[position]
        val remoteViews = createAssignmentRemoteViews(context, assignment)

        val fillInIntent = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        remoteViews.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)

        return remoteViews
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int {
        Log.d("WidgetService", "getViewTypeCount called")
        return 1
    }

    override fun getItemId(position: Int): Long {
        Log.d("WidgetService", "getItemId: pos=$position")
        return position.toLong()
    }
    override fun hasStableIds(): Boolean = true

    private fun loadAssignments() {
        val stored = assignmentStore.load()
        val storedAssignments = stored.assignments
        assignments = sortAssignments(storedAssignments)
        Log.d("WidgetService", "loadAssignments: loaded ${assignments.size} assignments, lastUpdated=${stored.lastUpdatedEpochSeconds}")
        assignments.forEachIndexed { index, assignment ->
            Log.d("WidgetService", "  [$index] ${assignment.courseName}: ${assignment.title}")
        }
    }
}
