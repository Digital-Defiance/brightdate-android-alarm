package org.brightchain.brightdate.alarm.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.brightchain.brightdate.alarm.data.AlarmEntity

/**
 * Wraps [AlarmManager] to schedule and cancel BrightDate alarms.
 *
 * Each alarm is keyed by its database [AlarmEntity.id] cast to Int, which is
 * used as the PendingIntent request code.  IDs are guaranteed unique by Room's
 * AUTOINCREMENT primary key.
 */
object AlarmScheduler {

    const val ACTION_ALARM_FIRE = "org.brightchain.brightdate.alarm.ACTION_ALARM_FIRE"
    const val EXTRA_ALARM_ID = "extra_alarm_id"

    /** Schedule (or reschedule) an alarm. Does nothing if [alarm.isEnabled] is false. */
    fun schedule(context: Context, alarm: AlarmEntity) {
        if (!alarm.isEnabled) return

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildPendingIntent(context, alarm.id)

        // Use setAlarmClock for user-visible alarms: shows clock icon in status bar and
        // wakes the device even in Doze mode.
        val info = AlarmManager.AlarmClockInfo(alarm.targetUnixMs, pi)
        am.setAlarmClock(info, pi)
    }

    /** Cancel a previously scheduled alarm. */
    fun cancel(context: Context, alarmId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(buildPendingIntent(context, alarmId))
    }

    /** Cancel all pending alarms in [alarms] and reschedule only the enabled ones. */
    fun rescheduleAll(context: Context, alarms: List<AlarmEntity>) {
        alarms.forEach { alarm ->
            cancel(context, alarm.id)
            if (alarm.isEnabled) schedule(context, alarm)
        }
    }

    private fun buildPendingIntent(context: Context, alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_FIRE
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getBroadcast(context, alarmId.toInt(), intent, flags)
    }
}
