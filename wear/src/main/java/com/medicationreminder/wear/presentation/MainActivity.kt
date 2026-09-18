package com.medicationreminder.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CardDefaults
import com.medicationreminder.shared.model.DoseEvent
import com.medicationreminder.shared.model.DoseStatus
import com.medicationreminder.shared.model.Medication
import com.medicationreminder.wear.WearApp
import com.medicationreminder.wear.alarm.WearAlarmScheduler
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val Forest = Color(0xFF0D6E4F)
private val ForestSoft = Color(0xFF1A8F68)
private val Mint = Color(0xFFD8F3E7)
private val Ink = Color(0xFF14241C)
private val Bg = Color(0xFF0C1612)
private val Surface = Color(0xFF15201B)
private val SurfaceRaised = Color(0xFF1C2A24)
private val OnBg = Color(0xFFE6F4EC)
private val Muted = Color(0xFF9BB5A8)
private val AccentWarm = Color(0xFFE8B86D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as WearApp
        setContent {
            MaterialTheme(
                colors = androidx.wear.compose.material.Colors(
                    primary = Forest,
                    primaryVariant = ForestSoft,
                    secondary = Mint,
                    secondaryVariant = Mint,
                    background = Bg,
                    surface = Surface,
                    error = Color(0xFFC45C4A),
                    onPrimary = Color.White,
                    onSecondary = Ink,
                    onBackground = OnBg,
                    onSurface = OnBg,
                    onError = Color.White
                )
            ) {
                WearHome(
                    app = app,
                    onOpenReminder = { eventId, medicationId ->
                        startActivity(
                            WearAlarmScheduler.reminderIntent(this, eventId, medicationId)
                        )
                    }
                )
            }
        }
        app.syncManager.requestSync()
    }
}

@Composable
fun WearHome(
    app: WearApp,
    onOpenReminder: (String, String) -> Unit
) {
    val payload by app.cache.payload.collectAsStateWithLifecycle()
    val now = remember { System.currentTimeMillis() }
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            tick++
        }
    }
    val currentNow = remember(tick) { System.currentTimeMillis() }.let {
        if (tick == 0) now else System.currentTimeMillis()
    }

    val upcoming = remember(payload, tick) {
        val medMap = payload.medications.associateBy { it.id }
        val t = System.currentTimeMillis()
        payload.events
            .filter { it.status == DoseStatus.PENDING && it.scheduledAt >= t - 15 * 60_000L }
            .sortedBy { it.scheduledAt }
            .mapNotNull { event ->
                val med = medMap[event.medicationId] ?: return@mapNotNull null
                med to event
            }
            .take(20)
    }
    val next = upcoming.firstOrNull()
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale("tr")) }
    var syncing by remember { mutableStateOf(false) }
    var syncHint by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(syncHint) {
        if (syncHint != null) {
            delay(2200)
            syncHint = null
            syncing = false
        }
    }

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { TimeText() }

        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "İlaç Hatırlatıcı",
                    style = MaterialTheme.typography.title3,
                    fontWeight = FontWeight.SemiBold,
                    color = OnBg
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when {
                        upcoming.isEmpty() -> "Yaklaşan doz yok"
                        else -> "${upcoming.size} yaklaşan · sıradaki vurgulu"
                    },
                    style = MaterialTheme.typography.caption2,
                    color = Muted,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (next != null) {
            item {
                NextDoseHero(
                    medication = next.first,
                    event = next.second,
                    timeLabel = timeFmt.format(Date(next.second.scheduledAt)),
                    relative = relativeLabel(next.second.scheduledAt, currentNow),
                    onClick = { onOpenReminder(next.second.id, next.first.id) }
                )
            }
        }

        if (upcoming.isEmpty()) {
            item {
                EmptyWatchState(
                    hasMeds = payload.medications.isNotEmpty()
                )
            }
        } else {
            itemsIndexed(
                upcoming.drop(if (next != null) 1 else 0),
                key = { _, item -> item.second.id }
            ) { _, pair ->
                val (med, event) = pair
                DoseListCard(
                    medication = med,
                    event = event,
                    timeLabel = timeFmt.format(Date(event.scheduledAt)),
                    relative = relativeLabel(event.scheduledAt, currentNow),
                    highlighted = false,
                    onClick = { onOpenReminder(event.id, med.id) }
                )
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            Chip(
                onClick = {
                    if (syncing) return@Chip
                    app.syncManager.requestSync()
                    syncing = true
                    syncHint = "Telefondan isteniyor…"
                },
                enabled = !syncing,
                label = {
                    Text(if (syncing) "Senkron…" else "Senkronize et")
                },
                colors = ChipDefaults.secondaryChipColors(
                    backgroundColor = SurfaceRaised,
                    contentColor = OnBg
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (syncHint != null) {
            item {
                Text(
                    text = syncHint!!,
                    style = MaterialTheme.typography.caption2,
                    color = AccentWarm,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun NextDoseHero(
    medication: Medication,
    event: DoseEvent,
    timeLabel: String,
    relative: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = Forest,
            endBackgroundColor = ForestSoft
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SIRADAKİ",
                style = MaterialTheme.typography.caption2,
                fontWeight = FontWeight.Bold,
                color = Mint.copy(alpha = 0.9f),
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.display3,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = relative,
                style = MaterialTheme.typography.caption2,
                color = Mint
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = medication.name,
                style = MaterialTheme.typography.title3,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (medication.dosage.isNotBlank()) {
                Text(
                    text = medication.dosage,
                    style = MaterialTheme.typography.body2,
                    color = Mint.copy(alpha = 0.95f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (medication.notes.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = medication.notes,
                    style = MaterialTheme.typography.caption2,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DoseListCard(
    medication: Medication,
    event: DoseEvent,
    timeLabel: String,
    relative: String,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 3.dp)
            .then(
                if (highlighted) {
                    Modifier.border(1.5.dp, ForestSoft, RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = SurfaceRaised,
            endBackgroundColor = Surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Forest.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.caption1,
                    fontWeight = FontWeight.Bold,
                    color = Mint,
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medication.name,
                    style = MaterialTheme.typography.button,
                    fontWeight = FontWeight.SemiBold,
                    color = OnBg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        if (medication.dosage.isNotBlank()) append(medication.dosage).append(" · ")
                        append(relative)
                    },
                    style = MaterialTheme.typography.caption2,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (medication.notes.isNotBlank()) {
                    Text(
                        text = medication.notes,
                        style = MaterialTheme.typography.caption2,
                        color = AccentWarm.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyWatchState(hasMeds: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceRaised)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Forest.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text("· · ·", color = Mint, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (hasMeds) "Şimdilik boş" else "Henüz ilaç yok",
            style = MaterialTheme.typography.title3,
            color = OnBg,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (hasMeds) {
                "Bekleyen doz yok. Telefondan saat ekleyebilirsin."
            } else {
                "Telefonda ilaç ekleyip Senkronize et."
            },
            style = MaterialTheme.typography.caption2,
            color = Muted,
            textAlign = TextAlign.Center
        )
    }
}

private fun relativeLabel(scheduledAt: Long, now: Long): String {
    val diff = scheduledAt - now
    return when {
        diff <= 0 -> "Şimdi · dokun"
        diff < TimeUnit.MINUTES.toMillis(1) -> "1 dk içinde"
        diff < TimeUnit.HOURS.toMillis(1) -> {
            val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
            "$mins dk içinde"
        }
        diff < TimeUnit.HOURS.toMillis(6) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val mins = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
            if (mins == 0L) "$hours sa içinde" else "$hours sa $mins dk"
        }
        else -> "Planlandı"
    }
}
