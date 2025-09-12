package org.falaeapp.falae

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import org.falaeapp.falae.service.TextToSpeechService

/**
 * Created by corream on 29/05/2017.
 * Atualizado para compatibilidade com Android 14/15
 */

class FalaeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Serviço será iniciado quando necessário, não na inicialização do aplicativo
        // Isso evita problemas com permissões em Android 13+

        // Inicialização de componentes importantes
        initializeComponents()
    }

    private fun initializeComponents() {
        // Inicialização de componentes se necessário
        Log.d(TAG, "Inicializando componentes do aplicativo")
    }

    companion object {
        private const val TAG = "FalaeApplication"
    }
}
