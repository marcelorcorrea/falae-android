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

    private val scanModeEnabledEvent: MutableLiveData<Event<Any>> = MutableLiveData()
    val isScanModeEnabled: LiveData<Boolean> = scanModeEnabledEvent.switchMap { event ->
        event?.getContentIfAny()?.let { checked ->
            viewModelScope.launch {
                settingsRepository.saveEnableScanMode(checked as Boolean)
            }
        }
        liveData {
            emit(settingsRepository.isScanModeEnabled())
        }
    }

    private val feedbackSoundEnabledEvent: MutableLiveData<Event<Any>> = MutableLiveData()
    val isFeedbackSoundEnabled: LiveData<Boolean> = feedbackSoundEnabledEvent.switchMap { event ->
        event?.getContentIfAny()?.let { checked ->
            viewModelScope.launch {
                settingsRepository.saveEnableFeedbackSound(checked as Boolean)
            }
        }
        liveData {
            emit(settingsRepository.isFeedbackSoundEnabled())
        }
    }

    private val automaticNextPageEnabledEvent: MutableLiveData<Event<Any>> = MutableLiveData()
    val isAutomaticNextPageEnabled: LiveData<Boolean> = automaticNextPageEnabledEvent.switchMap { event ->
        event?.getContentIfAny()?.let { checked ->
            viewModelScope.launch {
                settingsRepository.saveEnableAutomaticNextPage(checked as Boolean)
            }
        }
        liveData {
            emit(settingsRepository.isAutomaticNextPageEnabled())
        }
    }

    private val seekBarProgressEvent: MutableLiveData<Event<Any>> = MutableLiveData()
    val seekBarProgress: LiveData<Int> = seekBarProgressEvent.switchMap { event ->
        event?.getContentIfAny()?.let { progress ->
            viewModelScope.launch {
                settingsRepository.saveSeekBarProgress(progress as Int)
            }
        }
        liveData {
            emit(settingsRepository.getSeekBarProgress())
        }
    }

    fun loadScan() {
        scanModeEnabledEvent.value = Event(Unit)
    }

    fun loadFeedbackSound() {
        feedbackSoundEnabledEvent.value = Event(Unit)
    }

    fun loadAutomaticNextPage() {
        automaticNextPageEnabledEvent.value = Event(Unit)
    }

    fun loadSeekBarProgress() {
        seekBarProgressEvent.value = Event(Unit)
    }

    fun setScanModeChecked(checked: Boolean) {
        scanModeEnabledEvent.value = Event(checked)
    }

    fun setFeedbackSoundChecked(checked: Boolean) {
        feedbackSoundEnabledEvent.value = Event(checked)
    }

    fun setAutomaticNextPageChecked(checked: Boolean) {
        automaticNextPageEnabledEvent.value = Event(checked)
    }

    fun setSeekBarProgress(progress: Int) {
        seekBarProgressEvent.value = Event(progress)
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