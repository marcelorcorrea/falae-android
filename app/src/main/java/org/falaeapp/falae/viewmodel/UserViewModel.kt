package org.falaeapp.falae.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.util.Log
import org.falaeapp.falae.Event
import org.falaeapp.falae.model.User
import org.falaeapp.falae.repository.UserRepository
import org.jetbrains.anko.doAsync


class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository = UserRepository(application)
    val users: LiveData<List<User>>
    var currentUser: LiveData<User> = MutableLiveData()
    val reposResult = MutableLiveData<Event<Pair<User?, Exception?>>>()

    init {
        Log.d("FALAE", "INITIALIZING ")
        users = userRepository.getAllUsers()
    }

    fun loadUser(userId: Long) {
        currentUser = userRepository.getUser(userId)
    }

    fun login(email: String, password: String) {
        Log.d("FALAE", "executing login")
        userRepository.login(email, password) { user, error ->
            Log.d("FALAE", "executing callback: $user and $error")
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
}