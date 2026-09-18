package com.medicationreminder.wear.sync

import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.medicationreminder.shared.sync.SyncPaths
import com.medicationreminder.wear.WearApp

class WearDataListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        WearApp.get(this).syncManager.handleDataEvents(dataEvents)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        // Telefon bağlantısı geldiğinde kuyruktaki yanıtları gönder
        if (messageEvent.path == SyncPaths.MESSAGE_REQUEST_SYNC) {
            WearApp.get(this).syncManager.flushPending()
        }
    }
}
