package org.falaeapp.falae.room

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.content.Context
import org.falaeapp.falae.model.DownloadCache
import org.falaeapp.falae.model.MutableMapConverter
import org.falaeapp.falae.model.SpreadSheetConverter
import org.falaeapp.falae.model.User

@Database(entities = [User::class, DownloadCache::class], version = 1)
@TypeConverters(SpreadSheetConverter::class, MutableMapConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userModelDao(): UserModelDao

    abstract fun downloadCacheDao(): DownloadCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?:
                    Room.databaseBuilder(context.applicationContext,
                            AppDatabase::class.java, "falae.db").build().also { INSTANCE = it }
                }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}