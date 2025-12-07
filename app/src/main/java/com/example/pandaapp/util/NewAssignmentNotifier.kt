package com.example.pandaapp.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.example.pandaapp.R
import com.example.pandaapp.data.model.Assignment
import com.example.pandaapp.ui.MainActivity

class NewAssignmentNotifier(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    fun notify(newAssignments: List<Assignment>) {
        if (shouldSkipNotifications()) {
            return
        }

        ensureChannel()

        newAssignments.forEach { assignment ->
            val contentIntent = createContentIntent()
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(
                    context.getString(
                        R.string.notification_new_assignment_title,
                        assignment.courseName
                    )
                )
                .setContentText(
                    context.getString(
                        R.string.notification_new_assignment_message,
                        assignment.title
                    )
                )
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setGroup(GROUP_KEY)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(assignment.id.hashCode(), notification)
        }

        if (newAssignments.size > 1) {
            val summaryIntent = createContentIntent()
            val summary = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.notification_channel_name))
                .setContentText(
                    context.getString(
                        R.string.notification_new_assignment_summary,
                        newAssignments.size
                    )
                )
                .setStyle(
                    NotificationCompat.InboxStyle().also { style ->
                        newAssignments.take(MAX_LINES_IN_SUMMARY).forEach { assignment ->
                            style.addLine("${assignment.courseName}: ${assignment.title}")
                        }
                    }
                )
                .setContentIntent(summaryIntent)
                .setAutoCancel(true)
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(SUMMARY_ID, summary)
        }
    }

    private fun shouldSkipNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false
        }

        val permissionGranted = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted; skipping assignment notifications.")
        }

        return !permissionGranted
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun createContentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)

        val stackBuilder = TaskStackBuilder.create(context).apply {
            addNextIntentWithParentStack(intent)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return stackBuilder.getPendingIntent(REQUEST_CODE, flags)!!
    }

    private companion object {
        const val CHANNEL_ID = "assignments"
        const val TAG = "NewAssignmentNotifier"
        const val GROUP_KEY = "assignments:new"
        const val SUMMARY_ID = 0
        const val REQUEST_CODE = 1
        const val MAX_LINES_IN_SUMMARY = 5
    }
}
