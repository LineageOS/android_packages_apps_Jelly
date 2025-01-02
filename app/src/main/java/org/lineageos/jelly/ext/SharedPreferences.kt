/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ext

import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.lineageos.jelly.repository.HistoryRepository
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
fun SharedPreferences.suggestionProvider(historyRepository: HistoryRepository) =
    when (getString(SUGGESTION_PROVIDER_KEY, null)) {
        "BAIDU" -> SuggestionProvider.Baidu
        "BING" -> SuggestionProvider.Bing
        "BRAVE" -> SuggestionProvider.Brave
        "DUCK" -> SuggestionProvider.Duck
        "GOOGLE" -> SuggestionProvider.Google
        "YAHOO" -> SuggestionProvider.Yahoo
        "HISTORY" -> SuggestionProvider.History(historyRepository)
        else -> SuggestionProvider.None
    }
