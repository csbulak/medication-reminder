package com.medicationreminder.mobile

import android.app.Application
import com.medicationreminder.mobile.data.AppDatabase
import com.medicationreminder.mobile.data.MedicationRepository
import com.medicationreminder.mobile.notification.DoseScheduler
import com.medicationreminder.mobile.sync.PhoneSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MedicationReminderApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: AppDatabase
        private set
    lateinit var repository: MedicationRepository
        private set
    lateinit var syncManager: PhoneSyncManager
        private set
    lateinit var doseScheduler: DoseScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.get(this)
        doseScheduler = DoseScheduler(this).also { it.ensureChannel() }

        // syncManager henüz yokken callback'i gecikmeli bağlarız
        lateinit var repo: MedicationRepository
        repo = MedicationRepository(database) {
            if (::syncManager.isInitialized) {
                syncManager.pushFullSync()
            }
            if (::doseScheduler.isInitialized) {
                doseScheduler.rescheduleAll(repo)
            }
            com.medicationreminder.mobile.widget.WidgetUpdater.refresh(this@MedicationReminderApp)
        }
        repository = repo
        syncManager = PhoneSyncManager(this, repository)

        appScope.launch {
            repository.ensureUpcomingEvents()
            doseScheduler.rescheduleAll(repository)
            syncManager.pushFullSync()
        }
    }

    companion object {
        @Volatile
        private var instance: MedicationReminderApp? = null

        fun get(context: android.content.Context): MedicationReminderApp =
            instance ?: context.applicationContext as MedicationReminderApp
    }
}
