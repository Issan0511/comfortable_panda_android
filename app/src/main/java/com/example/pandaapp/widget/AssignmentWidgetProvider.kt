package com.example.pandaapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.example.pandaapp.R
import com.example.pandaapp.ui.MainActivity
import com.example.pandaapp.util.AssignmentStore
import com.example.pandaapp.util.formatEpochSecondsToJst

class AssignmentWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?: appWidgetManager.getAppWidgetIds(ComponentName(context, javaClass))
            ids.forEach { appWidgetId ->
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_assignments_list)
            }
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_assignment)

            val serviceIntent = Intent(context, AssignmentWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_assignments_list, serviceIntent)
            views.setEmptyView(R.id.widget_assignments_list, R.id.widget_empty_view)

            val assignmentStore = AssignmentStore(context)
            val lastUpdatedLabel = assignmentStore.load().lastUpdatedEpochSeconds?.let {
                "最終更新: ${formatEpochSecondsToJst(it)}"
            } ?: "最終更新: -"
            views.setTextViewText(R.id.widget_last_updated, lastUpdatedLabel)

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_header, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_assignments_list)
        }
    }
}
