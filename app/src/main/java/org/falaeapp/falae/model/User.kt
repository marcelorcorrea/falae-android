package org.falaeapp.falae.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverters
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.falaeapp.falae.room.converter.SpreadSheetConverter

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