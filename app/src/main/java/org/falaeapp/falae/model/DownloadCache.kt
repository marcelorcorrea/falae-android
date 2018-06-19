package org.falaeapp.falae.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverter
import android.arch.persistence.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Created by marce on 03/02/2018.
 */
@Entity
data class DownloadCache(
        @PrimaryKey(autoGenerate = true)
        var id: Int = 0,
        val name: String,
        @TypeConverters(MutableMapConverter::class)
        val sources: MutableMap<String, String>)

object MutableMapConverter {
    private val gson = Gson()
    val typeOfHashMap = object : TypeToken<MutableMap<String, String?>>() {}.type

    @TypeConverter
    @JvmStatic
    fun toSpreadSheetList(json: String): MutableMap<String, String>? {
        return gson.fromJson<MutableMap<String, String>>(json, typeOfHashMap)
    }

    @TypeConverter
    @JvmStatic
    fun toJsonString(spreadsheets: MutableMap<String, String>): String {
        return gson.toJson(spreadsheets)
    }

}