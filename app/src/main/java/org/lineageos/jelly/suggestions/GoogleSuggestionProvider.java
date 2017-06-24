package org.lineageos.jelly.suggestions;

import android.support.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * Search suggestions provider for Google search engine.
 */
class GoogleSuggestionProvider extends SuggestionProvider {
    private static final String ENCODING = "UTF-8";
    private static XmlPullParser sXpp;

    GoogleSuggestionProvider() {
        super(ENCODING);
    }

    @NonNull
    protected String createQueryUrl(@NonNull String query,
                                    @NonNull String language) {
        return "https://suggestqueries.google.com/complete/search?output=toolbar&hl="
                + language + "&q=" + query;

    }

    @Override
    protected void parseResults(@NonNull InputStream inputStream,
                                @NonNull ResultCallback callback) throws Exception {
        BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
        XmlPullParser parser = getParser();
        parser.setInput(bufferedInput, ENCODING);
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && "suggestion".equals(parser.getName())) {
                String suggestion = parser.getAttributeValue(null, "data");
                if (!callback.addResult(new SuggestionItem(suggestion))) {
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
