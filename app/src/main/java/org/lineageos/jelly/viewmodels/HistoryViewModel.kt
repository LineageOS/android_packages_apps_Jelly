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
import org.lineageos.jelly.model.History
import org.lineageos.jelly.repository.HistoryRepository

class HistoryViewModel(application: Application, private val repository: HistoryRepository) :
    AndroidViewModel(application) {

    val history = repository.all.flowOn(Dispatchers.IO).stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        initialValue = listOf(),
    )

    suspend fun get(id: Long) = repository.get(id)

    fun update(history: History) = viewModelScope.launch {
        repository.update(history)
    }

    fun insert(history: History) = viewModelScope.launch {
        repository.insert(history)
    }

    fun delete(id: Long) = viewModelScope.launch {
        repository.delete(id)
    }

    fun deleteAll() = viewModelScope.launch {
        repository.deleteAll()
    }
}
