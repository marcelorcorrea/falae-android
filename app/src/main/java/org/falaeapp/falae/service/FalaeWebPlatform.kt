package org.falaeapp.falae.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.Volley
import org.falaeapp.falae.BuildConfig
import org.falaeapp.falae.TLSSocketFactory
import org.falaeapp.falae.model.DownloadCache
import org.falaeapp.falae.model.Item
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
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class FalaeWebPlatform(val context: Context) {

    private val numberOfCores: Int = Runtime.getRuntime().availableProcessors()
    private var executor: ThreadPoolExecutor? = null
    private lateinit var userDownloadCache: DownloadCache
    private lateinit var publicDownloadCache: DownloadCache

    init {
        initExecutor()
    }

    fun login(email: String, password: String, onComplete: (User?, VolleyError?) -> Unit) {
        initExecutor()
        try {
            val credentials = JSONObject()
            credentials.put("email", email)
            credentials.put("password", password)

            val jsonRequest = JSONObject()
            jsonRequest.put("user", credentials)

            val url = BuildConfig.BASE_URL + "/login.json"
            val gsonRequest = GsonRequest(url = url,
                clazz = User::class.java,
                jsonRequest = jsonRequest,
                listener = Response.Listener {
                    onComplete(it, null)
                },
                errorListener = Response.ErrorListener {
                    onComplete(null, it)
                })

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                Volley.newRequestQueue(
                    context,
                    HurlStack(null, TLSSocketFactory())
                )
                    .add(gsonRequest)
            } else {
                Volley.newRequestQueue(context).add(gsonRequest)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun initExecutor() {
        if (executor == null || executor?.isTerminated == true) {
            executor = ThreadPoolExecutor(
                numberOfCores * 2,
                numberOfCores * 2,
                60L,
                TimeUnit.SECONDS,
                LinkedBlockingQueue()
            )
        }
    }

    fun downloadImages(
        user: User,
        downloadCacheDao: DownloadCacheDao,
        fileHandler: FileHandler,
        onSyncComplete: (user: User) -> Unit
    ) {
        if (!hasNetworkConnection()) {
            return
        }
        userDownloadCache = loadCache(downloadCacheDao, user.email)
        publicDownloadCache = loadCache(downloadCacheDao, PUBLIC_CACHE_KEY)
        val publicFolder = fileHandler.createPublicFolder(context)
        val userFolder = fileHandler.createUserFolder(context, user.email)
        user.photo?.let {
            if (it.isNotEmpty()) {
                val imgSrc = "${BuildConfig.BASE_URL}$it"
                val file = fileHandler.createImg(userFolder, user.name, imgSrc)
                val localUri = userDownloadCache.sources[imgSrc] ?: download(file, user.authToken, user.name, imgSrc)
                storeInCache(localUri, imgSrc, userDownloadCache)
                user.photo = localUri
            }
        }
        val allItems = user.spreadsheets
            .flatMap { it.pages }
            .flatMap { it.items }
        val duplicatedItems = getDuplicatedItems(allItems)
        allItems.distinctBy { it.imgSrc }
            .forEach { item: Item ->
                executor?.execute {
                    val imgSrc = "${BuildConfig.BASE_URL}${item.imgSrc}"
                    val folder: File
                    val cache: DownloadCache
                    if (item.private) {
                        folder = userFolder
                        cache = userDownloadCache
                    } else {
                        folder = publicFolder
                        cache = publicDownloadCache
                    }
                    val file = fileHandler.createImg(folder, item.name, imgSrc)
                    val localUri = cache.sources[imgSrc] ?: download(file, user.authToken, item.name, imgSrc)
                    storeInCache(localUri, imgSrc, cache)
                    // Update imgSrc from duplicated items first
                    duplicatedItems[item.imgSrc]?.forEach { it.imgSrc = localUri }
                    // Update imgSrc of iterated item
                    item.imgSrc = localUri
                }
            }
        executor?.shutdown()
        try {
            executor?.awaitTermination(java.lang.Long.MAX_VALUE, TimeUnit.NANOSECONDS)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        saveOrUpdateCache(downloadCacheDao, userDownloadCache)
        saveOrUpdateCache(downloadCacheDao, publicDownloadCache)

        onSyncComplete(user)
    }

    private fun storeInCache(localUri: String, imgSrc: String, cache: DownloadCache) {
        if (localUri.isNotEmpty() && !cache.sources.containsKey(imgSrc)) {
            cache.sources[imgSrc] = localUri
        } else {
            cache.sources.remove(imgSrc)
        }
    }

    private fun download(
        imgReference: File,
        token: String,
        name: String,
        imgSrc: String
    ): String {
        val url = URL(imgSrc)
        println("Downloading item: $name - $imgSrc")
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

    private fun getDuplicatedItems(list: List<Item>): Map<String, MutableList<Item>> {
        val itemsMap = mutableMapOf<String, MutableList<Item>>()
        val uniqueItems = HashSet<String>()
        list.forEach { item ->
            if (!uniqueItems.add(item.imgSrc)) {
                val listItem = itemsMap[item.imgSrc] ?: mutableListOf()
                listItem.add(item)
                itemsMap[item.imgSrc] = listItem
            }
        }
        return itemsMap.toMap()
    }

    private fun loadCache(downloadCacheDao: DownloadCacheDao, key: String) =
        downloadCacheDao.findByName(key)
            ?: DownloadCache(name = key, sources = mutableMapOf())

    private fun saveOrUpdateCache(downloadCacheDao: DownloadCacheDao, cache: DownloadCache) {
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

    private fun isConnected(networkInfo: NetworkInfo): Boolean =
        (networkInfo.type == ConnectivityManager.TYPE_WIFI ||
            networkInfo.type == ConnectivityManager.TYPE_MOBILE) && networkInfo.isConnected

    companion object {

        private const val TIME_OUT = 6000
        const val PUBLIC_CACHE_KEY = "public"
    }
}