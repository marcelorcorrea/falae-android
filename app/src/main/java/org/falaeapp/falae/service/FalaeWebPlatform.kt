package org.falaeapp.falae.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.falaeapp.falae.BuildConfig
import org.falaeapp.falae.TLSSocketFactory
import org.falaeapp.falae.exception.NoNetworkConnectionException
import org.falaeapp.falae.model.DownloadCache
import org.falaeapp.falae.model.Item
import org.falaeapp.falae.model.Page
import org.falaeapp.falae.model.SpreadSheet
import org.falaeapp.falae.model.User
import org.falaeapp.falae.room.DownloadCacheDao
import org.falaeapp.falae.storage.FileHandler
import org.falaeapp.falae.task.GsonRequest
import org.falaeapp.falae.toFile
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FalaeWebPlatform(val context: Context) {

    suspend fun login(email: String, password: String): User = suspendCoroutine { continuation ->
        try {
            val credentials = JSONObject()
            credentials.put("email", email)
            credentials.put("password", password)

            val jsonRequest = JSONObject()
            jsonRequest.put("user", credentials)

            val url = BuildConfig.BASE_URL + "/login.json"
            val request = GsonRequest(
                url = url,
                clazz = User::class.java,
                jsonRequest = jsonRequest,
                listener = { response ->
                    continuation.resume(response)
                },
                errorListener = {
                    continuation.resumeWithException(it)
                }
            )
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                Volley.newRequestQueue(
                    context,
                    HurlStack(null, TLSSocketFactory())
                ).add(request)
            } else {
                Volley.newRequestQueue(context).add(request)
            }
        } catch (e: JSONException) {
            Log.e(javaClass.name, "Error while parsing response: ${e.message}")
            continuation.resumeWithException(e)
        }
    }

    suspend fun downloadImages(
        user: User,
        downloadCacheDao: DownloadCacheDao,
        fileHandler: FileHandler
    ): User = withContext(Dispatchers.IO) {
        if (!hasNetworkConnection()) {
            throw NoNetworkConnectionException("Could not detect any network connection.")
        }
        val userDownloadCache = loadCache(downloadCacheDao, user.email)
        val publicDownloadCache = loadCache(downloadCacheDao, PUBLIC_CACHE_KEY)
        val publicFolder = fileHandler.createPublicFolder(context)
        val userFolder = fileHandler.createUserFolder(context, user.email)

        user.photo?.let { imgSrc ->
            launch {
                if (imgSrc.isNotEmpty()) {
                    user.photo = fetchImage(imgSrc, user.name, fileHandler, userFolder, userDownloadCache, user)
                }
            }
        }
        val allItems = user.getItemsFromAllSpreadsheets()
        val duplicatedItems = getDuplicatedItems(allItems)
        coroutineScope {
            allItems.distinctBy { it.imgSrc }
                .forEach { item: Item ->
                    launch {
                        val folder: File
                        val cache: DownloadCache
                        if (item.private) {
                            folder = userFolder
                            cache = userDownloadCache
                        } else {
                            folder = publicFolder
                            cache = publicDownloadCache
                        }
                        val localUri = fetchImage(item.imgSrc, item.name, fileHandler, folder, cache, user)
                        // Update imgSrc from duplicated items first
                        duplicatedItems[item.imgSrc]?.forEach { it.imgSrc = localUri }
                        // Update imgSrc of iterated item
                        item.imgSrc = localUri
                    }
                }
        }
        saveOrUpdateCache(downloadCacheDao, userDownloadCache)
        saveOrUpdateCache(downloadCacheDao, publicDownloadCache)
        user
    }

    private fun fetchImage(
        relativeImgPath: String,
        imgName: String,
        fileHandler: FileHandler,
        folder: File,
        cache: DownloadCache,
        user: User
    ): String {
        val imgSrc = "${BuildConfig.BASE_URL}$relativeImgPath"
        val file = fileHandler.createImg(folder, imgName, imgSrc)
        val localUri = cache.sources[imgSrc] ?: download(file, user.authToken, imgName, imgSrc)
        cache.store(imgSrc, localUri)
        return localUri
    }

    private fun download(
        imgReference: File,
        token: String,
        name: String,
        imgSrc: String
    ): String {
        val url = URL(imgSrc)
        Log.d(this.javaClass.name, "Downloading item: $name - $imgSrc")
        try {
            with(url.openConnection()) {
                connectTimeout = TIME_OUT
                readTimeout = TIME_OUT
                setRequestProperty("Authorization", "Token $token")
                connect()
                inputStream.toFile(imgReference.absolutePath)
                return Uri.fromFile(imgReference).toString()
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        return ""
    }

    private fun getDuplicatedItems(items: List<Item>): Map<String, MutableList<Item>> {
        val itemsMap = mutableMapOf<String, MutableList<Item>>()
        val uniqueItems = HashSet<String>()
        items.forEach { item ->
            if (!uniqueItems.add(item.imgSrc)) {
                val listItem = itemsMap[item.imgSrc] ?: mutableListOf()
                listItem.add(item)
                itemsMap[item.imgSrc] = listItem
            }
        }
        return itemsMap.toMap()
    }

    private fun loadCache(downloadCacheDao: DownloadCacheDao, key: String) =
        downloadCacheDao.findByName(key) ?: DownloadCache(name = key, sources = mutableMapOf())

    private suspend fun saveOrUpdateCache(downloadCacheDao: DownloadCacheDao, cache: DownloadCache) =
        withContext(Dispatchers.IO) {
            Log.d(javaClass.name, "Saving ${cache.sources.size} images in ${cache.name} folder.")
            if (!downloadCacheDao.cacheExists(cache.name)) {
                downloadCacheDao.insert(cache)
            } else {
                downloadCacheDao.update(cache)
            }
        }

    private fun hasNetworkConnection(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cm.allNetworks.any { isConnected(cm.getNetworkInfo(it)) }
        } else {
            cm.allNetworkInfo.any { isConnected(it) }
        }
    }

    private fun isConnected(networkInfo: NetworkInfo?): Boolean =
        (
            networkInfo?.type == ConnectivityManager.TYPE_WIFI ||
                networkInfo?.type == ConnectivityManager.TYPE_MOBILE
            ) && networkInfo.isConnected

    suspend fun createSpreadSheet(name: String, user: User): SpreadSheet = withContext(Dispatchers.IO) {
        if (!hasNetworkConnection()) {
            throw NoNetworkConnectionException("Could not detect any network connection.")
        }
        val request = request(name, user)
        return@withContext request
    }

    suspend fun request(name: String, user: User): SpreadSheet = suspendCoroutine { continuation ->
        try {
            val spreadsheet = JSONObject()
            spreadsheet.put("name", name)

            val userJson = JSONObject()
            userJson.put("user_id", user.id)

            val jsonRequest = JSONObject()
            jsonRequest.put("spreadsheet", spreadsheet)

            val url = BuildConfig.BASE_URL + "/users/1/spreadsheets.json"

            val request = GsonRequest(
                url = url,
                clazz = SpreadSheet::class.java,
                headers = mapOf("Authorization" to "Token ${user.authToken}"),
                jsonRequest = jsonRequest,
                listener = { response ->
                    continuation.resume(response)
                },
                errorListener = {
                    continuation.resumeWithException(it)
                }
            )
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                Volley.newRequestQueue(
                    context,
                    HurlStack(null, TLSSocketFactory())
                ).add(request)
            } else {
                Volley.newRequestQueue(context).add(request)
            }
        } catch (e: JSONException) {
            Log.e(javaClass.name, "Error while parsing response: ${e.message}")
            continuation.resumeWithException(e)
        }
    }

    suspend fun createPage(name: String, columnsSize: Int, rowsSize: Int, user: User): Page =
        withContext(Dispatchers.IO) {
            if (!hasNetworkConnection()) {
                throw NoNetworkConnectionException("Could not detect any network connection.")
            }
            val request = pageRequest(name, columnsSize, rowsSize, user)
            return@withContext request
        }

    suspend fun pageRequest(name: String, columnsSize: Int, rowsSize: Int, user: User): Page =
        suspendCoroutine { continuation ->
            try {
                val page = JSONObject()
                page.put("name", name)
                page.put("columns", columnsSize)
                page.put("rows", rowsSize)

                val jsonRequest = JSONObject()
                jsonRequest.put("page", page)

                val url = BuildConfig.BASE_URL + "/users/1/spreadsheets/12/pages"

                val request = GsonRequest(
                    url = url,
                    clazz = Page::class.java,
                    headers = mapOf("Authorization" to "Token ${user.authToken}"),
                    jsonRequest = jsonRequest,
                    listener = { response ->
                        continuation.resume(response)
                    },
                    errorListener = {
                        continuation.resumeWithException(it)
                    }
                )
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                    Volley.newRequestQueue(
                        context,
                        HurlStack(null, TLSSocketFactory())
                    ).add(request)
                } else {
                    Volley.newRequestQueue(context).add(request)
                }
            } catch (e: JSONException) {
                Log.e(javaClass.name, "Error while parsing response: ${e.message}")
                continuation.resumeWithException(e)
            }
        }

    companion object {

        private const val TIME_OUT = 6000
        const val PUBLIC_CACHE_KEY = "public"
    }
}
