package org.brightchain.brightdate.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.brightchain.brightdate.alarm.data.AlarmRepository
import org.brightchain.brightdate.alarm.service.AlarmScheduler

/**
 * Reschedules all enabled alarms after the device reboots or the app is updated.
 *
 * AlarmManager alarms are cleared on reboot, so this receiver restores them
 * from the Room database.
 */
class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val result = goAsync()
        scope.launch {
            try {
                val repo = AlarmRepository(context)
                val enabled = repo.getEnabledAlarms()
                AlarmScheduler.rescheduleAll(context, enabled)
            } finally {
                result.finish()
            }
        }
    }
}
