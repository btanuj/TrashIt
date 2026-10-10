package com.example.trasher

import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Lets the user pick when pickup reminders fire. Colours match assets/trash_schedule.html. */
class SettingsActivity : AppCompatActivity() {

    private val bg     = 0xFF0F0F0F.toInt()
    private val card   = 0xFF1A1A1A.toInt()
    private val border = 0xFF2A2A2A.toInt()
    private val accent = 0xFFC8F060.toInt()
    private val fg     = 0xFFE8E8E8.toInt()
    private val muted  = 0xFF888888.toInt()
    private val warn   = 0xFFF06060.toInt()

    private lateinit var timeValue: TextView
    private lateinit var nextValue: TextView
    private lateinit var notifWarning: TextView
    private lateinit var dayGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(24))
        }

        // Header: back + title
        content.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(label("←", 24f, fg).apply {
                setPadding(dp(4), dp(12), dp(16), dp(12))
                contentDescription = "Back"
                setOnClickListener { finish() }
            })
            addView(label("REMINDER SETTINGS", 20f, fg).apply { typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) })
        })

        // Time
        content.addView(sectionTitle("REMINDER TIME"))
        timeValue = label("", 34f, accent).apply { typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) }
        content.addView(cardOf(
            timeValue,
            label("Tap to change", 12f, muted),
        ).apply { setOnClickListener { pickTime() } })

        // Day
        content.addView(sectionTitle("REMIND ME"))
        dayGroup = RadioGroup(this).apply {
            addView(radio(ID_DAY_BEFORE, "The day before pickup"))
            addView(radio(ID_DAY_OF, "On the day of pickup"))
            setOnCheckedChangeListener { _, id ->
                val t = ReminderSettings.get(this@SettingsActivity)
                val dayBefore = id == ID_DAY_BEFORE
                if (t.dayBefore != dayBefore) save(t.copy(dayBefore = dayBefore))
            }
        }
        content.addView(cardOf(dayGroup))

        // Next reminder
        content.addView(sectionTitle("NEXT REMINDER"))
        nextValue = label("", 15f, fg)
        content.addView(cardOf(nextValue))

        notifWarning = label(
            "Notifications are turned off for Trasher, so reminders won't appear. Tap to turn them on.",
            13f, warn
        ).apply {
            setPadding(0, dp(16), 0, 0)
            setOnClickListener { openNotificationSettings() }
        }
        content.addView(notifWarning)

        // Actions
        content.addView(button("Send test notification") {
            if (!Notifications.canNotify(this)) {
                openNotificationSettings()
            } else {
                val t = ReminderSettings.get(this)
                Notifications.show(this, "Test — this is how reminders will look", "TEST", t.dayBefore)
                Toast.makeText(this, "Test notification sent", Toast.LENGTH_SHORT).show()
            }
        })
        content.addView(label("Reset to default (7:00 PM, day before)", 13f, muted).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, dp(8))
            setOnClickListener { save(ReminderSettings.DEFAULT) }
        })

        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
            addView(content)
        }
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        setContentView(scroll)
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh() // user may have just changed notification permission in system settings
    }

    private fun pickTime() {
        val t = ReminderSettings.get(this)
        TimePickerDialog(this, { _, hour, minute ->
            save(t.copy(hour = hour, minute = minute))
        }, t.hour, t.minute, DateFormat.is24HourFormat(this)).show()
    }

    private fun save(t: ReminderSettings.Time) {
        ReminderSettings.set(this, t)
        refresh()
    }

    private fun refresh() {
        if (!::timeValue.isInitialized) return
        val t = ReminderSettings.get(this)
        timeValue.text = formatTime(t.hour, t.minute)
        dayGroup.check(if (t.dayBefore) ID_DAY_BEFORE else ID_DAY_OF)

        val next = Schedule.nextNotifiablePickup(t)
        nextValue.text = if (next == null) {
            "No upcoming special pickups in the schedule."
        } else {
            val (dateKey, entry) = next
            val cal = Schedule.alarmCalFor(dateKey, t)
            val day = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(cal.time)
            "$day at ${formatTime(t.hour, t.minute)}\nfor ${entry.label}"
        }
        notifWarning.visibility = if (Notifications.canNotify(this)) View.GONE else View.VISIBLE
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return DateFormat.getTimeFormat(this).format(cal.time)
    }

    private fun openNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(android.net.Uri.fromParts("package", packageName, null))
        }
        startActivity(intent)
    }

    // ── small view helpers ──────────────────────────────────────────────

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(s: String, size: Float, color: Int) = TextView(this).apply {
        text = s
        textSize = size
        setTextColor(color)
        typeface = Typeface.MONOSPACE
    }

    private fun sectionTitle(s: String) = label(s, 11f, muted).apply {
        letterSpacing = 0.15f
        setPadding(dp(4), dp(24), 0, dp(8))
    }

    private fun rounded(fill: Int, stroke: Int) = GradientDrawable().apply {
        cornerRadius = dp(12).toFloat()
        setColor(fill)
        setStroke(dp(1), stroke)
    }

    private fun cardOf(vararg views: View) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(card, border)
        setPadding(dp(16), dp(14), dp(16), dp(14))
        views.forEach { addView(it) }
    }

    private fun radio(id: Int, s: String) = RadioButton(this).apply {
        this.id = id
        text = s
        textSize = 15f
        setTextColor(fg)
        typeface = Typeface.MONOSPACE
        buttonTintList = android.content.res.ColorStateList.valueOf(accent)
        setPadding(dp(8), dp(10), 0, dp(10))
    }

    private fun button(s: String, onClick: () -> Unit) = label(s, 15f, bg).apply {
        gravity = Gravity.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        background = rounded(accent, accent)
        setPadding(dp(16), dp(14), dp(16), dp(14))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(28) }
        setOnClickListener { onClick() }
    }

    companion object {
        private const val ID_DAY_BEFORE = 1
        private const val ID_DAY_OF = 2
    }
}
