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
import org.falaeapp.falae.model.User
import org.falaeapp.falae.repository.UserRepository

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository = UserRepository(application)
    val users: LiveData<List<User>> = liveData {
        emitSource(userRepository.getAllUsers())
    }
    var currentUser: LiveData<User> = MutableLiveData()

    private val syncAccountEvent = MutableLiveData<Event<Pair<User?, Exception?>>>()
    val syncAccountResponse: LiveData<Event<Pair<User?, Exception?>>> = syncAccountEvent.switchMap { event ->
        liveData {
            emit(event)
        }
    }

    private val lastConnectedUserIdEvent: MutableLiveData<Event<Any>> = MutableLiveData()
    val lastConnectedUserId: LiveData<Long> = lastConnectedUserIdEvent.switchMap { event ->
        liveData {
            event?.getContentIfAny()?.let { userId ->
                emit(userId as Long)
            } ?: run {
                emit(userRepository.getLastConnectedUserId())
            }
        }
    }

    private val clearCacheEvent = MutableLiveData<Event<Any>>()
    val clearCache: LiveData<Event<Boolean>> = clearCacheEvent.switchMap { event ->
        liveData {
            event?.getContentIfAny()?.let { email ->
                emit(Event(userRepository.clearUserCache(email as String)))
            } ?: run {
                emit(Event(userRepository.clearPublicCache()))
            }
        }
    }

    fun loadLastConnectedUser() {
        lastConnectedUserIdEvent.value = Event(Unit)
    }

    fun loadUser(userId: Long) {
        currentUser = liveData {
            val user = userRepository.getUser(userId)
            userRepository.saveLastConnectedUserId(userId)
            emit(user)
        }
    }

    fun synchronizeUser(email: String, password: String) {
        viewModelScope.launch {
            try {
                val user = userRepository.syncAccount(email, password)
                lastConnectedUserIdEvent.value = Event(user.id.toLong())
                syncAccountEvent.value = Event(Pair(user, null))
            } catch (exception: Exception) {
                syncAccountEvent.value = Event(Pair(null, exception))
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
            clearCacheEvent.value = Event(user.email)
        }
    }

    fun clearPublicCache() {
        clearCacheEvent.value = Event(Unit)
    }
}
