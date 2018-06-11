package org.falaeapp.falae.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.os.Build
import android.util.Log
import com.android.volley.Response
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import org.falaeapp.falae.BuildConfig
import org.falaeapp.falae.TLSSocketFactory
import org.falaeapp.falae.database.DownloadCacheDbHelper
import org.falaeapp.falae.model.User
import org.falaeapp.falae.readText
import org.falaeapp.falae.room.AppDatabase
import org.falaeapp.falae.task.DownloadTask
import org.falaeapp.falae.task.GsonRequest
import org.json.JSONException
import org.json.JSONObject
import java.io.InputStream
import java.lang.ref.WeakReference


class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val userModelDao = AppDatabase.getInstance(application).userModelDao()
    private var downloadCacheDbHelper: DownloadCacheDbHelper = DownloadCacheDbHelper(application)
    val users: LiveData<List<User>> = userModelDao.getAllUsers()
    val loggedUser: MutableLiveData<User> = MutableLiveData()
    lateinit var currentUser: User

    fun loadUser(userId: Long): LiveData<User> {
        return userModelDao.findById(userId)
    }

    fun loadDemoUser(inputStream: InputStream): LiveData<User> {
        val mutableLiveData = MutableLiveData<User>()
        mutableLiveData.value = Gson().fromJson(inputStream.readText(), User::class.java)!!
        return mutableLiveData
    }

    fun findById(id: Long): LiveData<User> {
        return userModelDao.findById(id)
    }

    fun login(email: String, password: String) {
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
                        loggedUser.value = it
                    },
                    errorListener = Response.ErrorListener {
                        loggedUser.value = null
                    })

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                    && Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                Volley.newRequestQueue(getApplication(),
                        HurlStack(null, TLSSocketFactory()))
                        .add(gsonRequest)
            } else {
                Volley.newRequestQueue(getApplication()).add(gsonRequest)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun onUserAuthenticated(user: User) {
        println("CALLING ONCE")

        DownloadTask(WeakReference(getApplication()), downloadCacheDbHelper, { u ->
            if (!userModelDao.doesUserExist(u.email)) {
                val id = userModelDao.insert(u)
                Log.d("FALAE", "generated ID: $id")
//                loggedUser.value = u.copy(id = id.toInt())
            } else {
                userModelDao.update(u)
            }
        }).execute(user)
    }


}