/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.lineageos.jelly.JellyApplication

class FavoriteViewModel(application: Application) : AndroidViewModel(application) {
    private val favoritesRepository by lazy {
        getApplication<JellyApplication>().favoriteRepository
    }

    val favorites = favoritesRepository.all.flowOn(Dispatchers.IO).stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        initialValue = listOf(),
    )

    fun insert(title: String, url: String, color: Int) = viewModelScope.launch {
        favoritesRepository.insert(title, url, color)
    }

    fun update(id: Long, title: String, url: String) = viewModelScope.launch {
        favoritesRepository.update(id, title, url)
    }

    fun delete(id: Long) = viewModelScope.launch {
        favoritesRepository.delete(id)
    }
}
