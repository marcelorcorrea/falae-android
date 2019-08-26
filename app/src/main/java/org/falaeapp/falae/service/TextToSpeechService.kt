package org.falaeapp.falae.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import java.util.*


class TextToSpeechService : Service(), TextToSpeech.OnInitListener {

    private lateinit var mTextToSpeech: TextToSpeech

    override fun onBind(arg0: Intent): IBinder? = null

    override fun onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService()
        }
        mTextToSpeech = TextToSpeech(this, this, TTS_ENGINE)
        super.onCreate()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun startForegroundService() {
        val chan = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Foreground Service", NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(chan)

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(android.R.drawable.arrow_up_float)
                .setContentTitle(getString(org.falaeapp.falae.R.string.background_run))
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
        startForeground(2, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val message = intent.getStringExtra(TEXT_TO_SPEECH_MESSAGE)
            if (message != null) {
                speak(message)
            }
        }
        return START_STICKY
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
        const val NOTIFICATION_CHANNEL_ID = "org.falaeapp.falae"
    }
}