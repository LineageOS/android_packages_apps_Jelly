/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.repository

import org.lineageos.jelly.dao.HistoryDao
import org.lineageos.jelly.model.History

class HistoryRepository(private val historyDao: HistoryDao) {
    val all = historyDao.getAll()

    suspend fun get(id: Long) = historyDao.get(id)

    suspend fun insert(history: History) {
        historyDao.insert(history)
    }

    suspend fun insertOrUpdate(title: String, url: String) {
        historyDao.insertOrUpdate(title, url)
    }

    suspend fun replace(title: String, url: String, newUrl: String) {
        historyDao.replace(title, url, newUrl)
    }

    suspend fun delete(id: Long) {
        historyDao.delete(id)
    }

    suspend fun deleteAll() {
        historyDao.deleteAll()
    }
}
