package org.lineageos.jelly.ui;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;

import org.lineageos.jelly.R;

public class UrlBarController implements View.OnFocusChangeListener {
    private EditTextExt mEditor;
    private ImageView mSecureIcon;

    private String mUrl;
    private String mTitle;
    private boolean mLoading;
    private boolean mUrlBarHasFocus;
    private boolean mIncognito;

    public UrlBarController(Context context, EditTextExt editor,
                            ImageView customIcon, boolean incognito) {
        mEditor = editor;
        mSecureIcon = customIcon;
        mIncognito = incognito;
        mEditor.setOnFocusChangeListener(this);

        if (mIncognito) {
            customIcon.setImageDrawable(
                    ContextCompat.getDrawable(context, R.drawable.ic_incognito));
            customIcon.setVisibility(View.VISIBLE);
        } else {
            customIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_lock));
        }
    }

    public void onPageLoadStarted(String url) {
        mUrl = url;
        mLoading = true;
        updateUrlBarText();
        updateCustomIconVisibility();
    }

    public void onPageLoadFinished() {
        mLoading = false;
        updateUrlBarText();
        updateCustomIconVisibility();
    }

    public void onTitleReceived(String title) {
        mTitle = title;
        updateUrlBarText();
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        mUrlBarHasFocus = hasFocus;
        updateUrlBarText();
        updateCustomIconVisibility();

        if (hasFocus) {
            mEditor.selectAll();
        }
    }

    private void updateCustomIconVisibility() {
        if (mIncognito) {
            updateIncognitoIconVisibility();
        } else {
            updateSecureIconVisibility();
        }
    }

    private void updateIncognitoIconVisibility() {
        mSecureIcon.setVisibility(!mUrlBarHasFocus ? View.VISIBLE : View.GONE);
    }

    private void updateSecureIconVisibility() {
        mSecureIcon.setVisibility(
                !mLoading && !mUrlBarHasFocus && isSecure() ? View.VISIBLE : View.GONE);
    }

    private void updateUrlBarText() {
        mEditor.setText(!mIncognito && !mUrlBarHasFocus && !mLoading && mTitle != null && isSecure()
                ? mTitle : mUrl);
    }

    private boolean isSecure() {
        return mUrl != null && mUrl.startsWith("https");
    }
}
