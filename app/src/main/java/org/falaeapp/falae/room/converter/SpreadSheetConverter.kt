package org.falaeapp.falae.room.converter

import android.arch.persistence.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.falaeapp.falae.model.SpreadSheet

object SpreadSheetConverter {
    private val gson = Gson()
    private val listType = object : TypeToken<List<SpreadSheet>>() {}.type

    @TypeConverter
    @JvmStatic
    fun toSpreadSheetList(json: String): List<SpreadSheet> {
        return gson.fromJson<List<SpreadSheet>>(json, listType)
    }

    @TypeConverter
    @JvmStatic
    fun toJsonString(spreadsheets: List<SpreadSheet>): String {
        return gson.toJson(spreadsheets)
    }
}