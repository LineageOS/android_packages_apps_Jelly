/*
 * SPDX-FileCopyrightText: 2020-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ext

import android.content.SharedPreferences
import androidx.core.content.edit

private const val SEARCH_ENGINE_KEY = "key_search_engine"
private const val SEARCH_ENGINE_DEFAULT =
    "https://google.com/search?ie=UTF-8&amp;source=android-browser&amp;q={searchTerms}"
val SharedPreferences.searchEngine: String
    get() = getString(SEARCH_ENGINE_KEY, SEARCH_ENGINE_DEFAULT) ?: SEARCH_ENGINE_DEFAULT

private const val HOME_PAGE_KEY = "key_home_page"
const val HOME_PAGE_DEFAULT = "https://google.com"
var SharedPreferences.homePage: String
    get() = getString(HOME_PAGE_KEY, HOME_PAGE_DEFAULT) ?: HOME_PAGE_DEFAULT
    set(value) = edit { putString(HOME_PAGE_KEY, value) }

private const val ADVANCED_SHARE_ENABLED_KEY = "key_advanced_share"
private const val ADVANCED_SHARE_ENABLED_DEFAULT = false
val SharedPreferences.advancedShareEnabled: Boolean
    get() = getBoolean(ADVANCED_SHARE_ENABLED_KEY, ADVANCED_SHARE_ENABLED_DEFAULT)

private const val LOOK_LOCK_ENABLED_KEY = "key_looklock"
private const val LOOK_LOCK_ENABLED_DEFAULT = false
val SharedPreferences.lookLockEnabled: Boolean
    get() = getBoolean(LOOK_LOCK_ENABLED_KEY, LOOK_LOCK_ENABLED_DEFAULT)

private const val JAVASCRIPT_ENABLED_KEY = "key_javascript"
private const val JAVASCRIPT_ENABLED_DEFAULT = true
val SharedPreferences.javascriptEnabled: Boolean
    get() = getBoolean(JAVASCRIPT_ENABLED_KEY, JAVASCRIPT_ENABLED_DEFAULT)

private const val LOCATION_ENABLED_KEY = "key_location"
private const val LOCATION_ENABLED_DEFAULT = true
val SharedPreferences.locationEnabled: Boolean
    get() = getBoolean(LOCATION_ENABLED_KEY, LOCATION_ENABLED_DEFAULT)

private const val COOKIES_ENABLED_KEY = "key_cookie"
private const val COOKIES_ENABLED_DEFAULT = true
val SharedPreferences.cookiesEnabled: Boolean
    get() = getBoolean(COOKIES_ENABLED_KEY, COOKIES_ENABLED_DEFAULT)

private const val DO_NOT_TRACK_ENABLED_KEY = "key_do_not_track"
private const val DO_NOT_TRACK_ENABLED_DEFAULT = false
val SharedPreferences.doNotTrackEnabled: Boolean
    get() = getBoolean(DO_NOT_TRACK_ENABLED_KEY, DO_NOT_TRACK_ENABLED_DEFAULT)

enum class SuggestionProviderType {
    BAIDU, BING, DUCK, GOOGLE, YAHOO, NONE
}
private const val SUGGESTION_PROVIDER_KEY = "key_suggestion_provider"
private const val SUGGESTION_PROVIDER_DEFAULT = "GOOGLE"
val SharedPreferences.suggestionProvider: SuggestionProviderType
    get() = runCatching {
        SuggestionProviderType.valueOf(getString(
            SUGGESTION_PROVIDER_KEY, SUGGESTION_PROVIDER_DEFAULT
        ) ?: SUGGESTION_PROVIDER_DEFAULT)
    }.getOrDefault(SuggestionProviderType.NONE)

private const val REACH_MODE_ENABLED_KEY = "key_reach_mode"
private const val REACH_MODE_ENABLED_DEFAULT = false
val SharedPreferences.reachModeEnabled: Boolean
    get() = getBoolean(REACH_MODE_ENABLED_KEY, REACH_MODE_ENABLED_DEFAULT)
