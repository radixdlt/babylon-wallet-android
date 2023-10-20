package com.babylon.wallet.android.data.repository.cache.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        AccountResourcesPortfolio::class,
        AccountDetailsEntity::class,
        AccountNFTsPortfolio::class,
        ResourceEntity::class,
        NFTEntity::class
    ],
    version = 1
)
@TypeConverters(StateDatabaseConverters::class)
abstract class StateDatabase : RoomDatabase() {

    abstract fun stateDao(): StateDao

    companion object {

        private const val NAME = "STATE_DATABASE"

        fun factory(applicationContext: Context): StateDatabase = Room
            .databaseBuilder(applicationContext, StateDatabase::class.java, NAME)
            .addTypeConverter(StateDatabaseConverters())
            .fallbackToDestructiveMigration() // Reconstruct database when schema changes.
            .build()
    }

}

