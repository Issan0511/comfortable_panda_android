package com.example.pandaapp.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pandaapp.data.repository.PandaRepository
import com.example.pandaapp.util.AssignmentStore
import com.example.pandaapp.util.CredentialsStore
import com.example.pandaapp.util.NewAssignmentNotifier

// ★ 追加 import
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import com.example.pandaapp.R
import com.example.pandaapp.widget.AssignmentWidgetProvider

class AssignmentFetchWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val credentialsStore = CredentialsStore(context)
        val assignmentStore = AssignmentStore(context)
        val notifier = NewAssignmentNotifier(context)
        val repository = PandaRepository(context)

        val credentials = credentialsStore.load()
        if (credentials == null) {
            Log.d(TAG, "No credentials; skipping background fetch")
            return Result.success()
        }

        return runCatching {
            Log.d(TAG, "Fetching assignments in background")
            val assignments = repository.fetchAssignments(credentials.username, credentials.password)
            val stored = assignmentStore.load()
            val savedIds = stored.assignments.map { it.id }.toSet()
            val distinctAssignments = assignments.distinctBy { it.id }
            val newAssignments = distinctAssignments.filterNot { it.id in savedIds }

            val now = currentEpochSeconds()
            assignmentStore.save(distinctAssignments, lastUpdatedEpochSeconds = now)

            if (newAssignments.isNotEmpty()) {
                notifier.notify(newAssignments)
            }

            // ★ ここから追加: ウィジェット全体を更新
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, AssignmentWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            Log.d(TAG, "Updating ${appWidgetIds.size} widgets after background fetch")

            appWidgetIds.forEach { appWidgetId ->
                // ヘッダ（最終更新時刻・ボタンなど）を更新
                AssignmentWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)
                // リスト部分を更新
                appWidgetManager.notifyAppWidgetViewDataChanged(
                    appWidgetId,
                    R.id.widget_assignments_list
                )
            }
            // ★ ここまで追加

            Log.d(TAG, "Background fetch complete: total=${distinctAssignments.size}, new=${newAssignments.size}")
            Result.success()
        }.getOrElse { throwable ->
            Log.e(TAG, "Background fetch failed", throwable)
            if (runAttemptCount < MAX_RETRY) Result.retry() else Result.failure()
        }
    }

    private fun currentEpochSeconds(): Long = System.currentTimeMillis() / 1000

    private companion object {
        const val TAG = "AssignmentFetchWorker"
        const val MAX_RETRY = 3
    }
}
