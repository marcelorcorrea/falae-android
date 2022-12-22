package org.falaeapp.falae.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.falaeapp.falae.fragment.SettingsFragment
import org.falaeapp.falae.storage.SharedPreferencesUtils

class SettingsRepository(val context: Context) {
    private val sharedPreferences: SharedPreferencesUtils = SharedPreferencesUtils(context.applicationContext)

    suspend fun isScanModeEnabled(): Boolean = withContext(Dispatchers.IO) {
        sharedPreferences.getBoolean(SCAN_MODE)
    }

    suspend fun isFeedbackSoundEnabled(): Boolean = withContext(Dispatchers.IO) {
        sharedPreferences.getBoolean(FEEDBACK_SOUND)
    }

    suspend fun isAutomaticNextPageEnabled(): Boolean = withContext(Dispatchers.IO) {
        sharedPreferences.getBoolean(AUTOMATIC_NEXT_PAGE)
    }

    suspend fun saveEnableScanMode(checked: Boolean) = withContext(Dispatchers.IO) {
        sharedPreferences.storeBoolean(SCAN_MODE, checked)
    }

    suspend fun saveEnableFeedbackSound(checked: Boolean) = withContext(Dispatchers.IO) {
        sharedPreferences.storeBoolean(FEEDBACK_SOUND, checked)
    }

    suspend fun saveEnableAutomaticNextPage(checked: Boolean) = withContext(Dispatchers.IO) {
        sharedPreferences.storeBoolean(AUTOMATIC_NEXT_PAGE, checked)
    }

    suspend fun saveSeekBarProgress(progress: Int) = withContext(Dispatchers.IO) {
        sharedPreferences.storeInt(SEEK_BAR_PROGRESS, progress)
    }

    suspend fun saveScanModeDuration(timeMillis: Long) = withContext(Dispatchers.IO) {
        sharedPreferences.storeLong(SCAN_MODE_DURATION, timeMillis)
    }

    suspend fun getSeekBarProgress(): Int = withContext(Dispatchers.IO) {
        sharedPreferences.getInt(SEEK_BAR_PROGRESS)
    }

    suspend fun getScanModeDuration(): Long = withContext(Dispatchers.IO) {
        sharedPreferences.getLong(SCAN_MODE_DURATION, 500L)
    }

    companion object {

        const val SCAN_MODE = "scanMode"
        const val FEEDBACK_SOUND = "feedbackSound"
        const val AUTOMATIC_NEXT_PAGE = "automaticNextPage"
        const val SEEK_BAR_PROGRESS = "seekBarProgress"
        const val SCAN_MODE_DURATION = "scanModeDuration"

        fun newInstance(): SettingsFragment = SettingsFragment()
    }
}
