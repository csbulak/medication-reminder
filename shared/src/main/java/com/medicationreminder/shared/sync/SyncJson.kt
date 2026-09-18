package com.medicationreminder.shared.sync

import com.medicationreminder.shared.model.DoseEvent
import com.medicationreminder.shared.model.DoseSchedule
import com.medicationreminder.shared.model.DoseStatus
import com.medicationreminder.shared.model.EventSource
import com.medicationreminder.shared.model.Medication
import com.medicationreminder.shared.model.SyncPayload
import org.json.JSONArray
import org.json.JSONObject

object SyncJson {

    fun medicationToJson(item: Medication): JSONObject = JSONObject().apply {
        put("id", item.id)
        put("name", item.name)
        put("dosage", item.dosage)
        put("notes", item.notes)
        put("active", item.active)
        if (item.stockQty == null) put("stockQty", JSONObject.NULL) else put("stockQty", item.stockQty)
        put("lowStockAt", item.lowStockAt)
    }

    fun medicationFromJson(obj: JSONObject): Medication = Medication(
        id = obj.getString("id"),
        name = obj.getString("name"),
        dosage = obj.getString("dosage"),
        notes = obj.optString("notes", ""),
        active = obj.optBoolean("active", true),
        stockQty = if (obj.isNull("stockQty")) null else obj.optInt("stockQty"),
        lowStockAt = obj.optInt("lowStockAt", 5)
    )

    fun scheduleToJson(item: DoseSchedule): JSONObject = JSONObject().apply {
        put("id", item.id)
        put("medicationId", item.medicationId)
        put("times", JSONArray(item.times))
        put("daysOfWeek", JSONArray(item.daysOfWeek))
        put("enabled", item.enabled)
    }

    fun scheduleFromJson(obj: JSONObject): DoseSchedule {
        val times = buildList {
            val arr = obj.getJSONArray("times")
            for (i in 0 until arr.length()) add(arr.getString(i))
        }
        val days = buildList {
            val arr = obj.getJSONArray("daysOfWeek")
            for (i in 0 until arr.length()) add(arr.getInt(i))
        }
        return DoseSchedule(
            id = obj.getString("id"),
            medicationId = obj.getString("medicationId"),
            times = times,
            daysOfWeek = days,
            enabled = obj.optBoolean("enabled", true)
        )
    }

    fun eventToJson(item: DoseEvent): JSONObject = JSONObject().apply {
        put("id", item.id)
        put("medicationId", item.medicationId)
        put("scheduledAt", item.scheduledAt)
        put("status", item.status.name)
        put("respondedAt", item.respondedAt ?: JSONObject.NULL)
        put("source", item.source.name)
    }

    fun eventFromJson(obj: JSONObject): DoseEvent = DoseEvent(
        id = obj.getString("id"),
        medicationId = obj.getString("medicationId"),
        scheduledAt = obj.getLong("scheduledAt"),
        status = DoseStatus.valueOf(obj.getString("status")),
        respondedAt = if (obj.isNull("respondedAt")) null else obj.getLong("respondedAt"),
        source = EventSource.valueOf(obj.optString("source", EventSource.PHONE.name))
    )

    fun payloadToJson(payload: SyncPayload): String = JSONObject().apply {
        put("updatedAt", payload.updatedAt)
        put("medications", JSONArray().also { arr ->
            payload.medications.forEach { arr.put(medicationToJson(it)) }
        })
        put("schedules", JSONArray().also { arr ->
            payload.schedules.forEach { arr.put(scheduleToJson(it)) }
        })
        put("events", JSONArray().also { arr ->
            payload.events.forEach { arr.put(eventToJson(it)) }
        })
    }.toString()

    fun payloadFromJson(json: String): SyncPayload {
        val obj = JSONObject(json)
        val medications = buildList {
            val arr = obj.optJSONArray("medications") ?: JSONArray()
            for (i in 0 until arr.length()) add(medicationFromJson(arr.getJSONObject(i)))
        }
        val schedules = buildList {
            val arr = obj.optJSONArray("schedules") ?: JSONArray()
            for (i in 0 until arr.length()) add(scheduleFromJson(arr.getJSONObject(i)))
        }
        val events = buildList {
            val arr = obj.optJSONArray("events") ?: JSONArray()
            for (i in 0 until arr.length()) add(eventFromJson(arr.getJSONObject(i)))
        }
        return SyncPayload(
            medications = medications,
            schedules = schedules,
            events = events,
            updatedAt = obj.optLong("updatedAt", 0L)
        )
    }
}
