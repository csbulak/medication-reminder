package com.medicationreminder.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medicationreminder.mobile.ui.MainViewModel

private val dayLabels = listOf(
    2 to "Pzt",
    3 to "Sal",
    4 to "Çar",
    5 to "Per",
    6 to "Cum",
    7 to "Cmt",
    1 to "Paz"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MedicationEditScreen(
    medicationId: String?,
    viewModel: MainViewModel,
    onDone: () -> Unit
) {
    val medications by viewModel.medications.collectAsStateWithLifecycle()
    val existing = medications.find { it.id == medicationId }

    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(true) }
    var trackStock by remember { mutableStateOf(false) }
    var stockText by remember { mutableStateOf("30") }
    var lowStockText by remember { mutableStateOf("5") }
    val times = remember { mutableStateListOf("08:00", "20:00") }
    val selectedDays = remember { mutableStateListOf(1, 2, 3, 4, 5, 6, 7) }
    var showTimePicker by remember { mutableStateOf(false) }
    var editingTimeIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(existing?.id) {
        if (existing != null) {
            name = existing.name
            dosage = existing.dosage
            notes = existing.notes
            active = existing.active
            trackStock = existing.stockQty != null
            stockText = (existing.stockQty ?: 30).toString()
            lowStockText = existing.lowStockAt.toString()
            val schedules = viewModel.loadSchedules(existing.id)
            val schedule = schedules.firstOrNull()
            if (schedule != null) {
                times.clear()
                times.addAll(schedule.times.ifEmpty { listOf("08:00") })
                selectedDays.clear()
                selectedDays.addAll(schedule.daysOfWeek)
            }
        }
    }

    if (showTimePicker) {
        val initial = editingTimeIndex?.let { times.getOrNull(it) }
        TimePickerDialog(
            initialHour = initial?.substringBefore(":")?.toIntOrNull() ?: 8,
            initialMinute = initial?.substringAfter(":")?.toIntOrNull() ?: 0,
            onDismiss = {
                showTimePicker = false
                editingTimeIndex = null
            },
            onConfirm = { hour, minute ->
                val value = "%02d:%02d".format(hour, minute)
                val index = editingTimeIndex
                if (index != null && index in times.indices) {
                    times[index] = value
                } else if (!times.contains(value)) {
                    times.add(value)
                    times.sort()
                }
                showTimePicker = false
                editingTimeIndex = null
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (medicationId == null) "İlaç ekle" else "İlaç düzenle") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (medicationId != null) {
                        TextButton(onClick = {
                            viewModel.deleteMedication(medicationId)
                            onDone()
                        }) { Text("Sil") }
                    }
                    TextButton(
                        onClick = {
                            if (name.isBlank()) return@TextButton
                            viewModel.saveMedication(
                                id = medicationId,
                                name = name,
                                dosage = dosage.ifBlank { "1 doz" },
                                notes = notes,
                                times = times.toList().ifEmpty { listOf("08:00") },
                                daysOfWeek = selectedDays.sorted().ifEmpty { listOf(1, 2, 3, 4, 5, 6, 7) },
                                active = active,
                                stockQty = if (trackStock) stockText.toIntOrNull()?.coerceAtLeast(0) else null,
                                lowStockAt = lowStockText.toIntOrNull()?.coerceAtLeast(0) ?: 5
                            )
                            onDone()
                        },
                        enabled = name.isNotBlank()
                    ) { Text("Kaydet") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            SectionTitle("İlaç bilgisi")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("İlaç adı") },
                placeholder = { Text("Örn. Aspirin") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text("Doz") },
                placeholder = { Text("Örn. 1 tablet") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Bakıcı notu (isteğe bağlı)") },
                placeholder = { Text("Örn. Yemekten sonra, bol su ile") },
                supportingText = { Text("Doz kartlarında ve ilaç listesinde öne çıkar") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            SectionTitle("Hatırlatma saatleri")
            Text(
                "Her doz için bir saat ekleyin. Dokunarak düzenleyebilirsiniz.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                times.forEachIndexed { index, time ->
                    AssistChip(
                        onClick = {
                            editingTimeIndex = index
                            showTimePicker = true
                        },
                        label = { Text(time, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { times.removeAt(index) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Saati sil",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    )
                }
            }
            OutlinedButton(
                onClick = {
                    editingTimeIndex = null
                    showTimePicker = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Saat ekle")
            }

            SectionTitle("Hangi günler?")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dayLabels.forEach { (day, label) ->
                    FilterChip(
                        selected = selectedDays.contains(day),
                        onClick = {
                            if (selectedDays.contains(day)) {
                                if (selectedDays.size > 1) selectedDays.remove(day)
                            } else {
                                selectedDays.add(day)
                            }
                        },
                        label = { Text(label) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        selectedDays.clear()
                        selectedDays.addAll(listOf(1, 2, 3, 4, 5, 6, 7))
                    }
                ) { Text("Her gün") }
                TextButton(
                    onClick = {
                        selectedDays.clear()
                        selectedDays.addAll(listOf(2, 3, 4, 5, 6))
                    }
                ) { Text("Hafta içi") }
            }


            SectionTitle("Stok takibi")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Stok say", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Her alımda 1 düşer; eşik altında uyarı",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
                Switch(checked = trackStock, onCheckedChange = { trackStock = it })
            }
            if (trackStock) {
                OutlinedTextField(
                    value = stockText,
                    onValueChange = { stockText = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = { Text("Kalan miktar") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = lowStockText,
                    onValueChange = { lowStockText = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Düşük stok eşiği") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Aktif", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Kapalıysa hatırlatma gönderilmez",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
                Switch(checked = active, onCheckedChange = { active = it })
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Saat seç") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("Tamam")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}
