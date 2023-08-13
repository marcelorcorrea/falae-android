package org.falaeapp.falae.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.falaeapp.falae.R
import org.falaeapp.falae.model.DownloadCache
import org.falaeapp.falae.model.User
import org.falaeapp.falae.readText
import org.falaeapp.falae.room.converter.MutableMapConverter
import org.falaeapp.falae.room.converter.SpreadSheetConverter

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
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "falae.db",
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            GlobalScope.launch {
                                withContext(Dispatchers.IO) {
                                    val inputStream = context.assets.open(context.getString(R.string.sampleboard))
                                    val demoUser = Gson().fromJson(inputStream.readText(), User::class.java)!!
                                    getInstance(context).userModelDao().insert(demoUser)
                                }
                            }
                        }
                    }).build().also { INSTANCE = it }
            }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
