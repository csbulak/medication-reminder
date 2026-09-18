package com.medicationreminder.mobile.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetUpdater {
    fun refresh(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { NextDoseWidget().updateAll(context.applicationContext) }
        }
    }
}
