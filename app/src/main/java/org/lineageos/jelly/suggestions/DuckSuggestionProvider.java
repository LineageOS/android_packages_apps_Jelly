package org.lineageos.jelly.suggestions;

import android.support.annotation.NonNull;

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
