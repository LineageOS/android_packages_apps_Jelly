package org.lineageos.jelly.repository

import org.lineageos.jelly.dao.FavoriteDao
import org.lineageos.jelly.model.Favorite

class FavoriteRepository(private val favoriteDao: FavoriteDao) {
    val all = favoriteDao.getAll()

    suspend fun insert(favorite: Favorite) {
        favoriteDao.insert(favorite)
    }

    suspend fun update(title: String, url: String, color: Int) {
        favoriteDao.update(title, url, color)
    }

    suspend fun update(id: Long, title: String, url: String) {
        favoriteDao.update(id, title, url)
    }

    suspend fun update(title: String, url: String) {
        favoriteDao.update(title, url)
    }

    suspend fun delete(id: Long) {
        favoriteDao.delete(id)
    }
}
