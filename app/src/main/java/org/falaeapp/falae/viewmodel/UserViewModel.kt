package org.falaeapp.falae.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.falaeapp.falae.Event
import org.falaeapp.falae.model.User
import org.falaeapp.falae.repository.UserRepository

class UserViewModel(application: Application) : AndroidViewModel(application), CoroutineScope {

    private val viewModelJob = SupervisorJob()

    override val coroutineContext = Dispatchers.Main + viewModelJob

    private val userRepository: UserRepository = UserRepository(application)
    var users = MutableLiveData<List<User>>()
    var currentUser: MutableLiveData<User> = MutableLiveData()
    val reposResult = MutableLiveData<Event<Pair<User?, Exception?>>>()
    val lastConnectedUserId: MutableLiveData<Long> = MutableLiveData()
    val clearCache = MutableLiveData<Event<Boolean>>()

    init {
        launch {
            getAllUsers()
        }
    }

    fun loadLastConnectedUser() {
        launch {
            lastConnectedUserId.postValue(userRepository.getLastConnectedUserId())
        }
    }

    fun loadUser(userId: Long) {
        launch {
            userRepository.saveLastConnectedUserId(userId)
            currentUser.value = userRepository.getUser(userId)
        }
    }

    fun synchronizeUser(email: String, password: String) {
        launch {
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
            launch {
                userRepository.remove(user)
                getAllUsers()
            }
        }
    }

    private suspend fun getAllUsers() {
        users.value = userRepository.getAllUsers()
    }

    fun handleNewVersion(versionCode: Int) {
        launch {
            userRepository.handleNewVersion(versionCode)
        }
    }

    fun clearUserCache() {
        currentUser.value?.let { user ->
            launch {
                clearCache.value = Event(userRepository.clearUserCache(user.email))
            }
        }
    }

    fun clearPublicCache() {
        launch {
            clearCache.value = Event(userRepository.clearPublicCache())
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}