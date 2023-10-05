/*
 * SPDX-FileCopyrightText: 2020-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.suggestions

import android.util.Log
import org.json.JSONArray
import org.lineageos.jelly.ext.*
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * The base search suggestions API. Provides common
 * fetching and caching functionality for each potential
 * suggestions provider.
 */
enum class SuggestionProvider(private val encoding: String) {
    BAIDU("UTF-8") {
        override fun createQueryUrl(query: String, language: String) =
            "https://suggestion.baidu.com/su?ie=UTF-8&wd=$query&action=opensearch"
    },
    BING("UTF-8") {
        override fun createQueryUrl(query: String, language: String) =
            "https://api.bing.com/osjson.aspx?query=$query&language=$language"
    },
    BRAVE("UTF-8") {
        override fun createQueryUrl(query: String, language: String) =
            "https://search.brave.com/api/suggest?q=$query"
    },
    DUCK("UTF-8") {
        override fun createQueryUrl(query: String, language: String) =
            "https://duckduckgo.com/ac/?q=$query"

        override fun parseResults(content: String, callback: ResultCallback) {
            val jsonArray = JSONArray(content)
            val size = jsonArray.length()
            for (n in 0 until size) {
                val obj = jsonArray.getJSONObject(n)
                val suggestion = obj.getString("phrase")
                if (!callback.addResult(suggestion)) {
                    break
                }
            }
        }
    },
    GOOGLE("UTF-8") {
        override fun createQueryUrl(query: String, language: String) =
            "https://www.google.com/complete/search?client=android&oe=utf8&ie=utf8&cp=4&xssi=t&gs_pcrt=undefined&hl=$language&q=$query"
    },
    YAHOO("UTF-8") {
        override fun createQueryUrl(query: String, language: String) =
            "https://search.yahoo.com/sugg/chrome?output=fxjson&command=$query"
    },
    NONE("UTF-8") {
        override fun createQueryUrl(query: String, language: String) = ""

        override fun fetchResults(rawQuery: String) = listOf<String>()
    };

    /**
     * Create a URL for the given query in the given language.
     *
     * @param query    the query that was made.
     * @param language the locale of the user.
     * @return should return a URL that can be fetched using a GET.
     */
    protected abstract fun createQueryUrl(query: String, language: String): String

    /**
     * Parse the results of an input stream into a list of [String].
     *
     * @param content  the raw input to parse.
     * @param callback the callback to invoke for each received suggestion
     * @throws Exception throw an exception if anything goes wrong.
     */
    open fun parseResults(
        content: String,
        callback: ResultCallback
    ) {
        val respArray = JSONArray(content)
        val jsonArray = respArray.getJSONArray(1)
        val size = jsonArray.length()
        for (n in 0 until size) {
            val suggestion = jsonArray.getString(n)
            if (!callback.addResult(suggestion)) {
                break
            }
        }
    }

    /**
     * Retrieves the results for a query.
     *
     * @param rawQuery the raw query to retrieve the results for.
     * @return a list of history items for the query.
     */
    open fun fetchResults(rawQuery: String): List<String> {
        val filter = mutableListOf<String>()
        val query = try {
            URLEncoder.encode(rawQuery, encoding)
        } catch (e: UnsupportedEncodingException) {
            Log.e(TAG, "Unable to encode the URL", e)
            return filter
        }

        // There could be no suggestions for this query, return an empty list.
        val content = downloadSuggestionsForQuery(query, language)
            ?.replaceFirst(")]}'", "")
            ?: return filter
        try {
            parseResults(content) {
                filter.add(it)
                filter.size < 5
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to parse results", e)
        }
        return filter
    }

    /**
     * This method downloads the search suggestions for the specific query.
     * NOTE: This is a blocking operation, do not fetchResults on the UI thread.
     *
     * @param query the query to get suggestions for
     * @return the cache file containing the suggestions
     */
    private fun downloadSuggestionsForQuery(
        query: String,
        language: String
    ): String? {
        val url = URL(createQueryUrl(query, language))
        val urlConnection = url.openConnection() as HttpURLConnection
        urlConnection.addRequestProperty(
            "Cache-Control",
            "max-age=$INTERVAL_DAY, max-stale=$INTERVAL_DAY"
        )
        urlConnection.addRequestProperty("Accept-Charset", encoding)
        try {
            val charset = urlConnection.getCharset(encoding)
            urlConnection.inputStream.bufferedReader(charset).use {
                return it.readText()
            }
        } catch (e: IOException) {
            Log.d(TAG, "Problem getting search suggestions", e)
        } finally {
            urlConnection.disconnect()
        }
        return null
    }

    companion object {
        private const val TAG = "SuggestionProvider"
        private val INTERVAL_DAY = TimeUnit.DAYS.toSeconds(1)
        private const val DEFAULT_LANGUAGE = "en"
        private val language by lazy { Locale.getDefault().language.ifEmpty { DEFAULT_LANGUAGE } }

        fun interface ResultCallback {
            fun addResult(suggestion: String): Boolean
        }
    }
}
