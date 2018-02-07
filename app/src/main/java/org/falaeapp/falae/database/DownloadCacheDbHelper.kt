package org.falaeapp.falae.database

import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.falaeapp.falae.model.DownloadCache


/**
 * Created by marce on 03/02/2018.
 */
class DownloadCacheDbHelper(context: Context) {

    private val databaseHelper: DataBaseHelper = DataBaseHelper.getInstance(context)
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

    fun insert(downloadCache: DownloadCache): Long {
        val json = gson.toJson(downloadCache.sources)
        val contentValues = ContentValues()
        contentValues.put(DownloadCacheEntry.COLUMN_NAME, downloadCache.name)
        contentValues.put(DownloadCacheEntry.COLUMN_SOURCES, json)
        val db = databaseHelper.writableDatabase
        Log.d(javaClass.name, "Inserting data...")
        return db.insert(DownloadCacheEntry.TABLE_NAME, null, contentValues)
    }


    fun update(downloadCache: DownloadCache) {
        val json = gson.toJson(downloadCache.sources)
        val contentValues = ContentValues()
        contentValues.put(DownloadCacheEntry.COLUMN_NAME, downloadCache.name)
        contentValues.put(DownloadCacheEntry.COLUMN_SOURCES, json)
        val db = databaseHelper.writableDatabase
        Log.d(javaClass.name, "Updating entry...")
        db.update(DownloadCacheEntry.TABLE_NAME, contentValues,
                DownloadCacheDbHelper.DownloadCacheEntry.COLUMN_NAME + "= ? ",
                arrayOf(downloadCache.name))
    }

    fun cacheExist(downloadCache: DownloadCache): Boolean {
        val db = databaseHelper.readableDatabase
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
        val db = databaseHelper.readableDatabase
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
            val typeOfHashMap = object : TypeToken<MutableMap<String, String?>>() {}.type
            val gson = Gson()
            if (it.moveToFirst()) {
                val columnNameResult = it.getString(it.getColumnIndex(DownloadCacheEntry.COLUMN_NAME))
                val columnSourcesResult = it.getString(it.getColumnIndex(DownloadCacheEntry.COLUMN_SOURCES))
                val map = gson.fromJson<MutableMap<String, String>>(columnSourcesResult, typeOfHashMap)
                return DownloadCache(columnNameResult, map)
            }
        }
        return null
    }

    fun remove(name: String) {
        val db = databaseHelper.writableDatabase
        val whereClause = "${DownloadCacheEntry.COLUMN_NAME}=?"
        val whereArgs = arrayOf(name)
        db.delete(DownloadCacheEntry.TABLE_NAME, whereClause, whereArgs)
    }

    fun close() {
        databaseHelper.close()
    }
}