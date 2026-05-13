package org.brightchain.brightdate.alarm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.brightchain.brightdate.alarm.BrightDate
import org.brightchain.brightdate.alarm.data.AlarmEntity
import org.brightchain.brightdate.alarm.data.AlarmRepository
import org.brightchain.brightdate.alarm.service.AlarmScheduler

/**
 * Backs both [AddEditAlarmActivity] (alarm creation / editing) and drives form
 * validation.
 */
class AddEditAlarmViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AlarmRepository(app)

    /** The alarm being edited, or null when creating a new one. */
    val alarm = MutableLiveData<AlarmEntity?>(null)

    // Form fields
    val brightDateInput = MutableLiveData("")
    val label = MutableLiveData("")
    val useSound = MutableLiveData(true)
    val useVibrate = MutableLiveData(true)
    val useNotification = MutableLiveData(true)
    val useFullScreen = MutableLiveData(true)
    val soundUri = MutableLiveData("")
    val snoozeDuration = MutableLiveData(5)

    /** Validation error message, or null when valid. */
    val validationError = MutableLiveData<String?>(null)

    fun loadAlarm(id: Long) = viewModelScope.launch {
        val a = repo.getAlarmById(id) ?: return@launch
        alarm.postValue(a)
        brightDateInput.postValue(BrightDate.format(a.brightDateValue))
        label.postValue(a.label)
        useSound.postValue(a.useSound)
        useVibrate.postValue(a.useVibrate)
        useNotification.postValue(a.useNotification)
        useFullScreen.postValue(a.useFullScreen)
        soundUri.postValue(a.soundUri)
        snoozeDuration.postValue(a.snoozeDurationMinutes)
    }

    /**
     * Validate, persist, and schedule the alarm.
     * @return the saved alarm's ID on success, -1 on validation failure.
     */
    suspend fun save(): Long {
        val bdString = brightDateInput.value.orEmpty()
        val bdValue = BrightDate.parse(bdString)
        if (bdValue == null) {
            validationError.postValue("Invalid BrightDate value.")
            return -1L
        }
        val targetUnixMs = BrightDate.toUnixMs(bdValue)
        if (targetUnixMs <= System.currentTimeMillis()) {
            validationError.postValue("BrightDate is in the past.")
            return -1L
        }
        validationError.postValue(null)

        val existing = alarm.value
        val entity = AlarmEntity(
            id = existing?.id ?: 0L,
            brightDateValue = bdValue,
            targetUnixMs = targetUnixMs,
            label = label.value.orEmpty(),
            isEnabled = true,
            useSound = useSound.value ?: true,
            useVibrate = useVibrate.value ?: true,
            useNotification = useNotification.value ?: true,
            useFullScreen = useFullScreen.value ?: true,
            soundUri = soundUri.value.orEmpty(),
            snoozeDurationMinutes = snoozeDuration.value ?: 5,
        )
        val savedId = repo.save(entity)
        val saved = repo.getAlarmById(savedId)!!
        AlarmScheduler.schedule(getApplication(), saved)
        return savedId
    }

    fun delete() = viewModelScope.launch {
        alarm.value?.let {
            repo.delete(it)
            AlarmScheduler.cancel(getApplication(), it.id)
        }
    }
}
