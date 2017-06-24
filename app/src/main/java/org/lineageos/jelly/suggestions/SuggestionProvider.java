package org.lineageos.jelly.suggestions;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.lineageos.jelly.utils.FileUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * The base search suggestions API. Provides common
 * fetching and caching functionality for each potential
 * suggestions provider.
 */
abstract class SuggestionProvider {
    private static final String TAG = "SuggestionProvider";
    private static final long INTERVAL_DAY = TimeUnit.DAYS.toSeconds(1);
    private static final String DEFAULT_LANGUAGE = "en";
    @NonNull
    private final String mEncoding;
    @NonNull
    private final String mLanguage;

    SuggestionProvider(@NonNull String encoding) {
        mEncoding = encoding;
        mLanguage = getLanguage();
    }

    @NonNull
    private static String getLanguage() {
        String language = Locale.getDefault().getLanguage();
        if (TextUtils.isEmpty(language)) {
            language = DEFAULT_LANGUAGE;
        }
        return language;
    }

    /**
     * Create a URL for the given query in the given language.
     *
     * @param query    the query that was made.
     * @param language the locale of the user.
     * @return should return a URL that can be fetched using a GET.
     */
    @NonNull
    protected abstract String createQueryUrl(@NonNull String query,
                                             @NonNull String language);

    /**
     * Parse the results of an input stream into a list of {@link SuggestionItem}.
     *
     * @param inputStream the raw input to parse.
     * @param callback    the callback to invoke for each received suggestion
     * @throws Exception throw an exception if anything goes wrong.
     */
    void parseResults(@NonNull InputStream inputStream,
                      @NonNull ResultCallback callback) throws Exception {
        String content = FileUtils.readStringFromStream(inputStream, mEncoding);
        JSONArray respArray = new JSONArray(content);
        JSONArray jsonArray = respArray.getJSONArray(1);
        for (int n = 0, size = jsonArray.length(); n < size; n++) {
            String suggestion = jsonArray.getString(n);
            if (!callback.addResult(new SuggestionItem(suggestion))) {
                break;
            }
        }
    }

    /**
     * Retrieves the results for a query.
     *
     * @param rawQuery the raw query to retrieve the results for.
     * @return a list of history items for the query.
     */
    @NonNull
    final List<SuggestionItem> fetchResults(@NonNull final String rawQuery) {
        List<SuggestionItem> filter = new ArrayList<>(5);

        String query;
        try {
            query = URLEncoder.encode(rawQuery, mEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unable to encode the URL", e);

            return filter;
        }

        InputStream inputStream = downloadSuggestionsForQuery(query, mLanguage);
        if (inputStream == null) {
            // There are no suggestions for this query, return an empty list.
            return filter;
        }
        try {
            parseResults(inputStream, (SuggestionItem suggestion) -> {
                filter.add(suggestion);
                return filter.size() < 5;
            });
        } catch (Exception e) {
            Log.e(TAG, "Unable to parse results", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                // Ignore
            }
        }

        return filter;
    }

    /**
     * This method downloads the search suggestions for the specific query.
     * NOTE: This is a blocking operation, do not fetchResults on the UI thread.
     *
     * @param query the query to get suggestions for
     * @return the cache file containing the suggestions
     */
    @Nullable
    private InputStream downloadSuggestionsForQuery(@NonNull String query,
                                                    @NonNull String language) {
        String queryUrl = createQueryUrl(query, language);
        InputStream in = null;

        try {
            URL url = new URL(queryUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.addRequestProperty("Cache-Control",
                    "max-age=" + INTERVAL_DAY + ", max-stale=" + INTERVAL_DAY);
            urlConnection.addRequestProperty("Accept-Charset", mEncoding);
            try {
                in = new BufferedInputStream(urlConnection.getInputStream());
            } finally {
                urlConnection.disconnect();
            }
        } catch (IOException e) {
            Log.e(TAG, "Problem getting search suggestions", e);
        }

        return in;
    }

    interface ResultCallback {
        boolean addResult(SuggestionItem suggestion);
    }
}
