/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
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
import org.lineageos.jelly.model.Favorite
import org.lineageos.jelly.repository.FavoriteRepository

class FavoriteViewModel(application: Application, private val repository: FavoriteRepository) :
    AndroidViewModel(application) {

    val favorites = repository.all.flowOn(Dispatchers.IO).stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        initialValue = listOf(),
    )

    fun insert(favorite: Favorite) = viewModelScope.launch {
        repository.insert(favorite)
    }

    fun update(title: String, url: String, color: Int) = viewModelScope.launch {
        repository.update(title, url, color)
    }

    fun update(id: Long, title: String, url: String) = viewModelScope.launch {
        repository.update(id, title, url)
    }

    fun update(title: String, url: String) = viewModelScope.launch {
        repository.update(title, url)
    }

    fun delete(id: Long) = viewModelScope.launch {
        repository.delete(id)
    }
}
