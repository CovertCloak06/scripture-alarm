package com.covertcloak.scripturealarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_VERSE_CATEGORY = "verse_category"
        const val EXTRA_USE_SEQUENTIAL = "use_sequential"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        val verseCategory = intent.getStringExtra(EXTRA_VERSE_CATEGORY) ?: "GENERAL"
        val useSequential = intent.getBooleanExtra(EXTRA_USE_SEQUENTIAL, false)

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_VERSE_CATEGORY, verseCategory)
            putExtra(EXTRA_USE_SEQUENTIAL, useSequential)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
