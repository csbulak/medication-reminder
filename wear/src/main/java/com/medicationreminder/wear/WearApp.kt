package com.medicationreminder.wear

import android.app.Application
import com.medicationreminder.wear.alarm.WearAlarmScheduler
import com.medicationreminder.wear.data.WearCache
import com.medicationreminder.wear.sync.WearSyncManager

class WearApp : Application() {

    lateinit var cache: WearCache
        private set
    lateinit var syncManager: WearSyncManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        cache = WearCache(this)
        syncManager = WearSyncManager(this, cache)
        WearAlarmScheduler(this).reschedule(cache)
        syncManager.requestSync()
        syncManager.flushPending()
    }

    companion object {
        @Volatile
        private var instance: WearApp? = null

        fun get(context: android.content.Context): WearApp =
            instance ?: context.applicationContext as WearApp
    }
}
