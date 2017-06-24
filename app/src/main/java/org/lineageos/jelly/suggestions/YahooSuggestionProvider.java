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

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.lineageos.jelly.utils.FileUtils;

import java.io.InputStream;

/**
 * Search suggestions provider for Yahoo search engine.
 */
class YahooSuggestionProvider extends SuggestionProvider {
    private static final String ENCODING = "UTF-8";

    YahooSuggestionProvider() {
        super(ENCODING);
    }

    @NonNull
    protected String createQueryUrl(@NonNull String query,
                                    @NonNull String language) {
        return "http://ff.search.yahoo.com/gossip?output=fxjson&command=" + query;
    }

    @Override
    protected void parseResults(@NonNull InputStream inputStream,
                                @NonNull ResultCallback callback) throws Exception {
        String content = FileUtils.readStringFromStream(inputStream, ENCODING);
        JSONArray respArray = new JSONArray(content);
        JSONArray jsonArray = respArray.getJSONArray(1);
        for (int n = 0, size = jsonArray.length(); n < size; n++) {
            String suggestion = jsonArray.getString(n);
            if (!callback.addResult(new SuggestionItem(suggestion))) {
                break;
            }
        }
    }
}
