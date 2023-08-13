package org.falaeapp.falae.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Created by marcelo on 4/11/17.
 */

@Parcelize
data class Page(
    val name: String,
    val items: List<Item> = emptyList(),
    val columns: Int,
    val rows: Int,
    var initialPage: Boolean = false,
) : Parcelable
