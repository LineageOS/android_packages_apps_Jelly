/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.suggestions

internal class NoSuggestionProvider : SuggestionProvider("UTF-8") {
    override fun fetchResults(rawQuery: String) = listOf<String>()

    override fun createQueryUrl(query: String, language: String) = ""
}
