package org.falaeapp.falae.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Created by marcelo on 4/11/17.
 */
@Parcelize
data class Item(val name: String,
                var imgSrc: String = "",
                val speech: String,
                val category: Category,
                val linkTo: String?,
                val private: Boolean) : Parcelable
