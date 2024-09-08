/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.lineageos.jelly.model.History

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<History>>

    @Query("SELECT * FROM history WHERE _id = :id")
    suspend fun get(id: Long): History

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg history: History)

    @Update
    suspend fun update(vararg history: History)

    @Query("DELETE FROM history WHERE _id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM history")
    suspend fun deleteAll()
}
