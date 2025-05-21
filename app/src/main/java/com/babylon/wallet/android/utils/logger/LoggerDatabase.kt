package com.babylon.wallet.android.utils.logger

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.Date

@Database(
    entities = [
        LogEntity::class
    ],
    version = LoggerDatabase.VERSION_1
)
@TypeConverters(LoggerDatabase.Companion.LoggerConverters::class)
abstract class LoggerDatabase : RoomDatabase() {

    abstract fun logger(): LoggerDao

    companion object {
        private const val NAME = "LOGS"

        // Initial schema
        const val VERSION_1 = 1

        fun factory(applicationContext: Context): LoggerDatabase = Room
            .databaseBuilder(
                context = applicationContext,
                klass = LoggerDatabase::class.java,
                name = NAME
            )
            .fallbackToDestructiveMigration()
            .build()

        class LoggerConverters {
            @TypeConverter
            fun dateFromTimestamp(timestamp: Long): Date = Date(timestamp)

            @TypeConverter
            fun timestampFromDate(date: Date) = date.time
        }
    }
}