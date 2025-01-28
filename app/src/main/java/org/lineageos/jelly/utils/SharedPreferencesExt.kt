/*
 * SPDX-FileCopyrightText: 2023-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.utils

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.lineageos.jelly.R

class SharedPreferencesExt(context: Context) {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val defaultSearchEngine: String
    val defaultHomePage: String
    val defaultHomePageAutoload: Boolean
    private val defaultSuggestionProvider: String

    init {
        context.resources.let {
            defaultSearchEngine = it.getString(R.string.default_search_engine)
            defaultHomePage = it.getString(R.string.default_home_page)
            defaultHomePageAutoload = it.getBoolean(R.bool.default_home_page_autoload)
            defaultSuggestionProvider = it.getString(R.string.default_suggestion_provider)
        }
    }

    var backgroundShortcuts: Set<String>
        get() = sharedPreferences.getStringSet(BACKGROUND_SHORTCUTS_KEY, setOf<String>())!!
        set(value) = sharedPreferences.edit { putStringSet(BACKGROUND_SHORTCUTS_KEY, value) }

    val searchEngine: String
        get() = sharedPreferences.getString(
            SEARCH_ENGINE_KEY, defaultSearchEngine
        ) ?: defaultSearchEngine

    var homePage: String
        get() = sharedPreferences.getString(HOME_PAGE_KEY, defaultHomePage) ?: defaultHomePage
        set(value) = sharedPreferences.edit { putString(HOME_PAGE_KEY, value) }

    var homePageAutoload: Boolean
        get() = sharedPreferences.getBoolean(HOME_PAGE_AUTOLOAD_KEY, defaultHomePageAutoload)
        set(value) = sharedPreferences.edit { putBoolean(HOME_PAGE_AUTOLOAD_KEY, value) }

    val advancedShareEnabled: Boolean
        get() = sharedPreferences.getBoolean(
            ADVANCED_SHARE_ENABLED_KEY, ADVANCED_SHARE_ENABLED_DEFAULT
        )

    val lookLockEnabled: Boolean
        get() = sharedPreferences.getBoolean(LOOK_LOCK_ENABLED_KEY, LOOK_LOCK_ENABLED_DEFAULT)

    val dynamicPopupEnabled: Boolean
        get() = sharedPreferences.getBoolean(
            DYNAMIC_POPUP_ENABLED_KEY, DYNAMIC_POPUP_ENABLED_DEFAULT
        )

    val javascriptEnabled: Boolean
        get() = sharedPreferences.getBoolean(JAVASCRIPT_ENABLED_KEY, JAVASCRIPT_ENABLED_DEFAULT)

    val locationEnabled: Boolean
        get() = sharedPreferences.getBoolean(LOCATION_ENABLED_KEY, LOCATION_ENABLED_DEFAULT)

    val cookiesEnabled: Boolean
        get() = sharedPreferences.getBoolean(COOKIES_ENABLED_KEY, COOKIES_ENABLED_DEFAULT)

    val doNotTrackEnabled: Boolean
        get() = sharedPreferences.getBoolean(DO_NOT_TRACK_ENABLED_KEY, DO_NOT_TRACK_ENABLED_DEFAULT)

    val reachModeEnabled: Boolean
        get() = sharedPreferences.getBoolean(REACH_MODE_ENABLED_KEY, REACH_MODE_ENABLED_DEFAULT)

    companion object {
        private const val BACKGROUND_SHORTCUTS_KEY = "background_shortcuts"

        private const val SEARCH_ENGINE_KEY = "key_search_engine"

        private const val HOME_PAGE_KEY = "key_home_page"
        private const val HOME_PAGE_AUTOLOAD_KEY = "key_home_page_autoload"

        private const val ADVANCED_SHARE_ENABLED_KEY = "key_advanced_share"
        private const val ADVANCED_SHARE_ENABLED_DEFAULT = false

        private const val LOOK_LOCK_ENABLED_KEY = "key_looklock"
        private const val LOOK_LOCK_ENABLED_DEFAULT = false

        private const val DYNAMIC_POPUP_ENABLED_KEY = "key_dynamic_popup"
        private const val DYNAMIC_POPUP_ENABLED_DEFAULT = false

        private const val JAVASCRIPT_ENABLED_KEY = "key_javascript"
        private const val JAVASCRIPT_ENABLED_DEFAULT = true

        private const val LOCATION_ENABLED_KEY = "key_location"
        private const val LOCATION_ENABLED_DEFAULT = true

        private const val COOKIES_ENABLED_KEY = "key_cookie"
        private const val COOKIES_ENABLED_DEFAULT = true

        private const val DO_NOT_TRACK_ENABLED_KEY = "key_do_not_track"
        private const val DO_NOT_TRACK_ENABLED_DEFAULT = false

        private const val REACH_MODE_ENABLED_KEY = "key_reach_mode"
        private const val REACH_MODE_ENABLED_DEFAULT = false
    }
}
