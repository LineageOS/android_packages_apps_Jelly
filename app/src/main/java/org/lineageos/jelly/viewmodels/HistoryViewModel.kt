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
import org.lineageos.jelly.JellyApplication
import org.lineageos.jelly.model.History

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val historyRepository by lazy { getApplication<JellyApplication>().historyRepository }

    val history = historyRepository.all.flowOn(Dispatchers.IO).stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        initialValue = listOf(),
    )

    suspend fun get(id: Long) = historyRepository.get(id)

    fun insert(history: History) = viewModelScope.launch {
        historyRepository.insert(history)
    }

    fun insertOrUpdate(title: String, url: String) = viewModelScope.launch {
        historyRepository.insertOrUpdate(title, url)
    }

    fun replace(title: String, url: String, newUrl: String) = viewModelScope.launch {
        historyRepository.replace(title, url, newUrl)
    }

    fun delete(id: Long) = viewModelScope.launch {
        historyRepository.delete(id)
    }

    fun deleteAll() = viewModelScope.launch {
        historyRepository.deleteAll()
    }
}
