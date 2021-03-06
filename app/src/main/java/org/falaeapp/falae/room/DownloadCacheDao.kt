package org.falaeapp.falae.room

import androidx.room.*
import org.falaeapp.falae.model.DownloadCache

@Dao
interface DownloadCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(downloadCacheDao: DownloadCache): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(downloadCacheDao: DownloadCache)

    @Query("select 1 from DownloadCache where name = :name")
    fun cacheExists(name: String): Boolean

    @Query("select * from DownloadCache where name = :name")
    fun findByName(name: String): DownloadCache?

    @Delete
    fun removeUser(downloadCacheDao: DownloadCache)

    @Query("delete from DownloadCache where name = :email")
    fun remove(email: String)
}