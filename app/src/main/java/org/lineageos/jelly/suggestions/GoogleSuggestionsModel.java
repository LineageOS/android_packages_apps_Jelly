package org.lineageos.jelly.suggestions;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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

    public GoogleSuggestionsModel(@NonNull Application application) {
        super(application, ENCODING);
    }

    @NonNull
    protected String createQueryUrl(@NonNull String query,
                                    @NonNull String language) {
        return "https://suggestqueries.google.com/complete/search?output=toolbar&oe=latin1&hl="
                + language + "&q=" + query;
    }

    @Override
    protected void parseResults(@NonNull InputStream inputStream,
                                @NonNull List<SuggestionItem> results) throws Exception {
        BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
        XmlPullParser parser = getParser();
        parser.setInput(bufferedInput, ENCODING);
        int eventType = parser.getEventType();
        int counter = 0;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && "suggestion".equals(parser.getName())) {
                String suggestion = parser.getAttributeValue(null, "data");
                results.add(new SuggestionItem(suggestion));
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
