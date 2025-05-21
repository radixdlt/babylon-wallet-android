package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DAppDirectoryDao {

    @Query("SELECT * FROM DirectoryDefinitionEntity WHERE synced >= :minValidity")
    fun getDirectory(minValidity: Long): List<DirectoryDefinitionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDirectory(directory: List<DirectoryDefinitionEntity>)

}