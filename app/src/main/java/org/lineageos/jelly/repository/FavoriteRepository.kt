/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.repository

import org.lineageos.jelly.dao.FavoriteDao

class FavoriteRepository(private val favoriteDao: FavoriteDao) {
    val all = favoriteDao.getAll()

    suspend fun insert(title: String, url: String, color: Int) {
        favoriteDao.insert(title, url, color)
    }

    suspend fun update(id: Long, title: String, url: String) {
        favoriteDao.update(id, title, url)
    }

    suspend fun delete(id: Long) {
        favoriteDao.delete(id)
    }
}
