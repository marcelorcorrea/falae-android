package org.falaeapp.falae

import android.content.res.Resources
import android.support.v7.app.AlertDialog
import com.google.gson.Gson
import org.falaeapp.falae.model.User
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset

fun InputStream.toFile(path: String): File {
    val file = File(path)
    use { input ->
        file.outputStream().use { input.copyTo(it) }
    }
    return file
}

fun Resources.loadUser(name: String): User {
    val asset = assets.open(name)
    return Gson().fromJson(asset.readText(), User::class.java)
}

fun InputStream.readText(charset: Charset = Charsets.UTF_8): String =
        bufferedReader(charset).use { it.readText() }

fun AlertDialog.loadDefaultShowListener(resources: Resources) {
    setOnShowListener {
        val buttonPositive = getButton(AlertDialog.BUTTON_POSITIVE)
        val buttonNegative = getButton(AlertDialog.BUTTON_NEGATIVE)
        buttonPositive.setTextColor(resources.getColor(R.color.colorAccent))
        buttonNegative.setTextColor(resources.getColor(R.color.colorAccent))
    }
}