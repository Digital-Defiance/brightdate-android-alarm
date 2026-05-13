package org.brightchain.brightdate.alarm.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.brightchain.brightdate.alarm.BrightDate
import org.brightchain.brightdate.alarm.R
import org.brightchain.brightdate.alarm.data.AlarmRepository
import org.brightchain.brightdate.alarm.databinding.ActivityAlarmFireBinding
import org.brightchain.brightdate.alarm.service.AlarmRingtoneService
import org.brightchain.brightdate.alarm.service.AlarmScheduler

/**
 * Full-screen activity shown over the lock screen when an alarm fires.
 *
 * Displayed by [AlarmReceiver] via a full-screen PendingIntent.
 * The user can dismiss the alarm or snooze it.
 */
class AlarmFireActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmFireBinding
    private var alarmId = -1L
    private val tickHandler = Handler(Looper.getMainLooper())

    private val tickRunnable = object : Runnable {
        override fun run() {
            binding.textBrightDateNow.text = BrightDate.format(BrightDate.now())
            tickHandler.postDelayed(this, 1_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmFireBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Theme.applyTo(this)

        // Keep screen on while alarm is showing
        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)

        lifecycleScope.launch {
            if (alarmId >= 0) {
                val repo = AlarmRepository(this@AlarmFireActivity)
                val alarm = repo.getAlarmById(alarmId)
                if (alarm != null) {
                    binding.textAlarmBrightDate.text = BrightDate.format(alarm.brightDateValue)
                    binding.textAlarmLabel.text = alarm.label.ifBlank {
                        getString(R.string.alarm_no_label)
                    }
                    binding.textAlarmTargetDate.text = BrightDate.formatUnixMs(alarm.targetUnixMs)
                }
            }
        }

        binding.buttonDismiss.setOnClickListener { dismiss() }
        binding.buttonSnooze.setOnClickListener { snooze() }
    }

    override fun onResume() {
        super.onResume()
        Theme.applyTo(this)
        tickHandler.post(tickRunnable)
    }

    override fun onPause() {
        super.onPause()
        tickHandler.removeCallbacks(tickRunnable)
    }

    private fun dismiss() {
        stopRingtone()
        finish()
    }

    private fun snooze() {
        lifecycleScope.launch {
            if (alarmId >= 0) {
                val repo = AlarmRepository(this@AlarmFireActivity)
                val alarm = repo.getAlarmById(alarmId)
                if (alarm != null) {
                    val snoozeMs = alarm.targetUnixMs +
                            (alarm.snoozeDurationMinutes * 60 * 1000L)
                    val snoozedBd = BrightDate.fromUnixMs(snoozeMs)
                    val snoozed = alarm.copy(
                        id = 0L,  // new row
                        brightDateValue = snoozedBd,
                        targetUnixMs = snoozeMs,
                        label = "${alarm.label} (snooze)".trim(),
                        isEnabled = true
                    )
                    val savedId = repo.save(snoozed)
                    org.brightchain.brightdate.alarm.service.AlarmScheduler.schedule(
                        this@AlarmFireActivity, snoozed.copy(id = savedId)
                    )
                }
            }
            stopRingtone()
            finish()
        }
    }

    private fun stopRingtone() {
        val stopIntent = Intent(this, AlarmRingtoneService::class.java).apply {
            action = AlarmRingtoneService.ACTION_STOP
        }
        startService(stopIntent)
    }
}
