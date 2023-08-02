package org.falaeapp.falae.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kotlinx.parcelize.Parcelize
import org.falaeapp.falae.room.converter.SpreadSheetConverter

/**
 * Created by corream on 17/05/2017.
 */

@Parcelize
@Entity
data class User(
    @PrimaryKey
    var id: Int = 0,
    var name: String,
    @Ignore
    val authToken: String = "",
    var email: String,
    @TypeConverters(SpreadSheetConverter::class)
    var spreadsheets: List<SpreadSheet> = emptyList(),
    var profile: String? = "",
    var photo: String? = ""
) : Parcelable {
    constructor() : this(
        0,
        "",
        "",
        "",
        emptyList(),
        "",
        ""
    )

    fun getItemsFromAllSpreadsheets(): List<Item> {
        return spreadsheets
            .flatMap { it.pages }
            .flatMap { it.items }
    }

    fun isSampleUser(): Boolean {
        return email == SAMPLE_USER_EMAIL
    }

    companion object {
        private const val SAMPLE_USER_EMAIL = "demo@falaeapp.org"
    }
}
