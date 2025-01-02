/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import org.lineageos.jelly.ext.SUGGESTION_PROVIDER_KEY
import org.lineageos.jelly.ext.suggestionProvider
import org.lineageos.jelly.ext.valueFlow

class SuggestionProviderViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(application)
    }

    fun suggestionProvider() = sharedPreferences.valueFlow(
        SUGGESTION_PROVIDER_KEY
    ) {
        suggestionProvider
    }
}
