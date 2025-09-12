package org.falaeapp.falae.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.falaeapp.falae.Event
import org.falaeapp.falae.model.User
import org.falaeapp.falae.repository.UserRepository

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository = UserRepository(application)
    val users: LiveData<List<User>> = liveData {
        emitSource(userRepository.getAllUsers())
    }
    var currentUser: LiveData<User> = MutableLiveData()

    val syncAccountEvent = MutableLiveData<Event<Pair<User?, Exception?>>>()
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
        // Usar supervisorScope para garantir que erros em uma coroutine não afetem outras
        viewModelScope.launch(Dispatchers.Main + CoroutineExceptionHandler { _, throwable ->
            Log.e("UserViewModel", "synchronizeUser - Unhandled exception in coroutine: ${throwable.message}", throwable)
            try {
                syncAccountEvent.value = Event(Pair(null, Exception("Erro não tratado: ${throwable.message}")))
            } catch (e: Exception) {
                Log.e("UserViewModel", "synchronizeUser - Error posting event: ${e.message}", e)
            }
        }) {
            try {
                Log.d("UserViewModel", "synchronizeUser - Starting sync for email: $email")

                // Validar parâmetros
                if (email.isBlank() || password.isBlank()) {
                    Log.e("UserViewModel", "synchronizeUser - Invalid parameters: email or password is blank")
                    val exception = IllegalArgumentException(getApplication<Application>().getString(org.falaeapp.falae.R.string.invalid_email_password))
                    try {
                        syncAccountEvent.postValue(Event(Pair(null, exception)))
                    } catch (e: Exception) {
                        Log.e("UserViewModel", "synchronizeUser - Error posting validation event: ${e.message}", e)
                    }
                    return@launch
                }

                // Usar withContext para garantir que a operação de rede seja executada em uma thread de IO
                withContext(Dispatchers.IO) {
                    try {
                        // Chamar o repositório para sincronizar a conta
                        val user = userRepository.syncAccount(email, password)

                        // Voltar para a thread principal para atualizar a UI
                        withContext(Dispatchers.Main) {
                            try {
                                if (user != null) {
                                    // O usuário nunca será nulo aqui porque syncAccount lançaria uma exceção se falhasse
                                    Log.d("UserViewModel", "synchronizeUser - Sync successful for user: ${user.name}")

                                    // Atualizar o ID do último usuário conectado
                                    try {
                                        lastConnectedUserIdEvent.postValue(Event(user.id.toLong()))
                                        Log.d("UserViewModel", "synchronizeUser - Updated last connected user ID: ${user.id}")
                                    } catch (e: Exception) {
                                        Log.e("UserViewModel", "synchronizeUser - Error posting lastConnectedUserIdEvent: ${e.message}", e)
                                    }

                                    // Emitir evento de sucesso
                                    try {
                                        syncAccountEvent.postValue(Event(Pair(user, null)))
                                        Log.d("UserViewModel", "synchronizeUser - Emitted success event")
                                    } catch (e: Exception) {
                                        Log.e("UserViewModel", "synchronizeUser - Error posting success event: ${e.message}", e)
                                    }
                                } else {
                                    // Caso improvável, mas por segurança
                                    Log.e("UserViewModel", "synchronizeUser - User is null after sync")
                                    syncAccountEvent.postValue(Event(Pair(null, NullPointerException(getApplication<Application>().getString(org.falaeapp.falae.R.string.user_returned_null)))))
                                }
                            } catch (e: Exception) {
                                Log.e("UserViewModel", "synchronizeUser - Error processing success: ${e.message}", e)
                                try {
                                    syncAccountEvent.postValue(Event(Pair(null, e)))
                                } catch (e2: Exception) {
                                    Log.e("UserViewModel", "synchronizeUser - Error posting error event: ${e2.message}", e2)
                                }
                            }
                        }
                    } catch (exception: Exception) {
                        // Voltar para a thread principal para atualizar a UI
                        withContext(Dispatchers.Main) {
                            // Registrar o erro
                            Log.e("UserViewModel", "synchronizeUser - Error during sync: ${exception.message}", exception)

                            // Emitir evento de erro
                            try {
                                syncAccountEvent.postValue(Event(Pair(null, exception)))
                                Log.d("UserViewModel", "synchronizeUser - Emitted error event")
                            } catch (e: Exception) {
                                Log.e("UserViewModel", "synchronizeUser - Error posting error event: ${e.message}", e)
                            }
                        }
                    }
                }
            } catch (exception: Exception) {
                // Registrar erro crítico
                Log.e("UserViewModel", "synchronizeUser - Critical error: ${exception.message}", exception)

                // Emitir evento de erro
                try {
                    syncAccountEvent.postValue(Event(Pair(null, exception)))
                    Log.d("UserViewModel", "synchronizeUser - Emitted error event for critical error")
                } catch (e: Exception) {
                    Log.e("UserViewModel", "synchronizeUser - Error posting critical error event: ${e.message}", e)
                }
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