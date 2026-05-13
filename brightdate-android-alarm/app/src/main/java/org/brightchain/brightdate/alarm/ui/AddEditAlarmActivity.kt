package org.brightchain.brightdate.alarm.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.brightchain.brightdate.alarm.BrightDate
import org.brightchain.brightdate.alarm.R
import org.brightchain.brightdate.alarm.databinding.ActivityAddEditAlarmBinding
import org.brightchain.brightdate.alarm.viewmodel.AddEditAlarmViewModel
import java.util.Calendar

/**
 * Create or edit a BrightDate alarm.
 *
 * The user can either:
 *  - Type a BrightDate value directly, or
 *  - Pick a calendar date/time and have it auto-converted to a BrightDate.
 *
 * Both inputs stay in sync: editing one updates the other.
 */
class AddEditAlarmActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        private const val NO_ID = -1L
    }

    private lateinit var binding: ActivityAddEditAlarmBinding
    private val viewModel: AddEditAlarmViewModel by viewModels()

    private val ringtonePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri: Uri? = result.data
            ?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        viewModel.soundUri.value = uri?.toString() ?: ""
        updateRingtoneLabel(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, NO_ID)
        val isEdit = alarmId != NO_ID

        supportActionBar?.title = getString(
            if (isEdit) R.string.title_edit_alarm else R.string.title_add_alarm
        )

        Theme.applyTo(this)

        if (isEdit) {
            viewModel.loadAlarm(alarmId)
            binding.buttonDelete.visibility = View.VISIBLE
        }

        observeViewModel()
        bindInputs()
        setupButtonListeners()
    }

    override fun onResume() {
        super.onResume()
        Theme.applyTo(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // ── Observation ───────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.validationError.observe(this) { error ->
            binding.textInputBrightDate.error = error
        }
        viewModel.brightDateInput.observe(this) { bd ->
            if (binding.editBrightDate.text.toString() != bd) {
                binding.editBrightDate.setText(bd)
            }
        }
        viewModel.useSound.observe(this) { binding.checkSound.isChecked = it }
        viewModel.useVibrate.observe(this) { binding.checkVibrate.isChecked = it }
        viewModel.useNotification.observe(this) { binding.checkNotification.isChecked = it }
        viewModel.useFullScreen.observe(this) { binding.checkFullScreen.isChecked = it }
        viewModel.soundUri.observe(this) { uri ->
            updateRingtoneLabel(if (uri.isNotBlank()) Uri.parse(uri) else null)
        }
    }

    // ── Input bindings ────────────────────────────────────────────────────────

    private fun bindInputs() {
        binding.editBrightDate.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.brightDateInput.value = binding.editBrightDate.text.toString()
                // Sync calendar preview
                val bd = BrightDate.parse(binding.editBrightDate.text.toString())
                if (bd != null) {
                    binding.textCalendarPreview.text = BrightDate.formatUnixMs(BrightDate.toUnixMs(bd))
                }
            }
        }
        binding.editLabel.setOnFocusChangeListener { _, _ ->
            viewModel.label.value = binding.editLabel.text.toString()
        }

        binding.checkSound.setOnCheckedChangeListener { _, c -> viewModel.useSound.value = c }
        binding.checkVibrate.setOnCheckedChangeListener { _, c -> viewModel.useVibrate.value = c }
        binding.checkNotification.setOnCheckedChangeListener { _, c -> viewModel.useNotification.value = c }
        binding.checkFullScreen.setOnCheckedChangeListener { _, c -> viewModel.useFullScreen.value = c }
    }

    // ── Button listeners ──────────────────────────────────────────────────────

    private fun setupButtonListeners() {
        binding.buttonPickDate.setOnClickListener { showDateTimePicker() }

        binding.buttonPickRingtone.setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                val cur = viewModel.soundUri.value
                if (!cur.isNullOrBlank()) {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(cur))
                }
            }
            ringtonePicker.launch(intent)
        }

        binding.buttonSave.setOnClickListener {
            lifecycleScope.launch {
                val id = viewModel.save()
                if (id >= 0) finish()
            }
        }
        
        binding.buttonDelete.setOnClickListener {
            viewModel.delete()
            finish()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun showDateTimePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        cal.set(year, month, day, hour, minute, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        val bd = BrightDate.fromUnixMs(cal.timeInMillis)
                        val formatted = BrightDate.format(bd)
                        viewModel.brightDateInput.value = formatted
                        binding.editBrightDate.setText(formatted)
                        binding.textCalendarPreview.text =
                            BrightDate.formatUnixMs(cal.timeInMillis)
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

    private fun updateRingtoneLabel(uri: Uri?) {
        binding.textRingtoneLabel.text = if (uri != null) {
            RingtoneManager.getRingtone(this, uri)?.getTitle(this)
                ?: getString(R.string.ringtone_custom)
        } else {
            getString(R.string.ringtone_default)
        }
    }
}
