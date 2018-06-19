package org.falaeapp.falae.repository

import android.arch.lifecycle.LiveData
import android.content.Context
import android.util.Log
import com.android.volley.AuthFailureError
import org.falaeapp.falae.FalaeWebPlatform
import org.falaeapp.falae.exception.UserNotFoundException
import org.falaeapp.falae.model.User
import org.falaeapp.falae.room.AppDatabase
import org.falaeapp.falae.room.DownloadCacheDao
import org.falaeapp.falae.storage.FileHandler
import org.jetbrains.anko.doAsync

class UserRepository(val context: Context) {
    private val userModelDao = AppDatabase.getInstance(context).userModelDao()
    private val downloadCacheDao: DownloadCacheDao = AppDatabase.getInstance(context).downloadCacheDao()
    private val falaeWebPlatform: FalaeWebPlatform = FalaeWebPlatform(context.applicationContext)
    private val fileHandler: FileHandler = FileHandler()

    fun getAllUsers(): LiveData<List<User>> {
        return userModelDao.getAllUsers()
    }

    fun findById(userId: Long): LiveData<User> {
        return userModelDao.findById(userId)
    }

    fun findByEmail(email: String): User? {
        return userModelDao.findByEmail(email)
    }

    fun insert(user: User) {
        userModelDao.insert(user)
    }

    fun login(email: String, password: String, onComplete: (User?, Exception?) -> Unit) {
        falaeWebPlatform.login(email, password) { user, error ->
            Log.d("FALAE", "executing callback: $user and $error")
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
                        if (!userModelDao.doesUserExist(u.email)) {
                            val id = userModelDao.insert(u)
                            Log.d("FALAE", "generated ID: $id")
                        } else {
                            userModelDao.update(u)
                        }
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
    }

    fun getUser(userId: Long): LiveData<User> {
        return userModelDao.findById(userId)
    }

//    companion object {
//        private var INSTANCE: UserRepository? = null
//
//        fun getInstance(context: Context): UserRepository =
//                INSTANCE ?: synchronized(this) {
//                    INSTANCE ?: UserRepository(context).also { INSTANCE = it }
//                }
//    }
}