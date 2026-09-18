package com.medicationreminder.mobile.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.medicationreminder.mobile.MedicationReminderApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DoseAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DoseScheduler.ACTION_DOSE_ALARM) return
        val eventId = intent.getStringExtra(DoseScheduler.EXTRA_EVENT_ID) ?: return
        val medicationId = intent.getStringExtra(DoseScheduler.EXTRA_MEDICATION_ID) ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = MedicationReminderApp.get(context)
                val med = app.repository.getMedication(medicationId)
                val title = med?.name ?: "İlaç hatırlatması"
                val body = med?.let { "${it.dosage} — ilaç zamanı" } ?: "İlaç alma zamanı"
                app.doseScheduler.showDoseNotification(title, body, eventId)
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = MedicationReminderApp.get(context)
                app.doseScheduler.rescheduleAll(app.repository)
            } finally {
                pending.finish()
            }
        }
    }
}
