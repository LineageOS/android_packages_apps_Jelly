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

package org.lineageos.jelly.suggestions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.lineageos.jelly.utils.FileUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * The base search suggestions API. Provides common
 * fetching and caching functionality for each potential
 * suggestions provider.
 */
abstract class SuggestionProvider {
    private static final String TAG = "SuggestionProvider";
    private static final long INTERVAL_DAY = TimeUnit.DAYS.toSeconds(1);
    private static final String DEFAULT_LANGUAGE = "en";
    @NonNull
    private final String mEncoding;
    @NonNull
    private final String mLanguage;

    SuggestionProvider(@NonNull String encoding) {
        mEncoding = encoding;
        mLanguage = getLanguage();
    }

    @NonNull
    private static String getLanguage() {
        String language = Locale.getDefault().getLanguage();
        if (TextUtils.isEmpty(language)) {
            language = DEFAULT_LANGUAGE;
        }
        return language;
    }

    /**
     * Create a URL for the given query in the given language.
     *
     * @param query    the query that was made.
     * @param language the locale of the user.
     * @return should return a URL that can be fetched using a GET.
     */
    @NonNull
    protected abstract String createQueryUrl(@NonNull String query,
                                             @NonNull String language);

    /**
     * Parse the results of an input stream into a list of {@link String}.
     *
     * @param content  the raw input to parse.
     * @param callback the callback to invoke for each received suggestion
     * @throws Exception throw an exception if anything goes wrong.
     */
    void parseResults(@NonNull String content,
                      @NonNull ResultCallback callback) throws Exception {
        JSONArray respArray = new JSONArray(content);
        JSONArray jsonArray = respArray.getJSONArray(1);
        for (int n = 0, size = jsonArray.length(); n < size; n++) {
            String suggestion = jsonArray.getString(n);
            if (!callback.addResult(suggestion)) {
                break;
            }
        }
    }

    /**
     * Retrieves the results for a query.
     *
     * @param rawQuery the raw query to retrieve the results for.
     * @return a list of history items for the query.
     */
    @NonNull
    final List<String> fetchResults(@NonNull final String rawQuery) {
        List<String> filter = new ArrayList<>(5);

        String query;
        try {
            query = URLEncoder.encode(rawQuery, mEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unable to encode the URL", e);

            return filter;
        }

        String content = downloadSuggestionsForQuery(query, mLanguage);
        if (content == null) {
            // There are no suggestions for this query, return an empty list.
            return filter;
        }
        try {
            parseResults(content, suggestion -> {
                filter.add(suggestion);
                return filter.size() < 5;
            });
        } catch (Exception e) {
            Log.e(TAG, "Unable to parse results", e);
        }

        return filter;
    }

    /**
     * This method downloads the search suggestions for the specific query.
     * NOTE: This is a blocking operation, do not fetchResults on the UI thread.
     *
     * @param query the query to get suggestions for
     * @return the cache file containing the suggestions
     */
    @Nullable
    private String downloadSuggestionsForQuery(@NonNull String query,
                                               @NonNull String language) {
        try {
            URL url = new URL(createQueryUrl(query, language));

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.addRequestProperty("Cache-Control",
                    "max-age=" + INTERVAL_DAY + ", max-stale=" + INTERVAL_DAY);
            urlConnection.addRequestProperty("Accept-Charset", mEncoding);
            try (InputStream in = new BufferedInputStream(urlConnection.getInputStream())) {
                return FileUtils.readStringFromStream(in, getEncoding(urlConnection));
            } finally {
                urlConnection.disconnect();
            }
        } catch (IOException e) {
            Log.e(TAG, "Problem getting search suggestions", e);
        }

        return null;
    }

    private String getEncoding(HttpURLConnection connection) {
        String contentEncoding = connection.getContentEncoding();
        if (contentEncoding != null) {
            return contentEncoding;
        }

        String contentType = connection.getContentType();
        for (String value : contentType.split(";")) {
            value = value.trim();
            if (value.toLowerCase(Locale.US).startsWith("charset=")) {
                return value.substring(8);
            }
        }

        return mEncoding;
    }

    interface ResultCallback {
        boolean addResult(String suggestion);
    }
}
