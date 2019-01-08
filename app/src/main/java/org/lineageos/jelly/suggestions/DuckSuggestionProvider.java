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

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The search suggestions provider for the DuckDuckGo search engine.
 */
class DuckSuggestionProvider extends SuggestionProvider {
    DuckSuggestionProvider() {
        super("UTF-8");
    }

    @NonNull
    @Override
    protected String createQueryUrl(@NonNull String query,
                                    @NonNull String language) {
        return "https://duckduckgo.com/ac/?q=" + query;
    }

    @Override
    protected void parseResults(@NonNull String content,
                                @NonNull ResultCallback callback) throws Exception {
        JSONArray jsonArray = new JSONArray(content);
        for (int n = 0, size = jsonArray.length(); n < size; n++) {
            JSONObject object = jsonArray.getJSONObject(n);
            String suggestion = object.getString("phrase");
            if (!callback.addResult(suggestion)) {
                break;
            }
        }
    }

}
