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

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

public final class UrlUtils {
    public static final Pattern ACCEPTED_URI_SCHEMA = Pattern.compile(
            "(?i)" + // switch on case insensitive matching
                    "(" +    // begin group for schema
                    "(?:http|https|file|chrome):\\/\\/" +
                    "|(?:inline|data|about|javascript):" +
                    ")" +
                    "(.*)"
    );
    private static final String TAG = UrlUtils.class.getSimpleName();

    private UrlUtils() {
    }

    public static String fixUrl(String inUrl) {
        inUrl = inUrl.toLowerCase();
        int colon = inUrl.indexOf(':');
        boolean allLower = true;
        for (int index = 0; index < colon; index++) {
            char ch = inUrl.charAt(index);
            if (!Character.isLetter(ch)) {
                break;
            }
            allLower &= Character.isLowerCase(ch);
            if (index == colon - 1 && !allLower) {
                inUrl = inUrl.substring(0, colon).toLowerCase()
                        + inUrl.substring(colon);
            }
        }
        if (inUrl.startsWith("http://") || inUrl.startsWith("https://"))
            return inUrl;
        if (inUrl.startsWith("http:") ||
                inUrl.startsWith("https:")) {
            if (inUrl.startsWith("http:/") || inUrl.startsWith("https:/")) {
                inUrl = inUrl.replaceFirst("/", "//");
            } else inUrl = inUrl.replaceFirst(":", "://");
        }
        return inUrl;
    }

    /**
     * Formats a launch-able uri out of the template uri by replacing the template parameters with
     * actual values.
     */
    public static String getFormattedUri(String templateUri, String query) {
        if (templateUri.isEmpty()) {
            return null;
        }

        // Encode the query terms in the UTF-8 encoding
        try {
            return templateUri.replace("{searchTerms}", URLEncoder.encode(query, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Exception occurred when encoding query " + query + " to UTF-8");
            return null;
        }
    }


}
