/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.lineageos.jelly.model.Favorite

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite ORDER BY id DESC")
    fun getAll(): Flow<List<Favorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg favorites: Favorite)

    @Query("UPDATE favorite SET title = :title, color = :color WHERE url = :url")
    suspend fun update(title: String, url: String, color: Int)

    @Query("UPDATE favorite SET title = :title, url = :url WHERE id = :id")
    suspend fun update(id: Long, title: String, url: String)

    @Query("UPDATE favorite SET title = :title, url = :url WHERE url = :url")
    suspend fun update(title: String, url: String)

    @Query("DELETE FROM favorite WHERE id = :id")
    suspend fun delete(id: Long)
}
