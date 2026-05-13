package org.brightchain.brightdate.alarm.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.brightchain.brightdate.alarm.R
import org.brightchain.brightdate.alarm.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        Theme.applyTo(this)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settingsContainer, SettingsFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private val defaultAccentHex: String = Theme.DEFAULT_ACCENT_HEX

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            val colorPreference = findPreference<Preference>(getString(R.string.pref_key_accent_color))
            colorPreference?.setOnPreferenceClickListener { pref ->
                val current = preferenceManager.sharedPreferences
                    ?.getString(pref.key, defaultAccentHex) ?: defaultAccentHex
                showRgbColorPicker(current) { newHex ->
                    preferenceManager.sharedPreferences?.edit {
                        putString(pref.key, newHex)
                    }
                    pref.summary = newHex
                    activity?.recreate()
                }
                true
            }
        }

        override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            // Preference rows are inflated by an internal RecyclerView and may
            // not yet exist when SettingsActivity runs Theme.applyTo. Recolor
            // each row as it's attached.
            listView.addOnChildAttachStateChangeListener(
                object : androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener {
                    override fun onChildViewAttachedToWindow(child: android.view.View) {
                        Theme.applyAccentTo(child)
                    }
                    override fun onChildViewDetachedFromWindow(child: android.view.View) = Unit
                }
            )
        }

        override fun onResume() {
            super.onResume()
            val colorPreference = findPreference<Preference>(getString(R.string.pref_key_accent_color))
            val color = preferenceManager.sharedPreferences
                ?.getString(colorPreference?.key, defaultAccentHex)
            colorPreference?.summary = color
        }

        private fun showRgbColorPicker(initialHex: String, onSave: (String) -> Unit) {
            val ctx = requireContext()
            val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_color_picker_rgb, null)

            val preview = view.findViewById<android.view.View>(R.id.colorPreview)
            val hexLabel = view.findViewById<TextView>(R.id.hexValue)
            val seekRed = view.findViewById<SeekBar>(R.id.seekRed)
            val seekGreen = view.findViewById<SeekBar>(R.id.seekGreen)
            val seekBlue = view.findViewById<SeekBar>(R.id.seekBlue)
            val valueRed = view.findViewById<TextView>(R.id.valueRed)
            val valueGreen = view.findViewById<TextView>(R.id.valueGreen)
            val valueBlue = view.findViewById<TextView>(R.id.valueBlue)

            val initialColor = parseColorOrDefault(initialHex)
            seekRed.progress = Color.red(initialColor)
            seekGreen.progress = Color.green(initialColor)
            seekBlue.progress = Color.blue(initialColor)

            fun refresh() {
                val r = seekRed.progress
                val g = seekGreen.progress
                val b = seekBlue.progress
                val color = Color.rgb(r, g, b)
                preview.setBackgroundColor(color)
                hexLabel.text = String.format("#FF%02X%02X%02X", r, g, b)
                valueRed.text = r.toString()
                valueGreen.text = g.toString()
                valueBlue.text = b.toString()
            }

            val listener = object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) = refresh()
                override fun onStartTrackingTouch(s: SeekBar?) = Unit
                override fun onStopTrackingTouch(s: SeekBar?) = Unit
            }
            seekRed.setOnSeekBarChangeListener(listener)
            seekGreen.setOnSeekBarChangeListener(listener)
            seekBlue.setOnSeekBarChangeListener(listener)
            refresh()

            AlertDialog.Builder(ctx)
                .setTitle(R.string.color_picker_title)
                .setView(view)
                .setPositiveButton(R.string.btn_save) { _, _ ->
                    onSave(hexLabel.text.toString())
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .setNeutralButton(R.string.btn_reset) { _, _ ->
                    onSave(defaultAccentHex)
                }
                .show()
                .also { Theme.applyTo(it) }
        }

        private fun parseColorOrDefault(hex: String): Int = try {
            hex.toColorInt()
        } catch (_: IllegalArgumentException) {
            defaultAccentHex.toColorInt()
        }
    }
}
