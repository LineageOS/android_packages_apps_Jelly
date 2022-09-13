/*
 * Copyright (C) 2020 The LineageOS Project
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
package org.lineageos.jelly.suggestions

import org.json.JSONArray

/**
 * The search suggestions provider for the DuckDuckGo search engine.
 */
internal class DuckSuggestionProvider : SuggestionProvider("UTF-8") {
    override fun createQueryUrl(
        query: String,
        language: String
    ): String {
        return "https://duckduckgo.com/ac/?q=$query"
    }

    override fun parseResults(
        content: String,
        callback: ResultCallback
    ) {
        val jsonArray = JSONArray(content)
        var n = 0
        val size = jsonArray.length()
        while (n < size) {
            val obj = jsonArray.getJSONObject(n)
            val suggestion = obj.getString("phrase")
            if (!callback.addResult(suggestion)) {
                break
            }
            n++
        }
    }
}