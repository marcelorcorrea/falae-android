package org.falaeapp.falae.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import org.falaeapp.falae.model.Page
import org.falaeapp.falae.model.SpreadSheet

class DisplayViewModel(application: Application) : AndroidViewModel(application) {
    private var currentSpreadSheet: MutableLiveData<SpreadSheet> = MutableLiveData()
    var pageToOpen: MutableLiveData<Page> = MutableLiveData()
    var currentPage: MutableLiveData<Page> = MutableLiveData()

    fun init(spreadSheet: SpreadSheet?) {
        if (spreadSheet == null) {
            return
        }
        currentSpreadSheet.value = spreadSheet
        openPage(spreadSheet.initialPage)
    }

    fun openPage(linkTo: String) {
        val page = currentSpreadSheet.value?.pages?.find { it.name == linkTo }
        page?.initialPage = isInitialPage(page)
        pageToOpen.value = page
    }

    private fun isInitialPage(page: Page?) = page?.name == currentSpreadSheet.value?.initialPage

    fun setCurrentPage(page: Page) {
        currentPage.value = page
    }
}