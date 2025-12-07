package com.example.pandaapp.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
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

        ensureChannels()

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

    fun notifyUrgent(urgentAssignments: List<Assignment>) {
        if (shouldSkipNotifications()) {
            return
        }

        ensureChannels()

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val nowSeconds = System.currentTimeMillis() / 1000
        urgentAssignments.forEach { assignment ->
            val dueSeconds = assignment.dueTimeSeconds ?: nowSeconds
            val minutesLeft = ((dueSeconds - nowSeconds + 59) / 60).coerceAtLeast(1).toInt()
            val contentIntent = createContentIntent()
            val notification = NotificationCompat.Builder(context, CHANNEL_ID_URGENT)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(
                    context.getString(
                        R.string.notification_urgent_assignment_title,
                        minutesLeft
                    )
                )
                .setContentText(
                    context.getString(
                        R.string.notification_urgent_assignment_message,
                        assignment.title,
                        assignment.courseName,
                        minutesLeft
                    )
                )
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setGroup(GROUP_KEY_URGENT)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(soundUri)
                .setVibrate(VIBRATION_PATTERN)
                .setLights(Color.RED, 800, 400)
                .build()

            notificationManager.notify(URGENT_PREFIX + assignment.id.hashCode(), notification)
        }

        if (urgentAssignments.size > 1) {
            val summaryIntent = createContentIntent()
            val summary = NotificationCompat.Builder(context, CHANNEL_ID_URGENT)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.notification_channel_urgent_name))
                .setContentText(
                    context.getString(
                        R.string.notification_urgent_assignment_summary,
                        urgentAssignments.size
                    )
                )
                .setStyle(
                    NotificationCompat.InboxStyle().also { style ->
                        urgentAssignments.take(MAX_LINES_IN_SUMMARY).forEach { assignment ->
                            style.addLine("${assignment.courseName}: ${assignment.title}")
                        }
                    }
                )
                .setContentIntent(summaryIntent)
                .setAutoCancel(true)
                .setGroup(GROUP_KEY_URGENT)
                .setGroupSummary(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(soundUri)
                .setVibrate(VIBRATION_PATTERN)
                .setLights(Color.RED, 800, 400)
                .build()

            notificationManager.notify(URGENT_SUMMARY_ID, summary)
        }
    }

    fun notifySubmission(submittedAssignments: List<Assignment>) {
        if (shouldSkipNotifications()) {
            return
        }

        ensureChannels()

        submittedAssignments.forEach { assignment ->
            val contentIntent = createContentIntent()
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(
                    context.getString(
                        R.string.notification_submitted_title,
                        assignment.courseName
                    )
                )
                .setContentText(
                    context.getString(
                        R.string.notification_submitted_message,
                        assignment.title
                    )
                )
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setGroup(GROUP_KEY_SUBMISSION)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(SUBMISSION_PREFIX + assignment.id.hashCode(), notification)
        }

        if (submittedAssignments.size > 1) {
            val summaryIntent = createContentIntent()
            val summary = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.notification_channel_name))
                .setContentText(
                    context.getString(
                        R.string.notification_submitted_summary,
                        submittedAssignments.size
                    )
                )
                .setStyle(
                    NotificationCompat.InboxStyle().also { style ->
                        submittedAssignments.take(MAX_LINES_IN_SUMMARY).forEach { assignment ->
                            style.addLine("${assignment.courseName}: ${assignment.title}")
                        }
                    }
                )
                .setContentIntent(summaryIntent)
                .setAutoCancel(true)
                .setGroup(GROUP_KEY_SUBMISSION)
                .setGroupSummary(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(SUBMISSION_SUMMARY_ID, summary)
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

    private fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val defaultChannel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val urgentChannel = NotificationChannel(
            CHANNEL_ID_URGENT,
            context.getString(R.string.notification_channel_urgent_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_urgent_description)
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
            enableLights(true)
            lightColor = Color.RED
            setSound(soundUri, audioAttributes)
        }

        manager.createNotificationChannel(defaultChannel)
        manager.createNotificationChannel(urgentChannel)
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
        const val CHANNEL_ID_URGENT = "assignments_urgent"
        const val TAG = "NewAssignmentNotifier"
        const val GROUP_KEY = "assignments:new"
        const val GROUP_KEY_SUBMISSION = "assignments:submitted"
        const val GROUP_KEY_URGENT = "assignments:urgent"
        const val SUMMARY_ID = 0
        const val SUBMISSION_SUMMARY_ID = 1
        const val URGENT_SUMMARY_ID = 2
        const val REQUEST_CODE = 1
        const val SUBMISSION_PREFIX = 1000000
        const val URGENT_PREFIX = 2000000
        const val MAX_LINES_IN_SUMMARY = 5
        val VIBRATION_PATTERN = longArrayOf(0, 500, 200, 500, 200, 500)
    }
}
