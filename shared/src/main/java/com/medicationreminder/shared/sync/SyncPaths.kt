package com.medicationreminder.shared.sync

object SyncPaths {
    const val MEDICATIONS = "/medications"
    const val SCHEDULES = "/schedules"
    const val DOSE_EVENTS = "/dose_events"
    const val FULL_SYNC = "/full_sync"
    const val MESSAGE_DOSE_RESPONSE = "/dose_response"
    const val MESSAGE_REQUEST_SYNC = "/request_sync"
    const val MESSAGE_SNOOZE = "/snooze"
}

object SyncKeys {
    const val PAYLOAD_JSON = "payload_json"
    const val EVENT_ID = "event_id"
    const val MEDICATION_ID = "medication_id"
    const val SCHEDULED_AT = "scheduled_at"
    const val STATUS = "status"
    const val RESPONDED_AT = "responded_at"
    const val SOURCE = "source"
}
