package org.falaeapp.falae.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import kotlinx.coroutines.Dispatchers
import org.falaeapp.falae.model.Page
import org.falaeapp.falae.model.SpreadSheet

class DisplayViewModel(application: Application) : AndroidViewModel(application) {
    private lateinit var currentSpreadSheet: SpreadSheet

    private val linkToPage: MutableLiveData<String> = MutableLiveData()
    val pageToOpen: LiveData<Page> = linkToPage.switchMap { linkTo ->
        liveData(Dispatchers.Default) {
            val page = currentSpreadSheet.pages.find { it.name == linkTo }
            page?.apply {
                initialPage = isInitialPage(page)
                emit(this)
            }
        }
    }

    private val newPage: MutableLiveData<Page> = MutableLiveData()
    val currentPage: LiveData<Page> = newPage.switchMap { page ->
        liveData {
            emit(page)
        }
    }

    fun init(spreadSheet: SpreadSheet) {
        currentSpreadSheet = spreadSheet
        spreadSheet.initialPage?.let {
            openPage(it)
        }
    }

    fun openPage(linkTo: String) {
        linkToPage.value = linkTo
    }

    private fun isInitialPage(page: Page?) = page?.name == currentSpreadSheet.initialPage

    fun setCurrentPage(page: Page) {
        newPage.value = page
    }
}