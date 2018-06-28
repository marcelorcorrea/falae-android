package org.falaeapp.falae.repository

import android.content.Context
import org.falaeapp.falae.fragment.SettingsFragment
import org.falaeapp.falae.storage.SharedPreferencesUtils

class SettingsRepository(val context: Context) {
    private val sharedPreferences: SharedPreferencesUtils = SharedPreferencesUtils(context.applicationContext)

    fun isScanModeEnabled(): Boolean {
        return sharedPreferences.getBoolean(SCAN_MODE)
    }

    fun saveEnableScanMode(checked: Boolean) {
        sharedPreferences.storeBoolean(SCAN_MODE, checked)
    }

    fun saveSeekBarProgress(progress: Int) {
        sharedPreferences.storeInt(SEEK_BAR_PROGRESS, progress)
    }

    fun saveScanModeDuration(timeMillis: Long) {
        sharedPreferences.storeLong(SCAN_MODE_DURATION, timeMillis)
    }

    fun getSeekBarProgress(): Int {
        return sharedPreferences.getInt(SEEK_BAR_PROGRESS)
    }


    companion object {

        const val SCAN_MODE = "scanMode"
        const val SEEK_BAR_PROGRESS = "seekBarProgress"
        const val SCAN_MODE_DURATION = "scanModeDuration"

        fun newInstance(): SettingsFragment = SettingsFragment()
    }

}