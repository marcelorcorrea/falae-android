package org.falaeapp.falae.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import java.util.*

class TextToSpeechService : Service(), TextToSpeech.OnInitListener {

    private lateinit var mTextToSpeech: TextToSpeech

    override fun onBind(arg0: Intent): IBinder? = null

    override fun onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(1, Notification())
        }
        mTextToSpeech = TextToSpeech(this, this, TTS_ENGINE)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val message = intent.getStringExtra(TEXT_TO_SPEECH_MESSAGE)
            if (message != null) {
                speak(message)
            }
        }
        return Service.START_STICKY
    }

    private fun speak(msg: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTextToSpeech.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            mTextToSpeech.speak(msg, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    override fun onDestroy() {
        mTextToSpeech.stop()
        mTextToSpeech.shutdown()
        super.onDestroy()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            var currentLocation: Locale? = Locale.getDefault()
            if (currentLocation == null) {
                currentLocation = Locale("pt", "BR")
            }
            mTextToSpeech.language = currentLocation
        }
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        stopSelf()
    }

    companion object {
        const val TEXT_TO_SPEECH_MESSAGE = "TextToSpeechMessage"
        const val TTS_ENGINE = "com.google.android.tts"
    }
}