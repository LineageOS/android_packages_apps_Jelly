package org.lineageos.jelly.suggestions;

import android.support.annotation.NonNull;

public class SuggestionItem {
    private String mSuggestion;

    SuggestionItem(@NonNull String suggestion) {
        this.mSuggestion = suggestion;
    }

    @NonNull
    String getSuggestion() {
        return this.mSuggestion;
    }
}
