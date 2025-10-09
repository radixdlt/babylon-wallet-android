package com.babylon.wallet.android.data.repository.cache.database.accesscontroller

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AccessControllerDao {

    @Query("SELECT * FROM AccessControllerEntity WHERE synced >= :minValidity")
    fun getAccessControllers(minValidity: Long): List<AccessControllerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAccessControllers(accessControllers: List<AccessControllerEntity>)

    @Query("DELETE FROM AccessControllerEntity")
    fun deleteAll()
}
