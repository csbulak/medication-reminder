package com.medicationreminder.mobile.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.medicationreminder.mobile.MainActivity
import com.medicationreminder.mobile.R
import com.medicationreminder.mobile.data.MedicationRepository
import com.medicationreminder.shared.model.DoseStatus

class DoseScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_dose_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_dose_desc)
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    suspend fun rescheduleAll(repository: MedicationRepository) {
        repository.ensureUpcomingEvents()
        val now = System.currentTimeMillis()
        val doses = repository.buildSyncPayload().events
            .filter { it.status == DoseStatus.PENDING && it.scheduledAt > now }
            .sortedBy { it.scheduledAt }
            .take(40)

        doses.forEach { event ->
            scheduleExact(event.id, event.medicationId, event.scheduledAt)
        }
    }

    fun scheduleExact(eventId: String, medicationId: String, triggerAt: Long) {
        val intent = Intent(context, DoseAlarmReceiver::class.java).apply {
            action = ACTION_DOSE_ALARM
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

    fun showDoseNotification(title: String, body: String, eventId: String) {
        ensureChannel()
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        NotificationManagerCompat.from(context).notify(eventId.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "dose_reminders"
        const val ACTION_DOSE_ALARM = "com.medicationreminder.DOSE_ALARM"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_MEDICATION_ID = "medication_id"
    }
}
