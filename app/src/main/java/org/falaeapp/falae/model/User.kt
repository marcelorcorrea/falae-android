package org.falaeapp.falae.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Created by corream on 17/05/2017.
 */

@Parcelize
data class User(
        val id: Int,
        val name: String,
        val authToken: String = "",
        val email: String,
        val spreadsheets: List<SpreadSheet> = emptyList(),
        val profile: String? = "",
        var photo: String? = "") : Parcelable