package com.medicationreminder.wear.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.medicationreminder.wear.WearApp

class WearDoseAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WearAlarmScheduler.ACTION_WEAR_DOSE) return
        val eventId = intent.getStringExtra(WearAlarmScheduler.EXTRA_EVENT_ID) ?: return
        val medicationId = intent.getStringExtra(WearAlarmScheduler.EXTRA_MEDICATION_ID) ?: return

        vibrate(context)
        context.startActivity(
            WearAlarmScheduler.reminderIntent(context, eventId, medicationId)
        )
    }

    private fun vibrate(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(VibratorManager::class.java)
                vm.defaultVibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 400, 200, 400), -1)
                }
            }
        } catch (_: Exception) {
            // titreşim izni veya donanım yoksa sessizce geç
        }
    }
}

class WearBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = WearApp.get(context)
        WearAlarmScheduler(context).reschedule(app.cache)
        app.syncManager.requestSync()
    }
}
