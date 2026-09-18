package com.medicationreminder.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medicationreminder.mobile.ui.MainViewModel
import com.medicationreminder.mobile.ui.screens.MobileApp
import com.medicationreminder.mobile.ui.theme.MedicationReminderTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* kullanıcı kararı */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        val app = application as MedicationReminderApp
        setContent {
            MedicationReminderTheme {
                val vm: MainViewModel = viewModel(
                    factory = MainViewModel.Factory(
                        repository = app.repository,
                        syncManager = app.syncManager,
                        doseScheduler = app.doseScheduler
                    )
                )
                MobileApp(viewModel = vm)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
