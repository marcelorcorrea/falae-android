package org.falaeapp.falae.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.falaeapp.falae.Event
import org.falaeapp.falae.model.User
import org.falaeapp.falae.repository.UserRepository

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository = UserRepository(application)
    var users: LiveData<List<User>> = liveData {
        emitSource(userRepository.getAllUsers())
        loadLastConnectedUser()
    }
    var currentUser: LiveData<User> = MutableLiveData()
    val reposResult = MutableLiveData<Event<Pair<User?, Exception?>>>()
    var lastConnectedUserId: MutableLiveData<Long> = MutableLiveData()
    val clearCache = MutableLiveData<Event<Boolean>>()

    fun loadLastConnectedUser() {
        viewModelScope.launch {
            lastConnectedUserId.postValue(userRepository.getLastConnectedUserId())
        }
    }

    fun loadUser(userId: Long) {
        viewModelScope.launch {
            currentUser = liveData {
                val user = userRepository.getUser(userId)
                emit(user)
            }
            userRepository.saveLastConnectedUserId(userId)
        }
    }

    fun synchronizeUser(email: String, password: String) {
        viewModelScope.launch {
            try {
                val user = userRepository.syncAccount(email, password)
                lastConnectedUserId.postValue(user.id.toLong())
                reposResult.postValue(Event(Pair(user, null)))
            } catch (exception: Exception) {
                reposResult.postValue(Event(Pair(null, exception)))
            }
        }
    }

    fun removeUser() {
        currentUser.value?.let { user ->
            viewModelScope.launch {
                userRepository.remove(user)
            }
        }
    }

    fun handleNewVersion(versionCode: Int) {
        viewModelScope.launch {
            userRepository.handleNewVersion(versionCode)
        }
    }

    fun clearUserCache() {
        currentUser.value?.let { user ->
            viewModelScope.launch {
                clearCache.value = Event(userRepository.clearUserCache(user.email))
            }
        }
    }

    fun clearPublicCache() {
        viewModelScope.launch {
            clearCache.value = Event(userRepository.clearPublicCache())
        }
    }
}