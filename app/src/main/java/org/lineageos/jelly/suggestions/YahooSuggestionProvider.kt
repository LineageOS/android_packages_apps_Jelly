/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.suggestions

/**
 * Search suggestions provider for Yahoo search engine.
 */
internal class YahooSuggestionProvider : SuggestionProvider("UTF-8") {
    override fun createQueryUrl(
        query: String,
        language: String
    ): String {
        return "https://search.yahoo.com/sugg/chrome?output=fxjson&command=$query"
    }
}
