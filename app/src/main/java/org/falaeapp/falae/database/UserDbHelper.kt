package org.falaeapp.falae.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import mu.KotlinLogging
import org.falaeapp.falae.model.SpreadSheet
import org.falaeapp.falae.model.User
import java.util.*


/**
 * Created by corream on 11/05/2017.
 */

class UserDbHelper(context: Context) {
    private val databaseHelper: DataBaseHelper = DataBaseHelper.getInstance(context)
    private val logger = KotlinLogging.logger {}

    class UserEntry {
        companion object {

            const val TABLE_NAME = "user"
            const val COLUMN_SPREADSHEETS = "spreadsheets"
            const val _ID = "_id"
            const val COLUMN_NAME = "name"
            const val COLUMN_EMAIL = "email"
            const val COLUMN_PROFILE = "profile"
            const val COLUMN_PHOTO = "photo"
            const val _COUNT = "_count"
        }
    }

    private fun createUserContentValues(user: User): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(UserEntry.COLUMN_NAME, user.name)
        contentValues.put(UserEntry.COLUMN_EMAIL, user.email)
        contentValues.put(UserEntry.COLUMN_PROFILE, user.profile)
        contentValues.put(UserEntry.COLUMN_PHOTO, user.photo)
        contentValues.put(UserEntry.COLUMN_SPREADSHEETS, Gson().toJson(user.spreadsheets))
        return contentValues
    }

    fun insert(user: User): Long {
        val userContentValues = createUserContentValues(user)
        val db = databaseHelper.writableDatabase
        logger.debug(javaClass.name, "Inserting entry...")
        return db.insert(UserEntry.TABLE_NAME, null, userContentValues)
    }

    fun update(user: User) {
        val userContentValues = createUserContentValues(user)
        val db = databaseHelper.writableDatabase
        logger.debug(javaClass.name, "Updating entry...")
        db.update(UserEntry.TABLE_NAME, userContentValues, UserEntry.COLUMN_EMAIL + "= ? ", arrayOf(user.email))
    }

    fun remove(userId: Int) {
        val db = databaseHelper.writableDatabase
        val whereClause = "_id=?"
        val whereArgs = arrayOf(userId.toString())
        db.delete(UserEntry.TABLE_NAME, whereClause, whereArgs)
    }

    fun doesUserExist(user: User): Boolean {
        var cursor: Cursor? = null
        try {
            val db = databaseHelper.readableDatabase
            val projection = arrayOf(UserEntry.COLUMN_NAME)
            val selection = UserEntry.COLUMN_EMAIL + " = ?"
            val selectionArgs = arrayOf(user.email)
            cursor = db.query(UserEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, null)
            return cursor!!.moveToFirst()
        } finally {
            if (cursor != null && !cursor.isClosed) {
                cursor.close()
            }
        }
    }

    fun findByEmail(email: String): User? {
        var cursor: Cursor? = null
        try {
            val db = databaseHelper.readableDatabase
            val projection = arrayOf(UserEntry._ID,
                    UserEntry.COLUMN_NAME, UserEntry.COLUMN_EMAIL,
                    UserEntry.COLUMN_PROFILE, UserEntry.COLUMN_PHOTO, UserEntry.COLUMN_SPREADSHEETS)
            val selection = UserEntry.COLUMN_EMAIL + " = ?"
            val selectionArgs = arrayOf(email)
            cursor = db.query(UserEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, null)
            val listType = object : TypeToken<List<SpreadSheet>>() {

            }.type
            val gson = Gson()
            if (cursor!!.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndex(UserEntry._ID))
                val name = cursor.getString(cursor.getColumnIndex(UserEntry.COLUMN_NAME))
                val e = cursor.getString(cursor.getColumnIndex(UserEntry.COLUMN_EMAIL))
                val spreadSheetsJson = cursor.getString(cursor.getColumnIndex(UserEntry.COLUMN_SPREADSHEETS))
                val info = cursor.getString(cursor.getColumnIndex(UserEntry.COLUMN_PROFILE))
                val photoSrc = cursor.getString(cursor.getColumnIndex(UserEntry.COLUMN_PHOTO))
                val spreadSheets = gson.fromJson<List<SpreadSheet>>(spreadSheetsJson, listType)

                return User(id.toInt(), name = name, email = e, spreadsheets = spreadSheets, profile = info, photo = photoSrc)
            }
            if (!cursor.isClosed) {
                cursor.close()
            }
            return null
        } finally {
            if (cursor != null && !cursor.isClosed) {
                cursor.close()
            }
        }
    }

    fun read(): List<User> {
        val db = databaseHelper.readableDatabase
        val projection = arrayOf(UserEntry._ID, UserEntry.COLUMN_NAME, UserEntry.COLUMN_EMAIL)
        val cursor = db.query(UserEntry.TABLE_NAME, projection, null, null, null, null, null)
        val users = ArrayList<User>()
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndex(UserEntry._ID))
            val name = cursor.getString(cursor.getColumnIndex(UserEntry.COLUMN_NAME))
            val email = cursor.getString(cursor.getColumnIndex(UserEntry.COLUMN_EMAIL))
            val user = User(id.toInt(), name = name, email = email)
            users.add(user)
        }
        if (!cursor.isClosed) {
            cursor.close()
        }
        return users
    }

    fun close() {
        databaseHelper.close()
    }
}
