package org.falaeapp.falae.room

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import org.falaeapp.falae.model.User


@Dao
interface UserModelDao {

    @Query("select id, name, email, profile, photo, spreadsheets from User where email = :email")
    fun findByEmail(email: String): LiveData<User>

    @Query("select * from User where id = :id")
    fun findById(id: Long): LiveData<User>

    @Query("select * from User")
    fun getAllUsers(): LiveData<List<User>>

    @Insert(onConflict = REPLACE)
    fun insert(user: User): Long

    @Update(onConflict = REPLACE)
    fun update(user: User)

    @Delete
    fun remove(user: User)

    @Query("select 1 from User where email = :email")
    fun doesUserExist(email: String): Boolean
}