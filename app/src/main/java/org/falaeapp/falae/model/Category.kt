package org.falaeapp.falae.model

import android.graphics.Color
import androidx.core.graphics.toColorInt

/**
 * Created by marcelo on 4/12/17.
 */

enum class Category(private val color: Int) {
    GREETINGS_SOCIAL_EXPRESSIONS("#CC6699".toColorInt()),
    SUBJECT("#E6E600".toColorInt()),
    VERB("#009900".toColorInt()),
    NOUN("#FFA500".toColorInt()),
    ADJECTIVE(Color.BLUE),
    OTHER(Color.WHITE);

    fun color(): Int = color
}
