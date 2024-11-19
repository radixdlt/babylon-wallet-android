package com.babylon.wallet.android.data.repository.cache.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.babylon.wallet.android.data.repository.cache.database.locker.AccountLockerDao
import com.babylon.wallet.android.data.repository.cache.database.locker.AccountLockerTouchedAtEntity
import com.babylon.wallet.android.data.repository.cache.database.locker.AccountLockerVaultItemEntity

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
        PoolDAppJoin::class,
        TokenPriceEntity::class,
        AccountLockerTouchedAtEntity::class,
        AccountLockerVaultItemEntity::class
    ],
    version = StateDatabase.VERSION_12
)
@TypeConverters(StateDatabaseConverters::class)
abstract class StateDatabase : RoomDatabase() {

    abstract fun stateDao(): StateDao

    abstract fun tokenPriceDao(): TokenPriceDao

    abstract fun accountLockerDao(): AccountLockerDao

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

        @Deprecated("Add first tx timestamp to account details")
        const val VERSION_6 = 6

        @Deprecated("Add TokenPriceEntity to schema")
        const val VERSION_7 = 7

        @Deprecated("Replace BigDecimal with Decimal192")
        const val VERSION_8 = 8

        @Deprecated("Added next cursor to metadata column and locked flag")
        const val VERSION_9 = 9

        @Deprecated("Add account locker logic")
        const val VERSION_10 = 10

        @Deprecated("Updated metadata schema: Added Origin MetadataType")
        const val VERSION_11 = 11

        // Added account metadata
        const val VERSION_12 = 12

        private const val NAME = "STATE_DATABASE"

        fun factory(applicationContext: Context): StateDatabase = Room
            .databaseBuilder(applicationContext, StateDatabase::class.java, NAME)
            .addTypeConverter(StateDatabaseConverters())
            .fallbackToDestructiveMigration() // Reconstruct database when schema changes.
            .build()
    }
}
