package org.lineageos.jelly.history;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class HistoryItem {
    private String mUrl;
    private String mTitle;

    public HistoryItem(@NonNull String url, @NonNull String title) {
        this.mUrl = url;
        this.mTitle = title;
    }

    @NonNull
    public String getUrl() {
        return this.mUrl;
    }

    public void setUrl(@Nullable String url) {
        this.mUrl = (url == null) ? "" : url;
    }

    @NonNull
    public String getTitle() {
        return this.mTitle;
    }

    public void setTitle(@Nullable String title) {
        this.mTitle = (title == null) ? "" : title;
    }
}
