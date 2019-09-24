package org.falaeapp.falae.room

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.Query
import android.arch.persistence.room.Update
import org.falaeapp.falae.model.User

@Dao
interface UserModelDao {

    @Query("select id, name, email, profile, photo, spreadsheets from User where email = :email")
    fun findByEmail(email: String): User?

    @Query("select * from User where id = :id")
    fun findById(id: Long): User

    @Query("select * from User")
    fun getAllUsers(): List<User>

    @Insert(onConflict = REPLACE)
    fun insert(user: User): Long

    @Update(onConflict = REPLACE)
    fun update(user: User)

    @Delete
    fun remove(user: User)

    @Query("select 1 from User where email = :email")
    fun doesUserExist(email: String): Boolean
}