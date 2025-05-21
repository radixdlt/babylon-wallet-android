package com.babylon.wallet.android.utils.logger

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LoggerDao {

    @Insert
    fun insertLog(entity: LogEntity)

    @Query("DELETE FROM logs WHERE timestamp < (unixepoch('now','-1 days') * 1000)")
    fun clearEarlierLogs()

    @Query("SELECT * FROM logs WHERE timestamp >= (unixepoch('now','-1 days') * 1000) ORDER BY timestamp ASC")
    fun getLastDaysLogs(): List<LogEntity>
}