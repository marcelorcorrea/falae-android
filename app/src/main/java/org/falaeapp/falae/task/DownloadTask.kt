package com.marcelorcorrea.falae.task

import android.app.ProgressDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.marcelorcorrea.falae.BuildConfig
import com.marcelorcorrea.falae.R
import com.marcelorcorrea.falae.model.User
import com.marcelorcorrea.falae.storage.FileHandler
import com.marcelorcorrea.falae.toFile
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

class DownloadTask(val context: WeakReference<Context>, private val onSyncComplete: (user: User) -> Unit) : AsyncTask<User, Void, User>() {
    private val NUMBER_OF_CORES: Int = Runtime.getRuntime().availableProcessors()
    private val executor: ThreadPoolExecutor
    private var pDialog: ProgressDialog? = null

    init {
        executor = ThreadPoolExecutor(
                NUMBER_OF_CORES * 2,
                NUMBER_OF_CORES * 2,
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
        val folder = FileHandler.createUserFolder(context.get(), user.email)
        val photo = user.photo
        if (photo != null && photo.isNotEmpty()) {
            val userUri = download(folder, user.authToken, user.name,
                    "${BuildConfig.BASE_URL}${user.photo}")
            user.photo = userUri
        }
        user.spreadsheets
                .flatMap { it.pages }
                .flatMap { it.items }
                .forEach {
                    executor.execute {
                        val imgSrc = "${BuildConfig.BASE_URL}${it.imgSrc}"
                        Log.d("DEBUG", "Downloading item: ${it.name} - $imgSrc")
                        val uri = download(folder, user.authToken, it.name, imgSrc)
                        it.imgSrc = uri
                    }
                }
        executor.shutdown()
        try {
            executor.awaitTermination(java.lang.Long.MAX_VALUE, TimeUnit.NANOSECONDS)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return user
    }

    private fun download(folder: File, token: String, name: String, imgSrc: String): String {
        val file = FileHandler.createImg(folder, name, imgSrc)
        val url = URL(imgSrc)
        try {
            with(url.openConnection()) {
                connectTimeout = TIME_OUT
                readTimeout = TIME_OUT
                setRequestProperty("Authorization", "Token $token")
                connect()
                inputStream.toFile(file.absolutePath)
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        return Uri.fromFile(file).toString()
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

        private val TIME_OUT = 6000
    }
}
