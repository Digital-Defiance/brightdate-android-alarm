package org.brightchain.brightdate.alarm.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Single source of truth for alarm persistence.
 *
 * All write operations are performed on the IO dispatcher so callers on the
 * main thread can simply call `suspend` functions without blocking.
 */
class AlarmRepository(context: Context) {

    private val dao: AlarmDao = AlarmDatabase.getInstance(context).alarmDao()

    /** LiveData list of all alarms (ordered by target time, ascending). */
    val allAlarms = dao.getAllAlarms()

    suspend fun getEnabledAlarms(): List<AlarmEntity> =
        withContext(Dispatchers.IO) { dao.getEnabledAlarms() }

    suspend fun getAlarmById(id: Long): AlarmEntity? =
        withContext(Dispatchers.IO) { dao.getAlarmById(id) }

    /** Insert a new alarm or replace an existing one. Returns the row id. */
    suspend fun save(alarm: AlarmEntity): Long =
        withContext(Dispatchers.IO) { dao.insertOrUpdate(alarm) }

    suspend fun setEnabled(id: Long, enabled: Boolean) =
        withContext(Dispatchers.IO) { dao.setEnabled(id, enabled) }

    suspend fun delete(alarm: AlarmEntity) =
        withContext(Dispatchers.IO) { dao.delete(alarm) }

    suspend fun deleteById(id: Long) =
        withContext(Dispatchers.IO) { dao.deleteById(id) }
}
