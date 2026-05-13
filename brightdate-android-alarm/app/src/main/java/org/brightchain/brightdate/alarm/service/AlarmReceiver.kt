package org.brightchain.brightdate.alarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.brightchain.brightdate.alarm.BrightDate
import org.brightchain.brightdate.alarm.R
import org.brightchain.brightdate.alarm.data.AlarmRepository
import org.brightchain.brightdate.alarm.ui.AlarmFireActivity

/**
 * Receives the broadcast when an AlarmManager alarm fires.
 *
 * Responsibilities:
 * 1. Acquire a brief wakelock to ensure the rest of the logic runs.
 * 2. Load the alarm from the database.
 * 3. Start [AlarmRingtoneService] (sound + vibration).
 * 4. Post a heads-up notification and/or launch [AlarmFireActivity].
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "brightdate_alarm_channel"
        const val CHANNEL_NAME = "BrightDate Alarms"
        const val NOTIFICATION_BASE_ID = 1000
    }

    // goAsync() keeps the receiver alive while we do DB work on a coroutine.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_ALARM_FIRE) return

        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        if (alarmId < 0L) return

        val result = goAsync()

        val wl = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "brightdate:alarm:$alarmId")
        wl.acquire(10_000L)

        scope.launch {
            try {
                val repo = AlarmRepository(context)
                val alarm = repo.getAlarmById(alarmId) ?: return@launch

                if (!alarm.isEnabled) return@launch

                ensureNotificationChannel(context)

                // Start the ringtone / vibration foreground service
                val serviceIntent = Intent(context, AlarmRingtoneService::class.java).apply {
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                }
                context.startForegroundService(serviceIntent)

                // Full-screen intent (launches AlarmFireActivity over lock screen)
                if (alarm.useFullScreen) {
                    val fireIntent = Intent(context, AlarmFireActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
                        putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                    }
                    val firePi = PendingIntent.getActivity(
                        context,
                        alarmId.toInt(),
                        fireIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                                    PendingIntent.FLAG_IMMUTABLE else 0
                    )

                    if (alarm.useNotification) {
                        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                                as NotificationManager
                        nm.notify(
                            NOTIFICATION_BASE_ID + alarmId.toInt(),
                            buildNotification(context, alarm.label, alarm.brightDateValue, firePi)
                        )
                    }

                    fireIntent.also { context.startActivity(it) }
                }
            } finally {
                wl.release()
                result.finish()
            }
        }
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "BrightDate alarm notifications"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun buildNotification(
        context: Context,
        label: String,
        brightDate: Double,
        fullScreenPi: PendingIntent
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setContentTitle(label.ifBlank { context.getString(R.string.notification_default_title) })
            .setContentText(
                context.getString(
                    R.string.notification_body,
                    BrightDate.format(brightDate)
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPi, true)
            .setContentIntent(fullScreenPi)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
    }
}
