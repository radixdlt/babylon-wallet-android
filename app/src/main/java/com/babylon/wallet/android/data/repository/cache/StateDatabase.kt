package com.babylon.wallet.android.data.repository.cache

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        FungibleResourceEntity::class,
        NonFungibleResourceEntity::class,
        NFTEntity::class
    ],
    version = 1
)
@TypeConverters(
    BigDecimalColumnConverter::class,
    InstantColumnConverter::class,
    TagsColumnConverter::class,
    DappDefinitionsColumnConverter::class,
    BehavioursColumnConverter::class,
    StringMetadataColumnConverter::class
)
abstract class StateDatabase: RoomDatabase() {
}

