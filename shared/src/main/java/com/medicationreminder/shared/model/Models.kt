package com.medicationreminder.shared.model

enum class DoseStatus {
    PENDING,
    TAKEN,
    SKIPPED,
    MISSED
}

enum class EventSource {
    PHONE,
    WATCH
}

data class Medication(
    val id: String,
    val name: String,
    val dosage: String,
    val notes: String = "",
    val active: Boolean = true,
    /** null = stok takibi kapalı */
    val stockQty: Int? = null,
    val lowStockAt: Int = 5
)

data class DoseSchedule(
    val id: String,
    val medicationId: String,
    val times: List<String>,
    val daysOfWeek: List<Int>,
    val enabled: Boolean = true
)

data class DoseEvent(
    val id: String,
    val medicationId: String,
    val scheduledAt: Long,
    val status: DoseStatus,
    val respondedAt: Long? = null,
    val source: EventSource = EventSource.PHONE
)

data class SyncPayload(
    val medications: List<Medication> = emptyList(),
    val schedules: List<DoseSchedule> = emptyList(),
    val events: List<DoseEvent> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)
