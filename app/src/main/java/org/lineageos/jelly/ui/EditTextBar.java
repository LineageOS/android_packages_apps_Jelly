package org.lineageos.jelly.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;

public class EditTextBar extends EditTextExt {
    private String url;
    private String title;

    public EditTextBar(Context context) {
        super(context);
    }

    public EditTextBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction,
                                  Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        if (!gainFocus) {
            setText(title);
        } else {
            setText(url);
        }
    }
}
