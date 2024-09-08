/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly

import android.app.Application
import com.google.android.material.color.DynamicColors
import org.lineageos.jelly.database.FavoriteDatabase
import org.lineageos.jelly.database.HistoryDatabase
import org.lineageos.jelly.repository.FavoriteRepository
import org.lineageos.jelly.repository.HistoryRepository

class JellyApplication : Application() {
    private val historyDatabase by lazy { HistoryDatabase.getDatabase(this) }
    val historyRepository by lazy { HistoryRepository(historyDatabase.historyDao()) }

    private val favoriteDatabase by lazy { FavoriteDatabase.getDatabase(this) }
    val favoriteRepository by lazy { FavoriteRepository(favoriteDatabase.favoriteDao()) }

    override fun onCreate() {
        super.onCreate()

        // Observe dynamic colors changes
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
