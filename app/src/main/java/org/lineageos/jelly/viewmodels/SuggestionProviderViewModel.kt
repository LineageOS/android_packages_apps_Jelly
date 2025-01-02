/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import org.lineageos.jelly.JellyApplication
import org.lineageos.jelly.ext.SUGGESTION_PROVIDER_KEY
import org.lineageos.jelly.ext.preferenceFlow
import org.lineageos.jelly.ext.suggestionProvider
import org.lineageos.jelly.suggestions.SuggestionProvider

class SuggestionProviderViewModel(application: Application) : AndroidViewModel(application) {
    private val historyRepository by lazy { getApplication<JellyApplication>().historyRepository }

    private val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(application)
    }

    fun suggestionProvider() = sharedPreferences.preferenceFlow(
        SUGGESTION_PROVIDER_KEY,
        getter = { suggestionProvider(historyRepository) }
    ).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        initialValue = SuggestionProvider.None
    )
}
