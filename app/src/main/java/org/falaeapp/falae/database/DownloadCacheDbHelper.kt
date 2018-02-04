package org.falaeapp.falae.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.falaeapp.falae.model.DownloadCache
import java.util.concurrent.ConcurrentHashMap


/**
 * Created by marce on 03/02/2018.
 */
class DownloadCacheDbHelper(context: Context?) : SQLiteOpenHelper(context, DatabaseConstant.DATABASE_NAME, null, DatabaseConstant.DATABASE_VERSION) {

    private var gson = GsonBuilder().create()

    class DownloadCacheEntry {
        companion object {

            const val TABLE_NAME = "download_cache"
            const val _ID = "_id"
            const val COLUMN_NAME = "name"
            const val COLUMN_SOURCES = "sources"
            const val _COUNT = "_count"
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(DownloadCacheDbHelper.SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL(DownloadCacheDbHelper.SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    fun insert(downloadCache: DownloadCache): Long {
        val json = gson.toJson(downloadCache.sources)
        val contentValues = ContentValues()
        contentValues.put(DownloadCacheEntry.COLUMN_NAME, downloadCache.name)
        contentValues.put(DownloadCacheEntry.COLUMN_SOURCES, json)
        val db = writableDatabase
        Log.d(javaClass.name, "Inserting data...")
        return db.insert(DownloadCacheEntry.TABLE_NAME, null, contentValues)
    }


    fun update(downloadCache: DownloadCache) {
        val json = gson.toJson(downloadCache.sources)
        val contentValues = ContentValues()
        contentValues.put(DownloadCacheEntry.COLUMN_NAME, downloadCache.name)
        contentValues.put(DownloadCacheEntry.COLUMN_SOURCES, json)
        val db = writableDatabase
        Log.d(javaClass.name, "Updating entry...")
        db.update(DownloadCacheEntry.TABLE_NAME, contentValues,
                DownloadCacheDbHelper.DownloadCacheEntry.COLUMN_NAME + "= ? ",
                arrayOf(downloadCache.name))
    }

    fun cacheExist(downloadCache: DownloadCache): Boolean {
        val db = readableDatabase
        val projection = arrayOf(DownloadCacheEntry.COLUMN_NAME)
        val selection = DownloadCacheEntry.COLUMN_NAME + " = ?"
        val selectionArgs = arrayOf(downloadCache.name)
        db.query(DownloadCacheEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null).use {
            return it.moveToFirst()
        }
    }

    fun findByName(name: String): DownloadCache? {
        val db = readableDatabase
        val projection = arrayOf(DownloadCacheEntry._ID,
                DownloadCacheEntry.COLUMN_NAME,
                DownloadCacheEntry.COLUMN_SOURCES)
        val selection = DownloadCacheEntry.COLUMN_NAME + " = ?"
        val selectionArgs = arrayOf(name)
        db.query(DownloadCacheEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null).use {
            val typeOfHashMap = object : TypeToken<Map<String, String?>>() {}.type
            val gson = Gson()
            if (it.moveToFirst()) {
                val columnNameResult = it.getString(it.getColumnIndex(DownloadCacheEntry.COLUMN_NAME))
                val columnSourcesResult = it.getString(it.getColumnIndex(DownloadCacheEntry.COLUMN_SOURCES))
                val map = gson.fromJson<Map<String, String>>(columnSourcesResult, typeOfHashMap)
                return DownloadCache(columnNameResult, ConcurrentHashMap(map))
            }
        }
        return null
    }

    companion object {
        private const val SQL_CREATE_ENTRIES =
                "CREATE TABLE " + DownloadCacheDbHelper.DownloadCacheEntry.TABLE_NAME + " (" +
                        DownloadCacheDbHelper.DownloadCacheEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        DownloadCacheDbHelper.DownloadCacheEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                        DownloadCacheDbHelper.DownloadCacheEntry.COLUMN_SOURCES + " TEXT NOT NULL)"

        private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + DownloadCacheDbHelper.DownloadCacheEntry.TABLE_NAME
    }
}