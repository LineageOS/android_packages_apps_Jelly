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

/**
 * Search suggestions provider for Google search engine.
 */
class GoogleSuggestionProvider extends SuggestionProvider {
    GoogleSuggestionProvider() {
        super("UTF-8");
    }

    @NonNull
    protected String createQueryUrl(@NonNull String query,
                                    @NonNull String language) {
        return "https://www.google.com/complete/search?client=android&oe=utf8&ie=utf8"
                + "&hl=" + language + "&q=" + query;
    }
}
