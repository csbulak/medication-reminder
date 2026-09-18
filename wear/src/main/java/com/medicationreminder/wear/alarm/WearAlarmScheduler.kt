package com.medicationreminder.wear.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.medicationreminder.shared.model.DoseStatus
import com.medicationreminder.wear.data.WearCache
import com.medicationreminder.wear.presentation.ReminderActivity

class WearAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun reschedule(cache: WearCache) {
        val now = System.currentTimeMillis()
        cache.payload.value.events
            .filter { it.status == DoseStatus.PENDING && it.scheduledAt > now }
            .sortedBy { it.scheduledAt }
            .take(30)
            .forEach { event ->
                schedule(event.id, event.medicationId, event.scheduledAt)
            }
    }

    fun schedule(eventId: String, medicationId: String, triggerAt: Long) {
        val intent = Intent(context, WearDoseAlarmReceiver::class.java).apply {
            action = ACTION_WEAR_DOSE
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_MEDICATION_ID, medicationId)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    companion object {
        const val ACTION_WEAR_DOSE = "com.medicationreminder.wear.DOSE_ALARM"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_MEDICATION_ID = "medication_id"

        fun reminderIntent(context: Context, eventId: String, medicationId: String): Intent =
            Intent(context, ReminderActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_EVENT_ID, eventId)
                putExtra(EXTRA_MEDICATION_ID, medicationId)
            }
    }
}
