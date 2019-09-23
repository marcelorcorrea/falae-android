package org.falaeapp.falae.repository

import android.arch.lifecycle.LiveData
import android.content.Context
import com.android.volley.AuthFailureError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.falaeapp.falae.exception.UserNotFoundException
import org.falaeapp.falae.model.User
import org.falaeapp.falae.room.AppDatabase
import org.falaeapp.falae.room.DownloadCacheDao
import org.falaeapp.falae.service.FalaeWebPlatform
import org.falaeapp.falae.service.FalaeWebPlatform.Companion.PUBLIC_CACHE_KEY
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
                        val id = userModelDao.findByEmail(u.email)?.let { result ->
                            u.id = result.id
                            userModelDao.update(u)
                            u.id.toLong()
                        } ?: run {
                            userModelDao.insert(u)
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
        return sharedPreferences.getLong(LAST_CONNECTED_USER, 1L)
    }

    fun handleNewVersion(currentVersionCode: Int) {
        val doesntExist = -1
        // Get saved version code
        val storedVersionCode = sharedPreferences.getInt(VERSION_CODE, doesntExist)
        // Check for first run or upgrade
        when {
            currentVersionCode == storedVersionCode -> // This is just a normal run
                return
            storedVersionCode == doesntExist -> {
                // TODO This is a new install (or the user cleared the shared preferences)
            }
            currentVersionCode > storedVersionCode -> {
                sharedPreferences.clear()
            }
        }
        // Update the shared preferences with the current version code
        sharedPreferences.storeInt(VERSION_CODE, currentVersionCode)
    }

    suspend fun clearUserCache(email: String) = withContext(Dispatchers.IO) {
        downloadCacheDao.remove(email)
        fileHandler.deleteUserFolder(context, email)
    }

    suspend fun clearPublicCache() = withContext(Dispatchers.IO) {
        downloadCacheDao.remove(PUBLIC_CACHE_KEY)
        fileHandler.deletePublicFolder(context)
    }

    companion object {
        private const val LAST_CONNECTED_USER = "LastConnectedUser"
        private const val VERSION_CODE = "versionCode"
    }
}