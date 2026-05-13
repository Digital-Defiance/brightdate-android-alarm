package org.brightchain.brightdate.alarm.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import org.brightchain.brightdate.alarm.R
import org.brightchain.brightdate.alarm.data.AlarmEntity
import org.brightchain.brightdate.alarm.databinding.ActivityMainBinding
import org.brightchain.brightdate.alarm.viewmodel.AlarmListViewModel

/**
 * Entry-point: shows the list of BrightDate alarms.
 *
 * - FAB → [AddEditAlarmActivity] (create new alarm)
 * - Tap row → [AddEditAlarmActivity] (edit existing alarm)
 * - Toggle switch → enable / disable alarm
 * - Swipe left → delete alarm
 * - Menu → converter, settings
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: AlarmListViewModel by viewModels()
    private lateinit var adapter: AlarmAdapter

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result handled silently; notifications are opt-in */ }

    /**
     * Refreshes the countdown text ("in 4 m", "in 1 d 4 h", …) on every
     * visible alarm row. Sub-minute resolution is overkill since the smallest
     * unit shown is minutes, but ticking every 15 s keeps the boundary nice
     * (a row that says "in 1 m" flips to "in 0 m" within ~15 s of the minute
     * change, not up to a minute later).
     */
    private val tickHandler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            refreshVisibleCountdowns()
            tickHandler.postDelayed(this, TICK_INTERVAL_MS)
        }
    }

    private companion object {
        const val TICK_INTERVAL_MS = 1_000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        Theme.applyTo(this)

        requestNotificationPermissionIfNeeded()
        setupRecyclerView()
        observeAlarms()

        binding.fab.setOnClickListener {
            startActivity(Intent(this, AddEditAlarmActivity::class.java))
        }
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_converter -> {
            startActivity(Intent(this, ConverterActivity::class.java))
            true
        }
        R.id.action_settings -> {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = AlarmAdapter(
            onAlarmClicked = { alarm ->
                startActivity(
                    Intent(this, AddEditAlarmActivity::class.java).apply {
                        putExtra(AddEditAlarmActivity.EXTRA_ALARM_ID, alarm.id)
                    }
                )
            },
            onToggleEnabled = { alarm, enabled ->
                viewModel.setEnabled(alarm, enabled)
            },
            onDeleteClicked = { alarm ->
                viewModel.delete(alarm)
                Snackbar.make(binding.root, R.string.alarm_deleted, Snackbar.LENGTH_SHORT).show()
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Swipe-to-delete
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val alarm = adapter.currentList[viewHolder.bindingAdapterPosition]
                viewModel.delete(alarm)
                Snackbar.make(binding.root, R.string.alarm_deleted, Snackbar.LENGTH_SHORT).show()
            }
        }).attachToRecyclerView(binding.recyclerView)
    }

    private fun observeAlarms() {
        viewModel.alarms.observe(this) { alarms ->
            adapter.submitList(alarms)
            binding.emptyView.visibility =
                if (alarms.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    /**
     * Walks visible RecyclerView children and refreshes each row's countdown
     * text in place. Avoids `notifyItemChanged` so we don't trigger rebinds
     * (which would reset switch listeners and animations).
     */
    private fun refreshVisibleCountdowns() {
        val rv = binding.recyclerView
        for (i in 0 until rv.childCount) {
            val child = rv.getChildAt(i)
            val holder = rv.getChildViewHolder(child) as? AlarmAdapter.ViewHolder ?: continue
            holder.refreshCountdown()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
