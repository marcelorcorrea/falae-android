package org.falaeapp.falae.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.falaeapp.falae.Event
import org.falaeapp.falae.repository.SettingsRepository

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository: SettingsRepository = SettingsRepository(application)

    private val scanModeEnabled: MutableLiveData<Event<Any>> = MutableLiveData()
    val isScanModeEnabled: LiveData<Boolean> = scanModeEnabled.switchMap { event ->
        event?.getContentIfNotHandled()?.let { checked ->
            viewModelScope.launch {
                settingsRepository.saveEnableScanMode(checked as Boolean)
            }
        }
        liveData {
            emit(settingsRepository.isScanModeEnabled())
        }
    }

    private val feedbackSoundEnabled: MutableLiveData<Event<Any>> = MutableLiveData()
    val isFeedbackSoundEnabled: LiveData<Boolean> = feedbackSoundEnabled.switchMap { event ->
        event?.getContentIfNotHandled()?.let { checked ->
            viewModelScope.launch {
                settingsRepository.saveEnableFeedbackSound(checked as Boolean)
            }
        }
        liveData {
            emit(settingsRepository.isFeedbackSoundEnabled())
        }
    }

    private val automaticNextPageEnabled: MutableLiveData<Event<Any>> = MutableLiveData()
    val isAutomaticNextPageEnabled: LiveData<Boolean> = automaticNextPageEnabled.switchMap { event ->
        event?.getContentIfNotHandled()?.let { checked ->
            viewModelScope.launch {
                settingsRepository.saveEnableAutomaticNextPage(checked as Boolean)
            }
        }
        liveData {
            emit(settingsRepository.isAutomaticNextPageEnabled())
        }
    }

    private val progressSeekBar: MutableLiveData<Event<Any>> = MutableLiveData()
    val seekBarProgress: LiveData<Int> = progressSeekBar.switchMap { event ->
        event?.getContentIfNotHandled()?.let { progress ->
            viewModelScope.launch {
                settingsRepository.saveSeekBarProgress(progress as Int)
            }
        }
        liveData {
            emit(settingsRepository.getSeekBarProgress())
        }
    }

    fun loadScan() {
        scanModeEnabled.value = Event(Unit)
    }

    fun loadFeedbackSound() {
        feedbackSoundEnabled.value = Event(Unit)
    }

    fun loadAutomaticNextPage() {
        automaticNextPageEnabled.value = Event(Unit)
    }

    fun loadSeekBarProgress() {
        progressSeekBar.value = Event(Unit)
    }

    fun setScanModeChecked(checked: Boolean) {
        scanModeEnabled.value = Event(checked)
    }

    fun setFeedbackSoundChecked(checked: Boolean) {
        feedbackSoundEnabled.value = Event(checked)
    }

    fun setAutomaticNextPageChecked(checked: Boolean) {
        automaticNextPageEnabled.value = Event(checked)
    }

    fun setSeekBarProgress(progress: Int) {
        progressSeekBar.value = Event(progress)
    }

    fun setScanModeDuration(timeMillis: Long) {
        viewModelScope.launch {
            settingsRepository.saveScanModeDuration(timeMillis)
        }
    }

    fun shouldEnableScanMode(): LiveData<Pair<Boolean, Long>> = liveData {
        val scanModeEnabled = settingsRepository.isScanModeEnabled()
        if (scanModeEnabled) {
            val duration = settingsRepository.getScanModeDuration()
            emit(Pair(scanModeEnabled, duration))
        }
    }

    fun shouldEnableFeedbackSound(): LiveData<Boolean> = liveData {
        emit(settingsRepository.isFeedbackSoundEnabled())
    }

    fun shouldEnableAutomaticNextPage(): LiveData<Boolean> = liveData {
        emit(settingsRepository.isAutomaticNextPageEnabled())
    }
}