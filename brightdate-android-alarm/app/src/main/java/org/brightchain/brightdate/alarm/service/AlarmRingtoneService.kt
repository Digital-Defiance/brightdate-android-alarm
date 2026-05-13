package org.brightchain.brightdate.alarm.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.brightchain.brightdate.alarm.R
import org.brightchain.brightdate.alarm.data.AlarmRepository

/**
 * Foreground service that plays the alarm sound and/or vibrates the device.
 *
 * Started by [AlarmReceiver]; stopped by [AlarmFireActivity] (dismiss/snooze)
 * or automatically after [MAX_RING_DURATION_MS].
 */
class AlarmRingtoneService : Service() {

    companion object {
        const val ACTION_STOP = "org.brightchain.brightdate.alarm.ACTION_STOP_RINGTONE"
        const val MAX_RING_DURATION_MS = 5 * 60 * 1000L  // 5 minutes auto-stop
        private val VIBRATION_PATTERN = longArrayOf(0, 500, 500, 500, 500)
        private const val FOREGROUND_NOTIFICATION_ID = 999
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val alarmId = intent?.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L) ?: -1L

        // Must call startForeground immediately.
        startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification())

        scope.launch {
            if (alarmId >= 0) {
                val repo = AlarmRepository(this@AlarmRingtoneService)
                val alarm = repo.getAlarmById(alarmId)
                if (alarm != null) {
                    if (alarm.useSound) startSound(alarm.soundUri)
                    if (alarm.useVibrate) startVibration()
                } else {
                    // Alarm not found — just play default sound
                    startSound("")
                }
            } else {
                startSound("")
            }
        }

        // Auto-stop after max duration
        android.os.Handler(mainLooper).postDelayed({ stopSelf() }, MAX_RING_DURATION_MS)

        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        super.onDestroy()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun startSound(soundUriString: String) {
        val uri: Uri = if (soundUriString.isNotBlank()) {
            Uri.parse(soundUriString)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        }
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setLegacyStreamType(AudioManager.STREAM_ALARM)
                        .build()
                )
                setDataSource(this@AlarmRingtoneService, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // If custom URI fails, fall back to default
            if (soundUriString.isNotBlank()) startSound("")
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(VIBRATION_PATTERN, 0)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(VIBRATION_PATTERN, 0)
        }
    }

    private fun buildForegroundNotification(): Notification =
        NotificationCompat.Builder(this, AlarmReceiver.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setContentTitle(getString(R.string.notification_default_title))
            .setContentText(getString(R.string.notification_ringing))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .build()
}
