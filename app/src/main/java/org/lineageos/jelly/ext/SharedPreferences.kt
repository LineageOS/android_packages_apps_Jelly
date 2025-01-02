/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ext

import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.lineageos.jelly.suggestions.SuggestionProvider

fun <T> SharedPreferences.preferenceFlow(
    vararg keys: String,
    getter: SharedPreferences.() -> T,
) = callbackFlow {
    val update = {
        trySend(getter())
    }

    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
        if (changedKey in keys) {
            update()
        }
    }

    registerOnSharedPreferenceChangeListener(listener)

    update()

    awaitClose {
        unregisterOnSharedPreferenceChangeListener(listener)
    }
}

const val SUGGESTION_PROVIDER_KEY = "key_suggestion_provider"
val SharedPreferences.suggestionProvider: SuggestionProvider
    get() = when (val name = getString(SUGGESTION_PROVIDER_KEY, null)) {
        null -> SuggestionProvider.GOOGLE
        else -> SuggestionProvider.valueOf(name)
    }
