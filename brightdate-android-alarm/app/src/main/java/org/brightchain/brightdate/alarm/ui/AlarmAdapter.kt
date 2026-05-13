package org.brightchain.brightdate.alarm.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.brightchain.brightdate.alarm.BrightDate
import org.brightchain.brightdate.alarm.R
import org.brightchain.brightdate.alarm.data.AlarmEntity
import org.brightchain.brightdate.alarm.databinding.ItemAlarmBinding

class AlarmAdapter(
    private val onAlarmClicked: (AlarmEntity) -> Unit,
    private val onToggleEnabled: (AlarmEntity, Boolean) -> Unit,
    private val onDeleteClicked: (AlarmEntity) -> Unit,
) : ListAdapter<AlarmEntity, AlarmAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AlarmEntity>() {
            override fun areItemsTheSame(a: AlarmEntity, b: AlarmEntity) = a.id == b.id
            override fun areContentsTheSame(a: AlarmEntity, b: AlarmEntity) = a == b
        }
    }

    inner class ViewHolder(private val binding: ItemAlarmBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /** Set in [bind] so [refreshCountdown] can recompute without going through the adapter. */
        var alarm: AlarmEntity? = null
            private set

        fun bind(alarm: AlarmEntity) {
            this.alarm = alarm
            binding.textBrightDate.text = BrightDate.format(alarm.brightDateValue)
            binding.textLabel.text = alarm.label.ifBlank {
                binding.root.context.getString(R.string.alarm_no_label)
            }
            binding.textTargetDate.text = BrightDate.formatUnixMs(alarm.targetUnixMs)
            binding.textCountdown.text = BrightDate.relativeDescription(alarm.targetUnixMs)

            // Suppress listener while updating to avoid feedback loop
            binding.switchEnabled.setOnCheckedChangeListener(null)
            binding.switchEnabled.isChecked = alarm.isEnabled
            binding.switchEnabled.setOnCheckedChangeListener { _, checked ->
                onToggleEnabled(alarm, checked)
            }

            binding.root.setOnClickListener { onAlarmClicked(alarm) }
            binding.buttonDelete.setOnClickListener { onDeleteClicked(alarm) }

            // Recolor accent-colored sub-views (BrightDate value, countdown) to
            // match the user's picked accent — the static theme paints them as
            // bd_yellow at inflate time.
            Theme.applyAccentTo(binding.root)
        }

        /** Refresh just the countdown text without rebinding the whole row. */
        fun refreshCountdown() {
            val a = alarm ?: return
            binding.textCountdown.text = BrightDate.relativeDescription(a.targetUnixMs)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemAlarmBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))
}
