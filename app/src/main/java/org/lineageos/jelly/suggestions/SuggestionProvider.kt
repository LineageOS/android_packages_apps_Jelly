/*
 * SPDX-FileCopyrightText: 2020-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.suggestions

import android.util.Log
import org.json.JSONArray
import org.lineageos.jelly.ext.readString
import java.io.BufferedInputStream
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
internal abstract class SuggestionProvider(private val encoding: String) {
    private val language = Companion.language

    /**
     * Create a URL for the given query in the given language.
     *
     * @param query    the query that was made.
     * @param language the locale of the user.
     * @return should return a URL that can be fetched using a GET.
     */
    protected abstract fun createQueryUrl(
        query: String,
        language: String
    ): String

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
        val content = downloadSuggestionsForQuery(query, language)
            ?: // There are no suggestions for this query, return an empty list.
            return filter
        try {
            parseResults(content, object : ResultCallback {
                override fun addResult(suggestion: String): Boolean {
                    filter.add(suggestion)
                    return filter.size < 5
                }
            })
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
        try {
            val url = URL(createQueryUrl(query, language))
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.addRequestProperty(
                "Cache-Control",
                "max-age=$INTERVAL_DAY, max-stale=$INTERVAL_DAY"
            )
            urlConnection.addRequestProperty("Accept-Charset", encoding)
            try {
                BufferedInputStream(urlConnection.inputStream).use {
                    return it.readString(getEncoding(urlConnection))
                }
            } finally {
                urlConnection.disconnect()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Problem getting search suggestions", e)
        }
        return null
    }

    private fun getEncoding(connection: HttpURLConnection): String {
        connection.contentEncoding?.let {
            return it
        }
        val contentType = connection.contentType
        for (value in contentType.split(";").toTypedArray().map { str -> str.trim { it <= ' ' } }) {
            if (value.lowercase(Locale.US).startsWith("charset=")) {
                return value.substring(8)
            }
        }
        return encoding
    }

    internal interface ResultCallback {
        fun addResult(suggestion: String): Boolean
    }

    companion object {
        private const val TAG = "SuggestionProvider"
        private val INTERVAL_DAY = TimeUnit.DAYS.toSeconds(1)
        private const val DEFAULT_LANGUAGE = "en"
        private val language: String
            get() = Locale.getDefault().language.ifEmpty { DEFAULT_LANGUAGE }
    }
}
