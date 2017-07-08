/*
 * Copyright (C) 2017 The LineageOS Project
 * Copyright (C) 2010 The Android Open Source Project
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

import android.util.Patterns;
import android.webkit.URLUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UrlUtils {
    public static final Pattern ACCEPTED_URI_SCHEMA = Pattern.compile(
            "(?i)" + // switch on case insensitive matching
                    "(" +    // begin group for schema
                    "(?:http|https|file|chrome)://" +
                    "|(?:inline|data|about|javascript):" +
                    ")" +
                    "(.*)"
    );

    private UrlUtils() {
    }

    /**
     * Attempts to determine whether user input is a URL or search
     * terms.  Anything with a space is passed to search if canBeSearch is true.
     *
     * Converts to lowercase any mistakenly uppercased schema (i.e.,
     * "Http://" converts to "http://"
     *
     * @return Original or modified URL
     *
     */
    public static String smartUrlFilter(String url) {
        String inUrl = url.trim();
        boolean hasSpace = inUrl.indexOf(' ') != -1;

        Matcher matcher = ACCEPTED_URI_SCHEMA.matcher(inUrl);
        if (matcher.matches()) {
            // force scheme to lowercase
            String scheme = matcher.group(1);
            String lcScheme = scheme.toLowerCase();
            if (!lcScheme.equals(scheme)) {
                inUrl = lcScheme + matcher.group(2);
            }
            if (hasSpace && Patterns.WEB_URL.matcher(inUrl).matches()) {
                inUrl = inUrl.replace(" ", "%20");
            }
            return inUrl;
        }
        if (!hasSpace && Patterns.WEB_URL.matcher(inUrl).matches()) {
            return URLUtil.guessUrl(inUrl);
        }
        return null;
    }

    /**
     * Formats a launch-able uri out of the template uri by replacing the template parameters with
     * actual values.
     */
    public static String getFormattedUri(String templateUri, String query) {
        return URLUtil.composeSearchUrl(query, templateUri, "{searchTerms}");
    }


}
