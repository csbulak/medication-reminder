package com.medicationreminder.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.medicationreminder.shared.model.DoseStatus
import com.medicationreminder.wear.WearApp
import com.medicationreminder.wear.alarm.WearAlarmScheduler

private val Forest = Color(0xFF0D6E4F)

class ReminderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val eventId = intent.getStringExtra(WearAlarmScheduler.EXTRA_EVENT_ID) ?: run {
            finish()
            return
        }
        val app = application as WearApp
        setContent {
            MaterialTheme(
                colors = androidx.wear.compose.material.Colors(
                    primary = Forest,
                    primaryVariant = Forest,
                    secondary = Color(0xFFD8F3E7),
                    secondaryVariant = Color(0xFFD8F3E7),
                    background = Color(0xFF0C1612),
                    surface = Color(0xFF15201B),
                    error = Color(0xFFC45C4A),
                    onPrimary = Color.White,
                    onSecondary = Color(0xFF14241C),
                    onBackground = Color(0xFFE6F4EC),
                    onSurface = Color(0xFFE6F4EC),
                    onError = Color.White
                )
            ) {
                ReminderScreen(
                    eventId = eventId,
                    app = app,
                    onDone = { finish() }
                )
            }
        }
    }
}

@Composable
fun ReminderScreen(
    eventId: String,
    app: WearApp,
    onDone: () -> Unit
) {
    val pair = app.cache.findEvent(eventId)
    var resultStatus by remember { mutableStateOf<DoseStatus?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (pair == null) {
            Text("Doz bulunamadı", textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onDone) { Text("Kapat") }
            return
        }

        val (med, event) = pair
        val effectiveStatus = resultStatus ?: event.status

        Text(
            text = med.name,
            style = MaterialTheme.typography.title2,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = med.dosage,
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))

        AnimatedVisibility(
            visible = effectiveStatus == DoseStatus.TAKEN,
            enter = fadeIn() + scaleIn()
        ) {
            Text(
                text = "Kaydedildi",
                color = Color(0xFF6DD4A8),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.title3
            )
        }

        if (effectiveStatus != DoseStatus.PENDING) {
            if (effectiveStatus != DoseStatus.TAKEN) {
                Text(
                    text = when (effectiveStatus) {
                        DoseStatus.SKIPPED -> "Atlandı"
                        DoseStatus.MISSED -> "Kaçırıldı"
                        else -> ""
                    },
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Kapat") }
        } else {
            Button(
                onClick = {
                    val updated = app.cache.markLocal(eventId, DoseStatus.TAKEN)
                    if (updated != null) app.syncManager.sendDoseResponse(updated)
                    resultStatus = DoseStatus.TAKEN
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.primaryButtonColors(backgroundColor = Forest)
            ) { Text("Aldım") }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val updated = app.cache.markLocal(eventId, DoseStatus.SKIPPED)
                    if (updated != null) app.syncManager.sendDoseResponse(updated)
                    resultStatus = DoseStatus.SKIPPED
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.secondaryButtonColors()
            ) { Text("Atla") }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val updated = app.cache.snoozeLocal(eventId, 10)
                    if (updated != null) {
                        com.medicationreminder.wear.alarm.WearAlarmScheduler(app).schedule(
                            updated.id, updated.medicationId, updated.scheduledAt
                        )
                        app.syncManager.sendSnooze(eventId, 10)
                    }
                    resultStatus = DoseStatus.PENDING
                    onDone()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.secondaryButtonColors()
            ) { Text("10 dk ertele") }
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = {
                    val updated = app.cache.snoozeLocal(eventId, 30)
                    if (updated != null) {
                        com.medicationreminder.wear.alarm.WearAlarmScheduler(app).schedule(
                            updated.id, updated.medicationId, updated.scheduledAt
                        )
                        app.syncManager.sendSnooze(eventId, 30)
                    }
                    onDone()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.secondaryButtonColors()
            ) { Text("30 dk ertele") }
        }
    }
}
