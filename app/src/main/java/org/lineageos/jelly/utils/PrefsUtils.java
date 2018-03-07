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
package org.lineageos.jelly.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.lineageos.jelly.R;

public final class PrefsUtils {
    private static final String KEY_SEARCH_ENGINE = "key_search_engine";
    private static final String KEY_HOME_PAGE = "key_home_page";
    private static final String KEY_ADVANCED_SHARE = "key_advanced_share";
    private static final String KEY_LOOKLOCK = "key_looklock";
    private static final String KEY_JS = "key_javascript";
    private static final String KEY_LOCATION = "key_location";
    private static final String KEY_COOKIE = "key_cookie";
    private static final String KEY_DO_NOT_TRACK = "key_do_not_track";
    private static final String KEY_SUGGESTION_PROVIDER = "key_suggestion_provider";

    public enum SuggestionProviderType {
        BAIDU,
        BING,
        DUCK,
        GOOGLE,
        YAHOO,
        NONE
    }

    private PrefsUtils() {
    }

    public static String getSearchEngine(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(KEY_SEARCH_ENGINE,
                context.getString(R.string.default_search_engine));
    }

    public static String getHomePage(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(KEY_HOME_PAGE, context.getString(R.string.default_home_page));
    }

    public static boolean getAdvancedShare(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(KEY_ADVANCED_SHARE, false);
    }

    public static boolean getLookLock(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(KEY_LOOKLOCK, false);
    }

    public static boolean getJavascript(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(KEY_JS, true);
    }

    public static boolean getLocation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(KEY_LOCATION, true);
    }

    public static boolean getCookie(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(KEY_COOKIE, true);
    }

    public static boolean getDoNotTrack(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(KEY_DO_NOT_TRACK, false);
    }

    public static SuggestionProviderType getSuggestionProvider(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            String value = prefs.getString(KEY_SUGGESTION_PROVIDER, null);
            if (value == null) {
                value = context.getString(R.string.default_suggestion_provider);
            }
            return SuggestionProviderType.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return SuggestionProviderType.NONE;
        }
    }

    public static void setHomePage(Context context, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(KEY_HOME_PAGE, value).apply();
    }
}
