package com.example.pandaapp.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.pandaapp.R
import com.example.pandaapp.util.AssignmentStore
import com.example.pandaapp.util.formatEpochSecondsToJst
import com.example.pandaapp.worker.AssignmentFetchWorker

class AssignmentWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called with ${appWidgetIds.size} widgets")
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        schedulePeriodicUpdate(context)
        Log.d(TAG, "Periodic update scheduled")
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "Widget enabled, scheduling periodic update")
        schedulePeriodicUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelPeriodicUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive: action=${intent.action}")
        when (intent.action) {
            FETCH_ACTION -> {
                Log.d(TAG, "FETCH_ACTION received")
                // 既に reloading 中なら無視
                if (isReloading(context)) {
                    Log.d(TAG, "Ignoring FETCH_ACTION - already reloading")
                    return
                }
                Log.d(TAG, "Starting fetch process...")

                // reloading 状態を設定
                setReloadingState(context, true)

                // 即座に reloading 表示へ
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, AssignmentWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                appWidgetIds.forEach { appWidgetId ->
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }

                // WorkManager でフェッチ実行
                val workRequest = OneTimeWorkRequestBuilder<AssignmentFetchWorker>().build()
                WorkManager.getInstance(context).enqueue(workRequest)
                Log.d(TAG, "WorkManager enqueued for fetch")

                // 30 秒後に reloading 状態をクリア
                scheduleDelayedUpdate(context)
                Log.d(TAG, "Scheduled delayed update in 30 seconds")
            }

            REFRESH_ACTION -> {
                Log.d(TAG, "REFRESH_ACTION received, isReloading=${isReloading(context)}")
                // reloading 中は定期更新スキップ
                if (isReloading(context)) {
                    Log.d(TAG, "Skipping REFRESH_ACTION because reloading is in progress")
                    return
                }
                
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, AssignmentWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                Log.d(TAG, "Refreshing ${appWidgetIds.size} widgets")
                appWidgetIds.forEach { appWidgetId ->
                    // リストビューのデータ更新を通知
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_assignments_list)
                    Log.d(TAG, "Notified data changed for widget $appWidgetId")
                    // notifyの後に最新データで再描画
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                    Log.d(TAG, "Re-updated widget $appWidgetId after notify")
                }
            }

            CLEAR_RELOADING_ACTION -> {
                Log.d(TAG, "CLEAR_RELOADING_ACTION received")
                setReloadingState(context, false)

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, AssignmentWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                appWidgetIds.forEach { appWidgetId ->
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                    appWidgetManager.notifyAppWidgetViewDataChanged(
                        appWidgetId,
                        R.id.widget_assignments_list
                    )
                }
            }

            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                    ?: appWidgetManager.getAppWidgetIds(
                        ComponentName(context, AssignmentWidgetProvider::class.java)
                    )
                ids.forEach { appWidgetId ->
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
        }
    }

    companion object {
        private const val TAG = "WidgetProvider"
        private const val REFRESH_ACTION = "com.example.pandaapp.WIDGET_REFRESH"
        private const val FETCH_ACTION = "com.example.pandaapp.WIDGET_FETCH"
        private const val CLEAR_RELOADING_ACTION = "com.example.pandaapp.WIDGET_CLEAR_RELOADING"
        private const val UPDATE_INTERVAL_MS = 300_000L // 5ｍごと
        private const val PREFS_NAME = "widget_state"
        private const val KEY_RELOADING = "is_reloading"

        private fun isReloading(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_RELOADING, false)
        }

        private fun setReloadingState(context: Context, reloading: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_RELOADING, reloading).apply()
        }

        private fun schedulePeriodicUpdate(context: Context) {
            Log.d(TAG, "schedulePeriodicUpdate called")
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AssignmentWidgetProvider::class.java).apply {
                action = REFRESH_ACTION
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setRepeating(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + UPDATE_INTERVAL_MS,
                    UPDATE_INTERVAL_MS,
                    pendingIntent
                )
                Log.d(
                    TAG,
                    "Periodic update scheduled successfully (every ${UPDATE_INTERVAL_MS / 1000} seconds)"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule periodic update", e)
            }
        }

        private fun cancelPeriodicUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AssignmentWidgetProvider::class.java).apply {
                action = REFRESH_ACTION
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }

        private fun scheduleDelayedUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AssignmentWidgetProvider::class.java).apply {
                action = CLEAR_RELOADING_ACTION
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                2,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            Log.d(TAG, "Scheduling update in 30 seconds")
            alarmManager.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 30_000L,
                pendingIntent
            )
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            Log.d(TAG, "updateAppWidget called for widgetId=$appWidgetId")

            val views = RemoteViews(context.packageName, R.layout.widget_assignment)

            val serviceIntent = Intent(context, AssignmentWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_assignments_list, serviceIntent)
            views.setEmptyView(R.id.widget_assignments_list, R.id.widget_empty_view)

            val clickIntent = Intent(context, AssignmentRedirectActivity::class.java)
            val clickPendingIntent = PendingIntent.getActivity(
                context,
                0,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_assignments_list, clickPendingIntent)

            val assignmentStore = AssignmentStore(context)
            val stored = assignmentStore.load()
            
            Log.d(TAG, "Loaded from store: lastUpdatedEpochSeconds=${stored.lastUpdatedEpochSeconds}, assignments=${stored.assignments.size}")

            val lastUpdatedLabel = stored.lastUpdatedEpochSeconds?.let {
                val formatted = formatEpochSecondsToJst(it)
                Log.d(TAG, "Formatted time: $formatted (from epoch: $it)")
                "最終更新: $formatted"
            } ?: "最終更新: -"
            Log.d(
                TAG,
                "Setting last updated label: $lastUpdatedLabel"
            )
            views.setTextViewText(R.id.widget_last_updated, lastUpdatedLabel)

            // ヘッダークリックでアプリを開く
            val openAppIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, Class.forName("com.example.pandaapp.ui.MainActivity")),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_header, openAppIntent)

            // 更新ボタンの表示と機能を制御
            val reloading = isReloading(context)
            Log.d(TAG, "Button state - reloading: $reloading")

            val refreshIntent = Intent(context, AssignmentWidgetProvider::class.java).apply {
                action = if (reloading) {
                    "com.example.pandaapp.WIDGET_DUMMY"
                } else {
                    FETCH_ACTION
                }
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (reloading) {
                views.setTextViewText(R.id.widget_refresh_button, "reloading...")
                Log.d(TAG, "Button set to reloading state with dummy intent")
            } else {
                views.setTextViewText(R.id.widget_refresh_button, "reload")
                Log.d(TAG, "Button set to reload state with FETCH_ACTION")
            }
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d(TAG, "Widget $appWidgetId updated successfully")
        }
    }
}
