package com.medicationreminder.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.medicationreminder.mobile.data.MedicationRepository
import com.medicationreminder.mobile.data.UpcomingDose
import com.medicationreminder.mobile.data.WeeklyAdherence
import com.medicationreminder.mobile.notification.DoseScheduler
import com.medicationreminder.mobile.sync.PhoneSyncManager
import com.medicationreminder.shared.model.DoseEvent
import com.medicationreminder.shared.model.DoseSchedule
import com.medicationreminder.shared.model.DoseStatus
import com.medicationreminder.shared.model.EventSource
import com.medicationreminder.shared.model.Medication
import com.medicationreminder.mobile.sync.SyncResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: MedicationRepository,
    private val syncManager: PhoneSyncManager,
    private val doseScheduler: DoseScheduler
) : ViewModel() {

    val medications: StateFlow<List<Medication>> = repository.observeMedications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayDoses: StateFlow<List<UpcomingDose>> = repository.observeTodayDoses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val events: StateFlow<List<DoseEvent>> = repository.observeEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weeklyAdherence: StateFlow<WeeklyAdherence> = repository.observeWeeklyAdherence()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            WeeklyAdherence(0, 0, 0, "Bu hafta henüz tamamlanan doz yok")
        )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncFeedback = MutableSharedFlow<SyncResult>(extraBufferCapacity = 1)
    val syncFeedback: SharedFlow<SyncResult> = _syncFeedback.asSharedFlow()

    fun saveMedication(
        id: String?,
        name: String,
        dosage: String,
        notes: String,
        times: List<String>,
        daysOfWeek: List<Int>,
        active: Boolean,
        stockQty: Int?,
        lowStockAt: Int = 5
    ) {
        viewModelScope.launch {
            val medId = id ?: MedicationRepository.newId()
            val medication = Medication(
                id = medId,
                name = name.trim(),
                dosage = dosage.trim(),
                notes = notes.trim(),
                active = active,
                stockQty = stockQty,
                lowStockAt = lowStockAt.coerceAtLeast(0)
            )
            val schedule = DoseSchedule(
                id = MedicationRepository.newId(),
                medicationId = medId,
                times = times.ifEmpty { listOf("08:00") },
                daysOfWeek = daysOfWeek.ifEmpty { listOf(1, 2, 3, 4, 5, 6, 7) },
                enabled = active
            )
            repository.saveMedication(medication, schedule)
            doseScheduler.rescheduleAll(repository)
            syncManager.pushFullSync()
        }
    }

    fun deleteMedication(id: String) {
        viewModelScope.launch {
            repository.deleteMedication(id)
            doseScheduler.rescheduleAll(repository)
            syncManager.pushFullSync()
        }
    }

    fun markDose(eventId: String, status: DoseStatus) {
        viewModelScope.launch {
            repository.recordDoseResponse(eventId, status, EventSource.PHONE)
            doseScheduler.rescheduleAll(repository)
            syncManager.pushFullSync()
        }
    }

    fun snoozeDose(eventId: String, minutes: Int) {
        viewModelScope.launch {
            val updated = repository.snoozeDose(eventId, minutes, EventSource.PHONE) ?: return@launch
            doseScheduler.scheduleExact(updated.id, updated.medicationId, updated.scheduledAt)
            syncManager.pushFullSync()
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            if (_isSyncing.value) return@launch
            _isSyncing.value = true
            try {
                val result = syncManager.pushFullSync()
                _syncFeedback.emit(result)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    suspend fun loadSchedules(medicationId: String): List<DoseSchedule> =
        repository.getSchedulesFor(medicationId)

    class Factory(
        private val repository: MedicationRepository,
        private val syncManager: PhoneSyncManager,
        private val doseScheduler: DoseScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository, syncManager, doseScheduler) as T
        }
    }
}
