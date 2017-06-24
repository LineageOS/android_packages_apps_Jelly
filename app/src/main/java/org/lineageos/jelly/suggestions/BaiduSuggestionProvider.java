package org.lineageos.jelly.suggestions;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.lineageos.jelly.utils.FileUtils;

import java.io.InputStream;

/**
 * The search suggestions provider for the Baidu search engine.
 */
class BaiduSuggestionProvider extends SuggestionProvider {
    BaiduSuggestionProvider() {
        super("UTF-8");
    }

    @NonNull
    protected String createQueryUrl(@NonNull String query,
                                    @NonNull String language) {
        // see http://unionsug.baidu.com/su?wd=encodeURIComponent(U)
        // see http://suggestion.baidu.com/s?wd=encodeURIComponent(U)&action=opensearch
        return "http://suggestion.baidu.com/s?wd=" + query + "&action=opensearch";
    }

    @Override
    protected void parseResults(@NonNull InputStream inputStream,
                                @NonNull ResultCallback callback) throws Exception {
        String content = FileUtils.readStringFromStream(inputStream, "GBK");
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
