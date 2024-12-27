/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.lineageos.jelly.model.History

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<History>>

    @Query("SELECT * FROM history WHERE _id = :id")
    suspend fun get(id: Long): History

    @Query("SELECT _id FROM history WHERE url = :url")
    suspend fun getId(url: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: History)

    @Query("INSERT INTO history (title, url, timestamp) VALUES (:title, :url, :timestamp)")
    suspend fun insert(title: String, url: String, timestamp: Long)

    @Query("UPDATE history SET title = :title WHERE url = :url")
    suspend fun update(title: String, url: String)

    @Transaction
    suspend fun insertOrUpdate(title: String, url: String) {
        val id = getId(url)
        if (id != null) {
            update(title, url)
        } else {
            insert(title, url, System.currentTimeMillis())
        }
    }

    @Query("UPDATE history SET url = :newUrl, title = :title WHERE url = :url")
    suspend fun replace(title: String, url: String, newUrl: String)

    @Query("DELETE FROM history WHERE _id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM history")
    suspend fun deleteAll()
}
