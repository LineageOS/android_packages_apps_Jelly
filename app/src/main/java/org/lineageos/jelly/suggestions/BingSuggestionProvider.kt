/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.suggestions

/**
 * Search suggestions provider for Bing search engine.
 */
internal class BingSuggestionProvider : SuggestionProvider("UTF-8") {
    override fun createQueryUrl(query: String,
                                language: String): String {
        return "https://api.bing.com/osjson.aspx?query=$query&language=$language"
    }
}
