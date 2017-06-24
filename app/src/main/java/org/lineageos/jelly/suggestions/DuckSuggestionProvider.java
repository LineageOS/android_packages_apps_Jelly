package org.lineageos.jelly.suggestions;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;
import org.lineageos.jelly.utils.FileUtils;

import java.io.InputStream;

/**
 * The search suggestions provider for the DuckDuckGo search engine.
 */
class DuckSuggestionProvider extends SuggestionProvider {
    private static final String ENCODING = "UTF-8";

    DuckSuggestionProvider() {
        super(ENCODING);
    }

    @NonNull
    @Override
    protected String createQueryUrl(@NonNull String query,
                                    @NonNull String language) {
        return "https://duckduckgo.com/ac/?q=" + query;
    }

    @Override
    protected void parseResults(@NonNull InputStream inputStream,
                                @NonNull ResultCallback callback) throws Exception {
        String content = FileUtils.readStringFromStream(inputStream, ENCODING);
        JSONArray jsonArray = new JSONArray(content);
        for (int n = 0, size = jsonArray.length(); n < size; n++) {
            JSONObject object = jsonArray.getJSONObject(n);
            String suggestion = object.getString("phrase");
            if (!callback.addResult(new SuggestionItem(suggestion))) {
                break;
            }
        }
    }

}
