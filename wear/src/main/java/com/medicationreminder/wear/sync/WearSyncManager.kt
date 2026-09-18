package com.medicationreminder.wear.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.medicationreminder.shared.model.DoseEvent
import com.medicationreminder.shared.sync.SyncJson
import com.medicationreminder.shared.sync.SyncKeys
import com.medicationreminder.shared.sync.SyncPaths
import com.medicationreminder.wear.WearApp
import com.medicationreminder.wear.alarm.WearAlarmScheduler
import com.medicationreminder.wear.data.WearCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WearSyncManager(
    context: Context,
    private val cache: WearCache
) {
    private val appContext = context.applicationContext
    private val messageClient = Wearable.getMessageClient(appContext)
    private val nodeClient = Wearable.getNodeClient(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun handleDataEvents(events: DataEventBuffer) {
        for (event in events) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val path = event.dataItem.uri.path ?: continue
            if (path == SyncPaths.FULL_SYNC || path == SyncPaths.MEDICATIONS) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val json = dataMap.getString(SyncKeys.PAYLOAD_JSON) ?: continue
                val payload = SyncJson.payloadFromJson(json)
                cache.applyPayload(payload)
                WearAlarmScheduler(appContext).reschedule(cache)
                flushPending()
                requestComplicationUpdate()
                Log.d(TAG, "Senkron alındı: ${payload.medications.size} ilaç")
            }
        }
    }

    private fun requestComplicationUpdate() {
        runCatching {
            val component = android.content.ComponentName(
                appContext,
                com.medicationreminder.wear.complication.NextDoseComplicationService::class.java
            )
            androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
                .create(appContext, component)
                .requestUpdateAll()
        }
    }

    fun sendDoseResponse(event: DoseEvent) {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                val bytes = SyncJson.eventToJson(event).toString().toByteArray(Charsets.UTF_8)
                if (nodes.isEmpty()) {
                    cache.queuePending(event)
                    Log.d(TAG, "Telefon bağlı değil, yanıt kuyruğa alındı")
                    return@launch
                }
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, SyncPaths.MESSAGE_DOSE_RESPONSE, bytes).await()
                }
            } catch (e: Exception) {
                cache.queuePending(event)
                Log.e(TAG, "Yanıt gönderilemedi", e)
            }
        }
    }

    fun sendSnooze(eventId: String, minutes: Int) {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                val bytes = org.json.JSONObject()
                    .put("eventId", eventId)
                    .put("minutes", minutes)
                    .toString()
                    .toByteArray(Charsets.UTF_8)
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, SyncPaths.MESSAGE_SNOOZE, bytes).await()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erteleme gönderilemedi", e)
            }
        }
    }

    fun flushPending() {
        scope.launch {
            val pending = cache.peekPending()
            if (pending.isEmpty()) return@launch
            val nodes = runCatching { nodeClient.connectedNodes.await() }.getOrDefault(emptyList())
            if (nodes.isEmpty()) return@launch
            val drained = cache.drainPending()
            drained.forEach { event ->
                val bytes = SyncJson.eventToJson(event).toString().toByteArray(Charsets.UTF_8)
                nodes.forEach { node ->
                    runCatching {
                        messageClient.sendMessage(node.id, SyncPaths.MESSAGE_DOSE_RESPONSE, bytes).await()
                    }.onFailure {
                        cache.queuePending(event)
                    }
                }
            }
        }
    }

    fun requestSync() {
        scope.launch {
            val nodes = runCatching { nodeClient.connectedNodes.await() }.getOrDefault(emptyList())
            nodes.forEach { node ->
                messageClient.sendMessage(node.id, SyncPaths.MESSAGE_REQUEST_SYNC, ByteArray(0)).await()
            }
        }
    }

    companion object {
        private const val TAG = "WearSyncManager"
    }
}
