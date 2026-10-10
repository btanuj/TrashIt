package com.example.trasher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val label     = intent.getStringExtra("pickup_label") ?: "Recycling Pickup"
        val type      = intent.getStringExtra("pickup_type")  ?: ""
        val dayBefore = intent.getBooleanExtra("day_before", true)

        Notifications.show(context, label, type, dayBefore)

        // Schedule the next alarm in the chain
        Notifications.scheduleNext(context)
    }
}
