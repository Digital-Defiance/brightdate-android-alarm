package org.brightchain.brightdate.alarm.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import org.brightchain.brightdate.alarm.BrightDate
import org.brightchain.brightdate.alarm.databinding.ActivityConverterBinding
import java.util.Calendar

/**
 * BrightDate ↔ Calendar date/time converter utility.
 *
 * Two panels, bidirectional:
 *  - Left:  BrightDate text input  → shows equivalent ISO date and Unix ms.
 *  - Right: Date/time pickers      → shows BrightDate equivalent.
 */
class ConverterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConverterBinding

    // Prevent infinite update loops when one field triggers the other
    private var updating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConverterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Converter"
        Theme.applyTo(this)

        // Seed both panels with the current moment
        val now = System.currentTimeMillis()
        val nowBd = BrightDate.fromUnixMs(now)
        binding.editBrightDateInput.setText(BrightDate.format(nowBd))
        updateFromBrightDate(BrightDate.format(nowBd))

        // BrightDate → date direction
        binding.editBrightDateInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (updating) return
                updateFromBrightDate(s.toString())
            }
        })

        // Date → BrightDate direction
        binding.buttonPickDateConverter.setOnClickListener { showDateTimePicker() }
    }

    override fun onResume() {
        super.onResume()
        Theme.applyTo(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // ── Conversion helpers ────────────────────────────────────────────────────

    private fun updateFromBrightDate(input: String) {
        updating = true
        val bd = BrightDate.parse(input)
        if (bd != null) {
            val unixMs = BrightDate.toUnixMs(bd)
            binding.textResultIsoDate.text = BrightDate.formatUnixMs(unixMs)
            binding.textResultUnixMs.text = unixMs.toString()
            binding.textBrightDateError.visibility = android.view.View.GONE
        } else {
            binding.textResultIsoDate.text = "—"
            binding.textResultUnixMs.text = "—"
            if (input.isNotBlank()) {
                binding.textBrightDateError.visibility = android.view.View.VISIBLE
            }
        }
        updating = false
    }

    private fun showDateTimePicker() {
        val cal = Calendar.getInstance()
        android.app.DatePickerDialog(
            this,
            { _, year, month, day ->
                android.app.TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        cal.set(year, month, day, hour, minute, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        val bd = BrightDate.fromUnixMs(cal.timeInMillis)
                        val formatted = BrightDate.format(bd)
                        updating = true
                        binding.editBrightDateInput.setText(formatted)
                        binding.textResultIsoDate.text = BrightDate.formatUnixMs(cal.timeInMillis)
                        binding.textResultUnixMs.text = cal.timeInMillis.toString()
                        binding.textBrightDateError.visibility = android.view.View.GONE
                        updating = false
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
