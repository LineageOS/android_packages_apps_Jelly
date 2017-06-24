package org.lineageos.jelly.suggestions;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.lineageos.jelly.R;
import org.lineageos.jelly.history.HistoryItem;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Search suggestions provider for Google search engine.
 */
public class GoogleSuggestionsModel extends BaseSuggestionsModel {

    @NonNull
    private static final String ENCODING = "ISO-8859-1";
    @Nullable
    private static XmlPullParser sXpp;
    @NonNull
    private final String mSearchSubtitle;

    public GoogleSuggestionsModel(@NonNull Application application) {
        super(application, ENCODING);
        mSearchSubtitle = application.getString(R.string.suggestion);
    }

    @NonNull
    protected String createQueryUrl(@NonNull String query, @NonNull String language) {
        return "https://suggestqueries.google.com/complete/search?output=toolbar&oe=latin1&hl="
                + language + "&q=" + query;
    }

    @Override
    protected void parseResults(@NonNull InputStream inputStream, @NonNull List<HistoryItem> results) throws Exception {
        BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
        XmlPullParser parser = getParser();
        parser.setInput(bufferedInput, ENCODING);
        int eventType = parser.getEventType();
        int counter = 0;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && "suggestion".equals(parser.getName())) {
                String suggestion = parser.getAttributeValue(null, "data");
                results.add(new HistoryItem(mSearchSubtitle + " \"" + suggestion + '"',
                        suggestion));
                counter++;
                if (counter >= MAX_RESULTS) {
                    break;
                }
            }
            eventType = parser.next();
        }
    }

    @NonNull
    private static synchronized XmlPullParser getParser() throws XmlPullParserException {
        if (sXpp == null) {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            sXpp = factory.newPullParser();
        }
        return sXpp;
    }
}
