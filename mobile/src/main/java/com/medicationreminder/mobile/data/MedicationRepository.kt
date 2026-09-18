package com.medicationreminder.mobile.data

import com.medicationreminder.shared.model.DoseEvent
import com.medicationreminder.shared.model.DoseSchedule
import com.medicationreminder.shared.model.DoseStatus
import com.medicationreminder.shared.model.EventSource
import com.medicationreminder.shared.model.Medication
import com.medicationreminder.shared.model.SyncPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID

class MedicationRepository(
    private val db: AppDatabase,
    private val onDataChanged: suspend () -> Unit = {}
) {
    private val medicationDao = db.medicationDao()
    private val scheduleDao = db.scheduleDao()
    private val eventDao = db.doseEventDao()

    fun observeMedications(): Flow<List<Medication>> =
        medicationDao.observeAll().map { list -> list.map { it.toModel() } }

    fun observeSchedules(): Flow<List<DoseSchedule>> =
        scheduleDao.observeAll().map { list -> list.map { it.toModel() } }

    fun observeEvents(): Flow<List<DoseEvent>> =
        eventDao.observeAll().map { list -> list.map { it.toModel() } }

    fun observeTodayDoses(): Flow<List<UpcomingDose>> = combine(
        observeMedications(),
        observeSchedules(),
        observeEvents()
    ) { meds, schedules, events ->
        buildTodayDoses(meds, schedules, events)
    }

    fun observeWeeklyAdherence(): Flow<WeeklyAdherence> =
        observeEvents().map { events -> Companion.computeWeeklyAdherence(events) }

    suspend fun getMedication(id: String): Medication? =
        medicationDao.getById(id)?.toModel()

    suspend fun getSchedulesFor(medicationId: String): List<DoseSchedule> =
        scheduleDao.getForMedication(medicationId).map { it.toModel() }

    suspend fun saveMedication(
        medication: Medication,
        schedule: DoseSchedule
    ) {
        medicationDao.upsert(MedicationEntity.from(medication))
        scheduleDao.deleteForMedication(medication.id)
        scheduleDao.upsert(ScheduleEntity.from(schedule.copy(medicationId = medication.id)))
        ensureUpcomingEvents(medication.id)
        onDataChanged()
    }

    suspend fun deleteMedication(id: String) {
        scheduleDao.deleteForMedication(id)
        medicationDao.delete(id)
        onDataChanged()
    }

    suspend fun recordDoseResponse(
        eventId: String,
        status: DoseStatus,
        source: EventSource
    ) {
        val existing = eventDao.getById(eventId)?.toModel() ?: return
        eventDao.upsert(
            DoseEventEntity.from(
                existing.copy(
                    status = status,
                    respondedAt = System.currentTimeMillis(),
                    source = source
                )
            )
        )
        if (status == DoseStatus.TAKEN) {
            decrementStock(existing.medicationId)
        }
        onDataChanged()
    }

    suspend fun snoozeDose(
        eventId: String,
        minutes: Int,
        source: EventSource = EventSource.PHONE
    ): DoseEvent? {
        val existing = eventDao.getById(eventId)?.toModel() ?: return null
        val newAt = System.currentTimeMillis() + minutes * 60_000L
        val updated = existing.copy(
            scheduledAt = newAt,
            status = DoseStatus.PENDING,
            respondedAt = null,
            source = source
        )
        eventDao.upsert(DoseEventEntity.from(updated))
        onDataChanged()
        return updated
    }

    private suspend fun decrementStock(medicationId: String) {
        val med = medicationDao.getById(medicationId)?.toModel() ?: return
        val qty = med.stockQty ?: return
        val next = (qty - 1).coerceAtLeast(0)
        medicationDao.upsert(MedicationEntity.from(med.copy(stockQty = next)))
    }

    suspend fun nextPendingDose(): UpcomingDose? {
        val meds = medicationDao.getAll().map { it.toModel() }.associateBy { it.id }
        val now = System.currentTimeMillis()
        return eventDao.getRecent(200)
            .map { it.toModel() }
            .filter { it.status == DoseStatus.PENDING && it.scheduledAt >= now - 5 * 60_000L }
            .sortedBy { it.scheduledAt }
            .firstNotNullOfOrNull { event ->
                val med = meds[event.medicationId] ?: return@firstNotNullOfOrNull null
                UpcomingDose(event.id, med, event.scheduledAt, event.status)
            }
    }

    suspend fun upsertWatchEvent(event: DoseEvent) {
        eventDao.upsert(DoseEventEntity.from(event))
        if (event.status == DoseStatus.TAKEN) {
            decrementStock(event.medicationId)
        }
        onDataChanged()
    }

    suspend fun buildSyncPayload(): SyncPayload {
        ensureUpcomingEvents()
        return SyncPayload(
            medications = medicationDao.getAll().map { it.toModel() },
            schedules = scheduleDao.getAll().map { it.toModel() },
            events = eventDao.getRecent(300).map { it.toModel() },
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun ensureUpcomingEvents(medicationId: String? = null) {
        val meds = medicationDao.getAll().map { it.toModel() }
            .filter { it.active && (medicationId == null || it.id == medicationId) }
        val schedules = scheduleDao.getAll().map { it.toModel() }
        val now = System.currentTimeMillis()
        val horizon = now + 3L * 24 * 60 * 60 * 1000

        for (med in meds) {
            val medSchedules = schedules.filter { it.medicationId == med.id && it.enabled }
            for (schedule in medSchedules) {
                for (slot in expandScheduleSlots(schedule, now, horizon)) {
                    val id = eventId(med.id, slot)
                    if (eventDao.getById(id) == null) {
                        eventDao.upsert(
                            DoseEventEntity.from(
                                DoseEvent(
                                    id = id,
                                    medicationId = med.id,
                                    scheduledAt = slot,
                                    status = DoseStatus.PENDING,
                                    source = EventSource.PHONE
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    private fun buildTodayDoses(
        meds: List<Medication>,
        schedules: List<DoseSchedule>,
        events: List<DoseEvent>
    ): List<UpcomingDose> {
        val medMap = meds.associateBy { it.id }
        val start = startOfDayMillis()
        val end = start + 24L * 60 * 60 * 1000
        return events
            .filter { it.scheduledAt in start until end }
            .mapNotNull { event ->
                val med = medMap[event.medicationId] ?: return@mapNotNull null
                UpcomingDose(
                    eventId = event.id,
                    medication = med,
                    scheduledAt = event.scheduledAt,
                    status = event.status
                )
            }
            .sortedBy { it.scheduledAt }
    }

    companion object {
        fun newId(): String = UUID.randomUUID().toString()

        fun eventId(medicationId: String, scheduledAt: Long): String =
            "${medicationId}_$scheduledAt"

        fun startOfDayMillis(now: Long = System.currentTimeMillis()): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        fun startOfWeekMillis(now: Long = System.currentTimeMillis()): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        fun computeWeeklyAdherence(
            events: List<DoseEvent>,
            now: Long = System.currentTimeMillis()
        ): WeeklyAdherence {
            val weekStart = startOfWeekMillis(now)
            val scored = events
                .filter { it.scheduledAt in weekStart..now }
                .map { event ->
                    if (event.status == DoseStatus.PENDING && event.scheduledAt < now - 30 * 60_000L) {
                        event.copy(status = DoseStatus.MISSED)
                    } else event
                }
                .filter {
                    it.status == DoseStatus.TAKEN ||
                        it.status == DoseStatus.SKIPPED ||
                        it.status == DoseStatus.MISSED
                }
            val taken = scored.count { it.status == DoseStatus.TAKEN }
            val total = scored.size
            val percent = if (total == 0) 0 else ((taken * 100f) / total).toInt()
            val message = when {
                total == 0 -> "Bu hafta henüz tamamlanan doz yok"
                percent >= 90 -> "Harika gidiyorsun — düzenli kullanım"
                percent >= 70 -> "İyi ilerleme — biraz daha istikrar"
                percent >= 40 -> "Daha iyi olabilir — hatırlatmaları kaçırma"
                else -> "Bu hafta zor geçmiş olabilir — yarın yeni başlangıç"
            }
            return WeeklyAdherence(percent, taken, total, message)
        }

        fun expandScheduleSlots(
            schedule: DoseSchedule,
            from: Long,
            to: Long
        ): List<Long> {
            val result = mutableListOf<Long>()
            val cal = Calendar.getInstance().apply { timeInMillis = from }
            while (cal.timeInMillis < to) {
                val day = cal.get(Calendar.DAY_OF_WEEK) // 1=Pazar ... 7=Cumartesi
                if (schedule.daysOfWeek.contains(day)) {
                    for (time in schedule.times) {
                        val parts = time.split(":")
                        if (parts.size != 2) continue
                        val hour = parts[0].toIntOrNull() ?: continue
                        val minute = parts[1].toIntOrNull() ?: continue
                        val slotCal = Calendar.getInstance().apply {
                            timeInMillis = cal.timeInMillis
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val slot = slotCal.timeInMillis
                        if (slot in from until to) result.add(slot)
                    }
                }
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
            return result
        }
    }
}

data class UpcomingDose(
    val eventId: String,
    val medication: Medication,
    val scheduledAt: Long,
    val status: DoseStatus
)

data class WeeklyAdherence(
    val percent: Int,
    val taken: Int,
    val total: Int,
    val message: String
)
