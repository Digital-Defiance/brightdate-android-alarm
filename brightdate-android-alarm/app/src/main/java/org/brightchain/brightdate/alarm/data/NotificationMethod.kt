package org.brightchain.brightdate.alarm.data

/**
 * Notification/alert methods that can be toggled per alarm.
 */
enum class NotificationMethod {
    /** Play the chosen ringtone through the alarm audio stream. */
    SOUND,
    /** Vibrate the device in the alarm pattern. */
    VIBRATE,
    /** Post a standard heads-up notification. */
    NOTIFICATION,
    /** Launch [AlarmFireActivity] as a full-screen intent over the lock screen. */
    FULL_SCREEN,
}
