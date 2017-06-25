package org.lineageos.jelly.suggestions;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The base search suggestions API. Provides common
 * fetching and caching functionality for each potential
 * suggestions provider.
 */
public abstract class BaseSuggestionsModel {

    private static final String TAG = "BaseSuggestionsModel";

    static final int MAX_RESULTS = 5;
    @NonNull
    private static final String DEFAULT_LANGUAGE = "en";

    @NonNull
    private final String mEncoding;
    @NonNull
    private final String mLanguage;

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
     * @param results     the list to populate.
     * @throws Exception throw an exception if anything goes wrong.
     */
    protected abstract void parseResults(@NonNull InputStream inputStream,
                                         @NonNull List<SuggestionItem> results) throws Exception;

    BaseSuggestionsModel(@NonNull String encoding) {
        mEncoding = encoding;
        mLanguage = getLanguage();
    }

    /**
     * Retrieves the results for a query.
     *
     * @param rawQuery the raw query to retrieve the results for.
     * @return a list of history items for the query.
     */
    @NonNull
    public final List<SuggestionItem> fetchResults(@NonNull final String rawQuery) {
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
            parseResults(inputStream, filter);
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
            try {
                in = new BufferedInputStream(urlConnection.getInputStream());
            } catch (InterruptedIOException exception) {
                // Ignored, task cancelled
            } finally {
                urlConnection.disconnect();
            }
        } catch (IOException exception) {
            Log.e(TAG, "Problem getting search suggestions", exception);
        }

        return in;
    }

    @NonNull
    private static String getLanguage() {
        String language = Locale.getDefault().getLanguage();
        if (TextUtils.isEmpty(language)) {
            language = DEFAULT_LANGUAGE;
        }
        return language;
    }
}
