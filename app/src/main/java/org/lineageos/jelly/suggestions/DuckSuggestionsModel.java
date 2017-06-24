package org.lineageos.jelly.suggestions;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;
import org.lineageos.jelly.utils.FileUtils;

import java.io.InputStream;
import java.util.List;

/**
 * The search suggestions provider for the DuckDuckGo search engine.
 */
final class DuckSuggestionsModel extends BaseSuggestionsModel {

    @NonNull
    private static final String ENCODING = "UTF-8";

    DuckSuggestionsModel() {
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
                                @NonNull List<SuggestionItem> results) throws Exception {
        String content = FileUtils.readStringFromStream(inputStream, ENCODING);
        JSONArray jsonArray = new JSONArray(content);
        int counter = 0;
        for (int n = 0, size = jsonArray.length(); n < size; n++) {
            JSONObject object = jsonArray.getJSONObject(n);
            String suggestion = object.getString("phrase");
            results.add(new SuggestionItem(suggestion));
            counter++;
            if (counter >= MAX_RESULTS) {
                break;
            }
        }
    }

}
