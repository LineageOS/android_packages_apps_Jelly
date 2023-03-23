/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.suggestions

/**
 * Search suggestions provider for the Baidu search engine.
 */
internal class BaiduSuggestionProvider : SuggestionProvider("UTF-8") {
    override fun createQueryUrl(query: String,
                                language: String): String {
        return "http://suggestion.baidu.com/su?ie=UTF-8&wd=$query&action=opensearch"
    }
}
