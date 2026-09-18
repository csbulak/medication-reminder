package com.medicationreminder.wear.data

import android.content.Context
import com.medicationreminder.shared.model.DoseEvent
import com.medicationreminder.shared.model.DoseSchedule
import com.medicationreminder.shared.model.DoseStatus
import com.medicationreminder.shared.model.EventSource
import com.medicationreminder.shared.model.Medication
import com.medicationreminder.shared.model.SyncPayload
import com.medicationreminder.shared.sync.SyncJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/**
 * Saat tarafı önbellek. Kaynak gerçek telefon Room DB'dedir;
 * burada SharedPreferences ile son senkron tutulur (offline alarm için).
 */
class WearCache(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _payload = MutableStateFlow(load())
    val payload: StateFlow<SyncPayload> = _payload.asStateFlow()

    private val pendingResponses = mutableListOf<DoseEvent>()

    fun applyPayload(payload: SyncPayload) {
        prefs.edit()
            .putString(KEY_JSON, SyncJson.payloadToJson(payload))
            .apply()
        _payload.value = payload
    }

    fun upcomingPending(limit: Int = 20): List<Pair<Medication, DoseEvent>> {
        val current = _payload.value
        val medMap = current.medications.associateBy { it.id }
        val now = System.currentTimeMillis()
        return current.events
            .filter { it.status == DoseStatus.PENDING && it.scheduledAt >= now - 15 * 60_000L }
            .sortedBy { it.scheduledAt }
            .mapNotNull { event ->
                val med = medMap[event.medicationId] ?: return@mapNotNull null
                med to event
            }
            .take(limit)
    }

    fun findEvent(eventId: String): Pair<Medication, DoseEvent>? {
        val current = _payload.value
        val event = current.events.find { it.id == eventId } ?: return null
        val med = current.medications.find { it.id == event.medicationId } ?: return null
        return med to event
    }

    fun markLocal(eventId: String, status: DoseStatus): DoseEvent? {
        val current = _payload.value
        val updatedEvents = current.events.map { event ->
            if (event.id == eventId) {
                event.copy(
                    status = status,
                    respondedAt = System.currentTimeMillis(),
                    source = EventSource.WATCH
                )
            } else event
        }
        val changed = updatedEvents.find { it.id == eventId } ?: return null
        applyPayload(current.copy(events = updatedEvents, updatedAt = System.currentTimeMillis()))
        queuePending(changed)
        return changed
    }

    fun snoozeLocal(eventId: String, minutes: Int): DoseEvent? {
        val current = _payload.value
        val newAt = System.currentTimeMillis() + minutes * 60_000L
        val updatedEvents = current.events.map { event ->
            if (event.id == eventId) {
                event.copy(
                    scheduledAt = newAt,
                    status = DoseStatus.PENDING,
                    respondedAt = null,
                    source = EventSource.WATCH
                )
            } else event
        }
        val changed = updatedEvents.find { it.id == eventId } ?: return null
        applyPayload(current.copy(events = updatedEvents, updatedAt = System.currentTimeMillis()))
        return changed
    }

    fun nextPending(): Pair<Medication, DoseEvent>? = upcomingPending(1).firstOrNull()

    fun queuePending(event: DoseEvent) {
        pendingResponses.removeAll { it.id == event.id }
        pendingResponses.add(event)
        savePending()
    }

    fun drainPending(): List<DoseEvent> {
        val list = pendingResponses.toList()
        pendingResponses.clear()
        savePending()
        return list
    }

    fun peekPending(): List<DoseEvent> = pendingResponses.toList()

    private fun load(): SyncPayload {
        val json = prefs.getString(KEY_JSON, null) ?: return SyncPayload()
        return runCatching { SyncJson.payloadFromJson(json) }.getOrDefault(SyncPayload())
    }

    private fun savePending() {
        val arr = JSONArray()
        pendingResponses.forEach { arr.put(SyncJson.eventToJson(it)) }
        prefs.edit().putString(KEY_PENDING, arr.toString()).apply()
    }

    init {
        val pendingJson = prefs.getString(KEY_PENDING, null)
        if (!pendingJson.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(pendingJson)
                for (i in 0 until arr.length()) {
                    pendingResponses.add(SyncJson.eventFromJson(arr.getJSONObject(i)))
                }
            }
        }
    }

    companion object {
        private const val PREFS = "wear_cache"
        private const val KEY_JSON = "payload"
        private const val KEY_PENDING = "pending_responses"
    }
}
