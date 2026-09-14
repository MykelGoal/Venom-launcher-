package com.venom.launcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.venom.launcher.data.ChargeNotifier
import com.venom.launcher.data.ChargePrefs
import com.venom.launcher.data.VenomBus
import com.venom.launcher.data.VenomWidgetHost
import com.venom.launcher.ui.LauncherRoot
import com.venom.launcher.ui.theme.VenomTheme
import com.venom.launcher.vm.LauncherViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var vm: LauncherViewModel

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        vm = ViewModelProvider(this)[LauncherViewModel::class.java]

        lifecycleScope.launch {
            ChargePrefs.prime(this@MainActivity)
            ChargeNotifier.ensureChannel(this@MainActivity)
        }

        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            VenomTheme(settings = settings) {
                LauncherRoot(vm = vm)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                VenomBus.homePress.collect { /* consumed by LauncherRoot */ }
            }
        }

        handleIntent(intent)
        askNotificationPermissionIfNeeded()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val i = intent ?: return
        if (i.getBooleanExtra(EXTRA_OPEN_CHARGE_LAB, false)) {
            VenomBus.openChargeLab()
        }
        if (i.action == Intent.ACTION_MAIN &&
            i.categories?.contains(Intent.CATEGORY_HOME) == true
        ) {
            VenomBus.homePressed()
        }
    }

    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onStart() {
        super.onStart()
        runCatching { VenomWidgetHost.get(this).startListening() }
    }

    override fun onStop() {
        runCatching { VenomWidgetHost.get(this).stopListening() }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }

    companion object {
        const val EXTRA_OPEN_CHARGE_LAB = "extra_open_charge_lab"
    }
}
