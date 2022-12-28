package org.falaeapp.falae.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Update
import org.falaeapp.falae.model.User

@Dao
interface UserModelDao {

    @Query("select id, name, email, profile, photo, spreadsheets, userId from User where email = :email")
    fun findByEmail(email: String): User?

    @Query("select * from User where id = :id")
    fun findById(id: Long): User

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
