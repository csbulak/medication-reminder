package com.medicationreminder.wear.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.medicationreminder.wear.WearApp
import com.medicationreminder.wear.presentation.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NextDoseComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("08:00").build(),
            contentDescription = PlainComplicationText.Builder("Sıradaki doz").build()
        )
            .setTitle(PlainComplicationText.Builder("İlaç").build())
            .build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT) return null
        val next = WearApp.get(this).cache.nextPending()
        val tap = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return if (next == null) {
            ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder("--:--").build(),
                contentDescription = PlainComplicationText.Builder("Yaklaşan doz yok").build()
            )
                .setTitle(PlainComplicationText.Builder("İlaç").build())
                .setTapAction(tap)
                .build()
        } else {
            val (med, event) = next
            val time = SimpleDateFormat("HH:mm", Locale("tr")).format(Date(event.scheduledAt))
            ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(time).build(),
                contentDescription = PlainComplicationText.Builder(
                    "${med.name} · $time"
                ).build()
            )
                .setTitle(
                    PlainComplicationText.Builder(med.name.take(8)).build()
                )
                .setTapAction(tap)
                .build()
        }
    }
}
