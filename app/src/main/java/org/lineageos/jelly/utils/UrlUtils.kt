/*
 * Copyright (C) 2020 The LineageOS Project
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
package org.lineageos.jelly.utils

import android.util.Patterns
import android.webkit.URLUtil
import java.util.*
import java.util.regex.Pattern

object UrlUtils {
    val ACCEPTED_URI_SCHEMA: Pattern = Pattern.compile(
        "(?i)" +  // switch on case insensitive matching
                "(" +  // begin group for schema
                "(?:http|https|content|file|chrome)://" +
                "|(?:inline|data|about|javascript):" +
                ")" +
                "(.*)"
    )

    /**
     * Attempts to determine whether user input is a URL or search
     * terms.  Anything with a space is passed to search if canBeSearch is true.
     *
     *
     * Converts to lowercase any mistakenly uppercased schema (i.e.,
     * "Http://" converts to "http://"
     *
     * @return Original or modified URL
     */
    fun smartUrlFilter(url: String): String? {
        var inUrl = url.trim { it <= ' ' }
        val hasSpace = inUrl.indexOf(' ') != -1
        val matcher = ACCEPTED_URI_SCHEMA.matcher(inUrl)
        if (matcher.matches()) {
            // force scheme to lowercase
            val scheme = matcher.group(1)
            val lcScheme = scheme!!.lowercase(Locale.getDefault())
            if (lcScheme != scheme) {
                inUrl = lcScheme + matcher.group(2)
            }
            if (hasSpace && Patterns.WEB_URL.matcher(inUrl).matches()) {
                inUrl = inUrl.replace(" ", "%20")
            }
            return inUrl
        }
        return if (!hasSpace && Patterns.WEB_URL.matcher(inUrl).matches()) {
            URLUtil.guessUrl(inUrl)
        } else null
    }

    /**
     * Formats a launch-able uri out of the template uri by replacing the template parameters with
     * actual values.
     */
    fun getFormattedUri(templateUri: String?, query: String?): String {
        return URLUtil.composeSearchUrl(query, templateUri, "{searchTerms}")
    }
}