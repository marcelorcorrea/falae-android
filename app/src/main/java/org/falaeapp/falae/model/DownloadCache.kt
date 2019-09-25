package org.falaeapp.falae.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import org.falaeapp.falae.room.converter.MutableMapConverter

/**
 * Created by marce on 03/02/2018.
 */
@Entity
data class DownloadCache(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    val name: String,
    @TypeConverters(MutableMapConverter::class)
    val sources: MutableMap<String, String>
) {

    fun store(key: String, value: String) {
        if (value.isNotEmpty()) {
            if (!sources.containsKey(key)) {
                sources[key] = value
            }
        } else {
            sources.remove(key)
        }
    }
}