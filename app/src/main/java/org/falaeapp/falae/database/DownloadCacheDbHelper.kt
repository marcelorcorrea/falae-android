package org.falaeapp.falae.database

import android.content.ContentValues
import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import mu.KotlinLogging
import org.falaeapp.falae.model.DownloadCache


/**
 * Created by marce on 03/02/2018.
 */
class DownloadCacheDbHelper(context: Context) {

    private val databaseHelper: DataBaseHelper = DataBaseHelper.getInstance(context)
    private var gson = GsonBuilder().create()
    private val logger = KotlinLogging.logger {}

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
        logger.debug(javaClass.name, "Inserting data...")
        return db.insert(DownloadCacheEntry.TABLE_NAME, null, contentValues)
    }

    fun update(downloadCache: DownloadCache) {
        val json = gson.toJson(downloadCache.sources)
        val contentValues = ContentValues()
        contentValues.put(DownloadCacheEntry.COLUMN_NAME, downloadCache.name)
        contentValues.put(DownloadCacheEntry.COLUMN_SOURCES, json)
        val db = databaseHelper.writableDatabase
        logger.debug(javaClass.name, "Updating entry...")
        val whereClause = DownloadCacheEntry.COLUMN_NAME + " = ?"
        val whereArgs = arrayOf(downloadCache.name)
        db.update(DownloadCacheEntry.TABLE_NAME,
                contentValues,
                whereClause,
                whereArgs)
    }

    fun cacheExist(downloadCache: DownloadCache): Boolean {
        val db = databaseHelper.readableDatabase
        val projection = arrayOf(DownloadCacheEntry.COLUMN_NAME)
        val whereClause = DownloadCacheEntry.COLUMN_NAME + " = ?"
        val whereArgs = arrayOf(downloadCache.name)
        db.query(DownloadCacheEntry.TABLE_NAME,
                projection,
                whereClause,
                whereArgs,
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