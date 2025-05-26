package com.babylon.wallet.android.utils.logger

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import java.util.Date

@Dao
interface LoggerDao {

    @Insert
    fun insertLog(entity: LogEntity)

    @Query("DELETE FROM logs WHERE timestamp < :timestampThresholdMilliseconds")
    fun clearEarlierLogs(
        timestampThresholdMilliseconds: Long = oneDayBefore()
    )

    @Query("SELECT * FROM logs WHERE timestamp >= :timestampThresholdMilliseconds ORDER BY timestamp ASC")
    fun getLogs(
        timestampThresholdMilliseconds: Long = oneDayBefore()
    ): List<LogEntity>

    companion object {
        private fun oneDayBefore(): Long = Date().time - (86400 * 1000)
    }
}
