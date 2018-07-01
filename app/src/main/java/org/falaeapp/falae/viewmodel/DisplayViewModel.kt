package org.falaeapp.falae.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.falaeapp.falae.model.Page
import org.falaeapp.falae.model.SpreadSheet
import org.falaeapp.falae.repository.SettingsRepository

class DisplayViewModel(application: Application) : AndroidViewModel(application) {
    var currentSpreadSheet: MutableLiveData<SpreadSheet> = MutableLiveData()
    var currentPage: MutableLiveData<Page> = MutableLiveData()
    private val settingsRepository: SettingsRepository = SettingsRepository(application)

    fun init(spreadSheet: SpreadSheet?) {
        if (spreadSheet == null) {
            return
        }
        currentSpreadSheet.value = spreadSheet
    }

    fun openPage(linkTo: String) {
        currentPage.value = currentSpreadSheet.value?.pages?.find { it.name == linkTo }
    }

    fun getInitialPage(): LiveData<Page> {
        val data = MutableLiveData<Page>()
        data.value = currentSpreadSheet.value?.pages?.find { it.name == currentSpreadSheet.value?.initialPage }
        return data
    }
}