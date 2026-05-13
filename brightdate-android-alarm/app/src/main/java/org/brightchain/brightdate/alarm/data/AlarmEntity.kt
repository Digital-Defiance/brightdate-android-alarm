package org.brightchain.brightdate.alarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted alarm record.
 *
 * @param id              Auto-generated primary key; also used as the AlarmManager request code.
 * @param brightDateValue The BrightDate (decimal days since J2000.0) when the alarm should fire.
 * @param targetUnixMs    Pre-computed Unix ms equivalent; used by AlarmManager directly.
 * @param label           User-supplied name for the alarm.
 * @param isEnabled       Whether the alarm is scheduled.
 * @param useSound        Play the selected ringtone when firing.
 * @param useVibrate      Vibrate when firing.
 * @param useNotification Post a heads-up notification when firing.
 * @param useFullScreen   Launch the full-screen AlarmFireActivity when firing.
 * @param soundUri        Content URI (string) for the chosen ringtone; empty = system default.
 * @param snoozeDurationMinutes How long (minutes) a snooze re-schedules the alarm for.
 */
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val brightDateValue: Double,
    val targetUnixMs: Long,
    val label: String,

    val isEnabled: Boolean = true,

    val useSound: Boolean = true,
    val useVibrate: Boolean = true,
    val useNotification: Boolean = true,
    val useFullScreen: Boolean = true,

    val soundUri: String = "",
    val snoozeDurationMinutes: Int = 5,
)
