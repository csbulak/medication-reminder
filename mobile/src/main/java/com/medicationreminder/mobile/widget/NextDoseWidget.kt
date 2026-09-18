package com.medicationreminder.mobile.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.medicationreminder.mobile.MainActivity
import com.medicationreminder.mobile.MedicationReminderApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NextDoseWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = MedicationReminderApp.get(context)
        val payload = runCatching { app.repository.buildSyncPayload() }.getOrNull()
        val next = runCatching { app.repository.nextPendingDose() }.getOrNull()
        val lowCount = payload?.medications?.count {
            it.stockQty != null && it.stockQty!! <= it.lowStockAt
        } ?: 0

        provideContent {
            GlanceTheme {
                WidgetContent(
                    nextName = next?.medication?.name,
                    nextDose = next?.let {
                        val t = SimpleDateFormat("HH:mm", Locale("tr")).format(Date(it.scheduledAt))
                        "$t · ${it.medication.dosage}"
                    },
                    lowCount = lowCount
                )
            }
        }
    }
}

@Composable
private fun WidgetContent(
    nextName: String?,
    nextDose: String?,
    lowCount: Int
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.primaryContainer)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Sıradaki doz",
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = nextName ?: "Plan yok",
            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
        )
        if (nextDose != null) {
            Text(text = nextDose, style = TextStyle(fontSize = 14.sp))
        }
        if (lowCount > 0) {
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = "$lowCount ilaçta düşük stok",
                style = TextStyle(fontSize = 12.sp)
            )
        }
    }
}

class NextDoseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextDoseWidget()
}
