package org.falaeapp.falae.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.falaeapp.falae.Event
import org.falaeapp.falae.model.User
import org.falaeapp.falae.repository.UserRepository

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository = UserRepository(application)
    var users = MutableLiveData<List<User>>()
    var currentUser: MutableLiveData<User> = MutableLiveData()
    val reposResult = MutableLiveData<Event<Pair<User?, Exception?>>>()
    val lastConnectedUserId: MutableLiveData<Long> = MutableLiveData()
    val clearCache = MutableLiveData<Event<Boolean>>()

    init {
        viewModelScope.launch {
            getAllUsers()
        }
    }

    fun loadLastConnectedUser() {
        viewModelScope.launch {
            lastConnectedUserId.postValue(userRepository.getLastConnectedUserId())
        }
    }

    fun loadUser(userId: Long) {
        viewModelScope.launch {
            userRepository.saveLastConnectedUserId(userId)
            currentUser.value = userRepository.getUser(userId)
        }
    }

    fun synchronizeUser(email: String, password: String) {
        viewModelScope.launch {
            try {
                val user = userRepository.synchAccount(email, password)
                lastConnectedUserId.postValue(user.id.toLong())
                reposResult.postValue(Event(Pair(user, null)))
                getAllUsers()
            } catch (exception: Exception) {
                reposResult.postValue(Event(Pair(null, exception)))
            }
        }
    }

    fun removeUser() {
        currentUser.value?.let { user ->
            viewModelScope.launch {
                userRepository.remove(user)
                getAllUsers()
            }
        }
    }

    private suspend fun getAllUsers() {
        users.value = userRepository.getAllUsers()
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