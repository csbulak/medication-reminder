package com.medicationreminder.mobile.ui.screens

import androidx.compose.foundation.clickable
import com.medicationreminder.mobile.ui.components.TodayProgressCard
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.medicationreminder.mobile.data.UpcomingDose
import com.medicationreminder.mobile.ui.MainViewModel
import com.medicationreminder.mobile.data.WeeklyAdherence
import com.medicationreminder.mobile.ui.components.CareNoteBanner
import com.medicationreminder.mobile.ui.components.EmptyState
import com.medicationreminder.mobile.ui.components.WeeklyAdherenceCard
import com.medicationreminder.mobile.ui.components.MedAvatar
import com.medicationreminder.mobile.ui.components.StatusChip
import com.medicationreminder.mobile.ui.components.SummaryHeader
import com.medicationreminder.mobile.ui.components.TimeBadge
import com.medicationreminder.shared.model.DoseEvent
import com.medicationreminder.shared.model.DoseStatus
import com.medicationreminder.shared.model.Medication
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val medications by viewModel.medications.collectAsStateWithLifecycle()
            val today by viewModel.todayDoses.collectAsStateWithLifecycle()
            val events by viewModel.events.collectAsStateWithLifecycle()
            val weeklyAdherence by viewModel.weeklyAdherence.collectAsStateWithLifecycle()
            val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            val haptics = LocalHapticFeedback.current

            LaunchedEffect(Unit) {
                viewModel.syncFeedback.collect { result ->
                    haptics.performHapticFeedback(
                        if (result.success && result.connectedWatchCount > 0) {
                            HapticFeedbackType.LongPress
                        } else {
                            HapticFeedbackType.TextHandleMove
                        }
                    )
                    snackbarHostState.showSnackbar(result.message)
                }
            }

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text("İlaç Hatırlatıcı", fontWeight = FontWeight.SemiBold)
                                Text(
                                    when (tab) {
                                        0 -> "Bugünkü plan"
                                        1 -> "Kayıtlı ilaçlar"
                                        else -> "Geçmiş kayıtlar"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        actions = {
                            val syncTransition = rememberInfiniteTransition(label = "syncSpin")
                            val syncAngle by syncTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(900, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "syncAngle"
                            )
                            IconButton(
                                onClick = { viewModel.syncNow() },
                                enabled = !isSyncing
                            ) {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = if (isSyncing) {
                                        "Senkronize ediliyor"
                                    } else {
                                        "Saate senkronize et"
                                    },
                                    modifier = Modifier.rotate(if (isSyncing) syncAngle else 0f)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                floatingActionButton = {
                    if (tab == 1) {
                        FloatingActionButton(
                            onClick = { navController.navigate("edit") },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "İlaç ekle")
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Icon(Icons.Default.Today, contentDescription = null) },
                            label = { Text("Bugün") }
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Icon(Icons.Default.Medication, contentDescription = null) },
                            label = { Text("İlaçlar") }
                        )
                        NavigationBarItem(
                            selected = tab == 2,
                            onClick = { tab = 2 },
                            icon = { Icon(Icons.Default.History, contentDescription = null) },
                            label = { Text("Geçmiş") }
                        )
                    }
                }
            ) { padding ->
                when (tab) {
                    0 -> TodayScreen(
                        doses = today,
                        adherence = weeklyAdherence,
                        padding = padding,
                        onTaken = { viewModel.markDose(it, DoseStatus.TAKEN) },
                        onSkip = { viewModel.markDose(it, DoseStatus.SKIPPED) },
                        onSnooze = { id, min -> viewModel.snoozeDose(id, min) }
                    )
                    1 -> MedicationListScreen(
                        medications = medications,
                        padding = padding,
                        onOpen = { id -> navController.navigate("edit?id=$id") }
                    )
                    else -> HistoryScreen(events = events, medications = medications, adherence = weeklyAdherence, padding = padding)
                }
            }
        }
        composable(
            route = "edit?id={id}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            MedicationEditScreen(
                medicationId = entry.arguments?.getString("id"),
                viewModel = viewModel,
                onDone = { navController.popBackStack() }
            )
        }
        composable("edit") {
            MedicationEditScreen(
                medicationId = null,
                viewModel = viewModel,
                onDone = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun TodayScreen(
    doses: List<UpcomingDose>,
    adherence: WeeklyAdherence,
    padding: PaddingValues,
    onTaken: (String) -> Unit,
    onSkip: (String) -> Unit,
    onSnooze: (String, Int) -> Unit
) {
    val timeFmt = SimpleDateFormat("HH:mm", Locale("tr"))
    val dateFmt = SimpleDateFormat("d MMMM EEEE", Locale("tr"))
    val total = doses.size
    val taken = doses.count { it.status == DoseStatus.TAKEN }
    val pending = doses.count { it.status == DoseStatus.PENDING }
    val nextId = doses
        .filter { it.status == DoseStatus.PENDING }
        .minByOrNull { it.scheduledAt }
        ?.eventId

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SummaryHeader(
                title = dateFmt.format(Date()).replaceFirstChar { it.uppercase() },
                subtitle = when {
                    total == 0 -> "Bugün planlanmış doz yok"
                    pending == 0 -> "Harika, bugünkü dozlar tamam"
                    else -> "Sıradaki doz vurgulandı"
                }
            )
        }
        if (total > 0) {
            item {
                TodayProgressCard(
                    taken = taken,
                    total = total,
                    subtitle = when {
                        pending == 0 -> "Tüm dozlar tamamlandı"
                        else -> "$pending doz bekliyor"
                    }
                )
            }
        }
        item {
            WeeklyAdherenceCard(
                percent = adherence.percent,
                taken = adherence.taken,
                total = adherence.total,
                message = adherence.message
            )
        }
        if (doses.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.Schedule,
                    title = "Bugün boş",
                    subtitle = "İlaçlar sekmesinden ekleyip saat planlayabilirsiniz.",
                    modifier = Modifier.height(320.dp)
                )
            }
        } else {
            items(doses, key = { it.eventId }) { dose ->
                DoseCard(
                    title = dose.medication.name,
                    dosage = dose.medication.dosage,
                    time = timeFmt.format(Date(dose.scheduledAt)),
                    status = dose.status,
                    isNext = dose.eventId == nextId,
                    careNote = dose.medication.notes,
                    lowStock = dose.medication.stockQty != null &&
                        dose.medication.stockQty!! <= dose.medication.lowStockAt,
                    stockLabel = dose.medication.stockQty?.let { "Stok: $it" },
                    onTaken = { onTaken(dose.eventId) },
                    onSkip = { onSkip(dose.eventId) },
                    onSnooze = { min -> onSnooze(dose.eventId, min) }
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun MedicationListScreen(
    medications: List<Medication>,
    padding: PaddingValues,
    onOpen: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SummaryHeader(
                title = "İlaçlarım",
                subtitle = if (medications.isEmpty()) {
                    "Henüz ilaç eklenmedi"
                } else {
                    "${medications.size} kayıt · dokunarak düzenle"
                }
            )
        }
        if (medications.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.Medication,
                    title = "İlaç ekle",
                    subtitle = "Sağ alttaki + butonuyla ilk ilacını ekle.",
                    modifier = Modifier.height(360.dp)
                )
            }
        } else {
            items(medications, key = { it.id }) { med ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(med.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MedAvatar(med.name)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(med.name, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                buildString {
                                    append(med.dosage.ifBlank { "Doz belirtilmedi" })
                                    if (med.stockQty != null) append(" · stok ").append(med.stockQty)
                                    if (!med.active) append(" · pasif")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (med.notes.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                CareNoteBanner(note = med.notes)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

@Composable
fun HistoryScreen(
    events: List<DoseEvent>,
    medications: List<Medication>,
    adherence: WeeklyAdherence,
    padding: PaddingValues
) {
    val medMap = medications.associateBy { it.id }
    val fmt = SimpleDateFormat("d MMM · HH:mm", Locale("tr"))
    val history = events.filter { it.status != DoseStatus.PENDING }.take(100)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SummaryHeader(
                title = "Geçmiş",
                subtitle = if (history.isEmpty()) "Henüz kayıt yok" else "${history.size} kayıt"
            )
        }
        item {
            WeeklyAdherenceCard(
                percent = adherence.percent,
                taken = adherence.taken,
                total = adherence.total,
                message = adherence.message
            )
        }
        if (history.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.WatchLater,
                    title = "Kayıt yok",
                    subtitle = "Aldım veya Atla dediğinde burada görünecek.",
                    modifier = Modifier.height(360.dp)
                )
            }
        } else {
            items(history, key = { it.id }) { event ->
                val name = medMap[event.medicationId]?.name ?: "İlaç"
                val source = if (event.source.name == "WATCH") "Saat" else "Telefon"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${fmt.format(Date(event.scheduledAt))} · $source",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusChip(event.status)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun DoseCard(
    title: String,
    dosage: String,
    time: String,
    status: DoseStatus,
    isNext: Boolean = false,
    careNote: String = "",
    lowStock: Boolean = false,
    stockLabel: String? = null,
    onTaken: () -> Unit,
    onSkip: () -> Unit,
    onSnooze: (Int) -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    var showTakenFlash by remember { mutableStateOf(false) }

    LaunchedEffect(showTakenFlash) {
        if (showTakenFlash) {
            delay(900)
            showTakenFlash = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isNext) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isNext) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box {
            Column(modifier = Modifier.padding(16.dp)) {
                if (isNext && status == DoseStatus.PENDING) {
                    Text(
                        "SIRADAKİ",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimeBadge(time)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            buildString {
                                append(dosage)
                                if (!stockLabel.isNullOrBlank()) append(" · ").append(stockLabel)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (lowStock) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (lowStock) {
                            Text(
                                "Düşük stok",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    StatusChip(status)
                }
                if (careNote.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    CareNoteBanner(note = careNote)
                }
                if (status == DoseStatus.PENDING) {
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                showTakenFlash = true
                                onTaken()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Aldım")
                        }
                        OutlinedButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSkip()
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Atla") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onSnooze(10) },
                            modifier = Modifier.weight(1f)
                        ) { Text("10 dk ertele") }
                        OutlinedButton(
                            onClick = { onSnooze(30) },
                            modifier = Modifier.weight(1f)
                        ) { Text("30 dk ertele") }
                    }
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = showTakenFlash,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                androidx.compose.material3.Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(50),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Kaydedildi",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
