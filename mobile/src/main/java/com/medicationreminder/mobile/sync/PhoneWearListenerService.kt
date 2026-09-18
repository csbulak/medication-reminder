package com.medicationreminder.mobile.sync

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.medicationreminder.mobile.MedicationReminderApp
import com.medicationreminder.shared.model.DoseEvent
import com.medicationreminder.shared.model.EventSource
import com.medicationreminder.shared.sync.SyncJson
import com.medicationreminder.shared.sync.SyncPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

class PhoneWearListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            SyncPaths.MESSAGE_DOSE_RESPONSE -> handleDoseResponse(messageEvent.data)
            SyncPaths.MESSAGE_SNOOZE -> handleSnooze(messageEvent.data)
            SyncPaths.MESSAGE_REQUEST_SYNC -> {
                scope.launch {
                    MedicationReminderApp.get(this@PhoneWearListenerService).syncManager.pushFullSync()
                }
            }
        }
    }

    private fun handleDoseResponse(data: ByteArray) {
        scope.launch {
            try {
                val json = JSONObject(String(data, Charsets.UTF_8))
                val event = SyncJson.eventFromJson(json)
                val app = MedicationReminderApp.get(this@PhoneWearListenerService)
                val existing = app.repository.buildSyncPayload().events.find { it.id == event.id }
                if (existing != null) {
                    app.repository.recordDoseResponse(
                        eventId = event.id,
                        status = event.status,
                        source = EventSource.WATCH
                    )
                } else {
                    app.repository.upsertWatchEvent(
                        DoseEvent(
                            id = event.id,
                            medicationId = event.medicationId,
                            scheduledAt = event.scheduledAt,
                            status = event.status,
                            respondedAt = event.respondedAt ?: System.currentTimeMillis(),
                            source = EventSource.WATCH
                        )
                    )
                }
                app.doseScheduler.rescheduleAll(app.repository)
                app.syncManager.pushFullSync()
                Log.d(TAG, "Saatten doz yanıtı: ${event.id} -> ${event.status}")
            } catch (e: Exception) {
                Log.e(TAG, "Doz yanıtı işlenemedi", e)
            }
        }
    }

    private fun handleSnooze(data: ByteArray) {
        scope.launch {
            try {
                val json = JSONObject(String(data, Charsets.UTF_8))
                val eventId = json.getString("eventId")
                val minutes = json.optInt("minutes", 10)
                val app = MedicationReminderApp.get(this@PhoneWearListenerService)
                val updated = app.repository.snoozeDose(eventId, minutes, EventSource.WATCH)
                if (updated != null) {
                    app.doseScheduler.scheduleExact(
                        updated.id,
                        updated.medicationId,
                        updated.scheduledAt
                    )
                }
                app.syncManager.pushFullSync()
                Log.d(TAG, "Saatten erteleme: $eventId +${minutes}dk")
            } catch (e: Exception) {
                Log.e(TAG, "Erteleme işlenemedi", e)
            }
        }
    }

    companion object {
        private const val TAG = "PhoneWearListener"
    }
}
