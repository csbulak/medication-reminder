package com.medicationreminder.mobile.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.medicationreminder.shared.model.DoseEvent
import com.medicationreminder.shared.model.DoseSchedule
import com.medicationreminder.shared.model.DoseStatus
import com.medicationreminder.shared.model.EventSource
import com.medicationreminder.shared.model.Medication

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val dosage: String,
    val notes: String,
    val active: Boolean,
    val stockQty: Int? = null,
    val lowStockAt: Int = 5
) {
    fun toModel() = Medication(id, name, dosage, notes, active, stockQty, lowStockAt)

    companion object {
        fun from(model: Medication) = MedicationEntity(
            id = model.id,
            name = model.name,
            dosage = model.dosage,
            notes = model.notes,
            active = model.active,
            stockQty = model.stockQty,
            lowStockAt = model.lowStockAt
        )
    }
}

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val medicationId: String,
    val timesCsv: String,
    val daysCsv: String,
    val enabled: Boolean
) {
    fun toModel() = DoseSchedule(
        id = id,
        medicationId = medicationId,
        times = if (timesCsv.isBlank()) emptyList() else timesCsv.split(","),
        daysOfWeek = if (daysCsv.isBlank()) emptyList() else daysCsv.split(",").map { it.toInt() },
        enabled = enabled
    )

    companion object {
        fun from(model: DoseSchedule) = ScheduleEntity(
            id = model.id,
            medicationId = model.medicationId,
            timesCsv = model.times.joinToString(","),
            daysCsv = model.daysOfWeek.joinToString(","),
            enabled = model.enabled
        )
    }
}

@Entity(tableName = "dose_events")
data class DoseEventEntity(
    @PrimaryKey val id: String,
    val medicationId: String,
    val scheduledAt: Long,
    val status: String,
    val respondedAt: Long?,
    val source: String
) {
    fun toModel() = DoseEvent(
        id = id,
        medicationId = medicationId,
        scheduledAt = scheduledAt,
        status = DoseStatus.valueOf(status),
        respondedAt = respondedAt,
        source = EventSource.valueOf(source)
    )

    companion object {
        fun from(model: DoseEvent) = DoseEventEntity(
            id = model.id,
            medicationId = model.medicationId,
            scheduledAt = model.scheduledAt,
            status = model.status.name,
            respondedAt = model.respondedAt,
            source = model.source.name
        )
    }
}
