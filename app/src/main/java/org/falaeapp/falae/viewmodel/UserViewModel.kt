package org.falaeapp.falae.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.falaeapp.falae.Event
import org.falaeapp.falae.model.User
import org.falaeapp.falae.repository.UserRepository
import org.jetbrains.anko.doAsync

class UserViewModel(application: Application) : AndroidViewModel(application), CoroutineScope {

    private val viewModelJob = SupervisorJob()

    override val coroutineContext = Dispatchers.Main + viewModelJob

    private val userRepository: UserRepository = UserRepository(application)
    val users: LiveData<List<User>>
    var currentUser: LiveData<User> = MutableLiveData()
    val reposResult = MutableLiveData<Event<Pair<User?, Exception?>>>()
    val lastConnectedUserId: MutableLiveData<Long> = MutableLiveData()
    val clearCache = MutableLiveData<Event<Boolean>>()

    init {
        users = userRepository.getAllUsers()
    }

    fun loadLastConnectedUser() {
        lastConnectedUserId.postValue(userRepository.getLastConnectedUserId())
    }

    fun loadUser(userId: Long) {
        userRepository.saveLastConnectedUserId(userId)
        currentUser = userRepository.getUser(userId)
    }

    fun login(email: String, password: String) {
        userRepository.login(email, password) { user, error ->
            user?.let {
                lastConnectedUserId.postValue(user.id.toLong())
            }
            reposResult.postValue(Event(Pair(user, error)))
        }
    }

    fun removeUser() {
        currentUser.value?.let {
            doAsync {
                userRepository.remove(it)
            }
        }
    }

    fun handleNewVersion(versionCode: Int) {
        userRepository.handleNewVersion(versionCode)
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