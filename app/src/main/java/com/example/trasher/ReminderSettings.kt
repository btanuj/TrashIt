package com.example.trasher

import android.content.Context

/** User-chosen reminder time, persisted in SharedPreferences. Default: 7:00 PM the day before. */
object ReminderSettings {

    private const val PREFS = "reminder_settings"
    private const val KEY_HOUR = "hour"
    private const val KEY_MINUTE = "minute"
    private const val KEY_DAY_BEFORE = "day_before"

    data class Time(val hour: Int, val minute: Int, val dayBefore: Boolean)

    val DEFAULT = Time(hour = 19, minute = 0, dayBefore = true)

    fun get(context: Context): Time {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Time(
            hour = p.getInt(KEY_HOUR, DEFAULT.hour),
            minute = p.getInt(KEY_MINUTE, DEFAULT.minute),
            dayBefore = p.getBoolean(KEY_DAY_BEFORE, DEFAULT.dayBefore),
        )
    }

    /** Saves the new time and reschedules the pending alarm to match. */
    fun set(context: Context, time: Time) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_HOUR, time.hour)
            .putInt(KEY_MINUTE, time.minute)
            .putBoolean(KEY_DAY_BEFORE, time.dayBefore)
            .apply()
        Notifications.scheduleNext(context)
    }
}
