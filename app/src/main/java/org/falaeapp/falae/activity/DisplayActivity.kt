package org.falaeapp.falae.activity

import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import org.falaeapp.falae.R
import org.falaeapp.falae.fragment.PageFragment
import org.falaeapp.falae.fragment.ViewPagerItemFragment
import org.falaeapp.falae.model.Page
import org.falaeapp.falae.model.SpreadSheet
import org.falaeapp.falae.util.TTSHelper
import org.falaeapp.falae.viewmodel.DisplayViewModel

class DisplayActivity : AppCompatActivity(), PageFragment.PageFragmentListener,
    ViewPagerItemFragment.ViewPagerItemFragmentListener {
    private lateinit var displayViewModel: DisplayViewModel
    private lateinit var mediaPlayer: MediaPlayer

    private var textToSpeech: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display)

        // Aplicar modo imersivo para esconder barras do sistema
        applyImmersiveMode()

        // Configurar Window Insets para Display Activity
        setupWindowInsetsForDisplay()

        // Configurar a ActionBar para mostrar o botão de voltar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        // Registrando o callback para o botão voltar
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()

                // Usando o método tradicional já que estamos com compileSdk 34
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
            }
        })

        // Inicializar o TextToSpeech
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val configured = TTSHelper.configurePortugueseBrazilianTTS(textToSpeech!!)
                if (!configured) {
                    Toast.makeText(this, getString(R.string.tts_portuguese_not_available), Toast.LENGTH_LONG).show()
                } else {
                    Log.i("TTS", "TTS configurado com sucesso para português brasileiro")
                }
                
                // Debug: listar vozes portuguesas disponíveis
                TTSHelper.listPortugueseVoices(textToSpeech!!)
                
            } else {
                Toast.makeText(this, getString(R.string.error_initializing_tts), Toast.LENGTH_SHORT).show()
            }
        }

        val spreadSheet: SpreadSheet? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(SPREADSHEET, SpreadSheet::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(SPREADSHEET)
        }
        displayViewModel = ViewModelProvider(this).get(DisplayViewModel::class.java)
        spreadSheet?.let {
            displayViewModel.init(it)
        }

        displayViewModel.pageToOpen.observe(this, Observer {
            it?.let { page ->
                changeFragment(page, page.initialPage.not())
            } ?: run {
                Toast.makeText(this, getString(R.string.page_not_found), Toast.LENGTH_SHORT).show()
            }
        })
        mediaPlayer = MediaPlayer.create(this, R.raw.click_sound)
    }

    /**
     * Configura Window Insets para DisplayActivity (tela de prancha)
     * Como estamos usando modo imersivo, não aplicamos padding
     */
    private fun setupWindowInsetsForDisplay() {
        val rootView = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.display_root)
            ?: return
        
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            // No modo imersivo, não aplicamos padding para as barras do sistema
            // pois elas estão ocultas
            Log.d("DisplayActivity", "Window Insets configurados para modo imersivo")
            
            insets
        }
    }

    private fun changeFragment(page: Page, addToBackStack: Boolean = false) {
        val fragment = PageFragment.newInstance()
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager
            .beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out
            )
            .replace(R.id.page_container, fragment)
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(page.name)
        } else if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
        fragmentTransaction.commit()
        fragmentManager.executePendingTransactions()
        displayViewModel.setCurrentPage(page)
    }

    override fun onDestroy() {
        // Liberar recursos do TextToSpeech
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }

    override fun speak(msg: String) {
        try {
            textToSpeech?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_speaking_text, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    override fun playFeedbackSound() {
        mediaPlayer.start()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Quando o usuário clica no botão de voltar (ícone da casinha) na ActionBar
                try {
                    Log.d("DisplayActivity", "Home button clicked, returning to initial page")

                    // Simplesmente voltar para a página inicial da prancha atual
                    displayViewModel.init(displayViewModel.getCurrentSpreadSheet())

                    return true
                } catch (e: Exception) {
                    Log.e("DisplayActivity", "Error handling home button click: ${e.message}", e)

                    // Em caso de erro, tentar o comportamento padrão de voltar
                    try {
                        finish()
                        @Suppress("DEPRECATION")
                        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
                        return true
                    } catch (e2: Exception) {
                        Log.e("DisplayActivity", "Error in fallback navigation: ${e2.message}", e2)
                        return super.onOptionsItemSelected(item)
                    }
                }
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        // Reaplicar modo imersivo quando a atividade volta ao foco
        applyImmersiveMode()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Reaplicar modo imersivo quando a janela ganha foco
            applyImmersiveMode()
        }
    }

    private fun applyImmersiveMode() {
        try {
            // Para Android 15 e versões mais recentes, usar flags legados para esconder completamente a barra de navegação
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        } catch (e: Exception) {
            Log.e("DisplayActivity", "Erro ao aplicar modo imersivo: ${e.message}")
        }
    }

    companion object {
        const val SPREADSHEET = "SPREADSHEET"
    }
}
