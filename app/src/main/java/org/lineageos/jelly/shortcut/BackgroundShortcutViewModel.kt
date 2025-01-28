/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.shortcut

import android.app.Application
import android.content.pm.ShortcutManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BackgroundShortcutViewModel(application: Application) : AndroidViewModel(application) {

    private val shortcutManager by lazy {
        application.getSystemService(ShortcutManager::class.java)
    }

    private val shortcutsFlow = MutableSharedFlow<List<BackgroundShortcut>>()

    val shortcuts: StateFlow<List<BackgroundShortcut>> = shortcutsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        listOf()
    )

    fun next(running: Set<String>) = viewModelScope.launch {
        val list = shortcutManager.pinnedShortcuts.map {
            val id = it.id
            val name = (it.shortLabel ?: "").toString()
            val isRunning = running.contains(id)
            BackgroundShortcut(id, name, isRunning)
        }
        shortcutsFlow.emit(list)
    }

}
