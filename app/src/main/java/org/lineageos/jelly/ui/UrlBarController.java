package org.lineageos.jelly.ui;

import android.view.View;
import android.widget.ImageView;

public class UrlBarController implements View.OnFocusChangeListener {
    private EditTextExt mEditor;
    private ImageView mSecureIcon;

    private String mUrl;
    private String mTitle;
    private boolean mLoading;
    private boolean mUrlBarHasFocus;

    public UrlBarController(EditTextExt editor, ImageView secureIcon) {
        mEditor = editor;
        mSecureIcon = secureIcon;
        mEditor.setOnFocusChangeListener(this);
    }

    public void onPageLoadStarted(String url) {
        mUrl = url;
        mLoading = true;
        updateUrlBarText();
        updateSecureIconVisibility();
    }

    public void onPageLoadFinished() {
        mLoading = false;
        updateUrlBarText();
        updateSecureIconVisibility();
    }

    public void onTitleReceived(String title) {
        mTitle = title;
        updateUrlBarText();
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        mUrlBarHasFocus = hasFocus;
        updateUrlBarText();
        updateSecureIconVisibility();
        if (hasFocus) {
            mEditor.setSelection(mEditor.length(), mEditor.length());
        }
    }

    private void updateSecureIconVisibility() {
        mSecureIcon.setVisibility(
                !mLoading && !mUrlBarHasFocus && isSecure() ? View.VISIBLE : View.GONE);
    }

    private void updateUrlBarText() {
        mEditor.setText(!mUrlBarHasFocus && !mLoading && mTitle != null ? mTitle : mUrl);
    }

    private boolean isSecure() {
        return mUrl != null && mUrl.startsWith("https");
    }
}
