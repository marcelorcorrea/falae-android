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
    val users: LiveData<List<User>> = liveData {
        emitSource(userRepository.getAllUsers())
    }
    var currentUser: LiveData<User> = MutableLiveData()

    private val _reposResult = MutableLiveData<Event<Pair<User?, Exception?>>>()
    val reposResult: LiveData<Event<Pair<User?, Exception?>>> = _reposResult

    private var _lastConnectedUserId: MutableLiveData<Long> = MutableLiveData()
    val lastConnectedUserId: LiveData<Long> = _lastConnectedUserId

    private val _clearCache = MutableLiveData<Event<Boolean>>()
    val clearCache: LiveData<Event<Boolean>> = _clearCache

    fun loadLastConnectedUser() {
        viewModelScope.launch {
            _lastConnectedUserId.postValue(userRepository.getLastConnectedUserId())
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
                _lastConnectedUserId.postValue(user.id.toLong())
                _reposResult.postValue(Event(Pair(user, null)))
            } catch (exception: Exception) {
                _reposResult.postValue(Event(Pair(null, exception)))
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
                _clearCache.value = Event(userRepository.clearUserCache(user.email))
            }
        }
    }

    fun clearPublicCache() {
        viewModelScope.launch {
            _clearCache.value = Event(userRepository.clearPublicCache())
        }
    }
}