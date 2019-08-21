package org.falaeapp.falae.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.falaeapp.falae.repository.SettingsRepository

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository: SettingsRepository = SettingsRepository(application)
    var isScanModeEnabled: MutableLiveData<Boolean> = MutableLiveData()
    var seekBarProgress: MutableLiveData<Int> = MutableLiveData()
    var isFeedbackSoundEnabled: MutableLiveData<Boolean> = MutableLiveData()
    var isAutomaticNextPageEnabled: MutableLiveData<Boolean> = MutableLiveData()

    fun loadScan() {
        isScanModeEnabled.value = settingsRepository.isScanModeEnabled()
    }

    fun loadFeedbackSound() {
        isFeedbackSoundEnabled.value = settingsRepository.isFeedbackSoundEnabled()
    }

    fun loadAutomaticNextPage() {
        isAutomaticNextPageEnabled.value = settingsRepository.isAutomaticNextPageEnabled()
    }

    fun loadSeekBarProgress() {
        seekBarProgress.value = settingsRepository.getSeekBarProgress()
    }

    fun setScanModeChecked(checked: Boolean) {
        settingsRepository.saveEnableScanMode(checked)
    }

    fun setFeedbackSoundChecked(checked: Boolean) {
        settingsRepository.saveEnableFeedbackSound(checked)
    }

    fun setAutomaticNextPageChecked(checked: Boolean) {
        settingsRepository.saveEnableAutomaticNextPage(checked)
    }

    fun setSeekBarProgress(progress: Int) {
        settingsRepository.saveSeekBarProgress(progress)
    }

    fun setScanModeDuration(timeMillis: Long) {
        settingsRepository.saveScanModeDuration(timeMillis)
    }

    fun getScanMode(): LiveData<Pair<Boolean, Long>> {
        val data = MutableLiveData<Pair<Boolean, Long>>()
        val scanModeEnabled = settingsRepository.isScanModeEnabled()
        if (scanModeEnabled) {
            val duration = settingsRepository.getScanModeDuration()
            data.value = Pair(scanModeEnabled, duration)
        }
        return data
    }

    fun getFeedbackSound(): LiveData<Boolean> {
        val data = MutableLiveData<Boolean>()
        data.value = settingsRepository.isFeedbackSoundEnabled()
        return data
    }

    fun getAutomaticNextPage(): LiveData<Boolean> {
        val data = MutableLiveData<Boolean>()
        data.value = settingsRepository.isAutomaticNextPageEnabled()
        return data
    }
}