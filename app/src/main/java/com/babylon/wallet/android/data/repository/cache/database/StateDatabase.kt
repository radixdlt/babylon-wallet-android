package com.babylon.wallet.android.data.repository.cache.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        AccountResourceJoin::class,
        AccountEntity::class,
        AccountNFTJoin::class,
        ResourceEntity::class,
        NFTEntity::class,
        PoolEntity::class,
        ValidatorEntity::class,
        PoolResourceJoin::class
    ],
    version = StateDatabase.VERSION_2
)
@TypeConverters(StateDatabaseConverters::class)
abstract class StateDatabase : RoomDatabase() {

    abstract fun stateDao(): StateDao

    companion object {
        // Initial schema version
        const val VERSION_1 = 1
        // Update to metadata schema
        const val VERSION_2 = 2

        private const val NAME = "STATE_DATABASE"

        fun factory(applicationContext: Context): StateDatabase = Room
            .databaseBuilder(applicationContext, StateDatabase::class.java, NAME)
            .addTypeConverter(StateDatabaseConverters())
            .fallbackToDestructiveMigration() // Reconstruct database when schema changes.
            .build()
    }
}
