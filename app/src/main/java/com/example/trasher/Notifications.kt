package com.example.trasher

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notifications {

    const val CHANNEL_ID = "trasher_pickup"
    private const val ALARM_RC = 42
    private const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pickup Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminder before special recycling pickups"
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    fun scheduleNext(context: Context) {
        val time = ReminderSettings.get(context)
        val next = Schedule.nextNotifiablePickup(time) ?: return
        val (dateKey, entry) = next

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("pickup_date",  dateKey)
            putExtra("pickup_label", entry.label)
            putExtra("pickup_type",  entry.type)
            putExtra("day_before",   time.dayBefore)
        }

        val pi = PendingIntent.getBroadcast(
            context, ALARM_RC, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerMs = Schedule.alarmCalFor(dateKey, time).timeInMillis
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            // Exact alarms not granted — fall back to inexact, but still allowed in Doze
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        } else {
            // setExactAndAllowWhileIdle fires reliably even in Doze mode
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        }
    }

    /** True if the app is allowed to post notifications (runtime permission only exists on 13+). */
    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun show(context: Context, label: String, type: String, dayBefore: Boolean) {
        if (!canNotify(context)) return

        val tapPi = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(if (dayBefore) "Pickup Tomorrow" else "Pickup Today")
            .setContentText(label)
            .setSubText(type)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(tapPi)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post
        }
    }
}
