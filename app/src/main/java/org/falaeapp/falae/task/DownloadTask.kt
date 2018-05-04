package org.falaeapp.falae.task

import android.app.ProgressDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.widget.Toast
import mu.KotlinLogging
import org.falaeapp.falae.BuildConfig
import org.falaeapp.falae.R
import org.falaeapp.falae.database.DownloadCacheDbHelper
import org.falaeapp.falae.model.DownloadCache
import org.falaeapp.falae.model.Item
import org.falaeapp.falae.model.User
import org.falaeapp.falae.storage.FileHandler
import org.falaeapp.falae.toFile
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.URL
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


/**
 * Created by corream on 15/05/2017.
 */

class DownloadTask(val context: WeakReference<Context>, private val dbHelper: DownloadCacheDbHelper, private val onSyncComplete: (user: User) -> Unit) : AsyncTask<User, Void, User>() {
    private val numberOfCores: Int = Runtime.getRuntime().availableProcessors()
    private val executor: ThreadPoolExecutor
    private var pDialog: ProgressDialog? = null
    private lateinit var userDownloadCache: DownloadCache
    private lateinit var publicDownloadCache: DownloadCache
    private val logger = KotlinLogging.logger {}

    init {
        executor = ThreadPoolExecutor(
                numberOfCores * 2,
                numberOfCores * 2,
                60L,
                TimeUnit.SECONDS,
                LinkedBlockingQueue()
        )
    }

    override fun onPreExecute() {
        try {
            if (pDialog != null) {
                pDialog = null
            }
            context.get()?.let {
                pDialog = ProgressDialog(it)
                pDialog!!.setMessage(it.getString(R.string.synchronize_message))
                pDialog!!.isIndeterminate = false
                pDialog!!.setCancelable(false)
                pDialog!!.show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun doInBackground(vararg params: User): User? {
        if (!hasNetworkConnection()) {
            return null
        }
        val user = params[0]
        userDownloadCache = loadCache(user.email)
        publicDownloadCache = loadCache(PUBLIC_CACHE_KEY)
        val publicFolder = FileHandler.createPublicFolder(context.get())
        val userFolder = FileHandler.createUserFolder(context.get(), user.email)
        user.photo?.let {
            if (it.isNotEmpty()) {
                val imgSrc = "${BuildConfig.BASE_URL}$it"
                val file = FileHandler.createImg(userFolder, user.name, imgSrc)
                val userUri = userDownloadCache.sources[imgSrc]
                        ?: download(file, user.authToken, user.name, imgSrc, userDownloadCache)
                user.photo = userUri
            }
        }
        val allItems = user.spreadsheets
                .flatMap { it.pages }
                .flatMap { it.items }
        val duplicatedItems = getDuplicatedItems(allItems)
        allItems.distinctBy { it.imgSrc }
                .forEach { item: Item ->
                    executor.execute {
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
                        val file = FileHandler.createImg(folder, item.name, imgSrc)
                        val uri = cache.sources[imgSrc]
                                ?: download(file, user.authToken, item.name, imgSrc, cache)
                        // Update imgSrc from duplicated items first
                        duplicatedItems[item.imgSrc]?.forEach { it.imgSrc = uri }
                        // Update imgSrc of iterated item
                        item.imgSrc = uri
                    }
                }
        executor.shutdown()
        try {
            executor.awaitTermination(java.lang.Long.MAX_VALUE, TimeUnit.NANOSECONDS)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        saveOrUpdateCache(userDownloadCache)
        saveOrUpdateCache(publicDownloadCache)
        return user
    }

    private fun download(imgReference: File, token: String, name: String, imgSrc: String, cache: DownloadCache): String {
        val url = URL(imgSrc)
        logger.debug(this.javaClass.name, "Downloading item: $name - $imgSrc")
        try {
            with(url.openConnection()) {
                connectTimeout = TIME_OUT
                readTimeout = TIME_OUT
                setRequestProperty("Authorization", "Token $token")
                connect()
                inputStream.toFile(imgReference.absolutePath)
                val uri = Uri.fromFile(imgReference).toString()
                cache.sources[imgSrc] = uri
                return uri
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
            cache.sources.remove(imgSrc)
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

    private fun loadCache(key: String) = dbHelper.findByName(key)
            ?: DownloadCache(key, mutableMapOf())

    private fun saveOrUpdateCache(cache: DownloadCache) {
        logger.debug(javaClass.name, "Saving ${cache.sources.size} images in ${cache.name} folder.")
        if (!dbHelper.cacheExist(cache)) {
            dbHelper.insert(cache)
        } else {
            dbHelper.update(cache)
        }
    }

    override fun onPostExecute(user: User?) {
        user?.let { onSyncComplete(it) } ?: run {
            context.get()?.let {
                Toast.makeText(it, it.getString(R.string.download_failed), Toast.LENGTH_LONG).show()
            }
        }
        if (pDialog != null && pDialog!!.isShowing) {
            pDialog!!.dismiss()
        }
    }

    private fun hasNetworkConnection(): Boolean {
        val cm = context.get()?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
        private const val PUBLIC_CACHE_KEY = "public"
    }
}
