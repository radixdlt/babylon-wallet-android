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
        PoolResourceJoin::class,
        DAppEntity::class,
        PoolDAppJoin::class
    ],
    version = StateDatabase.VERSION_6
)
@TypeConverters(StateDatabaseConverters::class)
abstract class StateDatabase : RoomDatabase() {

    abstract fun stateDao(): StateDao

    companion object {
        @Deprecated("Initial schema version")
        const val VERSION_1 = 1

        @Deprecated("Updated metadata schema")
        const val VERSION_2 = 2

        @Deprecated("Add DAppEntity to schema")
        const val VERSION_3 = 3

        @Deprecated("Add PoolEntity.metadata to schema")
        const val VERSION_4 = 4

        @Deprecated("Add PoolDAppJoin to schema")
        const val VERSION_5 = 5

        // add first tx timestamp to account details
        const val VERSION_6 = 6

        private const val NAME = "STATE_DATABASE"

        fun factory(applicationContext: Context): StateDatabase = Room
            .databaseBuilder(applicationContext, StateDatabase::class.java, NAME)
            .addTypeConverter(StateDatabaseConverters())
            .fallbackToDestructiveMigration() // Reconstruct database when schema changes.
            .build()
    }
}
