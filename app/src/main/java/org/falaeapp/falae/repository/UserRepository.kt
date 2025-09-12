package org.falaeapp.falae.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
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
import java.io.IOException

class UserRepository(val context: Context) {
    private val userModelDao = AppDatabase.getInstance(context).userModelDao()
    private val downloadCacheDao: DownloadCacheDao = AppDatabase.getInstance(context).downloadCacheDao()
    private val falaeWebPlatform: FalaeWebPlatform = FalaeWebPlatform(context.applicationContext)
    private val fileHandler: FileHandler = FileHandler()
    private val sharedPreferences: SharedPreferencesUtils = SharedPreferencesUtils(context.applicationContext)

    suspend fun getAllUsers(): LiveData<List<User>> = withContext(Dispatchers.IO) {
        userModelDao.getAllUsers()
    }

    suspend fun getUser(userId: Long): User = withContext(Dispatchers.IO) {
        userModelDao.findById(userId)
    }

    private suspend fun saveOrUpdateUser(user: User): Long = withContext(Dispatchers.IO) {
        userModelDao.findByEmail(user.email)?.let { result ->
            user.id = result.id
            userModelDao.update(user)
            user.id.toLong()
        } ?: run {
            userModelDao.insert(user)
        }
    }

    suspend fun remove(user: User) = withContext(Dispatchers.IO) {
        userModelDao.remove(user)
        downloadCacheDao.remove(user.email)
        fileHandler.deleteUserFolder(context, user.email)
        sharedPreferences.remove(LAST_CONNECTED_USER)
    }

    suspend fun clearUserCache(email: String): Boolean = withContext(Dispatchers.IO) {
        downloadCacheDao.remove(email)
        fileHandler.deleteUserFolder(context, email)
    }

    suspend fun clearPublicCache(): Boolean = withContext(Dispatchers.IO) {
        downloadCacheDao.remove(PUBLIC_CACHE_KEY)
        fileHandler.deletePublicFolder(context)
    }

    suspend fun saveLastConnectedUserId(userId: Long) = withContext(Dispatchers.IO) {
        sharedPreferences.storeLong(LAST_CONNECTED_USER, userId)
    }

    suspend fun getLastConnectedUserId(): Long = withContext(Dispatchers.IO) {
        sharedPreferences.getLong(LAST_CONNECTED_USER, 1L)
    }

    suspend fun handleNewVersion(currentVersionCode: Int) = withContext(Dispatchers.IO) {
        val doesntExist = -1
        // Get saved version code
        val storedVersionCode = sharedPreferences.getInt(VERSION_CODE, doesntExist)
        // Check for first run or upgrade
        when {
            currentVersionCode == storedVersionCode -> // This is just a normal run
                return@withContext
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

    suspend fun syncAccount(email: String, password: String): User = withContext(Dispatchers.IO) {
        try {
            Log.d("UserRepository", "syncAccount - Starting sync for email: $email")

            // Fazer login
            Log.d("UserRepository", "syncAccount - Attempting login")
            var user = login(email, password)
            Log.d("UserRepository", "syncAccount - Login successful for user: ${user.name}")

            // Baixar imagens
            Log.d("UserRepository", "syncAccount - Downloading images")
            user = downloadImages(user)
            Log.d("UserRepository", "syncAccount - Images downloaded successfully")

            // Salvar ou atualizar usuário
            Log.d("UserRepository", "syncAccount - Saving/updating user")
            val userId = saveOrUpdateUser(user)
            Log.d("UserRepository", "syncAccount - User saved with ID: $userId")

            // Salvar ID do último usuário conectado
            saveLastConnectedUserId(userId)
            Log.d("UserRepository", "syncAccount - Last connected user ID saved")

            user
        } catch (e: Exception) {
            Log.e("UserRepository", "syncAccount - Error: ${e.message}", e)
            throw e
        }
    }

    private suspend fun login(email: String, password: String): User = withContext(Dispatchers.IO) {
        try {
            Log.d("UserRepository", "login - Attempting login for email: $email")
            val user = falaeWebPlatform.login(email, password)
            Log.d("UserRepository", "login - Login successful for user: ${user.name}")
            return@withContext user
        } catch (exception: Exception) {
            Log.e("UserRepository", "login - Error: ${exception.message}", exception)

            val error: Exception = if (exception is AuthFailureError && userModelDao.findByEmail(email) == null) {
                Log.e("UserRepository", "login - User not found in database, creating UserNotFoundException")
                UserNotFoundException()
            } else {
                Log.e("UserRepository", "login - Other error: ${exception.javaClass.simpleName}")
                exception
            }

            throw error
        }
    }

    private suspend fun downloadImages(user: User): User = withContext(Dispatchers.IO) {
        try {
            Log.d("UserRepository", "downloadImages - Starting download for user: ${user.name}")
            val updatedUser = falaeWebPlatform.downloadImages(user, downloadCacheDao, fileHandler)
            Log.d("UserRepository", "downloadImages - Download completed successfully")
            return@withContext updatedUser
        } catch (ex: IOException) {
            Log.e("UserRepository", "downloadImages - IOException: ${ex.message}", ex)
            throw ex
        } catch (ex: Exception) {
            Log.e("UserRepository", "downloadImages - Unexpected error: ${ex.message}", ex)
            throw ex
        }
    }

    companion object {
        private const val LAST_CONNECTED_USER = "LastConnectedUser"
        private const val VERSION_CODE = "versionCode"
    }
}