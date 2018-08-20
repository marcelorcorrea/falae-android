package org.falaeapp.falae.service

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*


object TextToSpeechManager : TextToSpeech.OnInitListener {
    private const val TTS_ENGINE = "com.google.android.tts"

    private var mTextToSpeech: TextToSpeech? = null
    private val isLoaded = false

    fun init(context: Context) {
        if (mTextToSpeech == null) {
            mTextToSpeech = TextToSpeech(context, this, TTS_ENGINE)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            var currentLocation: Locale? = Locale.getDefault()
            if (currentLocation == null) {
                currentLocation = Locale("pt", "BR")
            }
            mTextToSpeech?.language = currentLocation
        } else {
            Log.e("Falae", "Initialization Failed!")
        }
    }


    fun speak(msg: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTextToSpeech?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            mTextToSpeech?.speak(msg, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    fun shutdown() {
        mTextToSpeech?.stop()
        mTextToSpeech?.shutdown()
        mTextToSpeech = null
    }

}