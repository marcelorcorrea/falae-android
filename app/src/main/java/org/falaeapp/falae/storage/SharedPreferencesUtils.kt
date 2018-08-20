package org.falaeapp.falae.storage

import android.content.Context
import android.content.SharedPreferences

import org.falaeapp.falae.R

/**
 * Created by bellini on 26/05/2017.
 */

class SharedPreferencesUtils(val context: Context) {

    private var sharedPreferences: SharedPreferences? = null

    fun storeBoolean(key: String, value: Boolean) {
        sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
        sharedPreferences?.edit()?.putBoolean(key, value)?.apply()
    }

    fun storeString(key: String, value: String) {
        sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
        sharedPreferences?.edit()?.putString(key, value)?.apply()
    }

    fun storeInt(key: String, value: Int) {
        sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
        sharedPreferences?.edit()?.putInt(key, value)?.apply()
    }

    fun storeLong(key: String, value: Long) {
        sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
        sharedPreferences?.edit()?.putLong(key, value)?.apply()
    }

    fun getBoolean(key: String): Boolean {
        sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
        return sharedPreferences?.getBoolean(key, false)!!
    }

    fun getString(key: String): String {
        sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
        return sharedPreferences?.getString(key, "")!!
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
        return sharedPreferences?.getInt(key, defaultValue)!!
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
        return sharedPreferences?.getLong(key, defaultValue)!!
    }

    fun remove(key: String) {
        sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
        sharedPreferences?.edit()?.remove(key)?.apply()
    }
    fun clear() {
        sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
        sharedPreferences?.edit()?.clear()?.apply()
    }
}
