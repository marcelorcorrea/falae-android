package org.falaeapp.falae.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

/**
 * Helper class para configurar Text-to-Speech com vozes brasileiras
 */
object TTSHelper {
    
    private const val TAG = "TTSHelper"
    
    /**
     * Configura o TTS para usar voz brasileira da melhor qualidade disponível
     */
    fun configurePortugueseBrazilianTTS(tts: TextToSpeech): Boolean {
        try {
            // 1. Tentar definir português brasileiro como idioma
            val brazilianPortuguese = Locale("pt", "BR")
            val languageResult = tts.setLanguage(brazilianPortuguese)
            
            if (languageResult == TextToSpeech.LANG_MISSING_DATA || 
                languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                
                Log.w(TAG, "Português brasileiro não suportado, tentando português genérico")
                val genericPortuguese = Locale("pt")
                val fallbackResult = tts.setLanguage(genericPortuguese)
                
                if (fallbackResult == TextToSpeech.LANG_MISSING_DATA || 
                    fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Nenhuma voz portuguesa disponível")
                    return false
                }
            }
            
            // 2. Procurar por vozes brasileiras específicas
            val availableVoices = tts.voices
            var bestBrazilianVoice: Voice? = null
            
            availableVoices?.forEach { voice ->
                val locale = voice.locale
                Log.d(TAG, "Voz disponível: ${voice.name}, Idioma: ${locale.language}-${locale.country}, Qualidade: ${voice.quality}")
                
                // Priorizar vozes brasileiras
                if (locale.language == "pt" && locale.country == "BR") {
                    if (bestBrazilianVoice == null || voice.quality > bestBrazilianVoice.quality) {
                        bestBrazilianVoice = voice
                    }
                }
            }
            
            // 3. Se encontrou uma voz brasileira, usá-la
            bestBrazilianVoice?.let { voice ->
                Log.i(TAG, "Usando voz brasileira: ${voice.name}")
                tts.voice = voice
            }
            
            // 4. Configurar parâmetros de qualidade
            tts.setSpeechRate(1.0f) // Velocidade normal
            tts.setPitch(1.0f) // Tom normal
            
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao configurar TTS: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Lista todas as vozes portuguesas disponíveis para debug
     */
    fun listPortugueseVoices(tts: TextToSpeech): List<Voice> {
        val portugueseVoices = mutableListOf<Voice>()
        
        tts.voices?.forEach { voice ->
            if (voice.locale.language == "pt") {
                portugueseVoices.add(voice)
                Log.d(TAG, "Voz portuguesa encontrada: ${voice.name} (${voice.locale.country})")
            }
        }
        
        return portugueseVoices
    }
    
    /**
     * Verifica se há vozes portuguesas disponíveis
     */
    fun hasPortugueseVoice(tts: TextToSpeech): Boolean {
        return tts.voices?.any { voice -> 
            voice.locale.language == "pt" 
        } ?: false
    }
}
