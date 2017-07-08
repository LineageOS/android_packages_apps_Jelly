/*
 * Copyright (C) 2017 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lineageos.jelly.utils

import android.content.Context
import android.preference.PreferenceManager
import org.lineageos.jelly.R

object PrefsUtils {
    private val KEY_SEARCH_ENGINE = "key_search_engine"
    private val KEY_HOME_PAGE = "key_home_page"
    private val KEY_ADVANCED_SHARE = "key_advanced_share"
    private val KEY_LOOKLOCK = "key_looklock"
    private val KEY_JS = "key_javascript"
    private val KEY_LOCATION = "key_location"
    private val KEY_COOKIE = "key_cookie"
    private val KEY_DO_NOT_TRACK = "key_do_not_track"
    private val KEY_SUGGESTION_PROVIDER = "key_suggestion_provider"

    fun getSearchEngine(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(KEY_SEARCH_ENGINE,
                context.getString(R.string.default_search_engine))
    }

    fun getHomePage(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(KEY_HOME_PAGE, context.getString(R.string.default_home_page))
    }

    fun getAdvancedShare(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(KEY_ADVANCED_SHARE, false)
    }

    fun getLookLock(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(KEY_LOOKLOCK, false)
    }

    fun getJavascript(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(KEY_JS, true)
    }

    fun getLocation(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(KEY_LOCATION, true)
    }

    fun getCookie(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(KEY_COOKIE, true)
    }

    fun getDoNotTrack(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(KEY_DO_NOT_TRACK, false)
    }

    fun getSuggestionProvider(context: Context): SuggestionProviderType {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        try {
            var value = prefs.getString(KEY_SUGGESTION_PROVIDER, null)
            if (value == null) {
                value = context.getString(R.string.default_suggestion_provider)
            }
            return SuggestionProviderType.valueOf(value)
        } catch (ignored: IllegalArgumentException) {
            return SuggestionProviderType.NONE
        }

    }

    fun setHomePage(context: Context, value: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(KEY_HOME_PAGE, value).apply()
    }

    enum class SuggestionProviderType {
        BAIDU,
        BING,
        DUCK,
        GOOGLE,
        YAHOO,
        NONE
    }
}
