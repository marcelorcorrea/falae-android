package org.falaeapp.falae.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.falaeapp.falae.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Iniciar a MainActivity com tratamento de erros
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            // Registrar o erro e mostrar uma mensagem para o usuário
            Log.e("SplashActivity", "Erro ao iniciar MainActivity: ${e.message}", e)
            Toast.makeText(
                this,
                getString(R.string.error_starting_app, e.message),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}