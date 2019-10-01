package org.falaeapp.falae.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.falaeapp.falae.model.Page
import org.falaeapp.falae.model.SpreadSheet

class DisplayViewModel(application: Application) : AndroidViewModel(application) {
    private val currentSpreadSheet: MutableLiveData<SpreadSheet> = MutableLiveData()
    private val _pageToOpen: MutableLiveData<Page> = MutableLiveData()
    val pageToOpen: LiveData<Page> = _pageToOpen

    private val _currentPage: MutableLiveData<Page> = MutableLiveData()
    val currentPage: LiveData<Page> = _currentPage

    fun init(spreadSheet: SpreadSheet?) {
        if (spreadSheet == null) {
            return
        }
        currentSpreadSheet.value = spreadSheet
        spreadSheet.initialPage?.let {
            openPage(it)
        }
    }

    fun openPage(linkTo: String) {
        val page = currentSpreadSheet.value?.pages?.find { it.name == linkTo }
        page?.initialPage = isInitialPage(page)
        _pageToOpen.value = page
    }

    private fun isInitialPage(page: Page?) = page?.name == currentSpreadSheet.value?.initialPage

    fun setCurrentPage(page: Page) {
        _currentPage.value = page
    }
}