package org.lineageos.jelly.suggestions;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;
import org.lineageos.jelly.utils.FileUtils;

import java.io.InputStream;

/**
 * Search suggestions provider for Google search engine.
 */
class GoogleSuggestionProvider extends SuggestionProvider {
    private static final String ENCODING = "UTF-8";

    GoogleSuggestionProvider() {
        super(ENCODING);
    }

    @NonNull
    protected String createQueryUrl(@NonNull String query,
                                    @NonNull String language) {
        return "https://suggestqueries.google.com/complete/search?output=firefox&hl="
                + language + "&q=" + query;

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
