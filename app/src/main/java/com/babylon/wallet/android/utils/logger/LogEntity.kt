package com.babylon.wallet.android.utils.logger

import android.annotation.SuppressLint
import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date

@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val timestamp: Date,
    val priority: Int,
    val tag: String?,
    val message: String,
    val throwableTrace: String?
) {

    private fun priority(): String = when (priority) {
        Log.INFO -> "I"
        Log.WARN -> "W"
        Log.DEBUG -> "D"
        Log.ERROR -> "E"
        Log.ASSERT -> "A"
        Log.VERBOSE -> "V"
        else -> ""
    }

    fun asLogEntry(): String {
        val firstRow = "${dateFormat.format(timestamp)} ${priority()} $tag - $message"
        val secondRow = throwableTrace

        return if (secondRow != null) {
            "$firstRow\n\t$secondRow"
        } else {
            firstRow
        }
    }

    companion object {
        @SuppressLint("SimpleDateFormat")
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    }
}
