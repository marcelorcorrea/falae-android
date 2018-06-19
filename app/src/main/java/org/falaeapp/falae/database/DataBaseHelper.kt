//package org.falaeapp.falae.database
//
//import android.content.Context
//import android.database.sqlite.SQLiteDatabase
//import android.database.sqlite.SQLiteOpenHelper
//
//class DataBaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
//
//    override fun onCreate(db: SQLiteDatabase) {
//        db.execSQL(SQL_CREATE_DOWNLOAD_CACHE_TABLE)
//        db.execSQL(SQL_CREATE_USER_TABLE)
//    }
//
//    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
//        db.execSQL(SQL_DELETE_DOWNLOAD_CACHE_TABLE)
//        db.execSQL(SQL_DELETE_USER_TABLE)
//        onCreate(db)
//    }
//
//    companion object {
//
//        @Volatile
//        private var INSTANCE: DataBaseHelper? = null
//
//        fun getInstance(context: Context): DataBaseHelper =
//                INSTANCE ?: synchronized(this) {
//                    INSTANCE ?: DataBaseHelper(context).also { INSTANCE = it }
//                }
//
//        private const val DATABASE_NAME = "Falae.db"
//        private const val DATABASE_VERSION = 1
//
////        private const val SQL_CREATE_DOWNLOAD_CACHE_TABLE =
////                """CREATE TABLE ${DownloadCacheDbHelper.DownloadCacheEntry.TABLE_NAME}(
////                        ${DownloadCacheDbHelper.DownloadCacheEntry._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
////                        ${DownloadCacheDbHelper.DownloadCacheEntry.COLUMN_NAME} TEXT NOT NULL,
////                        ${DownloadCacheDbHelper.DownloadCacheEntry.COLUMN_SOURCES} TEXT)"""
//
//        private const val SQL_CREATE_USER_TABLE =
//                """CREATE TABLE ${UserDbHelper.UserEntry.TABLE_NAME}(
//                        ${UserDbHelper.UserEntry._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
//                        ${UserDbHelper.UserEntry.COLUMN_NAME} TEXT NOT NULL,
//                        ${UserDbHelper.UserEntry.COLUMN_EMAIL} TEXT NOT NULL UNIQUE,
//                        ${UserDbHelper.UserEntry.COLUMN_PROFILE} TEXT,
//                        ${UserDbHelper.UserEntry.COLUMN_PHOTO} TEXT,
//                        ${UserDbHelper.UserEntry.COLUMN_SPREADSHEETS} TEXT)"""
//
//        private const val SQL_DELETE_USER_TABLE = "DROP TABLE IF EXISTS " + DownloadCacheDbHelper.DownloadCacheEntry.TABLE_NAME
//
//        private const val SQL_DELETE_DOWNLOAD_CACHE_TABLE = "DROP TABLE IF EXISTS " + UserDbHelper.UserEntry.TABLE_NAME
//
//
//    }
//}