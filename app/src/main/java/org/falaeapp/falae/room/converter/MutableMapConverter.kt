package org.falaeapp.falae.room.converter

import android.arch.persistence.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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