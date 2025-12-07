package com.example.pandaapp.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.pandaapp.R
import com.example.pandaapp.data.model.Assignment

class NewAssignmentNotifier(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    fun notify(newAssignments: List<Assignment>) {
        if (shouldSkipNotifications()) {
            return
        }

        ensureChannel()

        newAssignments.forEach { assignment ->
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
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(assignment.id.hashCode(), notification)
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

    private companion object {
        const val CHANNEL_ID = "assignments"
        const val TAG = "NewAssignmentNotifier"
    }
}
