package org.falaeapp.falae.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Created by marcelo on 4/11/17.
 */

@Parcelize
data class SpreadSheet(
    val name: String,
    val initialPage: String?,
    val pages: List<Page>,
) : Parcelable
