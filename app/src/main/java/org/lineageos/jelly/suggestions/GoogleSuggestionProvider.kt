/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.suggestions

/**
 * Search suggestions provider for Google search engine.
 */
internal class GoogleSuggestionProvider : SuggestionProvider("UTF-8") {
    override fun createQueryUrl(
        query: String,
        language: String
    ): String {
        return ("https://www.google.com/complete/search?client=android&oe=utf8&ie=utf8"
                + "&hl=" + language + "&q=" + query)
    }
}
