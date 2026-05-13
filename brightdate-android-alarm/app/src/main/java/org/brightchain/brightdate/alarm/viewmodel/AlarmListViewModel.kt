package org.brightchain.brightdate.alarm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.brightchain.brightdate.alarm.data.AlarmEntity
import org.brightchain.brightdate.alarm.data.AlarmRepository
import org.brightchain.brightdate.alarm.service.AlarmScheduler

class AlarmListViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AlarmRepository(app)

    val alarms: LiveData<List<AlarmEntity>> = repo.allAlarms

    fun setEnabled(alarm: AlarmEntity, enabled: Boolean) = viewModelScope.launch {
        repo.setEnabled(alarm.id, enabled)
        if (enabled) {
            AlarmScheduler.schedule(getApplication(), alarm.copy(isEnabled = true))
        } else {
            AlarmScheduler.cancel(getApplication(), alarm.id)
        }
    }

    fun delete(alarm: AlarmEntity) = viewModelScope.launch {
        AlarmScheduler.cancel(getApplication(), alarm.id)
        repo.delete(alarm)
    }
}
