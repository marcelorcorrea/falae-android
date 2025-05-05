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
        // Serviço será iniciado quando necessário, não na inicialização do aplicativo
        // Isso evita problemas com permissões em Android 13+
    }
}
