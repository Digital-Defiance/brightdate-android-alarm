package org.brightchain.brightdate.alarm

import android.app.Application
import android.graphics.Color
import androidx.preference.PreferenceManager

class BrightDateAlarm : Application() {
    override fun onCreate() {
        super.onCreate()
        // No-op for now, but placeholder if needed
    }

    companion object {
        fun getAccentColor(context: android.content.Context): Int {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val colorStr = prefs.getString(context.getString(R.string.pref_key_accent_color), "#FFD4AF37")
            return try {
                Color.parseColor(colorStr)
            } catch (e: Exception) {
                Color.parseColor("#FFD4AF37")
            }
        }
    }
}
