package org.falaeapp.falae.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.falaeapp.falae.repository.SettingsRepository

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository: SettingsRepository = SettingsRepository(application)

    private val _isScanModeEnabled: MutableLiveData<Boolean> = MutableLiveData()
    val isScanModeEnabled: LiveData<Boolean> = _isScanModeEnabled

    private val _seekBarProgress: MutableLiveData<Int> = MutableLiveData()
    val seekBarProgress: LiveData<Int> = _seekBarProgress

    private val _isFeedbackSoundEnabled: MutableLiveData<Boolean> = MutableLiveData()
    val isFeedbackSoundEnabled: LiveData<Boolean> = _isFeedbackSoundEnabled

    private val _isAutomaticNextPageEnabled: MutableLiveData<Boolean> = MutableLiveData()
    val isAutomaticNextPageEnabled: LiveData<Boolean> = _isAutomaticNextPageEnabled

    fun loadScan() {
        _isScanModeEnabled.value = settingsRepository.isScanModeEnabled()
    }

    fun loadFeedbackSound() {
        _isFeedbackSoundEnabled.value = settingsRepository.isFeedbackSoundEnabled()
    }

    fun loadAutomaticNextPage() {
        _isAutomaticNextPageEnabled.value = settingsRepository.isAutomaticNextPageEnabled()
    }

    fun loadSeekBarProgress() {
        _seekBarProgress.value = settingsRepository.getSeekBarProgress()
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