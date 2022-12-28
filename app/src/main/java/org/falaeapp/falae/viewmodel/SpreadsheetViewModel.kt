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
import org.falaeapp.falae.model.Page
import org.falaeapp.falae.model.SpreadSheet
import org.falaeapp.falae.repository.UserRepository

class SpreadsheetViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository: UserRepository = UserRepository(application)

    private val createdSpreadsheetEvent = MutableLiveData<Event<Pair<SpreadSheet?, Exception?>>>()
    val createdSpreadsheetResponse: LiveData<Event<Pair<SpreadSheet?, Exception?>>> =
        createdSpreadsheetEvent.switchMap { event ->
            liveData {
                emit(event)
            }
        }

    private val createdPageEvent = MutableLiveData<Event<Pair<Page?, Exception?>>>()
    val createdPageResponse: LiveData<Event<Pair<Page?, Exception?>>> =
        createdPageEvent.switchMap { event ->
            liveData {
                emit(event)
            }
        }

    fun createSpreadsheet(name: String) {
        viewModelScope.launch {
            try {
                val createdSpreadsheet = userRepository.createSpreadSheet(name)
                createdSpreadsheetEvent.value = Event(Pair(createdSpreadsheet, null))
            } catch (exception: Exception) {
                createdSpreadsheetEvent.value = Event(Pair(null, exception))
            }
        }
    }

    suspend fun getLastConnectedUser() {
        userRepository.getLastConnectedUserId()
    }

    fun createPage(pageName: String, columnsSize: Int, rowsSize: Int) {
        viewModelScope.launch {
            createdSpreadsheetEvent.value?.peekContent()?.first?.let {
                try {
                    val createdPage = userRepository.createPage(
                        it.id,
                        pageName,
                        columnsSize,
                        rowsSize
                    )
                    createdPageEvent.value = Event(Pair(createdPage, null))
                } catch (exception: Exception) {
                    createdSpreadsheetEvent.value = Event(Pair(null, exception))
                }
            }
        }
    }
}
