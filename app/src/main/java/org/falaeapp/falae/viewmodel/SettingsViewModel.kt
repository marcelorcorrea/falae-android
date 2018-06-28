package org.falaeapp.falae.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.util.Log
import org.falaeapp.falae.repository.SettingsRepository

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository: SettingsRepository = SettingsRepository(application)
    var isScanModeEnabled: MutableLiveData<Boolean> = MutableLiveData()
    var seekBarProgress: MutableLiveData<Int> = MutableLiveData()

    fun loadScan() {
        val scanModeEnabled = settingsRepository.isScanModeEnabled()
        isScanModeEnabled.value = scanModeEnabled
    }

    fun loadSeekBarProgress() {
        val barProgress = settingsRepository.getSeekBarProgress()
        seekBarProgress.value = barProgress
    }

    fun setScanModeChecked(checked: Boolean) {
        settingsRepository.saveEnableScanMode(checked)
    }

    fun setSeekBarProgress(progress: Int) {
        settingsRepository.saveSeekBarProgress(progress)
    }

    fun setScanModeDuration(timeMillis: Long) {
        settingsRepository.saveScanModeDuration(timeMillis)
    }
}