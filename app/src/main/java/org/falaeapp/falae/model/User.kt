package org.falaeapp.falae.model

import android.arch.persistence.room.*
import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.parcel.Parcelize

/**
 * Created by corream on 17/05/2017.
 */

@Parcelize
@Entity
data class User(
        @PrimaryKey(autoGenerate = true)
        var id: Int = 0,
        var name: String,
        @Ignore
        val authToken: String = "",
        var email: String,
        @TypeConverters(SpreadSheetConverter::class)
        var spreadsheets: List<SpreadSheet> = emptyList(),
        var profile: String? = "",
        var photo: String? = "") : Parcelable {
    constructor() : this(
            0, "", "", "", emptyList(), "", ""
    )
}


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