package org.falaeapp.falae.repository

import android.arch.lifecycle.LiveData
import android.content.Context
import android.util.Log
import com.android.volley.AuthFailureError
import org.falaeapp.falae.service.FalaeWebPlatform
import org.falaeapp.falae.exception.UserNotFoundException
import org.falaeapp.falae.model.User
import org.falaeapp.falae.room.AppDatabase
import org.falaeapp.falae.room.DownloadCacheDao
import org.falaeapp.falae.storage.FileHandler
import org.falaeapp.falae.storage.SharedPreferencesUtils
import org.jetbrains.anko.doAsync

class UserRepository(val context: Context) {
    private val userModelDao = AppDatabase.getInstance(context).userModelDao()
    private val downloadCacheDao: DownloadCacheDao = AppDatabase.getInstance(context).downloadCacheDao()
    private val falaeWebPlatform: FalaeWebPlatform = FalaeWebPlatform(context.applicationContext)
    private val fileHandler: FileHandler = FileHandler()
    private val sharedPreferences: SharedPreferencesUtils = SharedPreferencesUtils(context.applicationContext)

    fun getAllUsers(): LiveData<List<User>> {
        return userModelDao.getAllUsers()
    }

    fun login(email: String, password: String, onComplete: (User?, Exception?) -> Unit) {
        falaeWebPlatform.login(email, password) { user, error ->
            error?.let { e ->
                doAsync {
                    val exception: Exception = if (e is AuthFailureError) {
                        if (userModelDao.findByEmail(email) == null) {
                            UserNotFoundException()
                        } else {
                            e
                        }
                    } else {
                        Exception(e)
                    }
                    onComplete(null, exception)
                }
            } ?: run {
                doAsync {
                    falaeWebPlatform.downloadImages(user!!, downloadCacheDao, fileHandler) { u ->
                        val id: Long
                        if (!userModelDao.doesUserExist(u.email)) {
                            id = userModelDao.insert(u)
                        } else {
                            id = u.id.toLong()
                            userModelDao.update(u)
                        }
                        saveLastConnectedUserId(id)
                        user.id = id.toInt()
                        onComplete(user, null)
                    }
                }
            }
        }
    }

    fun remove(user: User) {
        userModelDao.remove(user)
        downloadCacheDao.remove(user.email)
        fileHandler.deleteUserFolder(context, user.email)
        sharedPreferences.remove(LAST_CONNECTED_USER)
    }

    fun getUser(userId: Long): LiveData<User> {
        return userModelDao.findById(userId)
    }

    fun saveLastConnectedUserId(userId: Long) {
        sharedPreferences.storeLong(LAST_CONNECTED_USER, userId)
    }

    fun getLastConnectedUserId(): Long {
        return sharedPreferences.getLong(LAST_CONNECTED_USER, 1)
    }

    companion object {
        private const val LAST_CONNECTED_USER = "LastConnectedUser"
    }
}