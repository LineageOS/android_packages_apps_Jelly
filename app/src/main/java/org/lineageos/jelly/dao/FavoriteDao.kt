/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.lineageos.jelly.model.Favorite

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY _id DESC")
    fun getAll(): Flow<List<Favorite>>

    @Query("SELECT _id FROM favorites WHERE url = :url")
    suspend fun getId(url: String): Long?

    @Query("INSERT INTO favorites (title, url, color) VALUES (:title, :url, :color)")
    suspend fun insert(title: String, url: String, color: Int)

    @Query("UPDATE favorites SET title = :title, url = :url WHERE _id = :id")
    suspend fun update(id: Long, title: String, url: String)

    @Transaction
    suspend fun insertOrUpdate(title: String, url: String, color: Int) {
        val id = getId(url)
        if (id == null) {
            insert(title, url, color)
        } else {
            update(id, title, url)
        }
    }

    @Query("DELETE FROM favorites WHERE _id = :id")
    suspend fun delete(id: Long)
}
