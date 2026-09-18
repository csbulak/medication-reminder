package com.medicationreminder.mobile.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.medicationreminder.mobile.data.MedicationRepository
import com.medicationreminder.shared.sync.SyncKeys
import com.medicationreminder.shared.sync.SyncJson
import com.medicationreminder.shared.sync.SyncPaths
import kotlinx.coroutines.tasks.await

class PhoneSyncManager(
    context: Context,
    private val repository: MedicationRepository
) {
    private val dataClient = Wearable.getDataClient(context.applicationContext)
    private val messageClient = Wearable.getMessageClient(context.applicationContext)
    private val nodeClient = Wearable.getNodeClient(context.applicationContext)

    suspend fun pushFullSync(): SyncResult {
        return try {
            val payload = repository.buildSyncPayload()
            val json = SyncJson.payloadToJson(payload)
            val request = PutDataMapRequest.create(SyncPaths.FULL_SYNC).apply {
                dataMap.putString(SyncKeys.PAYLOAD_JSON, json)
                dataMap.putLong("ts", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            dataClient.putDataItem(request).await()

            val nodes = nodeClient.connectedNodes.await()
            nodes.forEach { node ->
                runCatching {
                    messageClient.sendMessage(node.id, SyncPaths.MESSAGE_REQUEST_SYNC, ByteArray(0)).await()
                }
            }

            val result = SyncResult(
                success = true,
                medicationCount = payload.medications.size,
                connectedWatchCount = nodes.size,
                message = when {
                    nodes.isEmpty() ->
                        "Veri hazır · saat bağlı değil (Bluetooth kontrol et)"
                    else ->
                        "Saate gönderildi · ${payload.medications.size} ilaç · ${nodes.size} cihaz"
                }
            )
            Log.d(TAG, result.message)
            result
        } catch (e: Exception) {
            Log.e(TAG, "Senkron başarısız", e)
            SyncResult(
                success = false,
                medicationCount = 0,
                connectedWatchCount = 0,
                message = "Senkron başarısız: ${e.message ?: "bilinmeyen hata"}"
            )
        }
    }

    suspend fun requestWatchAck() {
        val nodes = nodeClient.connectedNodes.await()
        nodes.forEach { node ->
            messageClient.sendMessage(node.id, SyncPaths.MESSAGE_REQUEST_SYNC, ByteArray(0)).await()
        }
    }

    companion object {
        private const val TAG = "PhoneSyncManager"
    }
}

data class SyncResult(
    val success: Boolean,
    val medicationCount: Int,
    val connectedWatchCount: Int,
    val message: String
)
