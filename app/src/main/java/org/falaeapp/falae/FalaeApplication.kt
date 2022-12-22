package org.falaeapp.falae

import android.app.Application
import android.content.Intent
import android.os.Build
import org.falaeapp.falae.service.TextToSpeechService

/**
 * Created by corream on 29/05/2017.
 */

class FalaeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, TextToSpeechService::class.java))
        } else {
            startService(Intent(this, TextToSpeechService::class.java))
        }
    }
}
